package com.workflow.politicas.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PolicyDetailResponse {
    private String id;
    private String name;
    private String description;
    private String type;
    private String status;
    private String version;
    private String responsible;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int activityCount;
    private int transitionCount;
    private int tramiteCount;
    private List<WorkflowActivityResponse> activities = new ArrayList<>();
    private List<WorkflowTransitionResponse> transitions = new ArrayList<>();
    private List<String> flowPreview = new ArrayList<>();
    private List<TramiteSummaryResponse> tramites = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getActivityCount() { return activityCount; }
    public void setActivityCount(int activityCount) { this.activityCount = activityCount; }

    public int getTransitionCount() { return transitionCount; }
    public void setTransitionCount(int transitionCount) { this.transitionCount = transitionCount; }

    public int getTramiteCount() { return tramiteCount; }
    public void setTramiteCount(int tramiteCount) { this.tramiteCount = tramiteCount; }

    public List<WorkflowActivityResponse> getActivities() { return activities; }
    public void setActivities(List<WorkflowActivityResponse> activities) { this.activities = activities; }

    public List<WorkflowTransitionResponse> getTransitions() { return transitions; }
    public void setTransitions(List<WorkflowTransitionResponse> transitions) { this.transitions = transitions; }

    public List<String> getFlowPreview() { return flowPreview; }
    public void setFlowPreview(List<String> flowPreview) { this.flowPreview = flowPreview; }

    public List<TramiteSummaryResponse> getTramites() { return tramites; }
    public void setTramites(List<TramiteSummaryResponse> tramites) { this.tramites = tramites; }
}
