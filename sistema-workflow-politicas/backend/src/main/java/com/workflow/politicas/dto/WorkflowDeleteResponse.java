package com.workflow.politicas.dto;

public class WorkflowDeleteResponse {
    private String message;
    private boolean logicalDelete;
    private int affectedConnections;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isLogicalDelete() { return logicalDelete; }
    public void setLogicalDelete(boolean logicalDelete) { this.logicalDelete = logicalDelete; }

    public int getAffectedConnections() { return affectedConnections; }
    public void setAffectedConnections(int affectedConnections) { this.affectedConnections = affectedConnections; }
}
