package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsRecommendationResponse {

    private String summary;
    private String source;
    private List<AnalyticsRecommendationItemDto> recommendations = new ArrayList<>();
    private List<AnalyticsSummaryCardDto> cards = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<AnalyticsRecommendationItemDto> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<AnalyticsRecommendationItemDto> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    public List<AnalyticsSummaryCardDto> getCards() {
        return cards;
    }

    public void setCards(List<AnalyticsSummaryCardDto> cards) {
        this.cards = cards != null ? cards : new ArrayList<>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
}
