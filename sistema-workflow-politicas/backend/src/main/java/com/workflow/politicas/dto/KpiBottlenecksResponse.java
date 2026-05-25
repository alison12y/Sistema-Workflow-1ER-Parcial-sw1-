package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class KpiBottlenecksResponse {
    private List<ActivityDelayDto> activitiesWithHighestDelay = new ArrayList<>();
    private List<RolePendingTasksDto> rolesWithMostPendingTasks = new ArrayList<>();
    private List<LongRunningProcessDto> longRunningProcesses = new ArrayList<>();
    private List<OverdueTaskDto> overdueTasks = new ArrayList<>();

    public List<ActivityDelayDto> getActivitiesWithHighestDelay() { return activitiesWithHighestDelay; }
    public void setActivitiesWithHighestDelay(List<ActivityDelayDto> activitiesWithHighestDelay) {
        this.activitiesWithHighestDelay = activitiesWithHighestDelay;
    }

    public List<RolePendingTasksDto> getRolesWithMostPendingTasks() { return rolesWithMostPendingTasks; }
    public void setRolesWithMostPendingTasks(List<RolePendingTasksDto> rolesWithMostPendingTasks) {
        this.rolesWithMostPendingTasks = rolesWithMostPendingTasks;
    }

    public List<LongRunningProcessDto> getLongRunningProcesses() { return longRunningProcesses; }
    public void setLongRunningProcesses(List<LongRunningProcessDto> longRunningProcesses) {
        this.longRunningProcesses = longRunningProcesses;
    }

    public List<OverdueTaskDto> getOverdueTasks() { return overdueTasks; }
    public void setOverdueTasks(List<OverdueTaskDto> overdueTasks) { this.overdueTasks = overdueTasks; }
}
