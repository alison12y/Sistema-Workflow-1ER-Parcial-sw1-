package com.workflow.politicas.dto;

public class RolePendingTasksDto {
    private String roleId;
    private long pendingTaskCount;

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public long getPendingTaskCount() { return pendingTaskCount; }
    public void setPendingTaskCount(long pendingTaskCount) { this.pendingTaskCount = pendingTaskCount; }
}
