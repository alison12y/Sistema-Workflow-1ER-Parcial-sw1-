package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class DocumentDownloadResponse {

    private DocumentRecordResponse documento;
    private String presignedDownloadUrl;
    private LocalDateTime urlExpiraEn;
    private int urlExpiraEnMinutos;
    private String storageProvider;

    public DocumentRecordResponse getDocumento() {
        return documento;
    }

    public void setDocumento(DocumentRecordResponse documento) {
        this.documento = documento;
    }

    public String getPresignedDownloadUrl() {
        return presignedDownloadUrl;
    }

    public void setPresignedDownloadUrl(String presignedDownloadUrl) {
        this.presignedDownloadUrl = presignedDownloadUrl;
    }

    public LocalDateTime getUrlExpiraEn() {
        return urlExpiraEn;
    }

    public void setUrlExpiraEn(LocalDateTime urlExpiraEn) {
        this.urlExpiraEn = urlExpiraEn;
    }

    public int getUrlExpiraEnMinutos() {
        return urlExpiraEnMinutos;
    }

    public void setUrlExpiraEnMinutos(int urlExpiraEnMinutos) {
        this.urlExpiraEnMinutos = urlExpiraEnMinutos;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public void setStorageProvider(String storageProvider) {
        this.storageProvider = storageProvider;
    }
}
