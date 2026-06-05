package com.workflow.politicas.model;

/** @deprecated Parte de {@link ActivityDiagram} (modelo B). */
@Deprecated(since = "0.0.1-cycle1-f0")
public class DiagramNode {
    private String id;
    private String type;
    private String label;
    private double x;
    private double y;
    private String lane;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getLane() {
        return lane;
    }

    public void setLane(String lane) {
        this.lane = lane;
    }
}
