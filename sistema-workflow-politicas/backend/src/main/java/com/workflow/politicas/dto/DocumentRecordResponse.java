package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class DocumentRecordResponse {

    private String id;
    private String documentFamilyId;
    private String repositoryId;
    private String tramiteId;
    private String tramiteCodigo;
    private String nombreArchivo;
    private String nombreOriginal;
    private String extension;
    private String contentType;
    private long tamano;
    private String s3Key;
    private String bucket;
    private int version;
    private LocalDateTime fechaSubida;
    private String subidoPor;
    private String estado;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumentFamilyId() {
        return documentFamilyId;
    }

    public void setDocumentFamilyId(String documentFamilyId) {
        this.documentFamilyId = documentFamilyId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
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

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getTamano() {
        return tamano;
    }

    public void setTamano(long tamano) {
        this.tamano = tamano;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public String getSubidoPor() {
        return subidoPor;
    }

    public void setSubidoPor(String subidoPor) {
        this.subidoPor = subidoPor;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
