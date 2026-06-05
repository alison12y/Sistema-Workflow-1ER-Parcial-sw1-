package com.workflow.politicas.service;

import com.workflow.politicas.dto.FormFieldRequest;
import com.workflow.politicas.dto.FormFieldResponse;
import com.workflow.politicas.dto.WorkflowDeleteResponse;
import com.workflow.politicas.model.DynamicForm;
import com.workflow.politicas.model.FormField;
import com.workflow.politicas.repository.DynamicFormRepository;
import com.workflow.politicas.repository.FormFieldRepository;
import com.workflow.politicas.util.FormFieldKeyUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class FormFieldService {

    private final FormFieldRepository formFieldRepository;
    private final DynamicFormRepository dynamicFormRepository;
    private final DynamicFormService dynamicFormService;

    public FormFieldService(
            FormFieldRepository formFieldRepository,
            DynamicFormRepository dynamicFormRepository,
            DynamicFormService dynamicFormService
    ) {
        this.formFieldRepository = formFieldRepository;
        this.dynamicFormRepository = dynamicFormRepository;
        this.dynamicFormService = dynamicFormService;
    }

    public List<FormFieldResponse> findByFormId(String formId) {
        validateFormExists(formId);
        return formFieldRepository.findByFormIdOrderByOrderAsc(formId).stream()
                .map(dynamicFormService::toFieldResponse)
                .toList();
    }

    public Optional<FormFieldResponse> findById(String id) {
        return formFieldRepository.findById(id).map(dynamicFormService::toFieldResponse);
    }

    public FormFieldResponse create(FormFieldRequest request) {
        validateFieldRequest(request, true);
        DynamicForm form = validateFormExists(request.getFormId());

        FormField field = new FormField();
        applyRequest(field, request, form.getId());
        field.setName(resolveFieldName(request, form.getId(), null));
        field.setCreatedAt(LocalDateTime.now());
        field.setUpdatedAt(LocalDateTime.now());
        if (request.getActive() == null) {
            field.setActive(true);
        }

        touchForm(form);
        return dynamicFormService.toFieldResponse(formFieldRepository.save(field));
    }

    public FormFieldResponse update(String id, FormFieldRequest request) {
        FormField field = formFieldRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campo no encontrado"));
        validateFieldRequest(request, false);
        applyRequest(field, request, field.getFormId());
        if (request.getName() != null && !request.getName().isBlank()) {
            field.setName(resolveFieldName(request, field.getFormId(), field.getId()));
        } else if (field.getName() == null || field.getName().isBlank()) {
            field.setName(generateFieldName(request.getLabel(), field.getFormId()));
        }
        field.setUpdatedAt(LocalDateTime.now());
        dynamicFormRepository.findById(field.getFormId()).ifPresent(this::touchForm);
        return dynamicFormService.toFieldResponse(formFieldRepository.save(field));
    }

    public WorkflowDeleteResponse delete(String id) {
        FormField field = formFieldRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campo no encontrado"));
        field.setActive(false);
        field.setUpdatedAt(LocalDateTime.now());
        formFieldRepository.save(field);
        dynamicFormRepository.findById(field.getFormId()).ifPresent(this::touchForm);

        WorkflowDeleteResponse response = new WorkflowDeleteResponse();
        response.setLogicalDelete(true);
        response.setMessage("Campo eliminado correctamente.");
        return response;
    }

    public FormFieldResponse activate(String id) {
        FormField field = formFieldRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campo no encontrado"));
        field.setActive(true);
        field.setUpdatedAt(LocalDateTime.now());
        dynamicFormRepository.findById(field.getFormId()).ifPresent(this::touchForm);
        return dynamicFormService.toFieldResponse(formFieldRepository.save(field));
    }

    public FormFieldResponse deactivate(String id) {
        FormField field = formFieldRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campo no encontrado"));
        field.setActive(false);
        field.setUpdatedAt(LocalDateTime.now());
        dynamicFormRepository.findById(field.getFormId()).ifPresent(this::touchForm);
        return dynamicFormService.toFieldResponse(formFieldRepository.save(field));
    }

    // Compatibilidad legado
    public List<FormField> findEntitiesByFormId(String formId) {
        return formFieldRepository.findByFormId(formId);
    }

    public FormField createLegacy(FormField field) {
        if (field.getFormId() == null || field.getFormId().isBlank()) {
            throw new IllegalArgumentException("El formulario es obligatorio");
        }
        validateFormExists(field.getFormId());
        if (field.getLabel() == null || field.getLabel().isBlank()) {
            throw new IllegalArgumentException("La etiqueta del campo es obligatoria");
        }
        if (field.getType() == null || field.getType().isBlank()) {
            throw new IllegalArgumentException("El tipo de campo es obligatorio");
        }
        if (field.getName() == null || field.getName().isBlank()) {
            field.setName(generateFieldName(field.getLabel(), field.getFormId()));
        }
        field.setActive(true);
        field.setCreatedAt(LocalDateTime.now());
        field.setUpdatedAt(LocalDateTime.now());
        return formFieldRepository.save(field);
    }

    public FormField updateLegacy(String id, FormField details) {
        FormField field = formFieldRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campo no encontrado"));
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
            field.setOptions(details.getValidationRules());
        }
        field.setOrder(details.getOrder());
        field.setUpdatedAt(LocalDateTime.now());
        return formFieldRepository.save(field);
    }

    public void deletePhysical(String id) {
        if (!formFieldRepository.existsById(id)) {
            throw new IllegalArgumentException("Campo no encontrado");
        }
        formFieldRepository.deleteById(id);
    }

    private DynamicForm validateFormExists(String formId) {
        return dynamicFormRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado"));
    }

    private void validateFieldRequest(FormFieldRequest request, boolean creating) {
        if (creating && (request.getFormId() == null || request.getFormId().isBlank())) {
            throw new IllegalArgumentException("El formulario es obligatorio");
        }
        if (request.getLabel() == null || request.getLabel().isBlank()) {
            throw new IllegalArgumentException("La etiqueta del campo es obligatoria");
        }
        if (request.getFieldType() == null || request.getFieldType().isBlank()) {
            throw new IllegalArgumentException("El tipo de campo es obligatorio");
        }
        dynamicFormService.validateFieldType(request.getFieldType());
        String normalizedType = dynamicFormService.normalizeFieldType(request.getFieldType());
        if ("SELECT".equals(normalizedType)) {
            if (request.getOptions() == null || request.getOptions().isBlank()) {
                throw new IllegalArgumentException("Debe indicar opciones para campos de lista desplegable");
            }
        }
        if (request.getOrderIndex() != null && request.getOrderIndex() < 1) {
            throw new IllegalArgumentException("El orden debe ser un número positivo");
        }
    }

    private String resolveFieldName(FormFieldRequest request, String formId, String excludeFieldId) {
        return FormFieldKeyUtil.resolveTechnicalName(
                request.getName(),
                request.getLabel(),
                candidate -> nameExists(formId, candidate, excludeFieldId)
        );
    }

    private void applyRequest(FormField field, FormFieldRequest request, String formId) {
        field.setFormId(formId);
        field.setLabel(request.getLabel().trim());
        String normalizedType = dynamicFormService.normalizeFieldType(request.getFieldType());
        field.setType(normalizedType);
        field.setRequired(request.getRequired() != null && request.getRequired());
        field.setOptions(trimOrNull(request.getOptions()));
        field.setValidationRules(field.getOptions());
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            field.setOrder(request.getOrderIndex());
        } else if (field.getOrder() < 1) {
            field.setOrder(resolveNextOrder(formId));
        }
        field.setPlaceholder(trimOrNull(request.getPlaceholder()));
        field.setHelpText(trimOrNull(request.getHelpText()));
        if (request.getActive() != null) {
            field.setActive(request.getActive());
        }
    }

    private int resolveNextOrder(String formId) {
        return formFieldRepository.findByFormId(formId).stream()
                .mapToInt(FormField::getOrder)
                .max()
                .orElse(0) + 1;
    }

    private String generateFieldName(String label, String formId) {
        String base = label == null ? "campo" : java.text.Normalizer.normalize(label, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
        if (base.isBlank()) {
            base = "campo";
        }
        String candidate = base;
        int suffix = 1;
        while (nameExists(formId, candidate)) {
            suffix++;
            candidate = base + "_" + suffix;
        }
        return candidate;
    }

    private boolean nameExists(String formId, String name, String excludeFieldId) {
        return formFieldRepository.findByFormId(formId).stream()
                .filter(field -> excludeFieldId == null || !excludeFieldId.equals(field.getId()))
                .anyMatch(field -> name.equalsIgnoreCase(field.getName()));
    }

    private boolean nameExists(String formId, String name) {
        return nameExists(formId, name, null);
    }

    private void touchForm(DynamicForm form) {
        form.setUpdatedAt(LocalDateTime.now());
        dynamicFormRepository.save(form);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
