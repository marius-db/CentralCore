package com.centralcore.modules.trafficmodule.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IncidentUpdate {

    private int id;
    private int incidentId;
    private String estado;
    private String nota;
    private LocalDateTime createdAt;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public IncidentUpdate() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(int i) {
        this.incidentId = i;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String e) {
        this.estado = e;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String n) {
        this.nota = n;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createdAt = t;
    }

    @Override
    public String toString() {
        String ts = createdAt != null ? createdAt.format(FMT) : "—";
        return ts + "  [" + estado + "]" + (nota != null && !nota.isBlank() ? "  " + nota : "");
    }
}
