package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "document_permissions")
@CompoundIndex(name = "family_grantee", def = "{'documentFamilyId': 1, 'granteeType': 1, 'granteeKey': 1}", unique = true)
public class DocumentPermission {
    public static final String GRANTEE_USER = "USER";
    public static final String GRANTEE_ROLE = "ROLE";
    public static final String GRANTEE_DEPARTMENT = "DEPARTMENT";

    @Id
    private String id;
    @Indexed
    private String documentFamilyId;
    @Indexed
    private String repositoryId;
    @Indexed
    private String tramiteId;
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
    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
    public String getTramiteId() { return tramiteId; }
    public void setTramiteId(String tramiteId) { this.tramiteId = tramiteId; }
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
