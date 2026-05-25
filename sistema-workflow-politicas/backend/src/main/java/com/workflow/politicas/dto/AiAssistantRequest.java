package com.workflow.politicas.dto;

import java.util.HashMap;
import java.util.Map;

public class AiAssistantRequest {
    private String prompt;
    private String module;
    private Map<String, Object> context = new HashMap<>();
    private String userId;

    public AiAssistantRequest() {}

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
