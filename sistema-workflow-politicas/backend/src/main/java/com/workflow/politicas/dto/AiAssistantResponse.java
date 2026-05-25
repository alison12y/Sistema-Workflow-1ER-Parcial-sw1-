package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiAssistantResponse {
    private Boolean aiAvailable;
    private Boolean fallbackUsed;
    private String error;
    private String module;
    private String intent;
    private String answer;
    private Map<String, Object> suggestedData = new HashMap<>();
    private AiSuggestedEndpointDto suggestedEndpoint;
    private boolean requiresConfirmation;
    private List<String> suggestions = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public Boolean getAiAvailable() { return aiAvailable; }
    public void setAiAvailable(Boolean aiAvailable) { this.aiAvailable = aiAvailable; }

    public Boolean getFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(Boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public Map<String, Object> getSuggestedData() { return suggestedData; }
    public void setSuggestedData(Map<String, Object> suggestedData) { this.suggestedData = suggestedData; }

    public AiSuggestedEndpointDto getSuggestedEndpoint() { return suggestedEndpoint; }
    public void setSuggestedEndpoint(AiSuggestedEndpointDto suggestedEndpoint) {
        this.suggestedEndpoint = suggestedEndpoint;
    }

    public boolean isRequiresConfirmation() { return requiresConfirmation; }
    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
