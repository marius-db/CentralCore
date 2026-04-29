package com.centralcore.modules.trafficmodule;

import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.function.Consumer;

//cliente websocket para comunicarse con la simulacion python
//reconexion automatica cada 3s si se pierde la conexion
public class SimConnection {

    private static final int RECONNECT_DELAY_SEC = 3;
    private static final String DEFAULT_URL = "ws://localhost:8765";

    private HttpClient httpClient;
    private WebSocket webSocket;
    private String url = DEFAULT_URL;

    private volatile boolean intentionalClose = false;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sim-conn-scheduler");
        t.setDaemon(true);
        return t;
    });

    private final StringBuilder msgBuffer = new StringBuilder();

    private Consumer<String> onMapReceived;
    private Consumer<String> onStateReceived;
    private Runnable onEvDone;
    private Runnable onConnected;
    private Runnable onDisconnected;

    public void setOnMapReceived(Consumer<String> cb) {
        this.onMapReceived = cb;
    }

    public void setOnStateReceived(Consumer<String> cb) {
        this.onStateReceived = cb;
    }

    public void setOnEvDone(Runnable cb) {
        this.onEvDone = cb;
    }

    public void setOnConnected(Runnable cb) {
        this.onConnected = cb;
    }

    public void setOnDisconnected(Runnable cb) {
        this.onDisconnected = cb;
    }

    public void connect(String serverUrl) {
        this.url = serverUrl;
        this.intentionalClose = false;
        doConnect();
    }

    public void connect() {
        connect(DEFAULT_URL);
    }

    private void doConnect() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .executor(Executors.newSingleThreadExecutor(r -> {
                        Thread t = new Thread(r, "sim-ws-io");
                        t.setDaemon(true);
                        return t;
                    }))
                    .build();
        }
        try {
            httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create(url), new Listener())
                    .whenComplete((ws, err) -> {
                        if (err != null) {
                            System.err.println("fallo la conexion con la sim: " + err.getMessage());
                            // notificar desconexion para que el boton quede en estado correcto
                            Platform.runLater(() -> {
                                if (onDisconnected != null) onDisconnected.run();
                            });
                            scheduleReconnect();
                        } else {
                            webSocket = ws;
                            Platform.runLater(() -> {
                                if (onConnected != null) onConnected.run();
                            });
                        }
                    });
        } catch (Exception e) {
            System.err.println("error al iniciar conexion: " + e.getMessage());
            Platform.runLater(() -> {
                if (onDisconnected != null) onDisconnected.run();
            });
            scheduleReconnect();
        }
    }

    public void disconnect() {
        intentionalClose = true;
        if (webSocket != null && !webSocket.isOutputClosed()) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "modulo cerrado");
        }
    }

    public boolean isConnected() {
        return webSocket != null && !webSocket.isOutputClosed() && !webSocket.isInputClosed();
    }

    //enviar ruta por id de nodo
    public void sendRoute(String fromNodeId, String toNodeId) {
        sendJson("{\"type\":\"route\",\"from\":\"" + fromNodeId + "\",\"to\":\"" + toNodeId + "\"}");
    }

    //enviar ruta por coordenadas de mapa (el simulador hace snap al nodo mas cercano)
    public void sendRouteByCoords(double fromX, double fromY, double toX, double toY) {
        sendJson("{\"type\":\"route\","
                + "\"from_xy\":[" + fromX + "," + fromY + "],"
                + "\"to_xy\":[" + toX + "," + toY + "]}");
    }

    public void cancelRoute() {
        sendJson("{\"type\":\"cancel_route\"}");
    }

    //override de semaforo individual
    public void overrideLight(String lightId, String state, int durationSecs) {
        sendJson("{\"type\":\"override_light\",\"light_id\":\"" + lightId
                + "\",\"state\":\"" + state
                + "\",\"dur\":" + durationSecs + "}");
    }

    private void sendJson(String json) {
        if (!isConnected()) return;
        webSocket.sendText(json, true).exceptionally(e -> {
            System.err.println("error al enviar mensaje: " + e.getMessage());
            return null;
        });
    }

    private void scheduleReconnect() {
        if (intentionalClose) return;
        System.out.println("reintentando conexion en " + RECONNECT_DELAY_SEC + "s...");
        scheduler.schedule(this::doConnect, RECONNECT_DELAY_SEC, TimeUnit.SECONDS);
    }

    public void shutdown() {
        intentionalClose = true;
        disconnect();
        scheduler.shutdownNow();
    }

    private class Listener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket ws) {
            System.out.println("websocket conectado a simulacion");
            msgBuffer.setLength(0);
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            msgBuffer.append(data);
            if (last) {
                String msg = msgBuffer.toString();
                msgBuffer.setLength(0);
                handleMessage(msg);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            System.out.println("websocket cerrado: " + statusCode + " " + reason);
            Platform.runLater(() -> {
                if (onDisconnected != null) onDisconnected.run();
            });
            if (!intentionalClose) scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            System.err.println("error en websocket: " + error.getMessage());
            Platform.runLater(() -> {
                if (onDisconnected != null) onDisconnected.run();
            });
            if (!intentionalClose) scheduleReconnect();
        }
    }

    private void handleMessage(String json) {
        String type = extractStringField(json, "type");
        if (type == null) return;
        switch (type) {
            case "map" -> Platform.runLater(() -> {
                if (onMapReceived != null) onMapReceived.accept(json);
            });
            case "state" -> Platform.runLater(() -> {
                if (onStateReceived != null) onStateReceived.accept(json);
            });
            case "ev_done" -> Platform.runLater(() -> {
                if (onEvDone != null) onEvDone.run();
            });
            default -> System.out.println("mensaje desconocido del sim: " + type);
        }
    }

    private String extractStringField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }
}