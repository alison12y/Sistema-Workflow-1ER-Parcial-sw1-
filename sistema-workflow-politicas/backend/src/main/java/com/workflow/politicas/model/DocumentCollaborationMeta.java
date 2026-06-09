package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "document_collaboration")
public class DocumentCollaborationMeta {
    @Id
    private String repositoryId;
    private String tramiteId;
    private List<DocumentEditorSession> activeSessions = new ArrayList<>();
    private List<DocumentActiveLock> activeLocks = new ArrayList<>();
    private List<DocumentCollaborationRecentAction> recentActions = new ArrayList<>();

    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
    public String getTramiteId() { return tramiteId; }
    public void setTramiteId(String tramiteId) { this.tramiteId = tramiteId; }
    public List<DocumentEditorSession> getActiveSessions() { return activeSessions; }
    public void setActiveSessions(List<DocumentEditorSession> activeSessions) { this.activeSessions = activeSessions; }
    public List<DocumentActiveLock> getActiveLocks() { return activeLocks; }
    public void setActiveLocks(List<DocumentActiveLock> activeLocks) { this.activeLocks = activeLocks; }
    public List<DocumentCollaborationRecentAction> getRecentActions() { return recentActions; }
    public void setRecentActions(List<DocumentCollaborationRecentAction> recentActions) { this.recentActions = recentActions; }
}
