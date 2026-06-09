package com.workflow.politicas.dto;

public class DocumentPermissionRequest {
    private String granteeType;
    private String granteeKey;
    private String granteeLabel;
    private String permissionLevel;

    public String getGranteeType() { return granteeType; }
    public void setGranteeType(String granteeType) { this.granteeType = granteeType; }
    public String getGranteeKey() { return granteeKey; }
    public void setGranteeKey(String granteeKey) { this.granteeKey = granteeKey; }
    public String getGranteeLabel() { return granteeLabel; }
    public void setGranteeLabel(String granteeLabel) { this.granteeLabel = granteeLabel; }
    public String getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(String permissionLevel) { this.permissionLevel = permissionLevel; }
}
