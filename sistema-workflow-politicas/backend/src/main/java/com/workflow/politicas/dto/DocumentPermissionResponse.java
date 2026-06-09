package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class DocumentPermissionResponse {
    private String id;
    private String documentFamilyId;
    private String granteeType;
    private String granteeKey;
    private String granteeLabel;
    private String permissionLevel;
    private String grantedBy;
    private LocalDateTime grantedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDocumentFamilyId() { return documentFamilyId; }
    public void setDocumentFamilyId(String documentFamilyId) { this.documentFamilyId = documentFamilyId; }
    public String getGranteeType() { return granteeType; }
    public void setGranteeType(String granteeType) { this.granteeType = granteeType; }
    public String getGranteeKey() { return granteeKey; }
    public void setGranteeKey(String granteeKey) { this.granteeKey = granteeKey; }
    public String getGranteeLabel() { return granteeLabel; }
    public void setGranteeLabel(String granteeLabel) { this.granteeLabel = granteeLabel; }
    public String getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(String permissionLevel) { this.permissionLevel = permissionLevel; }
    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
}
