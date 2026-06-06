package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsRiskResponse {

    private String summary;
    private String source;
    private List<AnalyticsRiskItemDto> risks = new ArrayList<>();
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

    public List<AnalyticsRiskItemDto> getRisks() {
        return risks;
    }

    public void setRisks(List<AnalyticsRiskItemDto> risks) {
        this.risks = risks != null ? risks : new ArrayList<>();
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
