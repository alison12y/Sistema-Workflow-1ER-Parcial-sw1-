package com.workflow.politicas.model;

import java.time.LocalDateTime;

/**
 * Tarea de bandeja del funcionario.
 * añadirá enlace a {@link WorkflowActivity#getId()}.
 */
public class TramiteTask {
    private String workflowActivityId;
    private String name;
    private String responsible;
    private String status;
    private int order;
    /** Ramas creadas por PARALLEL_SPLIT. */
    private String parallelGroupId;
    private LocalDateTime startedAt;
    private LocalDateTime takenAt;
    private String takenBy;
    private LocalDateTime completedAt;
    private String notes;

    public String getWorkflowActivityId() {
        return workflowActivityId;
    }

    public void setWorkflowActivityId(String workflowActivityId) {
        this.workflowActivityId = workflowActivityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getParallelGroupId() {
        return parallelGroupId;
    }

    public void setParallelGroupId(String parallelGroupId) {
        this.parallelGroupId = parallelGroupId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public String getTakenBy() {
        return takenBy;
    }

    public void setTakenBy(String takenBy) {
        this.takenBy = takenBy;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
