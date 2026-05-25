package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiWorkflowGenerateResponse {
    private List<Map<String, Object>> activities = new ArrayList<>();
    private List<Map<String, Object>> transitions = new ArrayList<>();
    private List<Map<String, Object>> swimlanes = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    public List<Map<String, Object>> getActivities() { return activities; }
    public void setActivities(List<Map<String, Object>> activities) { this.activities = activities; }

    public List<Map<String, Object>> getTransitions() { return transitions; }
    public void setTransitions(List<Map<String, Object>> transitions) { this.transitions = transitions; }

    public List<Map<String, Object>> getSwimlanes() { return swimlanes; }
    public void setSwimlanes(List<Map<String, Object>> swimlanes) { this.swimlanes = swimlanes; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}
