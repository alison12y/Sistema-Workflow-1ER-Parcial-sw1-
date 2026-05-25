package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class OverdueTaskDto {
    private String taskId;
    private String processInstanceId;
    private String activityId;
    private String activityName;
    private LocalDateTime dueDate;
    private double hoursOverdue;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public double getHoursOverdue() { return hoursOverdue; }
    public void setHoursOverdue(double hoursOverdue) { this.hoursOverdue = hoursOverdue; }
}
