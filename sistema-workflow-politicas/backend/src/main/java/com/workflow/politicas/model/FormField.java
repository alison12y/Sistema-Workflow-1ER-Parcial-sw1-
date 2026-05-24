package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "form_fields")
public class FormField {
    @Id
    private String id;
    private String formId;
    private String label;
    private String name;
    private String type; // "TEXT", "NUMBER", "DATE", "SELECT", etc.
    private boolean required;
    private String validationRules;
    private int order;

    public FormField() {
    }

    public FormField(String id, String formId, String label, String name, String type, boolean required, String validationRules, int order) {
        this.id = id;
        this.formId = formId;
        this.label = label;
        this.name = name;
        this.type = type;
        this.required = required;
        this.validationRules = validationRules;
        this.order = order;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(String validationRules) {
        this.validationRules = validationRules;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
