package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class AiValidateDiagramResponse {
    private Boolean aiAvailable;
    private Boolean fallbackUsed;
    private String error;
    private boolean valid;
    private List<String> errors = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public Boolean getAiAvailable() { return aiAvailable; }
    public void setAiAvailable(Boolean aiAvailable) { this.aiAvailable = aiAvailable; }

    public Boolean getFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(Boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
