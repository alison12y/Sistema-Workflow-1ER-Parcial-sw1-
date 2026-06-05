package com.workflow.politicas.dto;

import com.workflow.politicas.model.TramiteTask;

import java.util.ArrayList;
import java.util.List;

public class MonitoringTasksResponse {
    private String tramiteId;
    private String code;
    private MonitoringTaskCountsDto counts = new MonitoringTaskCountsDto();
    private List<TramiteTask> pending = new ArrayList<>();
    private List<TramiteTask> inProgress = new ArrayList<>();
    private List<TramiteTask> completed = new ArrayList<>();

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

    public MonitoringTaskCountsDto getCounts() {
        return counts;
    }

    public void setCounts(MonitoringTaskCountsDto counts) {
        this.counts = counts;
    }

    public List<TramiteTask> getPending() {
        return pending;
    }

    public void setPending(List<TramiteTask> pending) {
        this.pending = pending;
    }

    public List<TramiteTask> getInProgress() {
        return inProgress;
    }

    public void setInProgress(List<TramiteTask> inProgress) {
        this.inProgress = inProgress;
    }

    public List<TramiteTask> getCompleted() {
        return completed;
    }

    public void setCompleted(List<TramiteTask> completed) {
        this.completed = completed;
    }
}
