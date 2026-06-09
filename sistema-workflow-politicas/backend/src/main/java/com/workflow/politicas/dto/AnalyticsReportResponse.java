package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsReportResponse {

    private String title;
    private String explanation;
    /** Respuesta directa a la consulta del usuario. */
    private String conclusion;
    private String reportType;
    private List<String> columns = new ArrayList<>();
    private List<Map<String, Object>> rows = new ArrayList<>();
    private Map<String, Object> appliedFilters = new LinkedHashMap<>();
    /** PANTALLA, PDF, EXCEL, WORD */
    private String suggestedFormat;
    private String source;
    private List<String> warnings = new ArrayList<>();
    private List<AnalyticsSummaryCardDto> cards = new ArrayList<>();
    private AnalyticsChartDto chart;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns != null ? columns : new ArrayList<>();
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }

    public Map<String, Object> getAppliedFilters() {
        return appliedFilters;
    }

    public void setAppliedFilters(Map<String, Object> appliedFilters) {
        this.appliedFilters = appliedFilters != null ? appliedFilters : new LinkedHashMap<>();
    }

    public String getSuggestedFormat() {
        return suggestedFormat;
    }

    public void setSuggestedFormat(String suggestedFormat) {
        this.suggestedFormat = suggestedFormat;
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

    public List<AnalyticsSummaryCardDto> getCards() {
        return cards;
    }

    public void setCards(List<AnalyticsSummaryCardDto> cards) {
        this.cards = cards != null ? cards : new ArrayList<>();
    }

    public AnalyticsChartDto getChart() {
        return chart;
    }

    public void setChart(AnalyticsChartDto chart) {
        this.chart = chart;
    }
}
