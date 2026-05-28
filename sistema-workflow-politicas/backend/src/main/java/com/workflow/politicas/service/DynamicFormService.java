package com.workflow.politicas.service;

import com.workflow.politicas.dto.DynamicFormDetailResponse;
import com.workflow.politicas.dto.DynamicFormSaveRequest;
import com.workflow.politicas.dto.FormFieldDto;
import com.workflow.politicas.model.Activity;
import com.workflow.politicas.model.DynamicForm;
import com.workflow.politicas.model.FormField;
import com.workflow.politicas.repository.ActivityRepository;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.DynamicFormRepository;
import com.workflow.politicas.repository.FormFieldRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class DynamicFormService {

    private final DynamicFormRepository dynamicFormRepository;
    private final ActivityRepository activityRepository;
    private final FormFieldRepository formFieldRepository;
    private final BusinessPolicyRepository businessPolicyRepository;
    private final BitacoraService bitacoraService;

    public DynamicFormService(
            DynamicFormRepository dynamicFormRepository,
            ActivityRepository activityRepository,
            FormFieldRepository formFieldRepository,
            BusinessPolicyRepository businessPolicyRepository,
            BitacoraService bitacoraService
    ) {
        this.dynamicFormRepository = dynamicFormRepository;
        this.activityRepository = activityRepository;
        this.formFieldRepository = formFieldRepository;
        this.businessPolicyRepository = businessPolicyRepository;
        this.bitacoraService = bitacoraService;
    }

    public DynamicForm create(DynamicForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        form.setCreatedAt(LocalDateTime.now());
        form.setUpdatedAt(LocalDateTime.now());
        return dynamicFormRepository.save(form);
    }

    public Optional<DynamicForm> findByActivityId(String activityId) {
        Optional<Activity> activity = activityRepository.findById(activityId);
        if (activity.isEmpty()) {
            return Optional.empty();
        }
        String formId = activity.get().getDynamicFormId();
        if (formId == null || formId.isBlank()) {
            return Optional.empty();
        }
        return dynamicFormRepository.findById(formId);
    }

    public Optional<DynamicForm> findById(String id) {
        return dynamicFormRepository.findById(id);
    }

    public DynamicForm update(String id, DynamicForm details) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DynamicForm not found with id: " + id));
        if (details.getName() != null) {
            form.setName(details.getName());
        }
        if (details.getDescription() != null) {
            form.setDescription(details.getDescription());
        }
        form.setUpdatedAt(LocalDateTime.now());
        return dynamicFormRepository.save(form);
    }

    public DynamicFormDetailResponse getByPolicyAndActivity(String policyId, String activityName) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la política es obligatorio");
        }
        if (activityName == null || activityName.isBlank()) {
            throw new IllegalArgumentException("El nombre de la actividad es obligatorio");
        }

        Optional<DynamicForm> formOpt = dynamicFormRepository.findByPolicyIdAndActivityName(policyId, activityName);
        if (formOpt.isEmpty()) {
            DynamicFormDetailResponse empty = new DynamicFormDetailResponse();
            empty.setPolicyId(policyId);
            empty.setActivityName(activityName);
            empty.setFields(new ArrayList<>());
            return empty;
        }

        DynamicForm form = formOpt.get();
        DynamicFormDetailResponse response = new DynamicFormDetailResponse();
        response.setId(form.getId());
        response.setPolicyId(form.getPolicyId());
        response.setActivityName(form.getActivityName());
        response.setName(form.getName());
        response.setFields(mapFieldsToDto(formFieldRepository.findByFormId(form.getId())));
        return response;
    }

    public DynamicFormDetailResponse saveFull(DynamicFormSaveRequest request) {
        validateSaveRequest(request);

        DynamicForm form = dynamicFormRepository
                .findByPolicyIdAndActivityName(request.getPolicyId(), request.getActivityName())
                .orElseGet(DynamicForm::new);

        if (form.getId() == null) {
            form.setCreatedAt(LocalDateTime.now());
        }
        form.setPolicyId(request.getPolicyId());
        form.setActivityName(request.getActivityName());
        form.setName(request.getName());
        form.setDescription("Formulario de la actividad " + request.getActivityName());
        form.setUpdatedAt(LocalDateTime.now());
        DynamicForm savedForm = dynamicFormRepository.save(form);

        formFieldRepository.deleteByFormId(savedForm.getId());
        List<FormField> savedFields = new ArrayList<>();
        for (FormFieldDto fieldDto : request.getFields()) {
            FormField field = new FormField();
            field.setFormId(savedForm.getId());
            field.setLabel(fieldDto.getLabel().trim());
            field.setName(fieldDto.getName().trim());
            field.setType(normalizeFieldType(fieldDto.getType()));
            field.setRequired(fieldDto.isRequired());
            field.setValidationRules(fieldDto.getOptions());
            field.setOrder(fieldDto.getOrder());
            savedFields.add(formFieldRepository.save(field));
        }

        linkFormToActivityIfExists(savedForm);

        businessPolicyRepository.findById(savedForm.getPolicyId()).ifPresent(policy -> {
            String actor = bitacoraService.resolveActorDisplay();
            bitacoraService.registrar(
                    "Formularios",
                    "GUARDAR_FORMULARIO",
                    actor + " guardó el formulario dinámico de la política " + policy.getName(),
                    "DynamicForm",
                    savedForm.getId()
            );
        });

        DynamicFormDetailResponse response = new DynamicFormDetailResponse();
        response.setId(savedForm.getId());
        response.setPolicyId(savedForm.getPolicyId());
        response.setActivityName(savedForm.getActivityName());
        response.setName(savedForm.getName());
        response.setFields(mapFieldsToDto(savedFields));
        return response;
    }

    private void validateSaveRequest(DynamicFormSaveRequest request) {
        if (request.getPolicyId() == null || request.getPolicyId().isBlank()) {
            throw new IllegalArgumentException("El identificador de la política es obligatorio");
        }
        if (request.getActivityName() == null || request.getActivityName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la actividad es obligatorio");
        }
        if (request.getFields() == null || request.getFields().isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un campo al formulario");
        }
        for (int i = 0; i < request.getFields().size(); i++) {
            FormFieldDto field = request.getFields().get(i);
            if (field.getLabel() == null || field.getLabel().trim().isEmpty()) {
                throw new IllegalArgumentException("Todos los campos deben tener una etiqueta");
            }
            if (field.getName() == null || field.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Todos los campos deben tener un nombre");
            }
            if (field.getType() == null || field.getType().trim().isEmpty()) {
                throw new IllegalArgumentException("Todos los campos deben tener un tipo");
            }
            field.setOrder(i);
        }
        if (request.getName() == null || request.getName().isBlank()) {
            request.setName("Formulario - " + request.getActivityName());
        }
    }

    private void linkFormToActivityIfExists(DynamicForm savedForm) {
        activityRepository.findAll().stream()
                .filter(activity -> savedForm.getActivityName().equalsIgnoreCase(activity.getName()))
                .findFirst()
                .ifPresent(activity -> {
                    activity.setDynamicFormId(savedForm.getId());
                    activityRepository.save(activity);
                });
    }

    private List<FormFieldDto> mapFieldsToDto(List<FormField> fields) {
        return fields.stream()
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .map(field -> {
                    FormFieldDto dto = new FormFieldDto();
                    dto.setLabel(field.getLabel());
                    dto.setName(field.getName());
                    dto.setType(field.getType().toLowerCase(Locale.ROOT));
                    dto.setRequired(field.isRequired());
                    dto.setOptions(field.getValidationRules());
                    dto.setOrder(field.getOrder());
                    return dto;
                })
                .toList();
    }

    private String normalizeFieldType(String type) {
        return type == null ? "TEXT" : type.trim().toUpperCase(Locale.ROOT);
    }
}
