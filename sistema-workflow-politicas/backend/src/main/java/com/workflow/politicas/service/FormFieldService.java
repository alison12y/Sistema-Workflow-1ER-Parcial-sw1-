package com.workflow.politicas.service;

import com.workflow.politicas.model.FormField;
import com.workflow.politicas.repository.DynamicFormRepository;
import com.workflow.politicas.repository.FormFieldRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FormFieldService {

    private final FormFieldRepository formFieldRepository;
    private final DynamicFormRepository dynamicFormRepository;

    public FormFieldService(FormFieldRepository formFieldRepository, DynamicFormRepository dynamicFormRepository) {
        this.formFieldRepository = formFieldRepository;
        this.dynamicFormRepository = dynamicFormRepository;
    }

    public List<FormField> findByFormId(String formId) {
        return formFieldRepository.findByFormId(formId);
    }

    public Optional<FormField> findById(String id) {
        return formFieldRepository.findById(id);
    }

    public FormField create(FormField field) {
        if (field.getFormId() == null || field.getFormId().isBlank()) {
            throw new IllegalArgumentException("formId is required");
        }
        if (!dynamicFormRepository.existsById(field.getFormId())) {
            throw new IllegalArgumentException("DynamicForm not found: " + field.getFormId());
        }
        if (field.getName() == null || field.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (field.getLabel() == null || field.getLabel().isBlank()) {
            throw new IllegalArgumentException("label is required");
        }
        if (field.getType() == null || field.getType().isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        return formFieldRepository.save(field);
    }

    public FormField update(String id, FormField details) {
        FormField field = formFieldRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FormField not found with id: " + id));
        if (details.getLabel() != null) {
            field.setLabel(details.getLabel());
        }
        if (details.getName() != null) {
            field.setName(details.getName());
        }
        if (details.getType() != null) {
            field.setType(details.getType());
        }
        field.setRequired(details.isRequired());
        if (details.getValidationRules() != null) {
            field.setValidationRules(details.getValidationRules());
        }
        field.setOrder(details.getOrder());
        return formFieldRepository.save(field);
    }

    public void deleteById(String id) {
        if (!formFieldRepository.existsById(id)) {
            throw new RuntimeException("FormField not found with id: " + id);
        }
        formFieldRepository.deleteById(id);
    }
}
