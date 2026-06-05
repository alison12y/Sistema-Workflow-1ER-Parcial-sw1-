package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Metadatos colaborativos por política — presencia y revisión del diagrama UML. */
@Document(collection = "workflow_collaboration")
public class WorkflowCollaborationMeta {

    @Id
    private String policyId;
    private long revision;
    private LocalDateTime lastModifiedAt;
    private String lastModifiedByUserId;
    private String lastModifiedByUsername;
    private String lastModifiedByDisplayName;
    private String lastModifiedActionType;
    private String lastModifiedActionLabel;
    private String lastModifiedElementType;
    private String lastModifiedElementName;
    private List<WorkflowEditorSession> activeEditors = new ArrayList<>();
    private List<WorkflowActiveEdit> activeEdits = new ArrayList<>();
    private List<WorkflowCollaborationRecentAction> recentActions = new ArrayList<>();

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public String getLastModifiedByUserId() {
        return lastModifiedByUserId;
    }

    public void setLastModifiedByUserId(String lastModifiedByUserId) {
        this.lastModifiedByUserId = lastModifiedByUserId;
    }

    public String getLastModifiedByUsername() {
        return lastModifiedByUsername;
    }

    public void setLastModifiedByUsername(String lastModifiedByUsername) {
        this.lastModifiedByUsername = lastModifiedByUsername;
    }

    public String getLastModifiedByDisplayName() {
        return lastModifiedByDisplayName;
    }

    public void setLastModifiedByDisplayName(String lastModifiedByDisplayName) {
        this.lastModifiedByDisplayName = lastModifiedByDisplayName;
    }

    public List<WorkflowEditorSession> getActiveEditors() {
        return activeEditors;
    }

    public void setActiveEditors(List<WorkflowEditorSession> activeEditors) {
        this.activeEditors = activeEditors;
    }

    public List<WorkflowActiveEdit> getActiveEdits() {
        return activeEdits;
    }

    public void setActiveEdits(List<WorkflowActiveEdit> activeEdits) {
        this.activeEdits = activeEdits;
    }

    public String getLastModifiedActionType() {
        return lastModifiedActionType;
    }

    public void setLastModifiedActionType(String lastModifiedActionType) {
        this.lastModifiedActionType = lastModifiedActionType;
    }

    public String getLastModifiedActionLabel() {
        return lastModifiedActionLabel;
    }

    public void setLastModifiedActionLabel(String lastModifiedActionLabel) {
        this.lastModifiedActionLabel = lastModifiedActionLabel;
    }

    public String getLastModifiedElementType() {
        return lastModifiedElementType;
    }

    public void setLastModifiedElementType(String lastModifiedElementType) {
        this.lastModifiedElementType = lastModifiedElementType;
    }

    public String getLastModifiedElementName() {
        return lastModifiedElementName;
    }

    public void setLastModifiedElementName(String lastModifiedElementName) {
        this.lastModifiedElementName = lastModifiedElementName;
    }

    public List<WorkflowCollaborationRecentAction> getRecentActions() {
        return recentActions;
    }

    public void setRecentActions(List<WorkflowCollaborationRecentAction> recentActions) {
        this.recentActions = recentActions;
    }
}
