package com.workflow.politicas.dto;

public class FormFieldRequest {
    private String formId;
    private String label;
    /** Nombre técnico / variable usada en stepData y condiciones del workflow. */
    private String name;
    private String fieldType;
    private Boolean required;
    private String options;
    private Integer orderIndex;
    private String placeholder;
    private String helpText;
    private Boolean active;

    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
    public String getHelpText() { return helpText; }
    public void setHelpText(String helpText) { this.helpText = helpText; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
