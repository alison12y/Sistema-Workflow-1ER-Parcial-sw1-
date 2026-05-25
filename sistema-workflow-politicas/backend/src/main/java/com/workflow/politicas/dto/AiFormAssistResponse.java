package com.workflow.politicas.dto;

public class AiFormAssistResponse {
    private String suggestedText;
    private double confidence;

    public String getSuggestedText() { return suggestedText; }
    public void setSuggestedText(String suggestedText) { this.suggestedText = suggestedText; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
