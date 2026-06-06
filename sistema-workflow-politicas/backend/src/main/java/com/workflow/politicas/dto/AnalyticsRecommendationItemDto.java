package com.workflow.politicas.dto;

public class AnalyticsRecommendationItemDto {

    /** ALTA, MEDIA, BAJA */
    private String priority;
    private String type;
    private String title;
    private String action;
    private String rationale;
    private String tramiteCode;
    private String activityName;

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getTramiteCode() {
        return tramiteCode;
    }

    public void setTramiteCode(String tramiteCode) {
        this.tramiteCode = tramiteCode;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }
}
