package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class DocumentCollaborationActiveLockDto {
    private String documentFamilyId;
    private String documentId;
    private String documentName;
    private String userId;
    private String username;
    private String displayName;
    private LocalDateTime lockedAt;

    public String getDocumentFamilyId() { return documentFamilyId; }
    public void setDocumentFamilyId(String documentFamilyId) { this.documentFamilyId = documentFamilyId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
}
