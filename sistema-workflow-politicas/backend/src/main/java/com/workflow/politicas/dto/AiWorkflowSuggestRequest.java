package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class AiWorkflowSuggestRequest {
    private String policyId;
    private String prompt;
    private String userId;
    private List<AiWorkflowContextActivityDto> activities = new ArrayList<>();
    private List<AiWorkflowContextTransitionDto> transitions = new ArrayList<>();
    private List<AiWorkflowContextLaneDto> lanes = new ArrayList<>();

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<AiWorkflowContextActivityDto> getActivities() { return activities; }
    public void setActivities(List<AiWorkflowContextActivityDto> activities) { this.activities = activities; }

    public List<AiWorkflowContextTransitionDto> getTransitions() { return transitions; }
    public void setTransitions(List<AiWorkflowContextTransitionDto> transitions) { this.transitions = transitions; }

    public List<AiWorkflowContextLaneDto> getLanes() { return lanes; }
    public void setLanes(List<AiWorkflowContextLaneDto> lanes) { this.lanes = lanes; }
}
