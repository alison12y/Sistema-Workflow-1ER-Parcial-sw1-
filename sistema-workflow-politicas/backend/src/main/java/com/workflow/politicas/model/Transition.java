package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "transitions")
public class Transition {
    @Id
    private String id;
    private String diagramId;
    private String sourceActivityId;
    private String targetActivityId;
    private String condition;

    public Transition() {}

    public Transition(String id, String diagramId, String sourceActivityId, String targetActivityId, String condition) {
        this.id = id;
        this.diagramId = diagramId;
        this.sourceActivityId = sourceActivityId;
        this.targetActivityId = targetActivityId;
        this.condition = condition;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDiagramId() { return diagramId; }
    public void setDiagramId(String diagramId) { this.diagramId = diagramId; }

    public String getSourceActivityId() { return sourceActivityId; }
    public void setSourceActivityId(String sourceActivityId) { this.sourceActivityId = sourceActivityId; }

    public String getTargetActivityId() { return targetActivityId; }
    public void setTargetActivityId(String targetActivityId) { this.targetActivityId = targetActivityId; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
}
