package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class MyActivityDto {
    private String tramiteId;
    private String policyId;
    private int taskOrder;
    private String code;
    private String policyName;
    private String activityName;
    private String workflowActivityId;
    private String status;
    private String responsible;
    private String priority;
    private String tramiteStatus;
    private LocalDateTime assignedAt;
    private LocalDateTime takenAt;
    private LocalDateTime completedAt;
    private String takenBy;
    /** NORMAL, OBSERVADA, ERROR */
    private String inboxCategory;
    private boolean canTake;
    private boolean canComplete;
    private String workflowError;

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

    public int getTaskOrder() {
        return taskOrder;
    }

    public void setTaskOrder(int taskOrder) {
        this.taskOrder = taskOrder;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getWorkflowActivityId() {
        return workflowActivityId;
    }

    public void setWorkflowActivityId(String workflowActivityId) {
        this.workflowActivityId = workflowActivityId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getTramiteStatus() {
        return tramiteStatus;
    }

    public void setTramiteStatus(String tramiteStatus) {
        this.tramiteStatus = tramiteStatus;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getTakenBy() {
        return takenBy;
    }

    public void setTakenBy(String takenBy) {
        this.takenBy = takenBy;
    }

    public String getInboxCategory() {
        return inboxCategory;
    }

    public void setInboxCategory(String inboxCategory) {
        this.inboxCategory = inboxCategory;
    }

    public boolean isCanTake() {
        return canTake;
    }

    public void setCanTake(boolean canTake) {
        this.canTake = canTake;
    }

    public boolean isCanComplete() {
        return canComplete;
    }

    public void setCanComplete(boolean canComplete) {
        this.canComplete = canComplete;
    }

    public String getWorkflowError() {
        return workflowError;
    }

    public void setWorkflowError(String workflowError) {
        this.workflowError = workflowError;
    }
}
