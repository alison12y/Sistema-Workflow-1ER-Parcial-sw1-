package com.workflow.politicas.dto;

public class DocumentCollaborationConnectedUserDto {
    private String userId;
    private String username;
    private String displayName;
    private int sessionCount;
    private String viewingDocumentFamilyId;
    private String viewingDocumentName;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public int getSessionCount() { return sessionCount; }
    public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }
    public String getViewingDocumentFamilyId() { return viewingDocumentFamilyId; }
    public void setViewingDocumentFamilyId(String viewingDocumentFamilyId) { this.viewingDocumentFamilyId = viewingDocumentFamilyId; }
    public String getViewingDocumentName() { return viewingDocumentName; }
    public void setViewingDocumentName(String viewingDocumentName) { this.viewingDocumentName = viewingDocumentName; }
}
