package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;

/** @deprecated Modelo BPM legacy (C). Usar {@link TramiteTask}. */
@Deprecated(since = "0.0.1-cycle1-f0")
@Document(collection = "task_instances")
public class TaskInstance {
    @Id
    private String id;
    private String processInstanceId;
    private String activityId;
    private String assignedUserId;
    private String assignedRoleId;
    private String status;
    private Map<String, Object> stepData;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime dueDate;

    public TaskInstance() {}

    public TaskInstance(String id, String processInstanceId, String activityId, String assignedUserId, String assignedRoleId, String status, Map<String, Object> stepData, LocalDateTime createdAt, LocalDateTime completedAt, LocalDateTime dueDate) {
        this.id = id;
        this.processInstanceId = processInstanceId;
        this.activityId = activityId;
        this.assignedUserId = assignedUserId;
        this.assignedRoleId = assignedRoleId;
        this.status = status;
        this.stepData = stepData;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.dueDate = dueDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }

    public String getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(String assignedUserId) { this.assignedUserId = assignedUserId; }

    public String getAssignedRoleId() { return assignedRoleId; }
    public void setAssignedRoleId(String assignedRoleId) { this.assignedRoleId = assignedRoleId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getStepData() { return stepData; }
    public void setStepData(Map<String, Object> stepData) { this.stepData = stepData; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
}
