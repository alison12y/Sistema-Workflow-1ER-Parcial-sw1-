package com.workflow.politicas.dto;

import java.util.HashMap;
import java.util.Map;

public class KpiDashboardResponse {
    private long totalProcesses;
    private long processesInProgress;
    private long completedProcesses;
    private long pendingTasks;
    private long completedTasks;
    private double averageProcessDurationHours;
    private Map<String, Long> tasksByStatus = new HashMap<>();
    private Map<String, Long> processesByStatus = new HashMap<>();

    public long getTotalProcesses() { return totalProcesses; }
    public void setTotalProcesses(long totalProcesses) { this.totalProcesses = totalProcesses; }

    public long getProcessesInProgress() { return processesInProgress; }
    public void setProcessesInProgress(long processesInProgress) { this.processesInProgress = processesInProgress; }

    public long getCompletedProcesses() { return completedProcesses; }
    public void setCompletedProcesses(long completedProcesses) { this.completedProcesses = completedProcesses; }

    public long getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(long pendingTasks) { this.pendingTasks = pendingTasks; }

    public long getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(long completedTasks) { this.completedTasks = completedTasks; }

    public double getAverageProcessDurationHours() { return averageProcessDurationHours; }
    public void setAverageProcessDurationHours(double averageProcessDurationHours) {
        this.averageProcessDurationHours = averageProcessDurationHours;
    }

    public Map<String, Long> getTasksByStatus() { return tasksByStatus; }
    public void setTasksByStatus(Map<String, Long> tasksByStatus) { this.tasksByStatus = tasksByStatus; }

    public Map<String, Long> getProcessesByStatus() { return processesByStatus; }
    public void setProcessesByStatus(Map<String, Long> processesByStatus) { this.processesByStatus = processesByStatus; }
}
