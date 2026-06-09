package com.workflow.politicas.model;

import java.time.LocalDateTime;

public class DocumentEditorSession {
    private String sessionId;
    private String userId;
    private String username;
    private String displayName;
    private LocalDateTime openedAt;
    private LocalDateTime lastSeenAt;
    private String viewingDocumentFamilyId;
    private String viewingDocumentName;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public String getViewingDocumentFamilyId() { return viewingDocumentFamilyId; }
    public void setViewingDocumentFamilyId(String viewingDocumentFamilyId) { this.viewingDocumentFamilyId = viewingDocumentFamilyId; }
    public String getViewingDocumentName() { return viewingDocumentName; }
    public void setViewingDocumentName(String viewingDocumentName) { this.viewingDocumentName = viewingDocumentName; }
}
