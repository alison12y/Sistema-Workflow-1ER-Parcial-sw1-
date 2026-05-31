package com.workflow.politicas.dto;



public class WorkflowTransitionDedupeResponse {

    private int removed;

    private int kept;

    private int deactivatedCount;

    private String message;



    public int getRemoved() { return removed; }

    public void setRemoved(int removed) { this.removed = removed; }



    public int getKept() { return kept; }

    public void setKept(int kept) { this.kept = kept; }



    public int getDeactivatedCount() { return deactivatedCount; }

    public void setDeactivatedCount(int deactivatedCount) { this.deactivatedCount = deactivatedCount; }



    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

}

