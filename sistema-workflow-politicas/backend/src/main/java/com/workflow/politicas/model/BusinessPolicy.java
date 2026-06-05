package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * Política de negocio — raíz del modelo oficial Ciclo 1.
 * El flujo UML se define con {@link WorkflowActivity} y {@link WorkflowTransition} por {@code policyId}.
 *
 * @see com.workflow.politicas.workflow.cycle1.Cycle1WorkflowModel
 */
@Document(collection = "business_policies")
public class BusinessPolicy {
    @Id
    private String id;
    private String name;
    private String description;
    private String type; // e.g., "PURCHASE_REQUEST", "LEAVE_REQUEST"
    private String status; // "DRAFT", "ACTIVE", "ARCHIVED"
    private String version;
    private String responsible;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /**
     * @deprecated Ciclo 1 usa actividades en {@code workflow_activities} por {@code policyId}.
     *             Mantener solo por compatibilidad con modelo BPM legacy.
     */
    @Deprecated(since = "0.0.1-cycle1-f0")
    private String currentDiagramId;

    public BusinessPolicy() {
    }

    public BusinessPolicy(String id, String name, String description, String type, String status, String createdBy, LocalDateTime createdAt, LocalDateTime updatedAt, String currentDiagramId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.currentDiagramId = currentDiagramId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
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

    public String getCurrentDiagramId() {
        return currentDiagramId;
    }

    public void setCurrentDiagramId(String currentDiagramId) {
        this.currentDiagramId = currentDiagramId;
    }
}
