package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class DocumentRepositoryResponse {

    private String id;
    private String tramiteId;
    private String tramiteCodigo;
    private String nombre;
    private String descripcion;
    private LocalDateTime fechaCreacion;
    private String creadoPor;
    private String estado;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTramiteId() {
        return tramiteId;
    }

    public void setTramiteId(String tramiteId) {
        this.tramiteId = tramiteId;
    }

    public String getTramiteCodigo() {
        return tramiteCodigo;
    }

    public void setTramiteCodigo(String tramiteCodigo) {
        this.tramiteCodigo = tramiteCodigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
