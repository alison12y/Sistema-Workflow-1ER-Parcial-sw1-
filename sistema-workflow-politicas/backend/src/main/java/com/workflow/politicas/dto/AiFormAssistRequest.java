package com.workflow.politicas.dto;

import java.util.HashMap;
import java.util.Map;

public class AiFormAssistRequest {
    private String prompt;
    private String fieldName;
    private Map<String, Object> context = new HashMap<>();
    private String userId;

    public AiFormAssistRequest() {}

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
