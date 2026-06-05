package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated Modelo paralelo pre-consolidación. Ciclo 1 oficial: {@link WorkflowActivity}
 *             + {@link WorkflowTransition}. Mantener por compatibilidad con {@code TramiteService}
 *             y {@code /api/activity-diagrams} hasta F1.
 */
@Deprecated(since = "0.0.1-cycle1-f0")
@Document(collection = "activity_diagrams")
public class ActivityDiagram {
    @Id
    private String id;
    private String policyId;
    private String name;
    private List<String> lanes = new ArrayList<>();
    private List<DiagramNode> nodes = new ArrayList<>();
    private List<DiagramEdge> edges = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLanes() {
        return lanes;
    }

    public void setLanes(List<String> lanes) {
        this.lanes = lanes;
    }

    public List<DiagramNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<DiagramNode> nodes) {
        this.nodes = nodes;
    }

    public List<DiagramEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<DiagramEdge> edges) {
        this.edges = edges;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
