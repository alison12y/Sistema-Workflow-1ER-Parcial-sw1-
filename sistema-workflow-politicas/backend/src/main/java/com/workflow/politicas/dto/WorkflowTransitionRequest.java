package com.workflow.politicas.dto;

public class WorkflowTransitionRequest {
    private String policyId;
    private String fromActivityId;
    private String toActivityId;
    private String transitionType;
    private String conditionLabel;
    private String conditionExpression;
    private Integer orderIndex;
    private Boolean active;

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getFromActivityId() { return fromActivityId; }
    public void setFromActivityId(String fromActivityId) { this.fromActivityId = fromActivityId; }

    public String getToActivityId() { return toActivityId; }
    public void setToActivityId(String toActivityId) { this.toActivityId = toActivityId; }

    public String getTransitionType() { return transitionType; }
    public void setTransitionType(String transitionType) { this.transitionType = transitionType; }

    public String getConditionLabel() { return conditionLabel; }
    public void setConditionLabel(String conditionLabel) { this.conditionLabel = conditionLabel; }

    public String getConditionExpression() { return conditionExpression; }
    public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
