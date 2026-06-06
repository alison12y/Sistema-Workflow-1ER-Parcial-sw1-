package com.workflow.politicas.dto;

import java.util.ArrayList;
import java.util.List;

public class OfflineNotifyStoredRequest {

    private int pendingCount;
    private List<String> types = new ArrayList<>();

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types != null ? types : new ArrayList<>();
    }
}
