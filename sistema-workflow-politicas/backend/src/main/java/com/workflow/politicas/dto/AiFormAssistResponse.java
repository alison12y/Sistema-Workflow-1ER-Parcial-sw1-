package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiFormAssistResponse {
    private Boolean aiAvailable;
    private Boolean fallbackUsed;
    private String error;
    private String explanation;
    private Double confidence;
    private String suggestedText;
    private List<AiFormFieldSuggestionDto> fieldSuggestions = new ArrayList<>();
    private Map<String, String> suggestedValues = new HashMap<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> unmatchedFields = new ArrayList<>();

    public Boolean getAiAvailable() { return aiAvailable; }
    public void setAiAvailable(Boolean aiAvailable) { this.aiAvailable = aiAvailable; }

    public Boolean getFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(Boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getSuggestedText() { return suggestedText; }
    public void setSuggestedText(String suggestedText) { this.suggestedText = suggestedText; }

    public List<AiFormFieldSuggestionDto> getFieldSuggestions() { return fieldSuggestions; }
    public void setFieldSuggestions(List<AiFormFieldSuggestionDto> fieldSuggestions) {
        this.fieldSuggestions = fieldSuggestions;
    }

    public Map<String, String> getSuggestedValues() { return suggestedValues; }
    public void setSuggestedValues(Map<String, String> suggestedValues) {
        this.suggestedValues = suggestedValues;
    }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getUnmatchedFields() { return unmatchedFields; }
    public void setUnmatchedFields(List<String> unmatchedFields) { this.unmatchedFields = unmatchedFields; }
}
