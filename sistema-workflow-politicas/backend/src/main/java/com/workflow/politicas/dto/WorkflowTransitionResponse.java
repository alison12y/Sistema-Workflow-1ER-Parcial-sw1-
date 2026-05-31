package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class WorkflowTransitionResponse {
    private String id;
    private String policyId;
    private String fromActivityId;
    private String fromActivityName;
    private String toActivityId;
    private String toActivityName;
    private String transitionType;
    private String transitionTypeLabel;
    private String conditionLabel;
    private String conditionExpression;
    private int orderIndex;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** true cuando create reactivó una conexión inactiva existente. */
    private boolean reactivated;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getFromActivityId() { return fromActivityId; }
    public void setFromActivityId(String fromActivityId) { this.fromActivityId = fromActivityId; }

    public String getFromActivityName() { return fromActivityName; }
    public void setFromActivityName(String fromActivityName) { this.fromActivityName = fromActivityName; }

    public String getToActivityId() { return toActivityId; }
    public void setToActivityId(String toActivityId) { this.toActivityId = toActivityId; }

    public String getToActivityName() { return toActivityName; }
    public void setToActivityName(String toActivityName) { this.toActivityName = toActivityName; }

    public String getTransitionType() { return transitionType; }
    public void setTransitionType(String transitionType) { this.transitionType = transitionType; }

    public String getTransitionTypeLabel() { return transitionTypeLabel; }
    public void setTransitionTypeLabel(String transitionTypeLabel) { this.transitionTypeLabel = transitionTypeLabel; }

    public String getConditionLabel() { return conditionLabel; }
    public void setConditionLabel(String conditionLabel) { this.conditionLabel = conditionLabel; }

    public String getConditionExpression() { return conditionExpression; }
    public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isReactivated() { return reactivated; }
    public void setReactivated(boolean reactivated) { this.reactivated = reactivated; }
}
