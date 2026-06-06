package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsChartDto {

    /** bar, pie */
    private String type;
    private String title;
    private List<String> labels = new ArrayList<>();
    private List<Double> values = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels != null ? labels : new ArrayList<>();
    }

    public List<Double> getValues() {
        return values;
    }

    public void setValues(List<Double> values) {
        this.values = values != null ? values : new ArrayList<>();
    }
}
