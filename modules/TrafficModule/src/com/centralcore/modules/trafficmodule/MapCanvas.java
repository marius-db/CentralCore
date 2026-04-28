package com.centralcore.modules.trafficmodule;

import com.centralcore.modules.trafficmodule.model.*;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

//canvas personalizado para renderizar el mapa de trafico
//soporta pan (arrastrar), zoom (scroll), clic derecho para menu contextual
public class MapCanvas extends Canvas {

    //paleta de colores del mapa (tema oscuro, consistente con la app)
    private static final Color BG            = Color.web("#0b0d12");
    private static final Color ROAD_MAIN     = Color.web("#232840");
    private static final Color ROAD_SIDE     = Color.web("#191c2a");
    private static final Color ROAD_BORDER   = Color.web("#2e3450");
    private static final Color LANE_LINE     = Color.web("#3a4060");
    private static final Color NODE_FILL     = Color.web("#1e2236");
    private static final Color NODE_STROKE   = Color.web("#2b7fff").deriveColor(0, 1, 1, 0.5);
    private static final Color CAR_COLOR     = Color.web("#5a9fff");
    private static final Color EV_COLOR      = Color.web("#00e5ff");
    private static final Color ROUTE_COLOR   = Color.web("#2b7fff").deriveColor(0, 1, 1, 0.6);
    private static final Color POINT_A       = Color.web("#22c55e");
    private static final Color POINT_B       = Color.web("#ef4444");
    private static final Color INCIDENT_OPEN = Color.web("#f59e0b");
    private static final Color INCIDENT_CRI  = Color.web("#ef4444");
    private static final Color INCIDENT_MIN  = Color.web("#22c55e");
    private static final Color TEXT_MUTED    = Color.web("#555d75");
    private static final Color LIGHT_GREEN   = Color.web("#22c55e");
    private static final Color LIGHT_YELLOW  = Color.web("#f59e0b");
    private static final Color LIGHT_RED     = Color.web("#ef4444");

    //estado de la simulacion y datos de incidentes
    private SimState        simState   = null;
    private List<Incident>  incidents  = new ArrayList<>();
    private String          pointAId   = null;
    private String          pointBId   = null;
    private List<String>    routeNodes = new ArrayList<>();

    //transformacion del canvas: pan + zoom
    private double offsetX = 50;
    private double offsetY = 50;
    private double scale   = 1.0;
    private double dragStartX;
    private double dragStartY;
    private double dragOffsetX;
    private double dragOffsetY;

    //modo colocacion de incidente (clic izquierdo planta un pin)
    private boolean placingIncident = false;

    //incidente seleccionado (para highlight)
    private Incident selectedIncident = null;

    //efecto glow para el vehiculo de emergencia
    private final DropShadow evGlow;

    //callbacks
    private BiConsumer<Double, Double>    onIncidentPlaced;
    private Consumer<TrafficNode>         onNodeRightClicked;
    private Consumer<double[]>            onMapRightClicked;
    private Consumer<Incident>            onIncidentSelected;

    private volatile boolean dirty = true;

    public MapCanvas() {
        evGlow = new DropShadow(12, EV_COLOR);

        setupInput();
        startRenderLoop();

        //redibujar al cambiar el tamaño
        widthProperty().addListener((o, v, n) -> dirty = true);
        heightProperty().addListener((o, v, n) -> dirty = true);
    }

    @Override public boolean isResizable()              { return true; }
    @Override public double prefWidth(double h)         { return getWidth(); }
    @Override public double prefHeight(double w)        { return getHeight(); }

    //api publica

    public void setState(SimState state)                { this.simState = state; dirty = true; }
    public void setIncidents(List<Incident> list)       { this.incidents = list; dirty = true; }
    public void setPointA(String nodeId)                { this.pointAId = nodeId; dirty = true; }
    public void setPointB(String nodeId)                { this.pointBId = nodeId; dirty = true; }
    public void setRouteNodes(List<String> route)       { this.routeNodes = route; dirty = true; }
    public void setPlacingIncident(boolean placing)     { this.placingIncident = placing; dirty = true; }
    public boolean isPlacingIncident()                  { return placingIncident; }
    public double getScaleValue()                       { return scale; }
    public double getOffsetX()                          { return offsetX; }
    public double getOffsetY()                          { return offsetY; }
    public void setSelectedIncident(Incident i)         { this.selectedIncident = i; dirty = true; }

    public void setOnIncidentPlaced(BiConsumer<Double, Double> cb)  { this.onIncidentPlaced   = cb; }
    public void setOnNodeRightClicked(Consumer<TrafficNode> cb)     { this.onNodeRightClicked = cb; }
    public void setOnMapRightClicked(Consumer<double[]> cb)         { this.onMapRightClicked  = cb; }
    public void setOnIncidentSelected(Consumer<Incident> cb)        { this.onIncidentSelected = cb; }

    //convierte coordenadas del canvas a coordenadas del simulador
    public double[] canvasToSim(double cx, double cy) {
        return new double[]{ (cx - offsetX) / scale, (cy - offsetY) / scale };
    }

    //encuentra el nodo mas cercano a las coordenadas del simulador dado un umbral
    public TrafficNode findNearestNode(double simX, double simY, double threshold) {
        if (simState == null) return null;
        TrafficNode nearest  = null;
        double      minDist = Double.MAX_VALUE;
        for (TrafficNode n : simState.getNodes()) {
            double d = Math.hypot(n.getX() - simX, n.getY() - simY);
            if (d < minDist) { minDist = d; nearest = n; }
        }
        return (minDist <= threshold) ? nearest : null;
    }

    //encuentra el incidente cuyo pin esta cerca de las coordenadas del canvas
    private Incident findNearestIncident(double cx, double cy) {
        if (incidents.isEmpty()) return null;
        for (Incident i : incidents) {
            double px = i.getMapX() * scale + offsetX;
            double py = i.getMapY() * scale + offsetY;
            if (Math.hypot(px - cx, py - cy) < 14) return i;
        }
        return null;
    }

    //configuracion de eventos de entrada

    private void setupInput() {
        //inicio del drag
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX  = e.getX();
                dragStartY  = e.getY();
                dragOffsetX = offsetX;
                dragOffsetY = offsetY;
            }
        });

        //pan con drag
        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                offsetX = dragOffsetX + (e.getX() - dragStartX);
                offsetY = dragOffsetY + (e.getY() - dragStartY);
                dirty   = true;
            }
        });

        //zoom centrado en el cursor
        setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.1 : 0.91;
            double newScale = Math.max(0.3, Math.min(6.0, scale * factor));
            //ajustar offset para que el zoom sea relativo al cursor
            double mx = e.getX();
            double my = e.getY();
            offsetX = mx - (mx - offsetX) * (newScale / scale);
            offsetY = my - (my - offsetY) * (newScale / scale);
            scale   = newScale;
            dirty   = true;
        });

        //clic izquierdo: colocar incidente o seleccionar pin
        setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;

            Incident near = findNearestIncident(e.getX(), e.getY());
            if (near != null) {
                selectedIncident = near;
                dirty = true;
                if (onIncidentSelected != null) onIncidentSelected.accept(near);
                return;
            }

            if (placingIncident && onIncidentPlaced != null) {
                double[] sim = canvasToSim(e.getX(), e.getY());
                onIncidentPlaced.accept(sim[0], sim[1]);
            }
        });

        //clic derecho: notificar al controlador con coordenadas sim
        setOnContextMenuRequested(e -> {
            double[] sim = canvasToSim(e.getX(), e.getY());
            TrafficNode node = findNearestNode(sim[0], sim[1], 40);
            if (node != null && onNodeRightClicked != null) {
                onNodeRightClicked.accept(node);
            } else if (onMapRightClicked != null) {
                onMapRightClicked.accept(sim);
            }
        });
    }

    //loop de animacion a ~60fps, solo redibuja si dirty=true

    private void startRenderLoop() {
        new AnimationTimer() {
            @Override public void handle(long now) {
                if (dirty) { render(); dirty = false; }
            }
        }.start();
    }

    //renderizado principal

    private void render() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        //fondo
        gc.setFill(BG);
        gc.fillRect(0, 0, w, h);

        if (simState == null || simState.getNodes().isEmpty()) {
            drawNoConnectionMessage(gc, w, h);
            return;
        }

        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);

        drawEdges(gc);
        drawRouteOverlay(gc);
        drawNodes(gc);
        drawTrafficLights(gc);
        drawCars(gc);
        drawEmergencyVehicle(gc);
        drawIncidentPins(gc);
        drawPointMarkers(gc);

        gc.restore();

        //indicador de modo colocacion (fuera del transform, en esquina inferior)
        if (placingIncident) {
            gc.setFill(Color.web("#f59e0b", 0.85));
            gc.fillRoundRect(12, h - 36, 260, 26, 6, 6);
            gc.setFill(Color.web("#0d0f14"));
            gc.setFont(Font.font(12));
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText("  📍 Haz clic en el mapa para colocar el incidente", 16, h - 18);
        }
    }

    //dibuja mensaje cuando no hay conexion

    private void drawNoConnectionMessage(GraphicsContext gc, double w, double h) {
        gc.setFill(TEXT_MUTED);
        gc.setFont(Font.font(13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Sin conexión con la simulación", w / 2, h / 2 - 10);
        gc.setFont(Font.font(11));
        gc.fillText("Conecta desde el panel lateral para cargar el mapa", w / 2, h / 2 + 12);
    }

    //dibuja calles (aristas del grafo)

    private void drawEdges(GraphicsContext gc) {
        for (TrafficEdge edge : simState.getEdges()) {
            TrafficNode a = simState.findNode(edge.getFrom());
            TrafficNode b = simState.findNode(edge.getTo());
            if (a == null || b == null) continue;

            double roadWidth = edge.isMain() ? 22 : 14;

            //cuerpo de la calle
            gc.setStroke(edge.isMain() ? ROAD_MAIN : ROAD_SIDE);
            gc.setLineWidth(roadWidth);
            gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());

            //borde de la calle
            gc.setStroke(ROAD_BORDER);
            gc.setLineWidth(roadWidth + 2);
            gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());

            //repasar el cuerpo encima del borde
            gc.setStroke(edge.isMain() ? ROAD_MAIN : ROAD_SIDE);
            gc.setLineWidth(roadWidth);
            gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());

            //lineas de carril punteadas en calles principales
            if (edge.isMain() && edge.getLanes() > 1) {
                drawLaneLines(gc, a.getX(), a.getY(), b.getX(), b.getY(), edge.getLanes());
            }
        }
    }

    //dibuja lineas discontinuas de carril entre dos puntos

    private void drawLaneLines(GraphicsContext gc, double x1, double y1, double x2, double y2, int lanes) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len < 1) return;

        double nx = -dy / len;
        double ny =  dx / len;

        gc.setStroke(LANE_LINE);
        gc.setLineWidth(0.6);
        gc.setLineDashes(8, 6);

        for (int i = 1; i < lanes; i++) {
            double offset = (i - lanes / 2.0) * 5.5;
            gc.strokeLine(
                    x1 + nx * offset, y1 + ny * offset,
                    x2 + nx * offset, y2 + ny * offset
            );
        }
        gc.setLineDashes(null);
    }

    //highlight de la ruta activa del vehiculo de emergencia

    private void drawRouteOverlay(GraphicsContext gc) {
        if (routeNodes.size() < 2) return;
        gc.setStroke(ROUTE_COLOR);
        gc.setLineWidth(8);
        gc.setLineDashes(null);

        for (int i = 0; i < routeNodes.size() - 1; i++) {
            TrafficNode a = simState.findNode(routeNodes.get(i));
            TrafficNode b = simState.findNode(routeNodes.get(i + 1));
            if (a == null || b == null) continue;
            gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
        }
    }

    //dibuja los nodos (intersecciones)

    private void drawNodes(GraphicsContext gc) {
        for (TrafficNode node : simState.getNodes()) {
            double r = node.isMain() ? 10 : 7;
            gc.setFill(NODE_FILL);
            gc.fillOval(node.getX() - r, node.getY() - r, r * 2, r * 2);
            gc.setStroke(NODE_STROKE);
            gc.setLineWidth(1);
            gc.strokeOval(node.getX() - r, node.getY() - r, r * 2, r * 2);
        }
    }

    //dibuja los semaforos como pequenos circulos coloreados junto al nodo

    private void drawTrafficLights(GraphicsContext gc) {
        for (TrafficLight light : simState.getLights()) {
            TrafficNode node = simState.findNode(light.getNodeId());
            if (node == null) continue;

            Color color = switch (light.getState()) {
                case "ns_green", "ew_green" -> LIGHT_GREEN;
                case "yellow"               -> LIGHT_YELLOW;
                default                     -> LIGHT_RED;
            };

            //offset del circulo del semaforo respecto al centro del nodo
            double lx = node.getX() + 13;
            double ly = node.getY() - 13;

            gc.setFill(Color.web("#12141e"));
            gc.fillOval(lx - 5, ly - 5, 10, 10);
            gc.setFill(color);
            gc.fillOval(lx - 3.5, ly - 3.5, 7, 7);
        }
    }

    //dibuja los coches

    private void drawCars(GraphicsContext gc) {
        gc.setFill(CAR_COLOR);
        for (SimCar car : simState.getCars()) {
            //calcula orientacion del coche segun la arista en que viaja
            TrafficNode a = simState.findNode(car.getNodeA());
            TrafficNode b = simState.findNode(car.getNodeB());
            if (a == null || b == null) {
                gc.fillRect(car.getX() - 4, car.getY() - 2, 8, 4);
                continue;
            }
            double dx = b.getX() - a.getX();
            double dy = b.getY() - a.getY();
            double angle = Math.atan2(dy, dx);
            drawRotatedRect(gc, car.getX(), car.getY(), 9, 4, angle, CAR_COLOR, car.getLane());
        }
    }

    //dibuja el vehiculo de emergencia con efecto glow

    private void drawEmergencyVehicle(GraphicsContext gc) {
        if (!simState.isEvActive()) return;
        gc.setEffect(evGlow);
        gc.setFill(EV_COLOR);
        gc.fillRoundRect(simState.getEvX() - 7, simState.getEvY() - 4, 14, 8, 3, 3);
        gc.setEffect(null);
        //cruz pequeña encima del vehiculo
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeLine(simState.getEvX() - 3, simState.getEvY() - 9, simState.getEvX() + 3, simState.getEvY() - 9);
        gc.strokeLine(simState.getEvX(),     simState.getEvY() - 12, simState.getEvX(),     simState.getEvY() - 6);
    }

    //rectangulo rotado para los coches (desplazado por carril)

    private void drawRotatedRect(GraphicsContext gc, double cx, double cy,
                                 double w, double h, double angle, Color color, int lane) {
        double nx = -Math.sin(angle);
        double ny =  Math.cos(angle);
        double laneOffset = (lane - 1.5) * 4.5;

        gc.save();
        gc.translate(cx + nx * laneOffset, cy + ny * laneOffset);
        gc.rotate(Math.toDegrees(angle));
        gc.setFill(color);
        gc.fillRoundRect(-w / 2, -h / 2, w, h, 2, 2);
        gc.restore();
    }

    //dibuja pines de incidentes activos

    private void drawIncidentPins(GraphicsContext gc) {
        for (Incident i : incidents) {
            Color pinColor = switch (i.getEstado().toLowerCase()) {
                case "crítico", "critico" -> INCIDENT_CRI;
                case "resuelto"           -> INCIDENT_MIN;
                default                   -> INCIDENT_OPEN;
            };

            boolean selected = (selectedIncident != null && selectedIncident.getId() == i.getId());
            double  r        = selected ? 9 : 7;

            gc.setFill(pinColor);
            gc.fillOval(i.getMapX() - r, i.getMapY() - r - 12, r * 2, r * 2);
            //triangulo del pin
            double[] px = { i.getMapX() - 5, i.getMapX() + 5, i.getMapX() };
            double[] py = { i.getMapY() - 12, i.getMapY() - 12, i.getMapY() - 2 };
            gc.fillPolygon(px, py, 3);

            if (selected) {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.5);
                gc.strokeOval(i.getMapX() - r, i.getMapY() - r - 12, r * 2, r * 2);
            }
        }
    }

    //marcadores de punto A y B de la ruta de emergencia

    private void drawPointMarkers(GraphicsContext gc) {
        if (pointAId != null) drawPointMarker(gc, pointAId, POINT_A, "A");
        if (pointBId != null) drawPointMarker(gc, pointBId, POINT_B, "B");
    }

    private void drawPointMarker(GraphicsContext gc, String nodeId, Color color, String label) {
        TrafficNode n = simState.findNode(nodeId);
        if (n == null) return;

        gc.setFill(color.deriveColor(0, 1, 1, 0.25));
        gc.fillOval(n.getX() - 14, n.getY() - 14, 28, 28);
        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.strokeOval(n.getX() - 14, n.getY() - 14, 28, 28);

        gc.setFill(color);
        gc.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(label, n.getX(), n.getY() + 4);
    }
}
