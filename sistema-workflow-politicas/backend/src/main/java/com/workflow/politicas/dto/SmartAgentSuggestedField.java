package com.workflow.politicas.dto;

public class SmartAgentSuggestedField {

    private String name;
    private String label;
    private String type;
    private boolean required;
    private String suggestedValue;

    public SmartAgentSuggestedField() {
    }

    public SmartAgentSuggestedField(String name, String label, String type, boolean required, String suggestedValue) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.required = required;
        this.suggestedValue = suggestedValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getSuggestedValue() {
        return suggestedValue;
    }

    public void setSuggestedValue(String suggestedValue) {
        this.suggestedValue = suggestedValue;
    }
}
