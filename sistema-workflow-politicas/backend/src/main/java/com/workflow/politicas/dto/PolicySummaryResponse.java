package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class PolicySummaryResponse {
    private String id;
    private String name;
    private String description;
    private String type;
    private String status;
    private String version;
    private String responsible;
    private String createdBy;
    private LocalDateTime createdAt;
    private int activityCount;
    private int tramiteCount;

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

    public int getActivityCount() { return activityCount; }
    public void setActivityCount(int activityCount) { this.activityCount = activityCount; }

    public int getTramiteCount() { return tramiteCount; }
    public void setTramiteCount(int tramiteCount) { this.tramiteCount = tramiteCount; }
}
