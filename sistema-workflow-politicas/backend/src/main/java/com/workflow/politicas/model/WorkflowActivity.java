package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "workflow_activities")
public class WorkflowActivity {
    @Id
    private String id;
    private String name;
    private String description;
    private String policyId;
    /** Rol, usuario o departamento responsable */
    private String responsible;
    /** ROLE, USER, DEPARTMENT */
    private String responsibleType;
    /** MANUAL, REVIEW, APPROVAL, NOTIFICATION */
    private String activityType;
    private int order;
    private Integer estimatedMinutes;
    /** CONFIGURED, PENDING, ACTIVE */
    private String status;
    private String formId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

    public String getResponsibleType() { return responsibleType; }
    public void setResponsibleType(String responsibleType) { this.responsibleType = responsibleType; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
}
