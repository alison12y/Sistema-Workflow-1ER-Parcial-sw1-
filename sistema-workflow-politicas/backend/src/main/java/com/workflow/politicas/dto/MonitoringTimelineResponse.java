package com.workflow.politicas.dto;

import com.workflow.politicas.model.TraceItem;

import java.util.ArrayList;
import java.util.List;

public class MonitoringTimelineResponse {
    private String tramiteId;
    private String code;
    private List<TraceItem> events = new ArrayList<>();

    public String getTramiteId() {
        return tramiteId;
    }

    public void setTramiteId(String tramiteId) {
        this.tramiteId = tramiteId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<TraceItem> getEvents() {
        return events;
    }

    public void setEvents(List<TraceItem> events) {
        this.events = events;
    }
}
