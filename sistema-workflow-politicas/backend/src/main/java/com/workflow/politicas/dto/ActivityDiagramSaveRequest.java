package com.workflow.politicas.dto;

import com.workflow.politicas.model.DiagramEdge;
import com.workflow.politicas.model.DiagramNode;

import java.util.List;

public class ActivityDiagramSaveRequest {
    private String policyId;
    private String name;
    private List<String> lanes;
    private List<DiagramNode> nodes;
    private List<DiagramEdge> edges;

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
}
