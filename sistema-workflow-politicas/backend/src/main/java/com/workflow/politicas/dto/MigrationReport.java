package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class MigrationReport {

    private final List<String> rolesCreated = new ArrayList<>();
    private final List<String> rolesUpdated = new ArrayList<>();
    private final List<String> departmentsCreated = new ArrayList<>();
    private final List<String> departmentsUpdated = new ArrayList<>();
    private final List<String> usersCreated = new ArrayList<>();
    private final List<String> usersUpdated = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public List<String> getRolesCreated() {
        return rolesCreated;
    }

    public List<String> getRolesUpdated() {
        return rolesUpdated;
    }

    public List<String> getDepartmentsCreated() {
        return departmentsCreated;
    }

    public List<String> getDepartmentsUpdated() {
        return departmentsUpdated;
    }

    public List<String> getUsersCreated() {
        return usersCreated;
    }

    public List<String> getUsersUpdated() {
        return usersUpdated;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }
}
