package com.workflow.politicas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Instancia de trámite. El flujo diseñado vive en
 * {@link WorkflowActivity}/{@link WorkflowTransition}; F1 enlazará {@code currentWorkflowActivityId}.
 *
 * @see com.workflow.politicas.workflow.cycle1.Cycle1WorkflowModel
 */
@Document(collection = "tramites")
public class Tramite {
    @Id
    private String id;
    private String code;
    @JsonIgnore
    private String policyId;
    private String policyName;
    private String policyDescription;
    private String description;
    private String priority;
    @JsonIgnore
    private String requestedBy;
    private String requestedByName;
    /** Alias legible expuesto en API (mismo valor que requestedByName). */
    private String requesterName;
    private String status;
    private String currentActivity;
    /** Actividad UML actual (modelo oficial Ciclo 1). */
    private String currentWorkflowActivityId;
    /**
     * @deprecated ID de {@link DiagramNode} (modelo B). Usar {@link #currentWorkflowActivityId}.
     */
    @Deprecated(since = "0.0.1-cycle1-f0")
    @JsonIgnore
    private String currentNodeId;
    /** Tras PARALLEL_SPLIT: actividad JOIN a alcanzar cuando todas las ramas terminen. */
    @JsonIgnore
    private String pendingJoinActivityId;
    /** Grupo de tareas paralelas activas. */
    @JsonIgnore
    private String activeParallelGroupId;
    /** Último error de enrutamiento (si aplica). */
    private String workflowError;
    private String responsible;
    private int progress;
    @JsonIgnore
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TramiteTask> tasks = new ArrayList<>();
    private List<TraceItem> trace = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getPolicyDescription() {
        return policyDescription;
    }

    public void setPolicyDescription(String policyDescription) {
        this.policyDescription = policyDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
        this.requesterName = requestedByName;
    }

    public String getRequesterName() {
        return requesterName != null && !requesterName.isBlank() ? requesterName : requestedByName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
        if (requestedByName == null || requestedByName.isBlank()) {
            this.requestedByName = requesterName;
        }
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

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public String getCurrentWorkflowActivityId() {
        return currentWorkflowActivityId;
    }

    public void setCurrentWorkflowActivityId(String currentWorkflowActivityId) {
        this.currentWorkflowActivityId = currentWorkflowActivityId;
    }

    public String getPendingJoinActivityId() {
        return pendingJoinActivityId;
    }

    public void setPendingJoinActivityId(String pendingJoinActivityId) {
        this.pendingJoinActivityId = pendingJoinActivityId;
    }

    public String getActiveParallelGroupId() {
        return activeParallelGroupId;
    }

    public void setActiveParallelGroupId(String activeParallelGroupId) {
        this.activeParallelGroupId = activeParallelGroupId;
    }

    public String getWorkflowError() {
        return workflowError;
    }

    public void setWorkflowError(String workflowError) {
        this.workflowError = workflowError;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public List<TramiteTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<TramiteTask> tasks) {
        this.tasks = tasks;
    }

    public List<TraceItem> getTrace() {
        return trace;
    }

    public void setTrace(List<TraceItem> trace) {
        this.trace = trace;
    }
}
