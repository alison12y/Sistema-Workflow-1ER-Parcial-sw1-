package com.workflow.politicas.dto;

import java.util.Set;

public class UserRequest {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String departmentId;
    private Set<String> roleIds;
    private boolean active;

    public UserRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public Set<String> getRoleIds() { return roleIds; }
    public void setRoleIds(Set<String> roleIds) { this.roleIds = roleIds; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
