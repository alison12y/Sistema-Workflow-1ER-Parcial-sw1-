package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class WorkflowDesignerResponse {
    private String policyId;
    private String policyName;
    private String policyDescription;
    private String policyStatus;
    private List<ActivityNodeResponse> activities = new ArrayList<>();
    private List<TransitionEdgeResponse> transitions = new ArrayList<>();
    private List<LaneResponse> lanes = new ArrayList<>();
    private List<String> flowPreview = new ArrayList<>();
    private WorkflowFlowValidationResponse flowValidation;

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public String getPolicyDescription() { return policyDescription; }
    public void setPolicyDescription(String policyDescription) { this.policyDescription = policyDescription; }

    public String getPolicyStatus() { return policyStatus; }
    public void setPolicyStatus(String policyStatus) { this.policyStatus = policyStatus; }

    public List<ActivityNodeResponse> getActivities() { return activities; }
    public void setActivities(List<ActivityNodeResponse> activities) { this.activities = activities; }

    public List<TransitionEdgeResponse> getTransitions() { return transitions; }
    public void setTransitions(List<TransitionEdgeResponse> transitions) { this.transitions = transitions; }

    public List<LaneResponse> getLanes() { return lanes; }
    public void setLanes(List<LaneResponse> lanes) { this.lanes = lanes; }

    public List<String> getFlowPreview() { return flowPreview; }
    public void setFlowPreview(List<String> flowPreview) { this.flowPreview = flowPreview; }

    public WorkflowFlowValidationResponse getFlowValidation() { return flowValidation; }
    public void setFlowValidation(WorkflowFlowValidationResponse flowValidation) { this.flowValidation = flowValidation; }
}
