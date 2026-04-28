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
import java.util.List;

public class TrafficModuleController {

    //region fxml

    @FXML private StackPane    mapPane;
    @FXML private Label        lblConStatus;
    @FXML private Circle       dotConStatus;
    @FXML private Button       btnConectar;
    @FXML private TextField    fieldWsUrl;

    //tab semaforos
    @FXML private ListView<TrafficLight> listSemaforos;

    //tab trafico
    @FXML private ListView<TrafficEdge> listTrafico;

    //tab emergencia
    @FXML private Label  lblPuntoA;
    @FXML private Label  lblPuntoB;
    @FXML private Button btnEnviarRuta;
    @FXML private Button btnCancelarRuta;
    @FXML private Label  lblEstadoRuta;

    //tab incidentes
    @FXML private ComboBox<String>  comboTipoIncidente;
    @FXML private TextField         fieldDescIncidente;
    @FXML private ComboBox<String>  comboEstadoIncidente;
    @FXML private Button            btnMarcarMapa;
    @FXML private ListView<Incident> listIncidentes;
    @FXML private Button            btnActualizarInc;
    @FXML private Button            btnCerrarInc;
    @FXML private TextField         fieldNotaUpdate;
    @FXML private ComboBox<String>  comboEstadoUpdate;

    //tab historial
    @FXML private ListView<Incident>       listHistorial;
    @FXML private ListView<IncidentUpdate> listUpdates;

    //endregion

    //estado interno
    private final SimState       simState   = new SimState();
    private final SimConnection  connection = new SimConnection();
    private final TrafficDAO     dao        = new TrafficDAO();
    private final Gson           gson       = new Gson();
    private MapCanvas            mapCanvas;
    private boolean              connected  = false;

    //listas observables para la ui
    private final ObservableList<TrafficLight> lightItems     = FXCollections.observableArrayList();
    private final ObservableList<TrafficEdge>  edgeItems      = FXCollections.observableArrayList();
    private final ObservableList<Incident>     incidentItems  = FXCollections.observableArrayList();
    private final ObservableList<Incident>     historialItems = FXCollections.observableArrayList();

    //nodo seleccionado como punto a y b para la ruta de emergencia
    private String pointAId = null;
    private String pointBId = null;

    //umbral en unidades de simulacion para activar override de semaforo
    private static final double EV_LIGHT_TRIGGER_DIST = 80.0;

    @FXML
    public void initialize() {
        setupMapCanvas();
        setupListCells();
        setupCombos();
        setupConnectionCallbacks();
        setupIncidentActions();
        setupHistorialSelection();
        refreshIncidents();
    }

    //region setup

    private void setupMapCanvas() {
        mapCanvas = new MapCanvas();
        //el canvas ocupa todo el stackpane
        mapCanvas.widthProperty().bind(mapPane.widthProperty());
        mapCanvas.heightProperty().bind(mapPane.heightProperty());
        mapPane.getChildren().add(0, mapCanvas);

        //menu contextual al hacer clic derecho en un nodo
        mapCanvas.setOnNodeRightClicked(node -> {
            ContextMenu ctx = buildNodeContextMenu(node);
            javafx.geometry.Point2D screen = mapCanvas.localToScreen(0, 0); if (screen != null) ctx.show(mapCanvas, screen.getX(), screen.getY());
        });

        //colocar incidente al hacer clic mientras se esta en modo colocacion
        mapCanvas.setOnIncidentPlaced((simX, simY) -> {
            mapCanvas.setPlacingIncident(false);
            btnMarcarMapa.setText("Marcar en mapa");
            createIncidentAt(simX, simY);
        });

        //al seleccionar un pin de incidente en el mapa
        mapCanvas.setOnIncidentSelected(inc -> {
            listIncidentes.getSelectionModel().select(inc);
        });
    }

    private ContextMenu buildNodeContextMenu(TrafficNode node) {
        ContextMenu ctx = new ContextMenu();

        MenuItem itemA = new MenuItem("Marcar como Punto A");
        itemA.setOnAction(e -> {
            pointAId = node.getId();
            mapCanvas.setPointA(pointAId);
            lblPuntoA.setText(node.getId());
            refreshEnviarBtn();
        });

        MenuItem itemB = new MenuItem("Marcar como Punto B");
        itemB.setOnAction(e -> {
            pointBId = node.getId();
            mapCanvas.setPointB(pointBId);
            lblPuntoB.setText(node.getId());
            refreshEnviarBtn();
        });

        MenuItem itemInc = new MenuItem("Añadir incidente aquí");
        itemInc.setOnAction(e -> {
            createIncidentAt(node.getX(), node.getY());
        });

        ctx.getItems().addAll(itemA, itemB, new SeparatorMenuItem(), itemInc);
        return ctx;
    }

    private void setupListCells() {
        listSemaforos.setItems(lightItems);
        listSemaforos.setCellFactory(lv -> new ListCell<>() {
            private final Circle dot    = new Circle(5);
            private final Label  lblId  = new Label();
            private final Button btnOvr = new Button("Override");
            private final HBox   row    = new HBox(8, dot, lblId, new Pane(), btnOvr);

            {
                HBox.setHgrow(row.getChildren().get(2), Priority.ALWAYS);
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
                if (empty || l == null) { setGraphic(null); return; }
                Color c = lightStateColor(l.getState());
                dot.setFill(c);
                dot.setEffect(new javafx.scene.effect.DropShadow(6, c));
                lblId.setText(l.getId() + "  (" + l.getState() + ")  " + l.getTimer() + "s");
                lblId.getStyleClass().setAll("tm-list-label");
                setGraphic(row);
            }
        });

        listTrafico.setItems(edgeItems);
        listTrafico.setCellFactory(lv -> new ListCell<>() {
            private final Label     lblName    = new Label();
            private final Rectangle densBar    = new Rectangle(0, 6);
            private final Label     lblDens    = new Label();
            private final HBox      row        = new HBox(8, lblName, new Pane(), densBar, lblDens);

            {
                HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new Insets(6, 8, 6, 8));
                densBar.setArcWidth(3); densBar.setArcHeight(3);
            }

            @Override
            protected void updateItem(TrafficEdge e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setGraphic(null); return; }
                double d = e.getDensity();
                //anchura de la barra: maxima 80px
                densBar.setWidth(Math.max(4, d * 80));
                Color barColor = d < 0.4 ? Color.web("#22c55e") : d < 0.7 ? Color.web("#f59e0b") : Color.web("#ef4444");
                densBar.setFill(barColor);
                lblName.setText((e.getName() != null ? e.getName() : e.getId()));
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
                if (empty || i == null) { setText(null); return; }
                setText(i.toString());
                getStyleClass().removeAll("inc-abierto", "inc-critico", "inc-resuelto");
                switch (i.getEstado().toLowerCase()) {
                    case "crítico", "critico" -> getStyleClass().add("inc-critico");
                    case "resuelto"           -> getStyleClass().add("inc-resuelto");
                    default                   -> getStyleClass().add("inc-abierto");
                }
            }
        });

        listHistorial.setItems(historialItems);
        listHistorial.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Incident i, boolean empty) {
                super.updateItem(i, empty);
                if (empty || i == null) { setText(null); return; }
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
            System.out.println("modulo conectado a la simulacion");
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

        connection.setOnEvDone(this::onRouteDone);
    }

    private void setupIncidentActions() {
        //boton marcar en mapa: activa modo colocacion
        btnMarcarMapa.setOnAction(e -> {
            boolean placing = !mapCanvas.isPlacingIncident() && mapCanvas.isPlacingIncident() == false
                              && btnMarcarMapa.getText().equals("Marcar en mapa");
            //toggle
            if (btnMarcarMapa.getText().equals("Marcar en mapa")) {
                mapCanvas.setPlacingIncident(true);
                btnMarcarMapa.setText("Cancelar colocación");
            } else {
                mapCanvas.setPlacingIncident(false);
                btnMarcarMapa.setText("Marcar en mapa");
            }
        });

        btnActualizarInc.setOnAction(e -> {
            Incident sel = listIncidentes.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            String nuevoEstado = comboEstadoUpdate.getValue();
            String nota        = fieldNotaUpdate.getText().trim();
            dao.addUpdate(sel.getId(), nuevoEstado, nota.isBlank() ? null : nota);
            fieldNotaUpdate.clear();
            refreshIncidents();
            //actualizar pin en el mapa si cambio estado
            mapCanvas.setState(simState);
        });

        btnCerrarInc.setOnAction(e -> {
            Incident sel = listIncidentes.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            dao.closeIncident(sel.getId());
            refreshIncidents();
            mapCanvas.setSelectedIncident(null);
        });

        //al seleccionar un incidente en la lista, centrarlo en el mapa
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

    //endregion

    //region acciones de conexion

    @FXML
    private void onToggleConexion() {
        if (connected) {
            connection.disconnect();
            btnConectar.setText("Conectar");
        } else {
            String url = fieldWsUrl.getText().trim();
            connection.connect(url.isBlank() ? "ws://localhost:8765" : url);
            btnConectar.setText("Desconectar");
        }
    }

    private void setConnectionStatus(boolean ok) {
        dotConStatus.setFill(ok ? Color.web("#22c55e") : Color.web("#ef4444"));
        lblConStatus.setText(ok ? "Conectado" : "Sin conexión");
        btnConectar.setText(ok ? "Desconectar" : "Conectar");
    }

    //endregion

    //region acciones de emergencia

    @FXML
    private void onEnviarRuta() {
        if (pointAId == null || pointBId == null) return;
        connection.sendRoute(pointAId, pointBId);
        lblEstadoRuta.setText("Ruta activa: " + pointAId + " → " + pointBId);
        btnCancelarRuta.setDisable(false);
        btnEnviarRuta.setDisable(true);
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
    }

    private void onRouteDone() {
        lblEstadoRuta.setText("Ruta completada ✓");
        btnCancelarRuta.setDisable(true);
        btnEnviarRuta.setDisable(false);
        simState.setEvActive(false);
        simState.setEvRoute(new ArrayList<>());
        mapCanvas.setState(simState);
    }

    private void refreshEnviarBtn() {
        btnEnviarRuta.setDisable(pointAId == null || pointBId == null);
    }

    //override de semaforos automatico cuando el ve se acerca a un nodo

    private void handleEvLightOverride() {
        if (!simState.isEvActive()) return;
        String nextNodeId = simState.getEvNextNode();
        if (nextNodeId == null) return;

        TrafficNode nextNode = simState.findNode(nextNodeId);
        if (nextNode == null) return;

        double dist = Math.hypot(simState.getEvX() - nextNode.getX(), simState.getEvY() - nextNode.getY());
        if (dist < EV_LIGHT_TRIGGER_DIST) {
            TrafficLight light = simState.findLightAtNode(nextNodeId);
            if (light != null) {
                //calcular la direccion desde la que llega el ve para elegir la fase correcta
                String evState = inferEvApproachPhase(nextNodeId);
                connection.overrideLight(light.getId(), evState, 30);
            }
        }
    }

    //infiere que fase del semaforo favorece al ve segun la direccion de aproximacion

    private String inferEvApproachPhase(String targetNodeId) {
        List<String> route = simState.getEvRoute();
        int idx = route.indexOf(targetNodeId);
        if (idx <= 0) return "ns_green";

        TrafficNode prev = simState.findNode(route.get(idx - 1));
        TrafficNode next = simState.findNode(targetNodeId);
        if (prev == null || next == null) return "ns_green";

        double dx = Math.abs(next.getX() - prev.getX());
        double dy = Math.abs(next.getY() - prev.getY());
        //si el movimiento es mas horizontal -> fase ew, si es vertical -> fase ns
        return dx > dy ? "ew_green" : "ns_green";
    }

    //endregion

    //region incidentes

    private void createIncidentAt(double simX, double simY) {
        String tipo  = comboTipoIncidente.getValue();
        String desc  = fieldDescIncidente.getText().trim();
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
        List<Incident> activos  = dao.getActiveIncidents();
        List<Incident> cerrados = dao.getClosedIncidents();
        incidentItems.setAll(activos);
        historialItems.setAll(cerrados);
        mapCanvas.setIncidents(activos);
    }

    //endregion

    //region parsing de mensajes json del simulador

    //parsea el mensaje de tipo "map" y rellena el simState con nodos, aristas y semaforos
    private void parseMap(String json) {
        try {
            JsonObject root = gson.fromJson(json, JsonObject.class);

            List<TrafficNode> nodes = new ArrayList<>();
            if (root.has("nodes")) {
                for (JsonElement el : root.getAsJsonArray("nodes")) {
                    JsonObject o = el.getAsJsonObject();
                    TrafficNode n = new TrafficNode(
                        o.get("id").getAsString(),
                        o.get("x").getAsDouble(),
                        o.get("y").getAsDouble(),
                        o.has("main") && o.get("main").getAsBoolean()
                    );
                    nodes.add(n);
                }
            }

            List<TrafficEdge> edges = new ArrayList<>();
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
                }
            }

            List<TrafficLight> lights = new ArrayList<>();
            if (root.has("lights")) {
                for (JsonElement el : root.getAsJsonArray("lights")) {
                    JsonObject o = el.getAsJsonObject();
                    TrafficLight l = new TrafficLight();
                    l.setId(o.get("id").getAsString());
                    l.setNodeId(o.get("node").getAsString());
                    l.setState("red");
                    lights.add(l);
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

    //parsea el mensaje de tipo "state" y actualiza el estado de la simulacion
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

            //semaforos
            if (root.has("lights")) {
                for (JsonElement el : root.getAsJsonArray("lights")) {
                    JsonObject o  = el.getAsJsonObject();
                    String    lid = o.get("id").getAsString();
                    for (TrafficLight l : simState.getLights()) {
                        if (l.getId().equals(lid)) {
                            l.setState(o.get("state").getAsString());
                            l.setTimer(o.has("t") ? o.get("t").getAsInt() : 0);
                            break;
                        }
                    }
                }
            }

            //densidad por arista
            if (root.has("traffic")) {
                for (JsonElement el : root.getAsJsonArray("traffic")) {
                    JsonObject o   = el.getAsJsonObject();
                    String    eid  = o.get("id").getAsString();
                    double    dens = o.get("density").getAsDouble();
                    for (TrafficEdge e : simState.getEdges()) {
                        if (e.getId().equals(eid)) { e.setDensity(dens); break; }
                    }
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

    //endregion

    //region actualizar listas

    private void updateLightList() {
        lightItems.setAll(simState.getLights());
    }

    private void updateEdgeDensities() {
        //refrescar la lista de trafico para actualizar las barras de densidad
        edgeItems.setAll(simState.getEdges());
    }

    //endregion

    //region helpers ui

    private Color lightStateColor(String state) {
        if (state == null) return Color.web("#ef4444");
        return switch (state) {
            case "ns_green", "ew_green" -> Color.web("#22c55e");
            case "yellow"               -> Color.web("#f59e0b");
            default                     -> Color.web("#ef4444");
        };
    }

    private void showOverrideDialog(TrafficLight light) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Override de semáforo — " + light.getId());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> comboState = new ComboBox<>(FXCollections.observableArrayList(
            "ns_green", "ew_green", "yellow", "red"
        ));
        comboState.getSelectionModel().select(light.getState());

        Spinner<Integer> spinnerDur = new Spinner<>(5, 120, 30, 5);

        VBox content = new VBox(10,
            new Label("Estado:"),  comboState,
            new Label("Duración (seg):"), spinnerDur
        );
        content.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(content);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                connection.overrideLight(light.getId(), comboState.getValue(), spinnerDur.getValue());
            }
        });
    }

    private void showAlert(String message) {
        Alert a = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        a.showAndWait();
    }

    //endregion

    //llamado por TrafficModule al cerrar el modulo
    public void onShutdown() {
        connection.shutdown();
    }
}
