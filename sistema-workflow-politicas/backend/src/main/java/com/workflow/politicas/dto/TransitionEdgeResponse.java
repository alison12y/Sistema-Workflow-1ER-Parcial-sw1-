package com.workflow.politicas.dto;

public class TransitionEdgeResponse {
    private String id;
    private String fromActivityId;
    private String fromActivityName;
    private String toActivityId;
    private String toActivityName;
    private String transitionType;
    private String transitionTypeLabel;
    private String conditionLabel;
    private boolean active;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromActivityId() { return fromActivityId; }
    public void setFromActivityId(String fromActivityId) { this.fromActivityId = fromActivityId; }

    public String getFromActivityName() { return fromActivityName; }
    public void setFromActivityName(String fromActivityName) { this.fromActivityName = fromActivityName; }

    public String getToActivityId() { return toActivityId; }
    public void setToActivityId(String toActivityId) { this.toActivityId = toActivityId; }

    public String getToActivityName() { return toActivityName; }
    public void setToActivityName(String toActivityName) { this.toActivityName = toActivityName; }

    public String getTransitionType() { return transitionType; }
    public void setTransitionType(String transitionType) { this.transitionType = transitionType; }

    public String getTransitionTypeLabel() { return transitionTypeLabel; }
    public void setTransitionTypeLabel(String transitionTypeLabel) { this.transitionTypeLabel = transitionTypeLabel; }

    public String getConditionLabel() { return conditionLabel; }
    public void setConditionLabel(String conditionLabel) { this.conditionLabel = conditionLabel; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
