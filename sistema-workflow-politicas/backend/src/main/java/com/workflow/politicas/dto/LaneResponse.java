package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class LaneResponse {
    private String laneName;
    private String responsibleType;
    private List<ActivityNodeResponse> activities = new ArrayList<>();

    public String getLaneName() { return laneName; }
    public void setLaneName(String laneName) { this.laneName = laneName; }

    public String getResponsibleType() { return responsibleType; }
    public void setResponsibleType(String responsibleType) { this.responsibleType = responsibleType; }

    public List<ActivityNodeResponse> getActivities() { return activities; }
    public void setActivities(List<ActivityNodeResponse> activities) { this.activities = activities; }
}
