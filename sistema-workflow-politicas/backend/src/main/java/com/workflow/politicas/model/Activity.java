package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "activities")
public class Activity {
    @Id
    private String id;
    private String diagramId;
    private String name;
    private String type; // "START", "TASK", "DECISION", "PARALLEL_GATEWAY", "END"
    private String swimlaneId; // Department or Role responsible
    private String dynamicFormId;

    public Activity() {
    }

    public Activity(String id, String diagramId, String name, String type, String swimlaneId, String dynamicFormId) {
        this.id = id;
        this.diagramId = diagramId;
        this.name = name;
        this.type = type;
        this.swimlaneId = swimlaneId;
        this.dynamicFormId = dynamicFormId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDiagramId() {
        return diagramId;
    }

    public void setDiagramId(String diagramId) {
        this.diagramId = diagramId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSwimlaneId() {
        return swimlaneId;
    }

    public void setSwimlaneId(String swimlaneId) {
        this.swimlaneId = swimlaneId;
    }

    public String getDynamicFormId() {
        return dynamicFormId;
    }

    public void setDynamicFormId(String dynamicFormId) {
        this.dynamicFormId = dynamicFormId;
    }
}
