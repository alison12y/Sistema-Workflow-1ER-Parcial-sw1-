package com.workflow.politicas.dto;

import java.util.HashMap;
import java.util.Map;

public class TramiteAdvanceRequest {
    private String comment;
    private String completedWorkflowActivityId;
    private Map<String, Object> stepData = new HashMap<>();

    public TramiteAdvanceRequest() {
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCompletedWorkflowActivityId() {
        return completedWorkflowActivityId;
    }

    public void setCompletedWorkflowActivityId(String completedWorkflowActivityId) {
        this.completedWorkflowActivityId = completedWorkflowActivityId;
    }

    public Map<String, Object> getStepData() {
        return stepData;
    }

    public void setStepData(Map<String, Object> stepData) {
        this.stepData = stepData != null ? stepData : new HashMap<>();
    }
}
