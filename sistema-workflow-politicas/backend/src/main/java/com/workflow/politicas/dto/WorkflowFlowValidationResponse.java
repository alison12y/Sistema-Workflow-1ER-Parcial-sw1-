package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class WorkflowFlowValidationResponse {
    private boolean valid;
    private String message;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
