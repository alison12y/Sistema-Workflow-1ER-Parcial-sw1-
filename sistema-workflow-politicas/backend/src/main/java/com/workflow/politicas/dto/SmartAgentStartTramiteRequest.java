package com.workflow.politicas.dto;

public class SmartAgentStartTramiteRequest {

    private String policyId;
    private String description;
    private String requestedBy;
    private String priority;
    private String detectedIntent;
    private String agentExplanation;

    public SmartAgentStartTramiteRequest() {
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDetectedIntent() {
        return detectedIntent;
    }

    public void setDetectedIntent(String detectedIntent) {
        this.detectedIntent = detectedIntent;
    }

    public String getAgentExplanation() {
        return agentExplanation;
    }

    public void setAgentExplanation(String agentExplanation) {
        this.agentExplanation = agentExplanation;
    }
}
