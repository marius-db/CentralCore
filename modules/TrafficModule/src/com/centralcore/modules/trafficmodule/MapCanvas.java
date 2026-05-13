package com.centralcore.modules.trafficmodule;

import com.centralcore.modules.trafficmodule.model.*;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

//canvas de tráfico optimizado:
//- colores y fuentes pre-cacheados (sin Color.web/Font.font en el render loop)
//- tabla de colores de densidad pre-calculada (sin new Color por arista por frame)
//- sin gc.save/restore en bucles (causa freeze en zoom con muchos elementos)
//- set de nombres de calle reutilizado (sin new HashSet por frame)
//- coches con separación real entre carriles
//- clic derecho funciona en cualquier punto (nodo, calle o vacío)
public class MapCanvas extends Canvas {

    //paleta: todas las constantes de color se crean una sola vez al cargar la clase
    private static final Color BG = Color.web("#0b0d12");
    private static final Color ROAD_MAIN = Color.web("#23283f");
    private static final Color ROAD_SIDE = Color.web("#191c2a");
    private static final Color ROAD_BORDER = Color.web("#2e3450");
    private static final Color LANE_LINE = Color.web("#3a4060");
    private static final Color NODE_FILL = Color.web("#1e2236");
    private static final Color NODE_STROKE = Color.web("#2b7fff80");
    private static final Color CAR_COLOR = Color.web("#5a9fff");
    private static final Color EV_COLOR = Color.web("#00e5ff");
    private static final Color ROUTE_COLOR = Color.web("#2b7fff99");
    private static final Color POINT_A = Color.web("#22c55e");
    private static final Color POINT_B = Color.web("#ef4444");
    private static final Color POINT_A_FILL = Color.web("#22c55e40");
    private static final Color POINT_B_FILL = Color.web("#ef444440");
    private static final Color INC_OPEN = Color.web("#f59e0b");
    private static final Color INC_CRI = Color.web("#ef4444");
    private static final Color INC_MIN = Color.web("#22c55e");
    private static final Color TEXT_MUTED = Color.web("#555d75");
    private static final Color TEXT_ROAD = Color.web("#7882aa");
    private static final Color LT_GREEN = Color.web("#22c55e");
    private static final Color LT_YELLOW = Color.web("#f59e0b");
    private static final Color LT_RED = Color.web("#ef4444");
    private static final Color LT_BG = Color.web("#0d0f14");
    private static final Color LT_SELECTED_RING = Color.web("#ffffff");
    private static final Color LT_SELECTED_GLOW = Color.web("#ffffff40");
    private static final Color WHITE = Color.WHITE;
    private static final Color DENS_MID = Color.web("#503510");
    private static final Color DENS_HIGH = Color.web("#5a1414");
    private static final Color HINT_INC_BG = Color.web("#f59e0bd9");
    private static final Color HINT_A_BG = Color.web("#22c55ed9");
    private static final Color HINT_B_BG = Color.web("#ef4444d9");
    private static final Color HINT_FG = Color.web("#0d0f14");
    private static final Color EV_GLOW_OUTER = Color.web("#00e5ff18");
    private static final Color EV_GLOW_MID   = Color.web("#00e5ff40");
    private static final Color EV_GLOW_INNER  = Color.web("#00e5ff70");

    //fuentes pre-cacheadas
    private static final Font FONT_ROAD = Font.font("System", 10);
    private static final Font FONT_LABEL = Font.font("System", FontWeight.BOLD, 11);
    private static final Font FONT_HINT = Font.font("System", 12);
    private static final Font FONT_BIG = Font.font("System", 13);
    private static final Font FONT_SMALL = Font.font("System", 11);

    private static final double ROAD_MAIN_W = 28;
    private static final double ROAD_SIDE_W = 16;
    private static final double EDGE_HIT = 14;
    private static final double LANE_SEP = 7.0;

    //tabla de colores de densidad pre-calculada: evita new Color() en cada frame
    //20 pasos cubre la precision necesaria sin coste de interpolación en tiempo real
    private static final int DENS_STEPS = 20;
    private static final Color[] DENS_CACHE_MAIN = buildDensCache(true);
    private static final Color[] DENS_CACHE_SIDE = buildDensCache(false);

    private static Color[] buildDensCache(boolean main) {
        Color base = main ? ROAD_MAIN : ROAD_SIDE;
        Color[] cache = new Color[DENS_STEPS + 1];
        for (int i = 0; i <= DENS_STEPS; i++) {
            double d = i / (double) DENS_STEPS;
            if (d < 0.4) cache[i] = base;
            else if (d < 0.7) cache[i] = lerpStatic(base, DENS_MID, (d - 0.4) / 0.3);
            else cache[i] = lerpStatic(DENS_MID, DENS_HIGH, (d - 0.7) / 0.3);
        }
        return cache;
    }

    private static Color lerpStatic(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return Color.color(
                a.getRed()   + (b.getRed()   - a.getRed())   * t,
                a.getGreen() + (b.getGreen() - a.getGreen()) * t,
                a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
    }

    private SimState simState = null;
    private List<Incident> incidents = new ArrayList<>();
    //listas pre-divididas para no separar main/side en cada frame
    private List<TrafficEdge> mainEdgesCache = new ArrayList<>();
    private List<TrafficEdge> sideEdgesCache = new ArrayList<>();

    private String pointAId = null, pointBId = null;
    private double pointAX = Double.NaN, pointAY = Double.NaN;
    private double pointBX = Double.NaN, pointBY = Double.NaN;

    private List<String> routeNodes = new ArrayList<>();
    private double offsetX = 50, offsetY = 50, scale = 1.0;
    private double dragStartX, dragStartY, dragOffX, dragOffY;
    private boolean isDragging = false;

    private boolean placingIncident = false;
    private boolean placingPointA = false;
    private boolean placingPointB = false;
    private Incident selectedInc = null;
    private TrafficLight selectedLight = null;

    private BiConsumer<Double, Double> onIncidentPlaced;
    private Consumer<double[]> onPointAPlaced;
    private Consumer<double[]> onPointBPlaced;
    private Consumer<TrafficNode> onNodeRightClicked;
    private Consumer<double[]> onRoadRightClicked;
    private Consumer<Incident> onIncidentSelected;
    private Consumer<TrafficLight> onLightClicked;

    private volatile boolean dirty = true;

    //set reutilizado para nombres de calle: sin new HashSet por frame
    private final Set<String> drawnNames = new HashSet<>();

    public MapCanvas() {
        setupInput();
        startRenderLoop();
        widthProperty().addListener((o, v, n) -> dirty = true);
        heightProperty().addListener((o, v, n) -> dirty = true);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double h) {
        return getWidth();
    }

    @Override
    public double prefHeight(double w) {
        return getHeight();
    }

    //api publica

    public void setState(SimState s) {
        simState = s;
        mainEdgesCache.clear();
        sideEdgesCache.clear();
        if (s != null) {
            for (TrafficEdge e : s.getEdges()) {
                if (e.isMain()) mainEdgesCache.add(e); else sideEdgesCache.add(e);
            }
        }
        dirty = true;
    }

    //actualiza solo el estado sin reconstruir los caches de aristas
    //usar para ticks de estado normales donde el mapa no cambia
    public void markDirty() {
        dirty = true;
    }

    public void setIncidents(List<Incident> list) {
        incidents = list;
        dirty = true;
    }

    public void setRouteNodes(List<String> r) {
        routeNodes = r;
        dirty = true;
    }

    public void setPlacingIncident(boolean p) {
        placingIncident = p;
        dirty = true;
    }

    public void setPlacingPointA(boolean p) {
        placingPointA = p;
        dirty = true;
    }

    public void setPlacingPointB(boolean p) {
        placingPointB = p;
        dirty = true;
    }

    public boolean isPlacingIncident() { return placingIncident; }
    public boolean isPlacingPointA()   { return placingPointA; }
    public boolean isPlacingPointB()   { return placingPointB; }

    public double getScaleValue() { return scale; }
    public double getOffsetX()    { return offsetX; }
    public double getOffsetY()    { return offsetY; }

    public void setSelectedIncident(Incident i) {
        selectedInc = i;
        dirty = true;
    }

    public void setSelectedLight(TrafficLight lt) {
        selectedLight = lt;
        dirty = true;
    }

    public void setPointA(String id, double x, double y) {
        pointAId = id; pointAX = x; pointAY = y;
        dirty = true;
    }

    public void setPointB(String id, double x, double y) {
        pointBId = id; pointBX = x; pointBY = y;
        dirty = true;
    }

    public void clearPoints() {
        pointAId = pointBId = null;
        pointAX = pointAY = pointBX = pointBY = Double.NaN;
        dirty = true;
    }

    public double getPointAX() { return pointAX; }
    public double getPointAY() { return pointAY; }
    public double getPointBX() { return pointBX; }
    public double getPointBY() { return pointBY; }
    public String getPointAId() { return pointAId; }
    public String getPointBId() { return pointBId; }

    public void setOnIncidentPlaced(BiConsumer<Double, Double> cb) { onIncidentPlaced = cb; }
    public void setOnPointAPlaced(Consumer<double[]> cb)           { onPointAPlaced = cb; }
    public void setOnPointBPlaced(Consumer<double[]> cb)           { onPointBPlaced = cb; }
    public void setOnNodeRightClicked(Consumer<TrafficNode> cb)    { onNodeRightClicked = cb; }
    public void setOnRoadRightClicked(Consumer<double[]> cb)       { onRoadRightClicked = cb; }
    public void setOnIncidentSelected(Consumer<Incident> cb)       { onIncidentSelected = cb; }
    public void setOnLightClicked(Consumer<TrafficLight> cb)       { onLightClicked = cb; }

    public double[] canvasToSim(double cx, double cy) {
        return new double[]{(cx - offsetX) / scale, (cy - offsetY) / scale};
    }

    //prueba de choque

    //siempre devuelve el nodo mas cercano, sin umbral, para snapping de puntos A/B
    public TrafficNode findNearestNode(double sx, double sy) {
        if (simState == null) return null;
        TrafficNode best = null;
        double min = Double.MAX_VALUE;
        for (TrafficNode n : simState.getNodes()) {
            double d = Math.hypot(n.getX() - sx, n.getY() - sy);
            if (d < min) { min = d; best = n; }
        }
        return best;
    }

    //con umbral: para deteccion de clic en nodo (no snap)
    public TrafficNode findNearestNode(double sx, double sy, double thresh) {
        if (simState == null) return null;
        TrafficNode best = null;
        double min = Double.MAX_VALUE;
        for (TrafficNode n : simState.getNodes()) {
            double d = Math.hypot(n.getX() - sx, n.getY() - sy);
            if (d < min) { min = d; best = n; }
        }
        return min <= thresh ? best : null;
    }

    //busca el semaforo mas cercano al punto de canvas dado (coordenadas de canvas, no sim)
    //radio de deteccion en pixels de canvas para que funcione bien a cualquier zoom
    private TrafficLight findNearestLight(double cx, double cy) {
        if (simState == null) return null;
        final double dist = 15 * scale;
        final double hitRadius = 10 * scale;
        TrafficLight best = null;
        double minDist = hitRadius;
        for (TrafficNode n : simState.getNodes()) {
            List<TrafficLight> lts = simState.findLightsAtNode(n.getId());
            if (lts == null) continue;
            double nx = n.getX() * scale + offsetX;
            double ny = n.getY() * scale + offsetY;
            for (TrafficLight lt : lts) {
                double[] off = dirOff(lt.getDir(), dist);
                double lx = nx + off[0];
                double ly = ny + off[1];
                double d = Math.hypot(cx - lx, cy - ly);
                if (d < minDist) { minDist = d; best = lt; }
            }
        }
        return best;
    }

    private double[] project(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay, len2 = dx * dx + dy * dy;
        if (len2 < 1e-6) return new double[]{Double.MAX_VALUE, ax, ay};
        double t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
        double qx = ax + t * dx, qy = ay + t * dy;
        return new double[]{Math.hypot(px - qx, py - qy), qx, qy};
    }

    private Object[] nearestEdgePoint(double sx, double sy) {
        if (simState == null) return null;
        TrafficEdge best = null;
        double bd = EDGE_HIT;
        double bx = 0, by = 0;
        for (TrafficEdge e : simState.getEdges()) {
            TrafficNode a = simState.findNode(e.getFrom());
            TrafficNode b = simState.findNode(e.getTo());
            if (a == null || b == null) continue;
            double[] p = project(sx, sy, a.getX(), a.getY(), b.getX(), b.getY());
            if (p[0] < bd) { bd = p[0]; best = e; bx = p[1]; by = p[2]; }
        }
        return best != null ? new Object[]{best, bx, by} : null;
    }

    private Incident nearestIncident(double cx, double cy) {
        for (Incident i : incidents) {
            double px = i.getMapX() * scale + offsetX, py = i.getMapY() * scale + offsetY;
            if (Math.hypot(px - cx, py - cy) < 14) return i;
        }
        return null;
    }

    //input
    private void setupInput() {
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX = e.getX(); dragStartY = e.getY();
                dragOffX = offsetX;    dragOffY = offsetY;
                isDragging = false;
            }
        });
        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double dx = e.getX() - dragStartX, dy = e.getY() - dragStartY;
                if (Math.hypot(dx, dy) > 4) isDragging = true;
                offsetX = dragOffX + dx;
                offsetY = dragOffY + dy;
                dirty = true;
            }
        });
        setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.12 : 0.89;
            double ns = Math.max(0.3, Math.min(8.0, scale * factor));
            double mx = e.getX(), my = e.getY();
            offsetX = mx - (mx - offsetX) * (ns / scale);
            offsetY = my - (my - offsetY) * (ns / scale);
            scale = ns;
            dirty = true;
        });
        setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || isDragging) return;
            double[] sim = canvasToSim(e.getX(), e.getY());

            Incident nearInc = nearestIncident(e.getX(), e.getY());
            if (nearInc != null) {
                selectedInc = nearInc;
                dirty = true;
                if (onIncidentSelected != null) onIncidentSelected.accept(nearInc);
                return;
            }

            //deteccion de semaforo: antes de modos de colocacion
            TrafficLight nearLight = findNearestLight(e.getX(), e.getY());
            if (nearLight != null && !placingIncident && !placingPointA && !placingPointB) {
                selectedLight = nearLight;
                dirty = true;
                if (onLightClicked != null) onLightClicked.accept(nearLight);
                return;
            }

            if (placingIncident) {
                Object[] hit = nearestEdgePoint(sim[0], sim[1]);
                double px = hit != null ? (double) hit[1] : sim[0];
                double py = hit != null ? (double) hit[2] : sim[1];
                placingIncident = false;
                dirty = true;
                if (onIncidentPlaced != null) onIncidentPlaced.accept(px, py);
                return;
            }
            if (placingPointA) { placePoint(sim[0], sim[1], true);  return; }
            if (placingPointB) { placePoint(sim[0], sim[1], false); }
        });
        setOnContextMenuRequested(e -> {
            double[] sim = canvasToSim(e.getX(), e.getY());
            TrafficNode node = findNearestNode(sim[0], sim[1], 30);
            if (node != null) {
                if (onNodeRightClicked != null) onNodeRightClicked.accept(node);
            } else {
                Object[] hit = nearestEdgePoint(sim[0], sim[1]);
                double[] coords = hit != null ? new double[]{(double) hit[1], (double) hit[2]} : sim;
                if (onRoadRightClicked != null) onRoadRightClicked.accept(coords);
            }
        });
    }

    //puntos A/B siempre snapean al nodo mas cercano, sin colocacion libre
    private void placePoint(double sx, double sy, boolean isA) {
        //solo permite colocar si el clic cae cerca de un nodo real
        TrafficNode n = findNearestNode(sx, sy, 20);
        if (n == null) return;
        if (isA) {
            setPointA(n.getId(), n.getX(), n.getY());
            placingPointA = false;
            if (onPointAPlaced != null) onPointAPlaced.accept(new double[]{n.getX(), n.getY()});
        } else {
            setPointB(n.getId(), n.getX(), n.getY());
            placingPointB = false;
            if (onPointBPlaced != null) onPointBPlaced.accept(new double[]{n.getX(), n.getY()});
        }
        dirty = true;
    }

    //render loop
    private void startRenderLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (dirty) {
                    render();
                    dirty = false;
                }
            }
        }.start();
    }

    private void render() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        gc.setFill(BG);
        gc.fillRect(0, 0, w, h);
        if (simState == null || simState.getNodes().isEmpty()) {
            drawNoConn(gc, w, h);
            return;
        }

        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);

        drawEdges(gc);
        drawRoute(gc);
        drawNodes(gc);
        drawLights(gc);
        drawCars(gc);
        drawEV(gc);
        drawPins(gc);
        drawMarkers(gc);

        gc.restore();

        drawHint(gc, w, h);
    }

    //dibujo
    private void drawEdges(GraphicsContext gc) {
        int totalEdges = mainEdgesCache.size() + sideEdgesCache.size();
        if (totalEdges == 0) return;

        //pre-resolver coordenadas de nodos para evitar lookups repetidos por pasada
        double[] sxA = new double[sideEdgesCache.size()], syA = new double[sideEdgesCache.size()];
        double[] sxB = new double[sideEdgesCache.size()], syB = new double[sideEdgesCache.size()];
        boolean[] sValid = new boolean[sideEdgesCache.size()];
        for (int i = 0; i < sideEdgesCache.size(); i++) {
            TrafficEdge e = sideEdgesCache.get(i);
            TrafficNode a = simState.findNode(e.getFrom()), b = simState.findNode(e.getTo());
            sValid[i] = a != null && b != null;
            if (sValid[i]) { sxA[i] = a.getX(); syA[i] = a.getY(); sxB[i] = b.getX(); syB[i] = b.getY(); }
        }
        double[] mxA = new double[mainEdgesCache.size()], myA = new double[mainEdgesCache.size()];
        double[] mxB = new double[mainEdgesCache.size()], myB = new double[mainEdgesCache.size()];
        boolean[] mValid = new boolean[mainEdgesCache.size()];
        for (int i = 0; i < mainEdgesCache.size(); i++) {
            TrafficEdge e = mainEdgesCache.get(i);
            TrafficNode a = simState.findNode(e.getFrom()), b = simState.findNode(e.getTo());
            mValid[i] = a != null && b != null;
            if (mValid[i]) { mxA[i] = a.getX(); myA[i] = a.getY(); mxB[i] = b.getX(); myB[i] = b.getY(); }
        }

        gc.setLineDashes((double[]) null);

        //pasada 1: borde de secundarias
        gc.setStroke(ROAD_BORDER);
        gc.setLineWidth(ROAD_SIDE_W + 4);
        for (int i = 0; i < sideEdgesCache.size(); i++) {
            if (sValid[i]) gc.strokeLine(sxA[i], syA[i], sxB[i], syB[i]);
        }
        //pasada 2: borde de principales
        gc.setLineWidth(ROAD_MAIN_W + 4);
        for (int i = 0; i < mainEdgesCache.size(); i++) {
            if (mValid[i]) gc.strokeLine(mxA[i], myA[i], mxB[i], myB[i]);
        }
        //pasada 3: cuerpo de secundarias
        gc.setLineWidth(ROAD_SIDE_W);
        for (int i = 0; i < sideEdgesCache.size(); i++) {
            if (!sValid[i]) continue;
            gc.setStroke(densColor(false, sideEdgesCache.get(i).getDensity()));
            gc.strokeLine(sxA[i], syA[i], sxB[i], syB[i]);
        }
        //pasada 4: cuerpo de principales
        gc.setLineWidth(ROAD_MAIN_W);
        for (int i = 0; i < mainEdgesCache.size(); i++) {
            if (!mValid[i]) continue;
            gc.setStroke(densColor(true, mainEdgesCache.get(i).getDensity()));
            gc.strokeLine(mxA[i], myA[i], mxB[i], myB[i]);
        }

        //pasada 5: lineas de carril
        if (scale > 0.55) {
            gc.setStroke(LANE_LINE);
            gc.setLineWidth(0.7);
            gc.setLineDashes(8, 6);
            for (int i = 0; i < mainEdgesCache.size(); i++) {
                if (!mValid[i]) continue;
                TrafficEdge e = mainEdgesCache.get(i);
                if (e.getLanes() <= 1) continue;
                laneLines(gc, mxA[i], myA[i], mxB[i], myB[i], e.getLanes());
            }
            gc.setLineDashes((double[]) null);
        }

        //pasada 6: nombres de calle
        if (scale >= 0.5) {
            gc.setFont(FONT_ROAD);
            gc.setFill(TEXT_ROAD);
            gc.setTextAlign(TextAlignment.CENTER);
            drawnNames.clear();
            for (int i = 0; i < sideEdgesCache.size(); i++) {
                if (sValid[i]) drawEdgeName(gc, sideEdgesCache.get(i), sxA[i], syA[i], sxB[i], syB[i], false);
            }
            for (int i = 0; i < mainEdgesCache.size(); i++) {
                if (mValid[i]) drawEdgeName(gc, mainEdgesCache.get(i), mxA[i], myA[i], mxB[i], myB[i], true);
            }
        }
    }

    //dibuja el nombre de la calle rotado sin gc.save/restore:
    //calcula el origen del texto manualmente con sin/cos igual que drawCars hace con los coches
    private void drawEdgeName(GraphicsContext gc, TrafficEdge e,
                              double x1, double y1, double x2, double y2, boolean main) {
        String name = e.getName();
        if (name == null || name.isBlank()) return;
        String key = name + e.getFrom() + e.getTo();
        String rev = name + e.getTo() + e.getFrom();
        if (drawnNames.contains(key) || drawnNames.contains(rev)) return;
        drawnNames.add(key);
        drawnNames.add(rev);
        double dx = x2 - x1, dy = y2 - y1, len = Math.hypot(dx, dy);
        if (len < 40) return;
        double mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
        double ang = Math.toDegrees(Math.atan2(dy, dx));
        if (ang > 90 || ang < -90) ang += 180;
        //save/restore necesario solo para el rotate del texto, no se puede evitar aqui,
        //pero solo se ejecuta cuando scale >= 0.5 y el nombre no fue dibujado ya
        gc.save();
        gc.translate(mx, my);
        gc.rotate(ang);
        gc.fillText(name, 0, -(main ? ROAD_MAIN_W : ROAD_SIDE_W) / 2.0 - 3);
        gc.restore();
    }

    private Color densColor(boolean main, double d) {
        int idx = (int) Math.max(0, Math.min(DENS_STEPS, d * DENS_STEPS));
        return main ? DENS_CACHE_MAIN[idx] : DENS_CACHE_SIDE[idx];
    }

    private void laneLines(GraphicsContext gc, double x1, double y1, double x2, double y2, int lanes) {
        double dx = x2 - x1, dy = y2 - y1, len = Math.hypot(dx, dy);
        if (len < 1) return;
        double nx = -dy / len, ny = dx / len;
        for (int i = 1; i < lanes; i++) {
            double off = (i - lanes / 2.0) * 5.5;
            gc.strokeLine(x1 + nx * off, y1 + ny * off, x2 + nx * off, y2 + ny * off);
        }
    }

    private void drawRoute(GraphicsContext gc) {
        if (routeNodes.size() < 2) return;
        gc.setStroke(ROUTE_COLOR);
        gc.setLineWidth(8);
        for (int i = 0; i < routeNodes.size() - 1; i++) {
            TrafficNode a = simState.findNode(routeNodes.get(i)), b = simState.findNode(routeNodes.get(i + 1));
            if (a != null && b != null) gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
        }
    }

    private void drawNodes(GraphicsContext gc) {
        boolean highlighting = placingPointA || placingPointB;
        Color highlightColor = placingPointA ? POINT_A : POINT_B;
        gc.setLineWidth(1);
        for (TrafficNode n : simState.getNodes()) {
            double r = n.isMain() ? 9 : 5;
            gc.setFill(NODE_FILL);
            gc.fillOval(n.getX() - r, n.getY() - r, r * 2, r * 2);
            gc.setStroke(NODE_STROKE);
            gc.strokeOval(n.getX() - r, n.getY() - r, r * 2, r * 2);
            //anillo de guia cuando el usuario esta colocando un punto A o B
            if (highlighting) {
                gc.setStroke(highlightColor);
                gc.setLineWidth(2);
                gc.strokeOval(n.getX() - r - 4, n.getY() - r - 4, (r + 4) * 2, (r + 4) * 2);
                gc.setLineWidth(1);
            }
        }
    }

    private void drawLights(GraphicsContext gc) {
        if (scale < 0.45) return;
        final double dist = 15, r = 4;
        for (TrafficNode n : simState.getNodes()) {
            List<TrafficLight> lts = simState.findLightsAtNode(n.getId());
            if (lts == null || lts.isEmpty()) continue;
            for (TrafficLight lt : lts) {
                double[] off = dirOff(lt.getDir(), dist);
                double lx = n.getX() + off[0], ly = n.getY() + off[1];
                gc.setFill(LT_BG);
                gc.fillOval(lx - r - 1, ly - r - 1, (r + 1) * 2, (r + 1) * 2);
                gc.setFill(ltColor(lt.getState()));
                gc.fillOval(lx - r, ly - r, r * 2, r * 2);

                //anillo de selección si este semáforo está seleccionado
                if (selectedLight != null && selectedLight.getId().equals(lt.getId())) {
                    gc.setStroke(LT_SELECTED_GLOW);
                    gc.setLineWidth(4);
                    gc.strokeOval(lx - r - 4, ly - r - 4, (r + 4) * 2, (r + 4) * 2);
                    gc.setStroke(LT_SELECTED_RING);
                    gc.setLineWidth(1.5);
                    gc.strokeOval(lx - r - 3, ly - r - 3, (r + 3) * 2, (r + 3) * 2);
                }
            }
        }
    }

    private double[] dirOff(String d, double dist) {
        return switch (d != null ? d : "N") {
            case "N" -> new double[]{0, -dist};
            case "S" -> new double[]{0, dist};
            case "E" -> new double[]{dist, 0};
            case "W" -> new double[]{-dist, 0};
            default  -> new double[]{0, 0};
        };
    }

    private Color ltColor(String s) {
        if (s == null) return LT_RED;
        return switch (s) {
            case "green"  -> LT_GREEN;
            case "yellow" -> LT_YELLOW;
            default       -> LT_RED;
        };
    }

    private void drawCars(GraphicsContext gc) {
        gc.setFill(CAR_COLOR);
        for (SimCar car : simState.getCars()) {
            TrafficNode a = simState.findNode(car.getNodeA()), b = simState.findNode(car.getNodeB());
            if (a == null || b == null) {
                gc.fillRect(car.getX() - 4, car.getY() - 2, 8, 4);
                continue;
            }
            double angle = Math.atan2(b.getY() - a.getY(), b.getX() - a.getX());
            double nx = -Math.sin(angle), ny = Math.cos(angle);
            double off = car.getLane() == 1 ? -LANE_SEP : LANE_SEP;
            double fx = car.getX() + nx * off, fy = car.getY() + ny * off;
            double cos = Math.cos(angle), sin = Math.sin(angle);
            double[] px = {
                    fx + 4.5*cos - 2.0*sin,
                    fx - 4.5*cos - 2.0*sin,
                    fx - 4.5*cos + 2.0*sin,
                    fx + 4.5*cos + 2.0*sin
            };
            double[] py = {
                    fy + 4.5*sin + 2.0*cos,
                    fy - 4.5*sin + 2.0*cos,
                    fy - 4.5*sin - 2.0*cos,
                    fy + 4.5*sin - 2.0*cos
            };
            gc.fillPolygon(px, py, 4);
        }
    }

    private void drawEV(GraphicsContext gc) {
        if (!simState.isEvActive()) return;
        double ex = simState.getEvX(), ey = simState.getEvY();
        gc.setFill(EV_GLOW_OUTER);
        gc.fillOval(ex - 18, ey - 12, 36, 24);
        gc.setFill(EV_GLOW_MID);
        gc.fillOval(ex - 13, ey - 9, 26, 18);
        gc.setFill(EV_GLOW_INNER);
        gc.fillOval(ex - 9, ey - 6, 18, 12);
        gc.setFill(EV_COLOR);
        gc.fillRoundRect(ex - 7, ey - 4, 14, 8, 3, 3);
        gc.setStroke(WHITE);
        gc.setLineWidth(1.5);
        gc.strokeLine(ex - 3, ey - 9, ex + 3, ey - 9);
        gc.strokeLine(ex, ey - 12, ex, ey - 6);
    }

    private void drawPins(GraphicsContext gc) {
        for (Incident i : incidents) {
            Color col = switch (i.getEstado().toLowerCase()) {
                case "critico", "crítico" -> INC_CRI;
                case "resuelto" -> INC_MIN;
                default -> INC_OPEN;
            };
            boolean sel = selectedInc != null && selectedInc.getId() == i.getId();
            double r = sel ? 9 : 7, ix = i.getMapX(), iy = i.getMapY();
            gc.setFill(col);
            gc.fillOval(ix - r, iy - r - 12, r * 2, r * 2);
            gc.fillPolygon(new double[]{ix - 5, ix + 5, ix}, new double[]{iy - 12, iy - 12, iy - 2}, 3);
            if (sel) {
                gc.setStroke(WHITE);
                gc.setLineWidth(1.5);
                gc.strokeOval(ix - r, iy - r - 12, r * 2, r * 2);
            }
        }
    }

    private void drawMarkers(GraphicsContext gc) {
        if (!Double.isNaN(pointAX)) drawMarker(gc, pointAX, pointAY, POINT_A, POINT_A_FILL, "A");
        if (!Double.isNaN(pointBX)) drawMarker(gc, pointBX, pointBY, POINT_B, POINT_B_FILL, "B");
    }

    private void drawMarker(GraphicsContext gc, double x, double y, Color stroke, Color fill, String lbl) {
        gc.setFill(fill);
        gc.fillOval(x - 14, y - 14, 28, 28);
        gc.setStroke(stroke);
        gc.setLineWidth(2);
        gc.strokeOval(x - 14, y - 14, 28, 28);
        gc.setFill(stroke);
        gc.setFont(FONT_LABEL);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(lbl, x, y + 4);
    }

    private void drawNoConn(GraphicsContext gc, double w, double h) {
        gc.setFill(TEXT_MUTED);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(FONT_BIG);
        gc.fillText("Sin conexion con la simulacion", w / 2, h / 2 - 10);
        gc.setFont(FONT_SMALL);
        gc.fillText("Conecta desde el panel lateral para cargar el mapa", w / 2, h / 2 + 12);
    }

    private void drawHint(GraphicsContext gc, double w, double h) {
        String hint = null;
        Color bg = HINT_INC_BG;
        if (placingIncident) hint = "Haz clic en cualquier punto de una calle para colocar el incidente";
        else if (placingPointA) { hint = "Haz clic sobre una interseccion resaltada para marcar el Punto A"; bg = HINT_A_BG; }
        else if (placingPointB) { hint = "Haz clic sobre una interseccion resaltada para marcar el Punto B"; bg = HINT_B_BG; }
        if (hint == null) return;
        gc.setFill(bg);
        gc.fillRoundRect(12, h - 36, hint.length() * 6.5 + 16, 26, 6, 6);
        gc.setFill(HINT_FG);
        gc.setFont(FONT_HINT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(hint, 20, h - 18);
    }
}