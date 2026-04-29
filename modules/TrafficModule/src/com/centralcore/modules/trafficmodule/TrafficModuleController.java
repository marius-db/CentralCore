package com.centralcore.modules.trafficmodule;

import com.centralcore.modules.trafficmodule.model.*;
import com.google.gson.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrafficModuleController {

    //region fxml
    @FXML private StackPane mapPane;
    @FXML private Label lblConStatus;
    @FXML private Circle dotConStatus;
    @FXML private Button btnConectar;
    @FXML private TextField fieldWsUrl;

    //tab semaforos
    @FXML private ListView<TrafficLight> listSemaforos;

    //tab trafico
    @FXML private ListView<TrafficEdge> listTrafico;

    //tab emergencia
    @FXML private Label lblPuntoA;
    @FXML private Label lblPuntoB;
    @FXML private Button btnEnviarRuta;
    @FXML private Button btnCancelarRuta;
    @FXML private Label lblEstadoRuta;
    @FXML private Button btnMarcarA;
    @FXML private Button btnMarcarB;

    //tab incidentes
    @FXML private ComboBox<String> comboTipoIncidente;
    @FXML private TextField fieldDescIncidente;
    @FXML private ComboBox<String> comboEstadoIncidente;
    @FXML private Button btnMarcarMapa;
    @FXML private ListView<Incident> listIncidentes;
    @FXML private Button btnActualizarInc;
    @FXML private Button btnCerrarInc;
    @FXML private TextField fieldNotaUpdate;
    @FXML private ComboBox<String> comboEstadoUpdate;

    //tab historial
    @FXML private ListView<Incident> listHistorial;
    @FXML private ListView<IncidentUpdate> listUpdates;

    private final SimState simState = new SimState();
    private final SimConnection connection = new SimConnection();
    private final TrafficDAO dao = new TrafficDAO();
    private final Gson gson = new Gson();
    private MapCanvas mapCanvas;

    //true solo cuando onConnected ha disparado; no cuando se esta intentando conectar
    private boolean connected = false;

    private final ObservableList<TrafficLight> lightItems = FXCollections.observableArrayList();
    private final ObservableList<TrafficEdge> edgeItems = FXCollections.observableArrayList();
    private final ObservableList<Incident> incidentItems = FXCollections.observableArrayList();
    private final ObservableList<Incident> historialItems = FXCollections.observableArrayList();

    //distancia en unidades sim para activar override de semaforo ante el VE
    private static final double EV_LIGHT_TRIGGER_DIST = 80.0;

    //evita enviar el mismo override multiples veces al mismo semaforo por tick
    private final Set<String> overriddenLights = new HashSet<>();

    //lookup rapido arista por id para el parseState
    private final java.util.Map<String, TrafficEdge> edgeById = new java.util.HashMap<>();
    //lookup rapido semaforo por id
    private final java.util.Map<String, TrafficLight> lightById = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        setupMapCanvas();
        setupListCells();
        setupCombos();
        setupConnectionCallbacks();
        setupIncidentActions();
        setupEmergencyActions();
        setupHistorialSelection();
        refreshIncidents();
    }

    //region setup
    private void setupMapCanvas() {
        mapCanvas = new MapCanvas();
        mapCanvas.widthProperty().bind(mapPane.widthProperty());
        mapCanvas.heightProperty().bind(mapPane.heightProperty());
        mapPane.getChildren().add(0, mapCanvas);

        //clic derecho en nodo: menu contextual
        mapCanvas.setOnNodeRightClicked(node -> {
            ContextMenu ctx = buildNodeContextMenu(node);
            javafx.geometry.Point2D screen = mapCanvas.localToScreen(
                    node.getX() * mapCanvas.getScaleValue() + mapCanvas.getOffsetX(),
                    node.getY() * mapCanvas.getScaleValue() + mapCanvas.getOffsetY());
            if (screen != null) ctx.show(mapCanvas, screen.getX(), screen.getY());
        });

        //clic derecho en calle (no sobre un nodo): mismo menu con coordenadas sim
        mapCanvas.setOnRoadRightClicked(coords -> {
            ContextMenu ctx = buildRoadContextMenu(coords[0], coords[1]);
            javafx.geometry.Point2D screen = mapCanvas.localToScreen(
                    coords[0] * mapCanvas.getScaleValue() + mapCanvas.getOffsetX(),
                    coords[1] * mapCanvas.getScaleValue() + mapCanvas.getOffsetY());
            if (screen != null) ctx.show(mapCanvas, screen.getX(), screen.getY());
        });

        //colocar incidente al hacer clic mientras se esta en modo colocacion
        mapCanvas.setOnIncidentPlaced((simX, simY) -> {
            btnMarcarMapa.setText("Marcar en mapa");
            createIncidentAt(simX, simY);
        });

        //punto A colocado (por clic en calle o nodo)
        mapCanvas.setOnPointAPlaced(coords -> {
            btnMarcarA.setText("Marcar A");
            String nodeId = mapCanvas.getPointAId();
            lblPuntoA.setText(nodeId != null ? nodeId : String.format("(%.0f, %.0f)", coords[0], coords[1]));
            refreshEnviarBtn();
        });

        //punto B colocado
        mapCanvas.setOnPointBPlaced(coords -> {
            btnMarcarB.setText("Marcar B");
            String nodeId = mapCanvas.getPointBId();
            lblPuntoB.setText(nodeId != null ? nodeId : String.format("(%.0f, %.0f)", coords[0], coords[1]));
            refreshEnviarBtn();
        });

        //al seleccionar un pin de incidente en el mapa
        mapCanvas.setOnIncidentSelected(inc -> listIncidentes.getSelectionModel().select(inc));
    }

    private ContextMenu buildNodeContextMenu(TrafficNode node) {
        ContextMenu ctx = new ContextMenu();

        MenuItem itemA = new MenuItem("Marcar como Punto A");
        itemA.setOnAction(e -> {
            mapCanvas.setPointA(node.getId(), node.getX(), node.getY());
            lblPuntoA.setText(node.getId());
            refreshEnviarBtn();
        });

        MenuItem itemB = new MenuItem("Marcar como Punto B");
        itemB.setOnAction(e -> {
            mapCanvas.setPointB(node.getId(), node.getX(), node.getY());
            lblPuntoB.setText(node.getId());
            refreshEnviarBtn();
        });

        MenuItem itemInc = new MenuItem("Añadir incidente aquí");
        itemInc.setOnAction(e -> createIncidentAt(node.getX(), node.getY()));

        ctx.getItems().addAll(itemA, itemB, new SeparatorMenuItem(), itemInc);
        return ctx;
    }

    //menu contextual para clic derecho en una calle (sin nodo exacto)
    private ContextMenu buildRoadContextMenu(double simX, double simY) {
        ContextMenu ctx = new ContextMenu();
        MenuItem itemA = new MenuItem("Marcar como Punto A");
        itemA.setOnAction(e -> {
            mapCanvas.setPointA(null, simX, simY);
            lblPuntoA.setText(String.format("(%.0f, %.0f)", simX, simY));
            refreshEnviarBtn();
        });
        MenuItem itemB = new MenuItem("Marcar como Punto B");
        itemB.setOnAction(e -> {
            mapCanvas.setPointB(null, simX, simY);
            lblPuntoB.setText(String.format("(%.0f, %.0f)", simX, simY));
            refreshEnviarBtn();
        });
        MenuItem itemInc = new MenuItem("\u00c1\u00f1adir incidente aqu\u00ed");
        itemInc.setOnAction(e -> createIncidentAt(simX, simY));
        ctx.getItems().addAll(itemA, itemB, new SeparatorMenuItem(), itemInc);
        return ctx;
    }

    private void setupListCells() {
        listSemaforos.setItems(lightItems);
        listSemaforos.setCellFactory(lv -> new ListCell<>() {
            private final Circle dot = new Circle(5);
            private final Label lblId = new Label();
            private final Label lblDir = new Label();
            private final Button btnOvr = new Button("Override");
            private final HBox row = new HBox(8, dot, lblId, lblDir, new Pane(), btnOvr);

            {
                HBox.setHgrow(row.getChildren().get(3), Priority.ALWAYS);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 8, 4, 8));
                btnOvr.getStyleClass().add("btn-ghost");
                btnOvr.setOnAction(e -> {
                    TrafficLight l = getItem();
                    if (l != null) showOverrideDialog(l);
                });
            }

            @Override
            protected void updateItem(TrafficLight l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) {
                    setGraphic(null);
                    return;
                }
                Color c = lightStateColor(l.getState());
                dot.setFill(c);
                dot.setEffect(new javafx.scene.effect.DropShadow(6, c));
                String stateName = stateDisplayName(l.getState());
                lblId.setText(l.getId() + "  " + stateName + "  " + l.getTimer() + "s");
                lblId.getStyleClass().setAll("tm-list-label");
                lblDir.setText(dirDisplayName(l.getDir()));
                lblDir.getStyleClass().setAll("tm-hint");
                setGraphic(row);
            }
        });

        listTrafico.setItems(edgeItems);
        listTrafico.setCellFactory(lv -> new ListCell<>() {
            private final Label lblName = new Label();
            private final Rectangle densBar = new Rectangle(0, 6);
            private final Label lblDens = new Label();
            private final HBox row = new HBox(8, lblName, new Pane(), densBar, lblDens);

            {
                HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new Insets(6, 8, 6, 8));
                densBar.setArcWidth(3);
                densBar.setArcHeight(3);
            }

            @Override
            protected void updateItem(TrafficEdge e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) {
                    setGraphic(null);
                    return;
                }
                double d = e.getDensity();
                densBar.setWidth(Math.max(4, d * 80));
                Color barColor = d < 0.4 ? Color.web("#22c55e")
                        : d < 0.7 ? Color.web("#f59e0b")
                        : Color.web("#ef4444");
                densBar.setFill(barColor);
                lblName.setText(e.getName() != null ? e.getName() : e.getId());
                lblName.getStyleClass().setAll("tm-list-label");
                lblDens.setText(String.format("%.0f%%", d * 100));
                lblDens.getStyleClass().setAll("tm-density-label");
                setGraphic(row);
            }
        });

        listIncidentes.setItems(incidentItems);
        listIncidentes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Incident i, boolean empty) {
                super.updateItem(i, empty);
                if (empty || i == null) {
                    setText(null);
                    return;
                }
                setText(i.toString());
                getStyleClass().removeAll("inc-abierto", "inc-critico", "inc-resuelto");
                switch (i.getEstado().toLowerCase()) {
                    case "crítico", "critico" -> getStyleClass().add("inc-critico");
                    case "resuelto" -> getStyleClass().add("inc-resuelto");
                    default -> getStyleClass().add("inc-abierto");
                }
            }
        });

        listHistorial.setItems(historialItems);
        listHistorial.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Incident i, boolean empty) {
                super.updateItem(i, empty);
                if (empty || i == null) {
                    setText(null);
                    return;
                }
                String closed = i.getClosedAt() != null
                        ? i.getClosedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                        : "—";
                setText("[" + closed + "] " + i.getTipo() + " — " + i.getDescripcion());
            }
        });
    }

    private void setupCombos() {
        comboTipoIncidente.setItems(FXCollections.observableArrayList(
                "Accidente", "Corte de vía", "Obras", "Semáforo averiado",
                "Vehículo abandonado", "Desbordamiento", "Incendio", "Otro"
        ));
        comboTipoIncidente.getSelectionModel().selectFirst();

        comboEstadoIncidente.setItems(FXCollections.observableArrayList(
                "Abierto", "En curso", "Crítico", "Resuelto"
        ));
        comboEstadoIncidente.getSelectionModel().selectFirst();

        comboEstadoUpdate.setItems(FXCollections.observableArrayList(
                "Abierto", "En curso", "Crítico", "Resuelto"
        ));
        comboEstadoUpdate.getSelectionModel().selectFirst();
    }

    private void setupConnectionCallbacks() {
        connection.setOnConnected(() -> {
            connected = true;
            setConnectionStatus(true);
        });

        connection.setOnDisconnected(() -> {
            connected = false;
            setConnectionStatus(false);
        });

        connection.setOnMapReceived(json -> {
            parseMap(json);
            mapCanvas.setState(simState);
        });

        connection.setOnStateReceived(json -> {
            parseState(json);
            mapCanvas.setState(simState);
            updateLightList();
            updateEdgeDensities();
            handleEvLightOverride();
            if (simState.isRouteDone()) onRouteDone();
        });

        //ev_done separado del estado (protocolo legacy) — evitar doble llamada
        connection.setOnEvDone(() -> {
            if (!simState.isRouteDone()) onRouteDone();
        });
    }

    private void setupEmergencyActions() {
        //boton marcar punto A: activa modo colocacion en el canvas
        btnMarcarA.setOnAction(e -> {
            if (mapCanvas.isPlacingPointA()) {
                // cancelar
                mapCanvas.setPlacingPointA(false);
                btnMarcarA.setText("Marcar A");
            } else {
                mapCanvas.setPlacingPointA(true);
                mapCanvas.setPlacingPointB(false);
                mapCanvas.setPlacingIncident(false);
                btnMarcarA.setText("Cancelar");
                btnMarcarB.setText("Marcar B");
                btnMarcarMapa.setText("Marcar en mapa");
            }
        });

        btnMarcarB.setOnAction(e -> {
            if (mapCanvas.isPlacingPointB()) {
                mapCanvas.setPlacingPointB(false);
                btnMarcarB.setText("Marcar B");
            } else {
                mapCanvas.setPlacingPointB(true);
                mapCanvas.setPlacingPointA(false);
                mapCanvas.setPlacingIncident(false);
                btnMarcarB.setText("Cancelar");
                btnMarcarA.setText("Marcar A");
                btnMarcarMapa.setText("Marcar en mapa");
            }
        });
    }

    private void setupIncidentActions() {
        btnMarcarMapa.setOnAction(e -> {
            if (mapCanvas.isPlacingIncident()) {
                mapCanvas.setPlacingIncident(false);
                btnMarcarMapa.setText("Marcar en mapa");
            } else {
                mapCanvas.setPlacingIncident(true);
                mapCanvas.setPlacingPointA(false);
                mapCanvas.setPlacingPointB(false);
                btnMarcarMapa.setText("Cancelar colocación");
                btnMarcarA.setText("Marcar A");
                btnMarcarB.setText("Marcar B");
            }
        });

        btnActualizarInc.setOnAction(e -> {
            Incident sel = listIncidentes.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            String nuevoEstado = comboEstadoUpdate.getValue();
            String nota = fieldNotaUpdate.getText().trim();
            dao.addUpdate(sel.getId(), nuevoEstado, nota.isBlank() ? null : nota);
            fieldNotaUpdate.clear();
            refreshIncidents();
        });

        btnCerrarInc.setOnAction(e -> {
            Incident sel = listIncidentes.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            dao.closeIncident(sel.getId());
            refreshIncidents();
            mapCanvas.setSelectedIncident(null);
        });

        listIncidentes.getSelectionModel().selectedItemProperty().addListener((o, prev, curr) -> {
            if (curr != null) mapCanvas.setSelectedIncident(curr);
        });
    }

    private void setupHistorialSelection() {
        listHistorial.getSelectionModel().selectedItemProperty().addListener((o, prev, curr) -> {
            if (curr == null) {
                listUpdates.setItems(FXCollections.emptyObservableList());
                return;
            }
            List<IncidentUpdate> updates = dao.getUpdates(curr.getId());
            listUpdates.setItems(FXCollections.observableArrayList(updates));
        });
    }

    //region conexion
    @FXML
    private void onToggleConexion() {
        if (connected) {
            connection.disconnect();
            // setConnectionStatus lo llama onDisconnected; no tocar el boton aqui
        } else {
            // deshabilitar boton mientras se intenta conectar para evitar doble pulsacion
            btnConectar.setText("Conectando…");
            btnConectar.setDisable(true);
            String url = fieldWsUrl.getText().trim();
            connection.connect(url.isBlank() ? "ws://localhost:8765" : url);
        }
    }

    private void setConnectionStatus(boolean ok) {
        dotConStatus.setFill(ok ? Color.web("#22c55e") : Color.web("#ef4444"));
        lblConStatus.setText(ok ? "Conectado" : "Sin conexión");
        btnConectar.setText(ok ? "Desconectar" : "Conectar");
        btnConectar.setDisable(false);
    }

    //region emergencia
    @FXML
    private void onEnviarRuta() {
        double ax = mapCanvas.getPointAX(), ay = mapCanvas.getPointAY();
        double bx = mapCanvas.getPointBX(), by = mapCanvas.getPointBY();
        if (Double.isNaN(ax) || Double.isNaN(bx)) return;

        String aId = mapCanvas.getPointAId();
        String bId = mapCanvas.getPointBId();

        // si ambos puntos son nodos exactos usar ids; sino enviar coordenadas
        if (aId != null && bId != null) {
            connection.sendRoute(aId, bId);
        } else {
            connection.sendRouteByCoords(ax, ay, bx, by);
        }

        String aLabel = aId != null ? aId : String.format("(%.0f,%.0f)", ax, ay);
        String bLabel = bId != null ? bId : String.format("(%.0f,%.0f)", bx, by);
        lblEstadoRuta.setText("Ruta activa: " + aLabel + " → " + bLabel);
        btnCancelarRuta.setDisable(false);
        btnEnviarRuta.setDisable(true);
        overriddenLights.clear();
    }

    @FXML
    private void onCancelarRuta() {
        connection.cancelRoute();
        clearRoute();
    }

    private void clearRoute() {
        lblEstadoRuta.setText("Sin ruta activa");
        btnCancelarRuta.setDisable(true);
        btnEnviarRuta.setDisable(false);
        mapCanvas.setRouteNodes(new ArrayList<>());
        overriddenLights.clear();
    }

    private void onRouteDone() {
        lblEstadoRuta.setText("Ruta completada ✓");
        btnCancelarRuta.setDisable(true);
        btnEnviarRuta.setDisable(false);
        simState.setEvActive(false);
        simState.setEvRoute(new ArrayList<>());
        mapCanvas.setState(simState);
        overriddenLights.clear();
    }

    private void refreshEnviarBtn() {
        boolean aOk = !Double.isNaN(mapCanvas.getPointAX());
        boolean bOk = !Double.isNaN(mapCanvas.getPointBX());
        btnEnviarRuta.setDisable(!aOk || !bOk);
    }

    //override automatico de semaforos para el VE
    //se rastrea que semaforos ya han sido overrideados para no repetir el envio cada tick

    private void handleEvLightOverride() {
        if (!simState.isEvActive()) return;
        String nextNodeId = simState.getEvNextNode();
        if (nextNodeId == null) return;

        TrafficNode nextNode = simState.findNode(nextNodeId);
        if (nextNode == null) return;

        double dist = Math.hypot(simState.getEvX() - nextNode.getX(),
                simState.getEvY() - nextNode.getY());
        if (dist >= EV_LIGHT_TRIGGER_DIST) return;

        // calcular la direccion de aproximacion del VE
        String approachDir = inferEvApproachDir(nextNodeId);

        //poner en verde el semaforo de la direccion del VE y en rojo los perpendiculares
        //solo si no lo hemos hecho ya para este nodo en esta ruta
        List<TrafficLight> lts = simState.findLightsAtNode(nextNodeId);
        for (TrafficLight lt : lts) {
            if (overriddenLights.contains(lt.getId())) continue;
            boolean sameAxis = isSameAxis(lt.getDir(), approachDir);
            String state = sameAxis ? "green" : "red";
            connection.overrideLight(lt.getId(), state, 35);
            overriddenLights.add(lt.getId());
        }
    }

    //devuelve la direccion cardinal desde la que el VE llega al nodo destino
    private String inferEvApproachDir(String targetNodeId) {
        List<String> route = simState.getEvRoute();
        int idx = route.indexOf(targetNodeId);
        if (idx <= 0) return "N";
        TrafficNode prev = simState.findNode(route.get(idx - 1));
        TrafficNode next = simState.findNode(targetNodeId);
        if (prev == null || next == null) return "N";
        double dx = next.getX() - prev.getX();
        double dy = next.getY() - prev.getY();
        if (Math.abs(dx) >= Math.abs(dy)) return dx > 0 ? "E" : "W";
        return dy > 0 ? "S" : "N";
    }

    //N y S son el mismo eje; E y W son el mismo eje
    private boolean isSameAxis(String dirA, String dirB) {
        boolean nsA = dirA.equals("N") || dirA.equals("S");
        boolean nsB = dirB.equals("N") || dirB.equals("S");
        return nsA == nsB;
    }

    //region incidentes
    private void createIncidentAt(double simX, double simY) {
        String tipo = comboTipoIncidente.getValue();
        String desc = fieldDescIncidente.getText().trim();
        String estado = comboEstadoIncidente.getValue();
        if (tipo == null) {
            showAlert("Selecciona el tipo de incidente antes de colocarlo.");
            return;
        }
        Incident i = new Incident();
        i.setTipo(tipo);
        i.setDescripcion(desc.isBlank() ? null : desc);
        i.setMapX(simX);
        i.setMapY(simY);
        i.setEstado(estado != null ? estado : "Abierto");
        dao.insertIncident(i);
        fieldDescIncidente.clear();
        refreshIncidents();
    }

    private void refreshIncidents() {
        List<Incident> activos = dao.getActiveIncidents();
        List<Incident> cerrados = dao.getClosedIncidents();
        incidentItems.setAll(activos);
        historialItems.setAll(cerrados);
        mapCanvas.setIncidents(activos);
    }

    //region parsing json
    private void parseMap(String json) {
        try {
            JsonObject root = gson.fromJson(json, JsonObject.class);

            List<TrafficNode> nodes = new ArrayList<>();
            if (root.has("nodes")) {
                for (JsonElement el : root.getAsJsonArray("nodes")) {
                    JsonObject o = el.getAsJsonObject();
                    nodes.add(new TrafficNode(
                            o.get("id").getAsString(),
                            o.get("x").getAsDouble(),
                            o.get("y").getAsDouble(),
                            o.has("main") && o.get("main").getAsBoolean()
                    ));
                }
            }

            List<TrafficEdge> edges = new ArrayList<>();
            edgeById.clear();
            if (root.has("edges")) {
                for (JsonElement el : root.getAsJsonArray("edges")) {
                    JsonObject o = el.getAsJsonObject();
                    TrafficEdge e = new TrafficEdge();
                    e.setId(o.get("id").getAsString());
                    e.setFrom(o.get("from").getAsString());
                    e.setTo(o.get("to").getAsString());
                    e.setLanes(o.has("lanes") ? o.get("lanes").getAsInt() : 1);
                    e.setMain(o.has("main") && o.get("main").getAsBoolean());
                    e.setName(o.has("name") ? o.get("name").getAsString() : null);
                    edges.add(e);
                    edgeById.put(e.getId(), e);
                }
            }

            List<TrafficLight> lights = new ArrayList<>();
            lightById.clear();
            if (root.has("lights")) {
                for (JsonElement el : root.getAsJsonArray("lights")) {
                    JsonObject o = el.getAsJsonObject();
                    TrafficLight l = new TrafficLight();
                    l.setId(o.get("id").getAsString());
                    l.setNodeId(o.get("node").getAsString());
                    l.setDir(o.has("dir") ? o.get("dir").getAsString() : "N");
                    l.setState("red");
                    lights.add(l);
                    lightById.put(l.getId(), l);
                }
            }

            simState.setNodes(nodes);
            simState.setEdges(edges);
            simState.setLights(lights);
            simState.rebuildIndexes();
            edgeItems.setAll(edges);
            lightItems.setAll(lights);

        } catch (Exception e) {
            System.err.println("error al parsear mensaje de mapa: " + e.getMessage());
        }
    }

    private void parseState(String json) {
        try {
            JsonObject root = gson.fromJson(json, JsonObject.class);

            //coches
            List<SimCar> cars = new ArrayList<>();
            if (root.has("cars")) {
                for (JsonElement el : root.getAsJsonArray("cars")) {
                    JsonObject o = el.getAsJsonObject();
                    SimCar c = new SimCar();
                    c.setId(o.get("id").getAsString());
                    c.setX(o.get("x").getAsDouble());
                    c.setY(o.get("y").getAsDouble());
                    c.setNodeA(o.get("na").getAsString());
                    c.setNodeB(o.get("nb").getAsString());
                    c.setProgress(o.get("p").getAsDouble());
                    c.setLane(o.has("lane") ? o.get("lane").getAsInt() : 1);
                    cars.add(c);
                }
            }
            simState.setCars(cars);

            //semaforos: O(1) por lookup en vez de O(n^2)
            if (root.has("lights")) {
                for (JsonElement el : root.getAsJsonArray("lights")) {
                    JsonObject o = el.getAsJsonObject();
                    String lid = o.get("id").getAsString();
                    TrafficLight l = lightById.get(lid);
                    if (l != null) {
                        l.setState(o.get("state").getAsString());
                        l.setTimer(o.has("t") ? o.get("t").getAsInt() : 0);
                    }
                }
            }

            //densidad por arista: O(1) por lookup
            if (root.has("traffic")) {
                for (JsonElement el : root.getAsJsonArray("traffic")) {
                    JsonObject o = el.getAsJsonObject();
                    String eid = o.get("id").getAsString();
                    double dens = o.get("density").getAsDouble();
                    TrafficEdge edge = edgeById.get(eid);
                    if (edge != null) edge.setDensity(dens);
                }
            }

            //vehiculo de emergencia
            if (root.has("ev") && !root.get("ev").isJsonNull()) {
                JsonObject ev = root.getAsJsonObject("ev");
                simState.setEvActive(true);
                simState.setEvX(ev.get("x").getAsDouble());
                simState.setEvY(ev.get("y").getAsDouble());
                simState.setEvNextNode(ev.has("next") ? ev.get("next").getAsString() : null);
                if (ev.has("route")) {
                    List<String> route = new ArrayList<>();
                    for (JsonElement rn : ev.getAsJsonArray("route")) route.add(rn.getAsString());
                    simState.setEvRoute(route);
                    mapCanvas.setRouteNodes(route);
                }
            } else {
                simState.setEvActive(false);
            }

            simState.setRouteDone(root.has("ev_done") && root.get("ev_done").getAsBoolean());

        } catch (Exception e) {
            System.err.println("error al parsear estado: " + e.getMessage());
        }
    }

    //region listas
    private void updateLightList() {
        lightItems.setAll(simState.getLights());
    }

    private void updateEdgeDensities() {
        edgeItems.setAll(simState.getEdges());
    }

    //region helpers ui
    private Color lightStateColor(String state) {
        if (state == null) return Color.web("#ef4444");
        return switch (state) {
            case "green" -> Color.web("#22c55e");
            case "yellow" -> Color.web("#f59e0b");
            default -> Color.web("#ef4444");
        };
    }

    //nombre en espanol del estado del semaforo para mostrar en la lista
    private String stateDisplayName(String state) {
        if (state == null) return "Rojo";
        return switch (state) {
            case "green" -> "Verde";
            case "yellow" -> "Ámbar";
            default -> "Rojo";
        };
    }

    //nombre en espanol de la direccion del semaforo
    private String dirDisplayName(String dir) {
        if (dir == null) return "";
        return switch (dir) {
            case "N" -> "↑ Norte";
            case "S" -> "↓ Sur";
            case "E" -> "→ Este";
            case "W" -> "← Oeste";
            default -> dir;
        };
    }

    private void showOverrideDialog(TrafficLight light) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Override de semáforo — " + light.getId()
                + " (" + dirDisplayName(light.getDir()) + ")");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        //opciones en espanol mapeadas a los estados internos
        ComboBox<String> comboState = new ComboBox<>(FXCollections.observableArrayList(
                "Verde", "Ámbar", "Rojo"
        ));
        //preseleccionar el estado actual traducido
        comboState.getSelectionModel().select(stateDisplayName(light.getState()));

        Spinner<Integer> spinnerDur = new Spinner<>(5, 120, 30, 5);

        Label lblInfo = new Label("Nodo: " + light.getNodeId()
                + "   Dirección: " + dirDisplayName(light.getDir())
                + "   Estado actual: " + stateDisplayName(light.getState())
                + "   Contador: " + light.getTimer() + "s");
        lblInfo.setStyle("-fx-text-fill: -cc-text-secondary; -fx-font-size: 11px;");

        VBox content = new VBox(10,
                lblInfo,
                new Label("Forzar estado:"), comboState,
                new Label("Duración (segundos):"), spinnerDur
        );
        content.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(content);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                // traducir nombre en espanol al estado interno
                String selected = comboState.getValue();
                String internalState = switch (selected) {
                    case "Verde" -> "green";
                    case "Ámbar" -> "yellow";
                    default -> "red";
                };
                connection.overrideLight(light.getId(), internalState, spinnerDur.getValue());
            }
        });
    }

    private void showAlert(String message) {
        Alert a = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        a.showAndWait();
    }

    public void onShutdown() {
        connection.shutdown();
    }
}