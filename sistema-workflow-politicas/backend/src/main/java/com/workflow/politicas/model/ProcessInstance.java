package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "process_instances")
public class ProcessInstance {
    @Id
    private String id;
    private String policyId;
    private String status;
    private String initiatorId;
    private Map<String, Object> formData;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String currentActivityId;

    public ProcessInstance() {}

    public ProcessInstance(String id, String policyId, String status, String initiatorId, Map<String, Object> formData, LocalDateTime startedAt, LocalDateTime endedAt, String currentActivityId) {
        this.id = id;
        this.policyId = policyId;
        this.status = status;
        this.initiatorId = initiatorId;
        this.formData = formData;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.currentActivityId = currentActivityId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getInitiatorId() { return initiatorId; }
    public void setInitiatorId(String initiatorId) { this.initiatorId = initiatorId; }

    public Map<String, Object> getFormData() { return formData; }
    public void setFormData(Map<String, Object> formData) { this.formData = formData; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public String getCurrentActivityId() { return currentActivityId; }
    public void setCurrentActivityId(String currentActivityId) { this.currentActivityId = currentActivityId; }
}
