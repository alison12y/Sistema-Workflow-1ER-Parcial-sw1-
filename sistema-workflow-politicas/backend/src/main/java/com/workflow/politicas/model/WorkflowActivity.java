package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Actividad UML 2.5 del Ciclo 1 . Pertenece a una {@link BusinessPolicy}
 * y se organiza en swimlane mediante {@code responsibleName} / {@code responsibleType}.
 *
 * @see com.workflow.politicas.workflow.cycle1.Cycle1WorkflowModel
 */
@Document(collection = "workflow_activities")
public class WorkflowActivity {
    @Id
    private String id;
    private String policyId;
    private String name;
    private String description;
    /** ROLE, DEPARTMENT, USER */
    private String responsibleType;
    private String responsibleId;
    private String responsibleName;
    /** START, TASK, DECISION, END, FORK, JOIN (pasarelas UML — sin tarea humana) */
    private String activityType;
    /** BORRADOR, ACTIVA, INACTIVA */
    private String status;
    private int orderIndex;
    private Integer estimatedTimeHours;
    /** Posición visual X en el diseñador UML (píxeles). */
    private Integer positionX;
    /** Posición visual Y en el diseñador UML (píxeles). */
    private Integer positionY;
    private String formId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean active;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResponsibleType() { return responsibleType; }
    public void setResponsibleType(String responsibleType) { this.responsibleType = responsibleType; }

    public String getResponsibleId() { return responsibleId; }
    public void setResponsibleId(String responsibleId) { this.responsibleId = responsibleId; }

    public String getResponsibleName() { return responsibleName; }
    public void setResponsibleName(String responsibleName) { this.responsibleName = responsibleName; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public Integer getEstimatedTimeHours() { return estimatedTimeHours; }
    public void setEstimatedTimeHours(Integer estimatedTimeHours) { this.estimatedTimeHours = estimatedTimeHours; }

    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }

    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }

    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() {
        return active == null || Boolean.TRUE.equals(active);
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
