package com.workflow.politicas.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkflowCollaborationStateResponse {

    private String policyId;
    private long revision;
    private LocalDateTime lastModifiedAt;
    private String lastModifiedByUserId;
    private String lastModifiedByUsername;
    private String lastModifiedByDisplayName;
    private String lastModifiedByName;
    private String lastModifiedActionType;
    private String lastModifiedActionLabel;
    private String lastModifiedElementType;
    private String lastModifiedElementName;
    private String lastModifiedSummary;
    private String currentUsername;
    private boolean staleForClient;
    private long currentRevision;
    private int activeSessionsCount;
    private List<WorkflowCollaborationConnectedUserDto> connectedUsers = new ArrayList<>();
    private List<WorkflowCollaborationActiveEditDto> activeEdits = new ArrayList<>();
    private List<WorkflowCollaborationRecentActionDto> recentActions = new ArrayList<>();
    /** @deprecated Usar {@link #getConnectedUsers()} (lista agrupada por userId). */
    private List<WorkflowCollaborationEditorDto> connectedEditors = new ArrayList<>();

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

    public String getLastModifiedByName() {
        return lastModifiedByName;
    }

    public void setLastModifiedByName(String lastModifiedByName) {
        this.lastModifiedByName = lastModifiedByName;
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

    public String getLastModifiedSummary() {
        return lastModifiedSummary;
    }

    public void setLastModifiedSummary(String lastModifiedSummary) {
        this.lastModifiedSummary = lastModifiedSummary;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public boolean isStaleForClient() {
        return staleForClient;
    }

    public void setStaleForClient(boolean staleForClient) {
        this.staleForClient = staleForClient;
    }

    public long getCurrentRevision() {
        return currentRevision;
    }

    public void setCurrentRevision(long currentRevision) {
        this.currentRevision = currentRevision;
    }

    public int getActiveSessionsCount() {
        return activeSessionsCount;
    }

    public void setActiveSessionsCount(int activeSessionsCount) {
        this.activeSessionsCount = activeSessionsCount;
    }

    public List<WorkflowCollaborationConnectedUserDto> getConnectedUsers() {
        return connectedUsers;
    }

    public void setConnectedUsers(List<WorkflowCollaborationConnectedUserDto> connectedUsers) {
        this.connectedUsers = connectedUsers;
    }

    public List<WorkflowCollaborationActiveEditDto> getActiveEdits() {
        return activeEdits;
    }

    public void setActiveEdits(List<WorkflowCollaborationActiveEditDto> activeEdits) {
        this.activeEdits = activeEdits;
    }

    public List<WorkflowCollaborationRecentActionDto> getRecentActions() {
        return recentActions;
    }

    public void setRecentActions(List<WorkflowCollaborationRecentActionDto> recentActions) {
        this.recentActions = recentActions;
    }

    public List<WorkflowCollaborationEditorDto> getConnectedEditors() {
        return connectedEditors;
    }

    public void setConnectedEditors(List<WorkflowCollaborationEditorDto> connectedEditors) {
        this.connectedEditors = connectedEditors;
    }
}
