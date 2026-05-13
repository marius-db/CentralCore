package com.centralcore.modules.trafficmodule.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//estado completo de la simulación, actualizado en cada tick
public class SimState {

    private List<TrafficNode> nodes = new ArrayList<>();
    private List<TrafficEdge> edges = new ArrayList<>();
    private List<TrafficLight> lights = new ArrayList<>();
    private List<SimCar> cars = new ArrayList<>();

    //vehiculo de emergencia
    private boolean evActive = false;
    private double evX = 0;
    private double evY = 0;
    private String evNextNode = null;
    private List<String> evRoute = new ArrayList<>();
    private boolean routeDone = false;

    //índices para lookups rápidos
    private final Map<String, TrafficNode> nodeMap = new HashMap<>();
    private final Map<String, TrafficLight> lightById = new HashMap<>();
    //mapa nodeId -> lista de semáforos de esa intersección (4 por nodo)
    private final Map<String, List<TrafficLight>> lightsByNode = new HashMap<>();

    public void rebuildIndexes() {
        nodeMap.clear();
        lightById.clear();
        lightsByNode.clear();
        for (TrafficNode n : nodes) nodeMap.put(n.getId(), n);
        for (TrafficLight l : lights) {
            lightById.put(l.getId(), l);
            lightsByNode.computeIfAbsent(l.getNodeId(), k -> new ArrayList<>()).add(l);
        }
    }

    public TrafficNode findNode(String id) {
        return nodeMap.get(id);
    }

    public TrafficLight findLightById(String id) {
        return lightById.get(id);
    }

    //devuelve el semáforo de una dirección concreta en un nodo
    public TrafficLight findLightAtNodeDir(String nodeId, String dir) {
        List<TrafficLight> lts = lightsByNode.get(nodeId);
        if (lts == null) return null;
        for (TrafficLight lt : lts) {
            if (dir.equals(lt.getDir())) return lt;
        }
        return null;
    }

    //devuelve todos los semáforos de un nodo (para override masivo de la intersección)
    public List<TrafficLight> findLightsAtNode(String nodeId) {
        List<TrafficLight> lts = lightsByNode.get(nodeId);
        return lts != null ? lts : new ArrayList<>();
    }

    //getters / setters

    public List<TrafficNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<TrafficNode> n) {
        this.nodes = n;
    }

    public List<TrafficEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<TrafficEdge> e) {
        this.edges = e;
    }

    public List<TrafficLight> getLights() {
        return lights;
    }

    public void setLights(List<TrafficLight> l) {
        this.lights = l;
    }

    public List<SimCar> getCars() {
        return cars;
    }

    public void setCars(List<SimCar> c) {
        this.cars = c;
    }

    public boolean isEvActive() {
        return evActive;
    }

    public void setEvActive(boolean b) {
        this.evActive = b;
    }

    public double getEvX() {
        return evX;
    }

    public void setEvX(double x) {
        this.evX = x;
    }

    public double getEvY() {
        return evY;
    }

    public void setEvY(double y) {
        this.evY = y;
    }

    public String getEvNextNode() {
        return evNextNode;
    }

    public void setEvNextNode(String n) {
        this.evNextNode = n;
    }

    public List<String> getEvRoute() {
        return evRoute;
    }

    public void setEvRoute(List<String> r) {
        this.evRoute = r;
    }

    public boolean isRouteDone() {
        return routeDone;
    }

    public void setRouteDone(boolean b) {
        this.routeDone = b;
    }
}