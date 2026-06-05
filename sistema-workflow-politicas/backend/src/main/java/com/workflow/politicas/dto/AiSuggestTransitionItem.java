package com.workflow.politicas.dto;

public class AiSuggestTransitionItem {
    private String operation = "CREATE";
    private String fromActivityName;
    private String toActivityName;
    private String transitionType = "SEQUENTIAL";
    private String conditionLabel;

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getFromActivityName() { return fromActivityName; }
    public void setFromActivityName(String fromActivityName) { this.fromActivityName = fromActivityName; }

    public String getToActivityName() { return toActivityName; }
    public void setToActivityName(String toActivityName) { this.toActivityName = toActivityName; }

    public String getTransitionType() { return transitionType; }
    public void setTransitionType(String transitionType) { this.transitionType = transitionType; }

    public String getConditionLabel() { return conditionLabel; }
    public void setConditionLabel(String conditionLabel) { this.conditionLabel = conditionLabel; }
}
