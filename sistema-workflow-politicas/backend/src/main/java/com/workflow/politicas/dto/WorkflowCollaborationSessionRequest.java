package com.workflow.politicas.dto;

public class WorkflowCollaborationSessionRequest {

    private String sessionId;
    private Long baseRevision;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getBaseRevision() {
        return baseRevision;
    }

    public void setBaseRevision(Long baseRevision) {
        this.baseRevision = baseRevision;
    }
}
