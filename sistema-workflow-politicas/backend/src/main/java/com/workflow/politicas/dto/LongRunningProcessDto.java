package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class LongRunningProcessDto {
    private String processId;
    private String policyId;
    private String policyName;
    private double hoursInExecution;
    private LocalDateTime startedAt;

    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public double getHoursInExecution() { return hoursInExecution; }
    public void setHoursInExecution(double hoursInExecution) { this.hoursInExecution = hoursInExecution; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
}
