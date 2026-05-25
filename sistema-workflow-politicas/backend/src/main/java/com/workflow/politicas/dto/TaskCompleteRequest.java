package com.workflow.politicas.dto;

import java.util.Map;

public class TaskCompleteRequest {
    private Map<String, Object> stepData;

    public TaskCompleteRequest() {}

    public Map<String, Object> getStepData() { return stepData; }
    public void setStepData(Map<String, Object> stepData) { this.stepData = stepData; }
}
