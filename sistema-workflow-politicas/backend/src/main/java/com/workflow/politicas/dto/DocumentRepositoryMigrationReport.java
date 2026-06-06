package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class DocumentRepositoryMigrationReport {

    private int totalTramites;
    private int repositoriesCreated;
    private int repositoriesAlreadyPresent;
    private final List<String> createdTramiteCodes = new ArrayList<>();

    public int getTotalTramites() {
        return totalTramites;
    }

    public void setTotalTramites(int totalTramites) {
        this.totalTramites = totalTramites;
    }

    public int getRepositoriesCreated() {
        return repositoriesCreated;
    }

    public void setRepositoriesCreated(int repositoriesCreated) {
        this.repositoriesCreated = repositoriesCreated;
    }

    public int getRepositoriesAlreadyPresent() {
        return repositoriesAlreadyPresent;
    }

    public void setRepositoriesAlreadyPresent(int repositoriesAlreadyPresent) {
        this.repositoriesAlreadyPresent = repositoriesAlreadyPresent;
    }

    public List<String> getCreatedTramiteCodes() {
        return createdTramiteCodes;
    }

    public void addCreatedTramiteCode(String code) {
        createdTramiteCodes.add(code);
    }
}
