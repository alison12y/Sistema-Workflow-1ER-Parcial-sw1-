package com.workflow.politicas.dto;

public class AiFormFieldSuggestionDto {
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private String suggestedValue;
    private Double confidence;
    private Boolean applicable;
    private String message;

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }

    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public String getSuggestedValue() { return suggestedValue; }
    public void setSuggestedValue(String suggestedValue) { this.suggestedValue = suggestedValue; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Boolean getApplicable() { return applicable; }
    public void setApplicable(Boolean applicable) { this.applicable = applicable; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
