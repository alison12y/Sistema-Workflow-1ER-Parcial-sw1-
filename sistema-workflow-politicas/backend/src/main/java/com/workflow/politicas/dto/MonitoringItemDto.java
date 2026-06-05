package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class MonitoringItemDto {
    private String id;
    private String policyId;
    private String code;
    private String policyName;
    private String status;
    private String currentActivity;
    private String responsible;
    private String timeElapsed;
    private int progress;
    private String workflowError;
    private int pendingTaskCount;
    private int inProgressTaskCount;
    private int completedTaskCount;
    private LocalDateTime updatedAt;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(String currentActivity) {
        this.currentActivity = currentActivity;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public String getTimeElapsed() {
        return timeElapsed;
    }

    public void setTimeElapsed(String timeElapsed) {
        this.timeElapsed = timeElapsed;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getWorkflowError() {
        return workflowError;
    }

    public void setWorkflowError(String workflowError) {
        this.workflowError = workflowError;
    }

    public int getPendingTaskCount() {
        return pendingTaskCount;
    }

    public void setPendingTaskCount(int pendingTaskCount) {
        this.pendingTaskCount = pendingTaskCount;
    }

    public int getInProgressTaskCount() {
        return inProgressTaskCount;
    }

    public void setInProgressTaskCount(int inProgressTaskCount) {
        this.inProgressTaskCount = inProgressTaskCount;
    }

    public int getCompletedTaskCount() {
        return completedTaskCount;
    }

    public void setCompletedTaskCount(int completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
