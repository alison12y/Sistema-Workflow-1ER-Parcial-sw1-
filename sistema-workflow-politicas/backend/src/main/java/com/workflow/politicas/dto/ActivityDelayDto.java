package com.workflow.politicas.dto;

public class ActivityDelayDto {
    private String activityId;
    private String activityName;
    private double averageDelayHours;
    private long taskCount;

    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public double getAverageDelayHours() { return averageDelayHours; }
    public void setAverageDelayHours(double averageDelayHours) { this.averageDelayHours = averageDelayHours; }

    public long getTaskCount() { return taskCount; }
    public void setTaskCount(long taskCount) { this.taskCount = taskCount; }
}
