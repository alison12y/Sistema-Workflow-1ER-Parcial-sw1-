package com.workflow.politicas.dto;

/** Metadatos opcionales al registrar una modificación colaborativa. */
public class WorkflowCollaborationModificationRequest {

    private String actionType;
    private String actionLabel;
    private String elementType;
    private String elementName;

    public WorkflowCollaborationModificationRequest() {
    }

    public WorkflowCollaborationModificationRequest(
            String actionType,
            String actionLabel,
            String elementType,
            String elementName
    ) {
        this.actionType = actionType;
        this.actionLabel = actionLabel;
        this.elementType = elementType;
        this.elementName = elementName;
    }

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
}
