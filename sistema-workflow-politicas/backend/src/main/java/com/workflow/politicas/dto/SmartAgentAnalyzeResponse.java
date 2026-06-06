package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class SmartAgentAnalyzeResponse {

    private String detectedIntent;
    private String recommendedPolicyId;
    private String recommendedPolicyName;
    private Double confidenceScore;
    private String explanation;
    private List<String> requiredDocuments = new ArrayList<>();
    private List<SmartAgentSuggestedField> suggestedFields = new ArrayList<>();
    private String source;
    private List<String> warnings = new ArrayList<>();
    private String attachmentFileName;

    public SmartAgentAnalyzeResponse() {
    }

    public String getDetectedIntent() {
        return detectedIntent;
    }

    public void setDetectedIntent(String detectedIntent) {
        this.detectedIntent = detectedIntent;
    }

    public String getRecommendedPolicyId() {
        return recommendedPolicyId;
    }

    public void setRecommendedPolicyId(String recommendedPolicyId) {
        this.recommendedPolicyId = recommendedPolicyId;
    }

    public String getRecommendedPolicyName() {
        return recommendedPolicyName;
    }

    public void setRecommendedPolicyName(String recommendedPolicyName) {
        this.recommendedPolicyName = recommendedPolicyName;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<String> getRequiredDocuments() {
        return requiredDocuments;
    }

    public void setRequiredDocuments(List<String> requiredDocuments) {
        this.requiredDocuments = requiredDocuments != null ? requiredDocuments : new ArrayList<>();
    }

    public List<SmartAgentSuggestedField> getSuggestedFields() {
        return suggestedFields;
    }

    public void setSuggestedFields(List<SmartAgentSuggestedField> suggestedFields) {
        this.suggestedFields = suggestedFields != null ? suggestedFields : new ArrayList<>();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public String getAttachmentFileName() {
        return attachmentFileName;
    }

    public void setAttachmentFileName(String attachmentFileName) {
        this.attachmentFileName = attachmentFileName;
    }
}
