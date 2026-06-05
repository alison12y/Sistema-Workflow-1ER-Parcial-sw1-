package com.workflow.politicas.model;

/** @deprecated Parte de {@link ActivityDiagram} (modelo B). */
@Deprecated(since = "0.0.1-cycle1-f0")
public class DiagramEdge {
    private String id;
    private String sourceId;
    private String targetId;
    private String label;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
