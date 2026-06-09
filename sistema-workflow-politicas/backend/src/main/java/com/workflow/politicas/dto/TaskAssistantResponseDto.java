package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class TaskAssistantResponseDto {

    private String summary;
    private List<String> importantData = new ArrayList<>();
    private List<String> missingData = new ArrayList<>();
    private String recommendedAction;
    /** AI o LOCAL_FALLBACK */
    private String source;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getImportantData() {
        return importantData;
    }

    public void setImportantData(List<String> importantData) {
        this.importantData = importantData != null ? importantData : new ArrayList<>();
    }

    public List<String> getMissingData() {
        return missingData;
    }

    public void setMissingData(List<String> missingData) {
        this.missingData = missingData != null ? missingData : new ArrayList<>();
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
