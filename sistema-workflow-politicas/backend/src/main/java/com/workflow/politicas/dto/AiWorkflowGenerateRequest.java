package com.workflow.politicas.dto;

public class AiWorkflowGenerateRequest {
    private String prompt;
    private String userId;

    public AiWorkflowGenerateRequest() {}

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
