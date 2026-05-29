package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Set;

@Document(collection = "roles")
public class Role {
    @Id
    private String id;
    private String name;
    private String description;
    private Set<String> permissionIds;
    private Boolean active;

    public Role() {}

    public Role(String id, String name, String description, Set<String> permissionIds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.permissionIds = permissionIds;
        this.active = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(Set<String> permissionIds) { this.permissionIds = permissionIds; }

    public boolean isActive() {
        return active == null || Boolean.TRUE.equals(active);
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
