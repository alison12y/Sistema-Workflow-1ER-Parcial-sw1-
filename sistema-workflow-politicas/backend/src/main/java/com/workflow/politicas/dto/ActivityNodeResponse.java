package com.workflow.politicas.dto;

public class ActivityNodeResponse {
    private String id;
    private String name;
    private String description;
    private String responsibleName;
    private String activityType;
    private String activityTypeLabel;
    private String status;
    private int orderIndex;
    private Integer estimatedTimeHours;
    private int x;
    private int y;
    private boolean decisionNode;
    private int outgoingConditionalCount;
    private int incomingCount;
    private int outgoingCount;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResponsibleName() { return responsibleName; }
    public void setResponsibleName(String responsibleName) { this.responsibleName = responsibleName; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getActivityTypeLabel() { return activityTypeLabel; }
    public void setActivityTypeLabel(String activityTypeLabel) { this.activityTypeLabel = activityTypeLabel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public Integer getEstimatedTimeHours() { return estimatedTimeHours; }
    public void setEstimatedTimeHours(Integer estimatedTimeHours) { this.estimatedTimeHours = estimatedTimeHours; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public boolean isDecisionNode() { return decisionNode; }
    public void setDecisionNode(boolean decisionNode) { this.decisionNode = decisionNode; }

    public int getOutgoingConditionalCount() { return outgoingConditionalCount; }
    public void setOutgoingConditionalCount(int outgoingConditionalCount) {
        this.outgoingConditionalCount = outgoingConditionalCount;
    }

    public int getIncomingCount() { return incomingCount; }
    public void setIncomingCount(int incomingCount) { this.incomingCount = incomingCount; }

    public int getOutgoingCount() { return outgoingCount; }
    public void setOutgoingCount(int outgoingCount) { this.outgoingCount = outgoingCount; }
}
