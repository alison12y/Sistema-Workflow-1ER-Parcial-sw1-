package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class WorkflowValidationResponse {
    private boolean valid;
    private List<String> errors = new ArrayList<>();

    public WorkflowValidationResponse() {}

    public WorkflowValidationResponse(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
