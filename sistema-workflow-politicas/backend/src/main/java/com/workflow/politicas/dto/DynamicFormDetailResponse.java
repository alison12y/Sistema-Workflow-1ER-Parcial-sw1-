package com.workflow.politicas.dto;

import java.util.List;

public class DynamicFormDetailResponse {
    private String id;
    private String policyId;
    private String activityName;
    private String name;
    private List<FormFieldDto> fields;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FormFieldDto> getFields() {
        return fields;
    }

    public void setFields(List<FormFieldDto> fields) {
        this.fields = fields;
    }
}
