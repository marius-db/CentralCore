package com.centralcore.modules.trafficmodule;

import com.centralcore.modules.trafficmodule.model.*;
import com.centralcore.util.PreferencesStorage;
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

    //región fxml
    @FXML private SplitPane splitPane;
    @FXML private StackPane mapPane;
    @FXML private VBox sidePanel;
    @FXML private Label lblConStatus;
    @FXML private Circle dotConStatus;
    @FXML private Button btnConectar;
    @FXML private TextField fieldWsUrl;

    //pestaña semáforos
    @FXML private ListView<TrafficLight> listSemaforos;

    //pestaña trafico
    @FXML private ListView<TrafficEdge> listTrafico;

    //pestaña emergencia
    @FXML private Label lblPuntoA;
    @FXML private Label lblPuntoB;
    @FXML private Button btnEnviarRuta;
    @FXML private Button btnCancelarRuta;
    @FXML private Label lblEstadoRuta;
    @FXML private Button btnMarcarA;
    @FXML private Button btnMarcarB;

    //pestaña incidentes
    @FXML private Button btnMarcarMapa;
    @FXML private ListView<Incident> listIncidentes;
    @FXML private Button btnActualizarInc;
    @FXML private Button btnCerrarInc;
    @FXML private TextField fieldNotaUpdate;
    @FXML private ComboBox<String> comboEstadoUpdate;

    //pestaña historial
    @FXML private ListView<Incident> listHistorial;
    @FXML private ListView<IncidentUpdate> listUpdates;

    private final SimState simState = new SimState();
    private final SimConnection connection = new SimConnection();
    private final TrafficDAO dao = new TrafficDAO();
    private final Gson gson = new Gson();
    private MapCanvas mapCanvas;

    private boolean connected = false;

    private final ObservableList<TrafficLight> lightItems = FXCollections.observableArrayList();
    private final ObservableList<TrafficEdge> edgeItems = FXCollections.observableArrayList();
    private final ObservableList<Incident> incidentItems = FXCollections.observableArrayList();
    private final ObservableList<Incident> historialItems = FXCollections.observableArrayList();

    private static final double EV_LIGHT_TRIGGER_DIST = 80.0;

    private final Set<String> overriddenLights = new HashSet<>();

    private final java.util.Map<String, TrafficEdge> edgeById = new java.util.HashMap<>();
    private final java.util.Map<String, TrafficLight> lightById = new java.util.HashMap<>();

    //contador de ticks para throttling de las listas: se actualizan cada 5 ticks (~400ms)
    //los estados de semáforo cambian en ciclos de varios segundos, no hace falta refrescar a 12 fps
    private int stateTick = 0;
    private static final int LIST_REFRESH_INTERVAL = 5;

    //menu de contexto activo , solo puede existir uno a la vez
    private ContextMenu activeContextMenu = null;

    @FXML
    public void initialize() {
        setupMapCanvas();
        setupSplitPane();
        setupListCells();
        setupCombos();
        setupConnectionCallbacks();
        setupIncidentActions();
        setupEmergencyActions();
        setupHistorialSelection();
        refreshIncidents();
    }

    //región setup
    private void setupMapCanvas() {
        mapCanvas = new MapCanvas();
        mapCanvas.widthProperty().bind(mapPane.widthProperty());
        mapCanvas.heightProperty().bind(mapPane.heightProperty());
        mapPane.getChildren().add(0, mapCanvas);

        mapCanvas.setOnNodeRightClicked(node -> showContextMenu(buildNodeContextMenu(node), node.getX(), node.getY()));
        mapCanvas.setOnRoadRightClicked(coords -> showContextMenu(buildRoadContextMenu(coords[0], coords[1]), coords[0], coords[1]));

        mapCanvas.setOnIncidentPlaced((simX, simY) -> {
            btnMarcarMapa.setText("Marcar en mapa");
            createIncidentAt(simX, simY);
        });

        mapCanvas.setOnPointAPlaced(coords -> {
            btnMarcarA.setText("Marcar A");
            String nodeId = mapCanvas.getPointAId();
            lblPuntoA.setText(nodeId != null ? nodeId : String.format("(%.0f, %.0f)", coords[0], coords[1]));
            refreshEnviarBtn();
        });

        mapCanvas.setOnPointBPlaced(coords -> {
            btnMarcarB.setText("Marcar B");
            String nodeId = mapCanvas.getPointBId();
            lblPuntoB.setText(nodeId != null ? nodeId : String.format("(%.0f, %.0f)", coords[0], coords[1]));
            refreshEnviarBtn();
        });

        mapCanvas.setOnIncidentSelected(inc -> listIncidentes.getSelectionModel().select(inc));

        //seleccion de semáforo desde el mapa: resalta en lista y en el mapa
        mapCanvas.setOnLightClicked(lt -> {
            listSemaforos.getSelectionModel().select(lt);
            listSemaforos.scrollTo(lt);
        });

        //clic en el mapa fuera de semáforo/incidentes limpia la seleccion
        mapPane.setOnMousePressed(e -> hideActiveContextMenu());
    }

    //muestra un ContextMenu convirtiendo coordenadas sim a screen
    //cierra cualquier menu anterior antes de mostrar el nuevo
    private void showContextMenu(ContextMenu ctx, double simX, double simY) {
        hideActiveContextMenu();
        javafx.geometry.Point2D screen = mapCanvas.localToScreen(
                simX * mapCanvas.getScaleValue() + mapCanvas.getOffsetX(), simY * mapCanvas.getScaleValue() + mapCanvas.getOffsetY());
        if (screen == null) return;
        activeContextMenu = ctx;
        //al ocultarse por cualquier motivo limpia la referencia
        ctx.setOnHidden(e -> { if (activeContextMenu == ctx) activeContextMenu = null; });
        ctx.show(mapCanvas, screen.getX(), screen.getY());
    }

    private void hideActiveContextMenu() {
        if (activeContextMenu != null) {
            activeContextMenu.hide();
            activeContextMenu = null;
        }
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
        itemInc.setOnAction(e -> showIncidentDialog(node.getX(), node.getY()));

        ctx.getItems().addAll(itemA, itemB, new SeparatorMenuItem(), itemInc);
        return ctx;
    }

    private ContextMenu buildRoadContextMenu(double simX, double simY) {
        ContextMenu ctx = new ContextMenu();

        // check if a node is close enough to allow point placement
        TrafficNode nearNode = mapCanvas.findNearestNode(simX, simY, 30);

        MenuItem itemA = new MenuItem("Marcar como Punto A");
        MenuItem itemB = new MenuItem("Marcar como Punto B");

        if (nearNode != null) {
            //snap to the node, same as buildNodeContextMenu
            TrafficNode finalNode = nearNode;
            itemA.setOnAction(e -> {
                mapCanvas.setPointA(finalNode.getId(), finalNode.getX(), finalNode.getY());
                lblPuntoA.setText(finalNode.getId());
                refreshEnviarBtn();
            });
            itemB.setOnAction(e -> {
                mapCanvas.setPointB(finalNode.getId(), finalNode.getX(), finalNode.getY());
                lblPuntoB.setText(finalNode.getId());
                refreshEnviarBtn();
            });
        } else {
            //not near a node: disable A and B options
            itemA.setDisable(true);
            itemB.setDisable(true);
        }

        MenuItem itemInc = new MenuItem("Añadir incidente aquí");
        itemInc.setOnAction(e -> showIncidentDialog(simX, simY));

        ctx.getItems().addAll(itemA, itemB, new SeparatorMenuItem(), itemInc);
        return ctx;
    }

    private void setupSplitPane() {
        //cargar posicion guardada, por defecto 2/3 mapa y 1/3 panel lateral
        final double savedPos = PreferencesStorage.getDouble("traffic.splitpane.divider", 0.67);
        final double[] dividerPos = {savedPos};
        //bandera para suprimir guardado mientras se restaura la posicion
        final boolean[] settling = {true};

        //la forma más fiable de forzar la posicion es esperar al primer layout real del splitpane
        //layoutBoundsProperty cambia cuando el nodo tiene tamaño asignado
        splitPane.layoutBoundsProperty().addListener(new javafx.beans.value.ChangeListener<javafx.geometry.Bounds>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends javafx.geometry.Bounds> obs, javafx.geometry.Bounds oldB, javafx.geometry.Bounds newB) {
                if (newB.getWidth() > 0) {
                    splitPane.layoutBoundsProperty().removeListener(this);
                    //dos runLater para dejar que javafx termine el primer layout completo
                    Platform.runLater(() -> Platform.runLater(() -> {
                        splitPane.setDividerPositions(dividerPos[0]);
                        settling[0] = false;
                    }));
                }
            }
        });

        //cuando se maximiza o restaura, replicar la posicion guardada sin guardar el cambio
        splitPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            newScene.windowProperty().addListener((o2, oldWin, win) -> {
                if (win == null) return;
                javafx.stage.Stage stage = (javafx.stage.Stage) win;
                stage.maximizedProperty().addListener((o3, wasMax, isMax) -> {
                    boolean prev = settling[0];
                    settling[0] = true;
                    Platform.runLater(() -> {
                        splitPane.setDividerPositions(dividerPos[0]);
                        settling[0] = prev;
                    });
                });
            });
        });

        //guardar posicion solo cuando el usuario mueve el divider, no durante el layout
        splitPane.getDividers().get(0).positionProperty().addListener((obs, oldPos, newPos) -> {
            if (!settling[0]) {
                dividerPos[0] = newPos.doubleValue();
                PreferencesStorage.putDouble("traffic.splitpane.divider", dividerPos[0]);
            }
        });
    }

    private void setupListCells() {
        listSemaforos.setItems(lightItems);
        listSemaforos.setCellFactory(lv -> new ListCell<>() {
            private final Circle dot = new Circle(5);
            private final Label lblId = new Label();
            private final Label lblDir = new Label();
            private final Button btnOvr = new Button("Sobreescribir");
            private final HBox row = new HBox(8, dot, lblId, lblDir, new Pane(), btnOvr);
            //dropshadow creado una sola vez y reutilizado; solo se muta el color
            private final javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow(6, Color.RED);

            {
                HBox.setHgrow(row.getChildren().get(3), Priority.ALWAYS);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 8, 4, 8));
                btnOvr.getStyleClass().add("btn-ghost");
                btnOvr.setOnAction(e -> {
                    TrafficLight l = getItem();
                    if (l != null) showOverrideDialog(l);
                });
                dot.setEffect(shadow);
            }

            @Override
            protected void updateItem(TrafficLight l, boolean empty) {
                super.updateItem(l, empty);
                if (empty || l == null) { setGraphic(null); return; }
                Color c = lightStateColor(l.getState());
                dot.setFill(c);
                shadow.setColor(c);
                String stateName = stateDisplayName(l.getState());
                lblId.setText(l.getId() + "  " + stateName + "  " + l.getTimer() + "s");
                lblId.getStyleClass().setAll("tm-list-label");
                lblDir.setText(dirDisplayName(l.getDir()));
                lblDir.getStyleClass().setAll("tm-hint");
                setGraphic(row);
            }
        });

        //seleccion en lista de semaforos -> resaltar en mapa
        listSemaforos.getSelectionModel().selectedItemProperty().addListener((o, prev, curr) -> {
            mapCanvas.setSelectedLight(curr);
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
                if (empty || e == null) { setGraphic(null); return; }
                double d = e.getDensity();
                densBar.setWidth(Math.max(4, d * 80));
                Color barColor = d < 0.4 ? Color.web("#22c55e") : d < 0.7 ? Color.web("#f59e0b") : Color.web("#ef4444");
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
                if (empty || i == null) { setText(null); return; }
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
                if (empty || i == null) { setText(null); return; }
                String closed = i.getClosedAt() != null ? i.getClosedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "—";
                setText("[" + closed + "] " + i.getTipo() + " - " + i.getDescripcion());
            }
        });
    }

    private void setupCombos() {
        comboEstadoUpdate.setItems(FXCollections.observableArrayList("Abierto", "En curso", "Crítico", "Resuelto"));
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
            mapCanvas.markDirty();
            handleEvLightOverride();
            if (simState.isRouteDone()) onRouteDone();
            stateTick++;
            if (stateTick % LIST_REFRESH_INTERVAL == 0) {
                updateLightList();
                updateEdgeDensities();
            }
        });

        connection.setOnEvDone(() -> {
            if (!simState.isRouteDone()) onRouteDone();
        });
    }

    private void setupEmergencyActions() {
        btnMarcarA.setOnAction(e -> {
            if (mapCanvas.isPlacingPointA()) {
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
        //al pulsar "Marcar en mapa" abre el diálogo de creacion para rellenar datos
        //y luego activa el modo de colocacion en el canvas
        btnMarcarMapa.setOnAction(e -> {
            if (mapCanvas.isPlacingIncident()) {
                mapCanvas.setPlacingIncident(false);
                btnMarcarMapa.setText("Marcar en mapa");
            } else {
                //muestra el dialogo, si el usuario confirma, activa el modo de colocacion
                boolean confirmed = showIncidentFormDialog();
                if (confirmed) {
                    mapCanvas.setPlacingIncident(true);
                    mapCanvas.setPlacingPointA(false);
                    mapCanvas.setPlacingPointB(false);
                    btnMarcarMapa.setText("Cancelar colocación");
                    btnMarcarA.setText("Marcar A");
                    btnMarcarB.setText("Marcar B");
                }
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
            if (curr == null) { listUpdates.setItems(FXCollections.emptyObservableList()); return; }
            List<IncidentUpdate> updates = dao.getUpdates(curr.getId());
            listUpdates.setItems(FXCollections.observableArrayList(updates));
        });
    }

    //región conexion
    @FXML
    private void onToggleConexion() {
        if (connected) {
            connection.disconnect();
        } else {
            btnConectar.setText("Conectando...");
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

    //región emergencia
    @FXML
    private void onEnviarRuta() {
        double ax = mapCanvas.getPointAX(), ay = mapCanvas.getPointAY();
        double bx = mapCanvas.getPointBX(), by = mapCanvas.getPointBY();
        if (Double.isNaN(ax) || Double.isNaN(bx)) return;

        String aId = mapCanvas.getPointAId();
        String bId = mapCanvas.getPointBId();

        if (aId != null && bId != null) {
            connection.sendRoute(aId, bId);
        } else {
            connection.sendRouteByCoords(ax, ay, bx, by);
        }

        String aLabel = aId != null ? aId : String.format("(%.0f,%.0f)", ax, ay);
        String bLabel = bId != null ? bId : String.format("(%.0f,%.0f)", bx, by);
        lblEstadoRuta.setText("Ruta activa: " + aLabel + " -> " + bLabel);
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
        lblEstadoRuta.setText("Ruta completada");
        btnCancelarRuta.setDisable(true);
        btnEnviarRuta.setDisable(false);
        simState.setEvActive(false);
        simState.setEvRoute(new ArrayList<>());
        mapCanvas.markDirty();
        overriddenLights.clear();
    }

    private void refreshEnviarBtn() {
        boolean aOk = !Double.isNaN(mapCanvas.getPointAX());
        boolean bOk = !Double.isNaN(mapCanvas.getPointBX());
        btnEnviarRuta.setDisable(!aOk || !bOk);
    }

    private void handleEvLightOverride() {
        if (!simState.isEvActive()) return;
        String nextNodeId = simState.getEvNextNode();
        if (nextNodeId == null) return;

        TrafficNode nextNode = simState.findNode(nextNodeId);
        if (nextNode == null) return;

        double dist = Math.hypot(simState.getEvX() - nextNode.getX(), simState.getEvY() - nextNode.getY());
        if (dist >= EV_LIGHT_TRIGGER_DIST) return;

        String approachDir = inferEvApproachDir(nextNodeId);

        List<TrafficLight> lts = simState.findLightsAtNode(nextNodeId);
        for (TrafficLight lt : lts) {
            if (overriddenLights.contains(lt.getId())) continue;
            boolean sameAxis = isSameAxis(lt.getDir(), approachDir);
            connection.overrideLight(lt.getId(), sameAxis ? "green" : "red", 35);
            overriddenLights.add(lt.getId());
        }
    }

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

    private boolean isSameAxis(String dirA, String dirB) {
        boolean nsA = dirA.equals("N") || dirA.equals("S");
        boolean nsB = dirB.equals("N") || dirB.equals("S");
        return nsA == nsB;
    }

    //región incidentes

    //datos del incidente pendiente de colocacion, rellenados por el diálogo
    private String pendingTipo = null;
    private String pendingDesc = null;
    private String pendingEstado = null;

    //muestra el diálogo de creacion de incidente y guarda los datos en pendingXxx
    //devuelve true si el usuario confirmo, false si cancelo
    private boolean showIncidentFormDialog() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Nuevo incidente");
        ButtonType cancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, cancelarBtn);

        dlg.getDialogPane().getStylesheets().add(getClass().getResource("/css/traffic.css").toExternalForm());

        ComboBox<String> comboTipo = new ComboBox<>(FXCollections.observableArrayList(
                "Accidente", "Corte de vía", "Obras", "Semáforo averiado",
                "Vehículo abandonado", "Desbordamiento", "Incendio", "Otro"
        ));
        comboTipo.getStyleClass().add("tm-combo");
        comboTipo.setMaxWidth(Double.MAX_VALUE);
        comboTipo.getSelectionModel().selectFirst();

        ComboBox<String> comboEstado = new ComboBox<>(FXCollections.observableArrayList("Abierto", "En curso", "Crítico", "Resuelto"));
        comboEstado.getStyleClass().add("tm-combo");
        comboEstado.setMaxWidth(Double.MAX_VALUE);
        comboEstado.getSelectionModel().selectFirst();

        TextField fieldDesc = new TextField();
        fieldDesc.getStyleClass().add("tm-url-field");
        fieldDesc.setPromptText("Descripción breve…");

        Label hint = new Label("Después de confirmar haz clic en el mapa para colocar el incidente.");
        hint.getStyleClass().add("tm-hint");
        hint.setWrapText(true);

        VBox content = new VBox(10,
                new Label("Tipo") {{ getStyleClass().add("tm-field-label"); }},
                comboTipo,
                new Label("Estado inicial") {{ getStyleClass().add("tm-field-label"); }},
                comboEstado,
                new Label("Descripción") {{ getStyleClass().add("tm-field-label"); }},
                fieldDesc,
                hint
        );
        content.setPadding(new Insets(16));
        content.setPrefWidth(320);

        //estilo del panel del diálogo para que encaje con el tema oscuro
        dlg.getDialogPane().setContent(content);

        java.util.Optional<ButtonType> result = dlg.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return false;

        pendingTipo = comboTipo.getValue();
        pendingDesc = fieldDesc.getText().trim();
        pendingEstado = comboEstado.getValue();
        return true;
    }

    //abre el diálogo de incidente y lo crea directamente en las coordenadas dadas
    //usado desde el menu de contexto del mapa (clic derecho -> añadir incidente aquí)
    private void showIncidentDialog(double simX, double simY) {
        boolean confirmed = showIncidentFormDialog();
        if (confirmed) createIncidentAt(simX, simY);
    }

    private void createIncidentAt(double simX, double simY) {
        if (pendingTipo == null) {
            showAlert("Selecciona el tipo de incidente antes de colocarlo.");
            return;
        }
        Incident i = new Incident();
        i.setTipo(pendingTipo);
        i.setDescripcion(pendingDesc == null || pendingDesc.isBlank() ? null : pendingDesc);
        i.setMapX(simX);
        i.setMapY(simY);
        i.setEstado(pendingEstado != null ? pendingEstado : "Abierto");
        dao.insertIncident(i);
        pendingTipo = null;
        pendingDesc = null;
        pendingEstado = null;
        refreshIncidents();
    }

    private void refreshIncidents() {
        List<Incident> activos = dao.getActiveIncidents();
        List<Incident> cerrados = dao.getClosedIncidents();
        incidentItems.setAll(activos);
        historialItems.setAll(cerrados);
        mapCanvas.setIncidents(activos);
    }

    //región parsing json
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

            //lookup O(1) por id en vez de busqueda lineal
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

            if (root.has("traffic")) {
                for (JsonElement el : root.getAsJsonArray("traffic")) {
                    JsonObject o = el.getAsJsonObject();
                    TrafficEdge edge = edgeById.get(o.get("id").getAsString());
                    if (edge != null) edge.setDensity(o.get("density").getAsDouble());
                }
            }

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

    //región listas
    private void updateLightList() {
        //conservar seleccion antes del setAll , el replace de items dispara selectedItemProperty
        //con null brevemente, lo que llamaria a mapCanvas.setSelectedLight(null) y mataría el highlight
        TrafficLight sel = listSemaforos.getSelectionModel().getSelectedItem();
        lightItems.setAll(simState.getLights());
        if (sel != null) {
            //re-seleccionar por id porque setAll recrea los items aunque sean los mismos objetos
            for (TrafficLight lt : lightItems) {
                if (lt.getId().equals(sel.getId())) {
                    listSemaforos.getSelectionModel().select(lt);
                    break;
                }
            }
        }
    }

    private void updateEdgeDensities() {
        edgeItems.setAll(simState.getEdges());
    }

    //región helpers ui
    private Color lightStateColor(String state) {
        if (state == null) return Color.web("#ef4444");
        return switch (state) {
            case "green" -> Color.web("#22c55e");
            case "yellow" -> Color.web("#f59e0b");
            default -> Color.web("#ef4444");
        };
    }

    private String stateDisplayName(String state) {
        if (state == null) return "Rojo";
        return switch (state) {
            case "green" -> "Verde";
            case "yellow" -> "Ambar";
            default -> "Rojo";
        };
    }

    private String dirDisplayName(String dir) {
        if (dir == null) return "";
        return switch (dir) {
            case "N" -> "^ Norte";
            case "S" -> "v Sur";
            case "E" -> "> Este";
            case "W" -> "< Oeste";
            default -> dir;
        };
    }

    private void showOverrideDialog(TrafficLight light) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Sobreescribir semaforo: " + light.getId() + " (" + dirDisplayName(light.getDir()) + ")");
        ButtonType cancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, cancelarBtn);

        //mismo stylesheet que el dialogo de incidentes para mantener coherencia visual
        dlg.getDialogPane().getStylesheets().add(getClass().getResource("/css/traffic.css").toExternalForm());

        ComboBox<String> comboState = new ComboBox<>(FXCollections.observableArrayList("Verde", "Ambar", "Rojo"));
        comboState.getStyleClass().add("tm-combo");
        comboState.setMaxWidth(Double.MAX_VALUE);
        comboState.getSelectionModel().select(stateDisplayName(light.getState()));

        Spinner<Integer> spinnerDur = new Spinner<>(5, 120, 30, 5);
        spinnerDur.setMaxWidth(Double.MAX_VALUE);
        spinnerDur.getEditor().getStyleClass().add("tm-url-field");

        Label lblInfo = new Label("Nodo: " + light.getNodeId()
                + "   Direccion: " + dirDisplayName(light.getDir())
                + "   Estado actual: " + stateDisplayName(light.getState())
                + "   Contador: " + light.getTimer() + "s");
        lblInfo.getStyleClass().add("tm-hint");
        lblInfo.setWrapText(true);

        VBox content = new VBox(10,
                lblInfo,
                new Label("Forzar estado:") {{ getStyleClass().add("tm-field-label"); }},
                comboState,
                new Label("Duracion (segundos):") {{ getStyleClass().add("tm-field-label"); }},
                spinnerDur
        );
        content.setPadding(new Insets(16));
        content.setPrefWidth(320);
        dlg.getDialogPane().setContent(content);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String selected = comboState.getValue();
                String internalState = switch (selected) {
                    case "Verde" -> "green";
                    case "Ambar" -> "yellow";
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