package com.workflow.politicas.dto;

public class AiWorkflowContextActivityDto {
    private String id;
    private String name;
    private String activityType;
    private String responsibleName;
    private String responsibleType;
    private Integer orderIndex;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getResponsibleName() { return responsibleName; }
    public void setResponsibleName(String responsibleName) { this.responsibleName = responsibleName; }

    public String getResponsibleType() { return responsibleType; }
    public void setResponsibleType(String responsibleType) { this.responsibleType = responsibleType; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
