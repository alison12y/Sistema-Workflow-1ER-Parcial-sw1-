package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiFormAssistRequest {
    /** Informe o texto libre del funcionario (alias legacy: prompt). */
    private String report;
    private String prompt;
    private String fieldName;
    private String policyId;
    private String tramiteId;
    private String workflowActivityId;
    private String formId;
    private String activityName;
    private String userId;
    private List<AiFormFieldDefinitionDto> fields = new ArrayList<>();
    private Map<String, String> currentValues = new HashMap<>();
    private Map<String, Object> context = new HashMap<>();

    public String getReport() {
        return report != null && !report.isBlank() ? report : prompt;
    }

    public void setReport(String report) { this.report = report; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getTramiteId() { return tramiteId; }
    public void setTramiteId(String tramiteId) { this.tramiteId = tramiteId; }

    public String getWorkflowActivityId() { return workflowActivityId; }
    public void setWorkflowActivityId(String workflowActivityId) { this.workflowActivityId = workflowActivityId; }

    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<AiFormFieldDefinitionDto> getFields() { return fields; }
    public void setFields(List<AiFormFieldDefinitionDto> fields) { this.fields = fields; }

    public Map<String, String> getCurrentValues() { return currentValues; }
    public void setCurrentValues(Map<String, String> currentValues) { this.currentValues = currentValues; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}
