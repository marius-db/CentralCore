package com.centralcore.modules.trafficmodule.model;

public class TrafficNode {

    private String id;
    private double x;
    private double y;
    private boolean main;

    public TrafficNode() {
    }

    public TrafficNode(String id, double x, double y, boolean main) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.main = main;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean isMain() {
        return main;
    }

    public void setMain(boolean m) {
        this.main = m;
    }
}
