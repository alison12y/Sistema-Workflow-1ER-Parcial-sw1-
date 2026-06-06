package com.workflow.politicas.dto;

import com.workflow.politicas.model.Tramite;

public class SmartAgentStartTramiteResponse {

    private Tramite tramite;
    private DocumentRecordResponse attachedDocument;
    private String message;

    public SmartAgentStartTramiteResponse() {
    }

    public Tramite getTramite() {
        return tramite;
    }

    public void setTramite(Tramite tramite) {
        this.tramite = tramite;
    }

    public DocumentRecordResponse getAttachedDocument() {
        return attachedDocument;
    }

    public void setAttachedDocument(DocumentRecordResponse attachedDocument) {
        this.attachedDocument = attachedDocument;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
