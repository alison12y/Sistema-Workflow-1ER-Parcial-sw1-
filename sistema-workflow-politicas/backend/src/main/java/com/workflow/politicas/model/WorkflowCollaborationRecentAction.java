package com.workflow.politicas.model;

import java.time.LocalDateTime;

/** Entrada del historial reciente de cambios colaborativos. */
public class WorkflowCollaborationRecentAction {

    private String actionType;
    private String actionLabel;
    private String elementType;
    private String elementName;
    private String modifiedByUserId;
    private String modifiedByUsername;
    private String modifiedByDisplayName;
    private LocalDateTime modifiedAt;

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

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}
