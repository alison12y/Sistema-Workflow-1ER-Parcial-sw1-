package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class DocumentCollaborationStateResponse {
    private String repositoryId;
    private String tramiteId;
    private String currentUsername;
    private List<DocumentCollaborationConnectedUserDto> connectedUsers = new ArrayList<>();
    private List<DocumentCollaborationActiveLockDto> activeLocks = new ArrayList<>();

    private List<DocumentCollaborationRecentActionDto> recentActions = new ArrayList<>();

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
    public String getTramiteId() { return tramiteId; }
    public void setTramiteId(String tramiteId) { this.tramiteId = tramiteId; }
    public String getCurrentUsername() { return currentUsername; }
    public void setCurrentUsername(String currentUsername) { this.currentUsername = currentUsername; }
    public List<DocumentCollaborationConnectedUserDto> getConnectedUsers() { return connectedUsers; }
    public void setConnectedUsers(List<DocumentCollaborationConnectedUserDto> connectedUsers) { this.connectedUsers = connectedUsers; }
    public List<DocumentCollaborationActiveLockDto> getActiveLocks() { return activeLocks; }
    public void setActiveLocks(List<DocumentCollaborationActiveLockDto> activeLocks) { this.activeLocks = activeLocks; }
    public List<DocumentCollaborationRecentActionDto> getRecentActions() { return recentActions; }
    public void setRecentActions(List<DocumentCollaborationRecentActionDto> recentActions) { this.recentActions = recentActions; }
}
