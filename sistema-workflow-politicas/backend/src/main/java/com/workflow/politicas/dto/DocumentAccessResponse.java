package com.workflow.politicas.dto;

public class DocumentAccessResponse {
    private String documentId;
    private String documentFamilyId;
    private String permissionLevel;
    private boolean locked;
    private String lockedByUsername;
    private String lockedByDisplayName;
    private boolean lockHeldByCurrentUser;
    private boolean canRead;
    private boolean canEdit;
    private boolean canAdmin;
    private boolean canUploadVersion;

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getDocumentFamilyId() { return documentFamilyId; }
    public void setDocumentFamilyId(String documentFamilyId) { this.documentFamilyId = documentFamilyId; }
    public String getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(String permissionLevel) { this.permissionLevel = permissionLevel; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public String getLockedByUsername() { return lockedByUsername; }
    public void setLockedByUsername(String lockedByUsername) { this.lockedByUsername = lockedByUsername; }
    public String getLockedByDisplayName() { return lockedByDisplayName; }
    public void setLockedByDisplayName(String lockedByDisplayName) { this.lockedByDisplayName = lockedByDisplayName; }
    public boolean isLockHeldByCurrentUser() { return lockHeldByCurrentUser; }
    public void setLockHeldByCurrentUser(boolean lockHeldByCurrentUser) { this.lockHeldByCurrentUser = lockHeldByCurrentUser; }
    public boolean isCanRead() { return canRead; }
    public void setCanRead(boolean canRead) { this.canRead = canRead; }
    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }
    public boolean isCanAdmin() { return canAdmin; }
    public void setCanAdmin(boolean canAdmin) { this.canAdmin = canAdmin; }
    public boolean isCanUploadVersion() { return canUploadVersion; }
    public void setCanUploadVersion(boolean canUploadVersion) { this.canUploadVersion = canUploadVersion; }
}
