package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/** @deprecated Modelo BPM legacy (C). Usar {@link WorkflowActivity} por {@code policyId}. */
@Deprecated(since = "0.0.1-cycle1-f0")
@Document(collection = "workflow_diagrams")
public class WorkflowDiagram {
    @Id
    private String id;
    private String policyId;
    private String xmlData;
    private String jsonData;
    private String version;
    private LocalDateTime createdAt;

    public WorkflowDiagram() {}

    public WorkflowDiagram(String id, String policyId, String xmlData, String jsonData, String version, LocalDateTime createdAt) {
        this.id = id;
        this.policyId = policyId;
        this.xmlData = xmlData;
        this.jsonData = jsonData;
        this.version = version;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getXmlData() { return xmlData; }
    public void setXmlData(String xmlData) { this.xmlData = xmlData; }

    public String getJsonData() { return jsonData; }
    public void setJsonData(String jsonData) { this.jsonData = jsonData; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
