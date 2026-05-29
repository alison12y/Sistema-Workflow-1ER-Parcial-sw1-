package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AuthResponse {
    private String token;
    private String username;
    private String fullName;
    private String role;
    private String roleName;
    private List<String> roles = new ArrayList<>();
    private Set<String> permissions = new HashSet<>();

    public AuthResponse() {}

    public AuthResponse(String token, String username, String fullName, String role) {
        this.token = token;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
}
