package com.workflow.politicas.dto;

public class WorkflowTransitionCleanupResponse {
    private int removedDuplicates;
    private int removedOrphans;
    private int kept;
    private String message;
    private boolean duplicateActivitiesDetected;
    private String warning;

    public int getRemovedDuplicates() { return removedDuplicates; }
    public void setRemovedDuplicates(int removedDuplicates) { this.removedDuplicates = removedDuplicates; }

    public int getRemovedOrphans() { return removedOrphans; }
    public void setRemovedOrphans(int removedOrphans) { this.removedOrphans = removedOrphans; }

    public int getKept() { return kept; }
    public void setKept(int kept) { this.kept = kept; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isDuplicateActivitiesDetected() { return duplicateActivitiesDetected; }
    public void setDuplicateActivitiesDetected(boolean duplicateActivitiesDetected) {
        this.duplicateActivitiesDetected = duplicateActivitiesDetected;
    }

    public String getWarning() { return warning; }
    public void setWarning(String warning) { this.warning = warning; }
}
