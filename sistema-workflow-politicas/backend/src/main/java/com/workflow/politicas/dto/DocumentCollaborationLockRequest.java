package com.workflow.politicas.dto;

public class DocumentCollaborationLockRequest {
    private String sessionId;
    private String documentFamilyId;
    private String documentId;
    private String documentName;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getDocumentFamilyId() { return documentFamilyId; }
    public void setDocumentFamilyId(String documentFamilyId) { this.documentFamilyId = documentFamilyId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
}
