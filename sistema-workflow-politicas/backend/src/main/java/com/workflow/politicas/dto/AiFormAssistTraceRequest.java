package com.workflow.politicas.dto;

public class AiFormAssistTraceRequest {
    private String workflowActivityId;
    private Integer taskOrder;
    private String activityName;
    private Integer fieldsSuggested;
    private Integer fieldsApplied;

    public String getWorkflowActivityId() { return workflowActivityId; }
    public void setWorkflowActivityId(String workflowActivityId) { this.workflowActivityId = workflowActivityId; }

    public Integer getTaskOrder() { return taskOrder; }
    public void setTaskOrder(Integer taskOrder) { this.taskOrder = taskOrder; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public Integer getFieldsSuggested() { return fieldsSuggested; }
    public void setFieldsSuggested(Integer fieldsSuggested) { this.fieldsSuggested = fieldsSuggested; }

    public Integer getFieldsApplied() { return fieldsApplied; }
    public void setFieldsApplied(Integer fieldsApplied) { this.fieldsApplied = fieldsApplied; }
}
