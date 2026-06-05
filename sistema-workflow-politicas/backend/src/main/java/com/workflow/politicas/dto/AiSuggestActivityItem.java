package com.workflow.politicas.dto;

public class AiSuggestActivityItem {
    private String operation = "CREATE";
    private String name;
    private String activityType = "TASK";
    private String responsibleName;
    private String responsibleType = "ROLE";
    private Integer orderIndex;
    private String connectAfterActivityName;

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

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

    public String getConnectAfterActivityName() { return connectAfterActivityName; }
    public void setConnectAfterActivityName(String connectAfterActivityName) {
        this.connectAfterActivityName = connectAfterActivityName;
    }
}
