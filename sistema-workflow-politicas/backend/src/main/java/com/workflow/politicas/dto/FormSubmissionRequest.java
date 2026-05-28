package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class FormSubmissionRequest {
    private String tramiteId;
    private String policyId;
    private String activityName;
    private int taskOrder;
    private List<ResponseItemDto> responses = new ArrayList<>();

    public String getTramiteId() {
        return tramiteId;
    }

    public void setTramiteId(String tramiteId) {
        this.tramiteId = tramiteId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public int getTaskOrder() {
        return taskOrder;
    }

    public void setTaskOrder(int taskOrder) {
        this.taskOrder = taskOrder;
    }

    public List<ResponseItemDto> getResponses() {
        return responses;
    }

    public void setResponses(List<ResponseItemDto> responses) {
        this.responses = responses;
    }
}
