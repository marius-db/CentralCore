package com.centralcore.modules.citizenmodule;

import java.time.LocalDateTime;

public class CitizenDocument {

    private int id;
    private int citizenId;
    private String tipoDocumento;
    private String nombreArchivo;
    private String rutaArchivo;
    private LocalDateTime subidoEn;

    public CitizenDocument() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(int citizenId) {
        this.citizenId = citizenId;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public void setSubidoEn(LocalDateTime subidoEn) {
        this.subidoEn = subidoEn;
    }

    @Override
    public String toString() {
        return nombreArchivo;
    }
}