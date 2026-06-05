package com.workflow.politicas.dto;

import com.workflow.politicas.model.TraceItem;
import com.workflow.politicas.model.TramiteTask;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Estado completo de monitoreo de un trámite (F3). */
public class MonitoringTraceResponse {
    private String id;
    private String policyId;
    private String code;
    private String policyName;
    private String status;
    private String currentActivity;
    private String responsible;
    private int progress;
    private String workflowError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private MonitoringTaskCountsDto taskCounts = new MonitoringTaskCountsDto();
    private List<TramiteTask> pendingTasks = new ArrayList<>();
    private List<TramiteTask> inProgressTasks = new ArrayList<>();
    private List<TramiteTask> completedTasks = new ArrayList<>();
    private List<TramiteTask> tasks = new ArrayList<>();
    private List<MonitoringResponsibleDto> responsibles = new ArrayList<>();
    private List<TraceItem> events = new ArrayList<>();

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public MonitoringTaskCountsDto getTaskCounts() {
        return taskCounts;
    }

    public void setTaskCounts(MonitoringTaskCountsDto taskCounts) {
        this.taskCounts = taskCounts;
    }

    public List<TramiteTask> getPendingTasks() {
        return pendingTasks;
    }

    public void setPendingTasks(List<TramiteTask> pendingTasks) {
        this.pendingTasks = pendingTasks;
    }

    public List<TramiteTask> getInProgressTasks() {
        return inProgressTasks;
    }

    public void setInProgressTasks(List<TramiteTask> inProgressTasks) {
        this.inProgressTasks = inProgressTasks;
    }

    public List<TramiteTask> getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(List<TramiteTask> completedTasks) {
        this.completedTasks = completedTasks;
    }

    public List<TramiteTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<TramiteTask> tasks) {
        this.tasks = tasks;
    }

    public List<MonitoringResponsibleDto> getResponsibles() {
        return responsibles;
    }

    public void setResponsibles(List<MonitoringResponsibleDto> responsibles) {
        this.responsibles = responsibles;
    }

    public List<TraceItem> getEvents() {
        return events;
    }

    public void setEvents(List<TraceItem> events) {
        this.events = events;
    }
}
