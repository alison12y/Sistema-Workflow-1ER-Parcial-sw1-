package com.workflow.politicas.dto;

import java.util.Map;

public class ProcessStartRequest {
    private String policyId;
    private String initiatorId;
    private Map<String, Object> formData;

    public ProcessStartRequest() {}

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getInitiatorId() { return initiatorId; }
    public void setInitiatorId(String initiatorId) { this.initiatorId = initiatorId; }

    public Map<String, Object> getFormData() { return formData; }
    public void setFormData(Map<String, Object> formData) { this.formData = formData; }
}
