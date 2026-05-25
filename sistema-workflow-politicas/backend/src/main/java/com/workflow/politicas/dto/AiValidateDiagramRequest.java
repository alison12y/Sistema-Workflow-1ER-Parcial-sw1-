package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiValidateDiagramRequest {
    private List<Map<String, Object>> activities = new ArrayList<>();
    private List<Map<String, Object>> transitions = new ArrayList<>();
    private String userId;

    public List<Map<String, Object>> getActivities() { return activities; }
    public void setActivities(List<Map<String, Object>> activities) { this.activities = activities; }

    public List<Map<String, Object>> getTransitions() { return transitions; }
    public void setTransitions(List<Map<String, Object>> transitions) { this.transitions = transitions; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
