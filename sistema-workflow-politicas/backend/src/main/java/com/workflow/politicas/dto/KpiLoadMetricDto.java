package com.workflow.politicas.dto;

public class KpiLoadMetricDto {
    private String key;
    private String displayName;
    private String departmentName;
    private long pendingCount;
    private long inProgressCount;
    private long completedCount;
    private long totalActive;
    private String averageHandlingTime;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public long getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(long inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(long completedCount) {
        this.completedCount = completedCount;
    }

    public long getTotalActive() {
        return totalActive;
    }

    public void setTotalActive(long totalActive) {
        this.totalActive = totalActive;
    }

    public String getAverageHandlingTime() {
        return averageHandlingTime;
    }

    public void setAverageHandlingTime(String averageHandlingTime) {
        this.averageHandlingTime = averageHandlingTime;
    }
}
