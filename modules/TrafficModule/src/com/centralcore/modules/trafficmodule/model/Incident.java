package com.centralcore.modules.trafficmodule.model;

import java.time.LocalDateTime;

public class Incident {

    private int id;
    private String tipo;
    private String descripcion;
    private double mapX;
    private double mapY;
    private String estado;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

    public Incident() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String t) {
        this.tipo = t;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String d) {
        this.descripcion = d;
    }

    public double getMapX() {
        return mapX;
    }

    public void setMapX(double x) {
        this.mapX = x;
    }

    public double getMapY() {
        return mapY;
    }

    public void setMapY(double y) {
        this.mapY = y;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String e) {
        this.estado = e;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createdAt = t;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime t) {
        this.closedAt = t;
    }

    //para mostrar en el listview
    @Override
    public String toString() {
        return "[" + estado + "] " + tipo;
    }
}
