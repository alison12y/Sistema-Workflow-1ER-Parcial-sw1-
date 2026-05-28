package com.workflow.politicas.dto;

import com.workflow.politicas.model.TraceItem;
import com.workflow.politicas.model.TramiteTask;

import java.util.ArrayList;
import java.util.List;

public class MonitoringTraceResponse {
    private String code;
    private String policyName;
    private String status;
    private String currentActivity;
    private String responsible;
    private int progress;
    private List<TramiteTask> tasks = new ArrayList<>();
    private List<TraceItem> events = new ArrayList<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(String currentActivity) {
        this.currentActivity = currentActivity;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public List<TramiteTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<TramiteTask> tasks) {
        this.tasks = tasks;
    }

    public List<TraceItem> getEvents() {
        return events;
    }

    public void setEvents(List<TraceItem> events) {
        this.events = events;
    }
}
