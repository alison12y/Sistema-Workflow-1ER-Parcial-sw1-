package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class AiValidateDiagramResponse {
    private boolean valid;
    private List<String> errors = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}
