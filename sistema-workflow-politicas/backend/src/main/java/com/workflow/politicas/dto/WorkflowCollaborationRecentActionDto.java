package com.workflow.politicas.dto;

import java.time.LocalDateTime;

public class WorkflowCollaborationRecentActionDto {

    private String actionType;
    private String actionLabel;
    private String elementType;
    private String elementName;
    private String modifiedByUserId;
    private String modifiedByUsername;
    private String modifiedByDisplayName;
    private String modifiedByName;
    private LocalDateTime modifiedAt;
    private String summary;

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public String getElementType() {
        return elementType;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public String getModifiedByUserId() {
        return modifiedByUserId;
    }

    public void setModifiedByUserId(String modifiedByUserId) {
        this.modifiedByUserId = modifiedByUserId;
    }

    public String getModifiedByUsername() {
        return modifiedByUsername;
    }

    public void setModifiedByUsername(String modifiedByUsername) {
        this.modifiedByUsername = modifiedByUsername;
    }

    public String getModifiedByDisplayName() {
        return modifiedByDisplayName;
    }

    public void setModifiedByDisplayName(String modifiedByDisplayName) {
        this.modifiedByDisplayName = modifiedByDisplayName;
    }

    public String getModifiedByName() {
        return modifiedByName;
    }

    public void setModifiedByName(String modifiedByName) {
        this.modifiedByName = modifiedByName;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
