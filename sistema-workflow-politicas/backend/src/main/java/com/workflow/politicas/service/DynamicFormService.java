package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.*;
import com.workflow.politicas.model.Activity;
import com.workflow.politicas.model.DynamicForm;
import com.workflow.politicas.model.FormField;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.repository.ActivityRepository;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.DynamicFormRepository;
import com.workflow.politicas.repository.FormFieldRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.util.FormFieldKeyUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DynamicFormService {

    private static final Map<String, String> FIELD_TYPE_LABELS = Map.of(
            "TEXT", "Texto corto",
            "TEXTAREA", "Texto largo",
            "NUMBER", "Número",
            "DATE", "Fecha",
            "SELECT", "Lista desplegable",
            "CHECKBOX", "Checkbox",
            "FILE", "Archivo",
            "OBSERVATION", "Observación"
    );

    private static final Set<String> ALLOWED_FIELD_TYPES = Set.of(
            "TEXT", "TEXTAREA", "NUMBER", "DATE", "SELECT", "CHECKBOX", "FILE", "OBSERVATION"
    );

    private final DynamicFormRepository dynamicFormRepository;
    private final ActivityRepository activityRepository;
    private final FormFieldRepository formFieldRepository;
    private final BusinessPolicyRepository businessPolicyRepository;
    private final WorkflowActivityRepository workflowActivityRepository;
    private final BitacoraService bitacoraService;

    public DynamicFormService(
            DynamicFormRepository dynamicFormRepository,
            ActivityRepository activityRepository,
            FormFieldRepository formFieldRepository,
            BusinessPolicyRepository businessPolicyRepository,
            WorkflowActivityRepository workflowActivityRepository,
            BitacoraService bitacoraService
    ) {
        this.dynamicFormRepository = dynamicFormRepository;
        this.activityRepository = activityRepository;
        this.formFieldRepository = formFieldRepository;
        this.businessPolicyRepository = businessPolicyRepository;
        this.workflowActivityRepository = workflowActivityRepository;
        this.bitacoraService = bitacoraService;
    }

    public DynamicFormResponse getByActivityId(String activityId) {
        WorkflowActivity activity = findWorkflowActivity(activityId);
        Optional<DynamicForm> formOpt = resolveFormForActivity(activity);
        if (formOpt.isEmpty()) {
            DynamicFormResponse empty = new DynamicFormResponse();
            empty.setActivityId(activity.getId());
            empty.setActivityName(activity.getName());
            empty.setPolicyId(activity.getPolicyId());
            empty.setActive(false);
            empty.setFields(List.of());
            return empty;
        }
        return toResponse(formOpt.get(), activity.getName(), true);
    }

    public DynamicFormResponse create(DynamicFormRequest request) {
        validateFormRequest(request, true);
        WorkflowActivity activity = findWorkflowActivity(request.getActivityId());
        String policyId = request.getPolicyId() != null ? request.getPolicyId() : activity.getPolicyId();
        validatePolicyExists(policyId);

        if (dynamicFormRepository.findByActivityIdAndActiveTrue(activity.getId()).stream()
                .anyMatch(form -> form.isActive())) {
            throw new IllegalArgumentException(
                    "La actividad ya tiene un formulario activo. Edite el existente o desactívelo primero.");
        }

        DynamicForm form = new DynamicForm();
        form.setActivityId(activity.getId());
        form.setPolicyId(policyId);
        form.setActivityName(activity.getName());
        form.setName(request.getName().trim());
        form.setDescription(trimOrNull(request.getDescription()));
        form.setActive(request.getActive() == null || Boolean.TRUE.equals(request.getActive()));
        form.setCreatedAt(LocalDateTime.now());
        form.setUpdatedAt(LocalDateTime.now());

        DynamicForm saved = dynamicFormRepository.save(form);
        linkFormToWorkflowActivity(activity, saved);
        linkFormToLegacyActivityIfExists(saved);

        return toResponse(saved, activity.getName(), true);
    }

    public DynamicFormResponse update(String id, DynamicFormRequest request) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado"));

        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new IllegalArgumentException("El nombre del formulario es obligatorio");
            }
            form.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            form.setDescription(trimOrNull(request.getDescription()));
        }
        if (request.getActive() != null) {
            form.setActive(request.getActive());
        }
        if (request.getActivityId() != null && !request.getActivityId().equals(form.getActivityId())) {
            WorkflowActivity activity = findWorkflowActivity(request.getActivityId());
            form.setActivityId(activity.getId());
            form.setActivityName(activity.getName());
            form.setPolicyId(activity.getPolicyId());
            linkFormToWorkflowActivity(activity, form);
        }
        form.setUpdatedAt(LocalDateTime.now());
        return toResponse(dynamicFormRepository.save(form), resolveActivityName(form), true);
    }

    public WorkflowDeleteResponse delete(String id) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado"));
        form.setActive(false);
        form.setUpdatedAt(LocalDateTime.now());
        dynamicFormRepository.save(form);
        unlinkFormFromWorkflowActivity(form.getActivityId());
        auditFormAction(AuditActions.ELIMINAR_FORMULARIO, "eliminó", form);

        WorkflowDeleteResponse response = new WorkflowDeleteResponse();
        response.setLogicalDelete(true);
        response.setMessage("Formulario desactivado correctamente.");
        return response;
    }

    public DynamicFormResponse activate(String id) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado"));
        if (form.getActivityId() != null
                && dynamicFormRepository.existsByActivityIdAndActiveTrueAndIdNot(form.getActivityId(), form.getId())) {
            throw new IllegalArgumentException("Ya existe otro formulario activo para esta actividad.");
        }
        form.setActive(true);
        form.setUpdatedAt(LocalDateTime.now());
        DynamicForm saved = dynamicFormRepository.save(form);
        if (form.getActivityId() != null) {
            workflowActivityRepository.findById(form.getActivityId()).ifPresent(activity -> linkFormToWorkflowActivity(activity, saved));
        }
        return toResponse(saved, resolveActivityName(saved), true);
    }

    public DynamicFormResponse deactivate(String id) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado"));
        form.setActive(false);
        form.setUpdatedAt(LocalDateTime.now());
        dynamicFormRepository.save(form);
        unlinkFormFromWorkflowActivity(form.getActivityId());
        return toResponse(form, resolveActivityName(form), true);
    }

    // ——— Compatibilidad diseño anterior ———

    public DynamicForm createLegacy(DynamicForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del formulario es obligatorio");
        }
        form.setActive(true);
        form.setCreatedAt(LocalDateTime.now());
        form.setUpdatedAt(LocalDateTime.now());
        return dynamicFormRepository.save(form);
    }

    public Optional<DynamicForm> findByActivityIdLegacy(String activityId) {
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

    public DynamicForm updateLegacy(String id, DynamicForm details) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado"));
        if (details.getName() != null) {
            form.setName(details.getName());
        }
        if (details.getDescription() != null) {
            form.setDescription(details.getDescription());
        }
        form.setUpdatedAt(LocalDateTime.now());
        return dynamicFormRepository.save(form);
    }

    /**
     * Formulario para ejecución (F4) — clave oficial {@code workflowActivityId}.
     */
    public DynamicFormDetailResponse getDetailByWorkflowActivityId(String workflowActivityId) {
        WorkflowActivity activity = findWorkflowActivity(workflowActivityId);
        Optional<DynamicForm> formOpt = resolveFormForActivity(activity);
        DynamicFormDetailResponse response = new DynamicFormDetailResponse();
        response.setWorkflowActivityId(activity.getId());
        response.setPolicyId(activity.getPolicyId());
        response.setActivityName(activity.getName());
        if (formOpt.isEmpty()) {
            response.setFields(new ArrayList<>());
            return response;
        }
        DynamicForm form = formOpt.get();
        response.setId(form.getId());
        response.setName(form.getName());
        response.setFields(mapFieldsToLegacyDto(
                formFieldRepository.findByFormIdOrderByOrderAsc(form.getId()).stream()
                        .filter(FormField::isActive)
                        .toList()
        ));
        return response;
    }

    public DynamicFormDetailResponse getByPolicyAndActivity(String policyId, String activityName) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("La política de negocio es obligatoria");
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
        response.setFields(mapFieldsToLegacyDto(formFieldRepository.findByFormIdOrderByOrderAsc(form.getId())));
        return response;
    }

    public DynamicFormDetailResponse saveFull(DynamicFormSaveRequest request) {
        validateSaveRequest(request);

        DynamicForm form = dynamicFormRepository
                .findByPolicyIdAndActivityName(request.getPolicyId(), request.getActivityName())
                .orElseGet(DynamicForm::new);

        boolean creating = form.getId() == null;
        List<FormField> previousFields = creating
                ? List.of()
                : formFieldRepository.findByFormIdOrderByOrderAsc(form.getId());

        if (creating) {
            form.setCreatedAt(LocalDateTime.now());
            form.setActive(true);
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
            FormField field = buildFieldFromLegacyDto(savedForm.getId(), fieldDto);
            savedFields.add(formFieldRepository.save(field));
        }

        linkFormToLegacyActivityIfExists(savedForm);
        syncWorkflowActivityByName(savedForm);

        auditFormAction(
                creating ? AuditActions.CREAR_FORMULARIO : AuditActions.EDITAR_FORMULARIO,
                creating ? "creó" : "editó",
                savedForm
        );
        auditFieldChanges(previousFields, savedFields, savedForm);

        DynamicFormDetailResponse response = new DynamicFormDetailResponse();
        response.setId(savedForm.getId());
        response.setPolicyId(savedForm.getPolicyId());
        response.setActivityName(savedForm.getActivityName());
        response.setName(savedForm.getName());
        response.setFields(mapFieldsToLegacyDto(savedFields));
        return response;
    }

    public DynamicFormResponse getById(String id) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado"));
        return toResponse(form, resolveActivityName(form), true);
    }

    public DynamicFormResponse toResponse(DynamicForm form, String activityName, boolean includeFields) {
        DynamicFormResponse response = new DynamicFormResponse();
        response.setId(form.getId());
        response.setActivityId(form.getActivityId());
        response.setActivityName(activityName != null ? activityName : form.getActivityName());
        response.setPolicyId(form.getPolicyId());
        response.setName(form.getName());
        response.setDescription(form.getDescription());
        response.setActive(form.isActive());
        response.setCreatedAt(form.getCreatedAt());
        response.setUpdatedAt(form.getUpdatedAt());
        if (includeFields && form.getId() != null) {
            response.setFields(formFieldRepository.findByFormIdOrderByOrderAsc(form.getId()).stream()
                    .map(this::toFieldResponse)
                    .toList());
        } else {
            response.setFields(List.of());
        }
        return response;
    }

    FormFieldResponse toFieldResponse(FormField field) {
        FormFieldResponse response = new FormFieldResponse();
        response.setId(field.getId());
        response.setFormId(field.getFormId());
        response.setName(field.getName());
        response.setLabel(field.getLabel());
        String normalizedType = normalizeFieldType(field.getType());
        response.setFieldType(normalizedType);
        response.setFieldTypeLabel(fieldTypeLabel(normalizedType));
        response.setRequired(field.isRequired());
        response.setOptions(field.resolvedOptions());
        response.setOrderIndex(field.getOrder());
        response.setPlaceholder(field.getPlaceholder());
        response.setHelpText(field.getHelpText());
        response.setActive(field.isActive());
        response.setCreatedAt(field.getCreatedAt());
        response.setUpdatedAt(field.getUpdatedAt());
        return response;
    }

    String normalizeFieldType(String type) {
        if (type == null || type.isBlank()) {
            return "TEXT";
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TEXT", "TEXTAREA", "NUMBER", "DATE", "SELECT", "CHECKBOX", "FILE", "OBSERVATION" -> normalized;
            default -> normalized;
        };
    }

    String fieldTypeLabel(String type) {
        return FIELD_TYPE_LABELS.getOrDefault(normalizeFieldType(type), type);
    }

    void validateFieldType(String fieldType) {
        String normalized = normalizeFieldType(fieldType);
        if (!ALLOWED_FIELD_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Tipo de campo no válido: " + fieldType);
        }
    }

    private WorkflowActivity findWorkflowActivity(String activityId) {
        if (activityId == null || activityId.isBlank()) {
            throw new IllegalArgumentException("La actividad es obligatoria");
        }
        return workflowActivityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
    }

    private void validatePolicyExists(String policyId) {
        businessPolicyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("La política seleccionada no existe"));
    }

    private void validateFormRequest(DynamicFormRequest request, boolean creating) {
        if (creating && (request.getActivityId() == null || request.getActivityId().isBlank())) {
            throw new IllegalArgumentException("La actividad es obligatoria");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del formulario es obligatorio");
        }
    }

    private Optional<DynamicForm> resolveFormForActivity(WorkflowActivity activity) {
        if (activity.getFormId() != null && !activity.getFormId().isBlank()) {
            Optional<DynamicForm> byId = dynamicFormRepository.findById(activity.getFormId());
            if (byId.isPresent()) {
                return byId;
            }
        }
        return dynamicFormRepository.findByActivityId(activity.getId());
    }

    private void linkFormToWorkflowActivity(WorkflowActivity activity, DynamicForm form) {
        activity.setFormId(form.getId());
        activity.setUpdatedAt(LocalDateTime.now());
        workflowActivityRepository.save(activity);
        form.setActivityId(activity.getId());
        form.setActivityName(activity.getName());
        form.setPolicyId(activity.getPolicyId());
        dynamicFormRepository.save(form);
    }

    private void unlinkFormFromWorkflowActivity(String activityId) {
        if (activityId == null || activityId.isBlank()) {
            return;
        }
        workflowActivityRepository.findById(activityId).ifPresent(activity -> {
            activity.setFormId(null);
            activity.setUpdatedAt(LocalDateTime.now());
            workflowActivityRepository.save(activity);
        });
    }

    private void syncWorkflowActivityByName(DynamicForm savedForm) {
        if (savedForm.getPolicyId() == null || savedForm.getActivityName() == null) {
            return;
        }
        workflowActivityRepository.findByPolicyIdOrderByOrderIndexAsc(savedForm.getPolicyId()).stream()
                .filter(activity -> savedForm.getActivityName().equalsIgnoreCase(activity.getName()))
                .findFirst()
                .ifPresent(activity -> linkFormToWorkflowActivity(activity, savedForm));
    }

    private String resolveActivityName(DynamicForm form) {
        if (form.getActivityId() != null) {
            return workflowActivityRepository.findById(form.getActivityId())
                    .map(WorkflowActivity::getName)
                    .orElse(form.getActivityName());
        }
        return form.getActivityName();
    }

    private void linkFormToLegacyActivityIfExists(DynamicForm savedForm) {
        activityRepository.findAll().stream()
                .filter(activity -> savedForm.getActivityName() != null
                        && savedForm.getActivityName().equalsIgnoreCase(activity.getName()))
                .findFirst()
                .ifPresent(activity -> {
                    activity.setDynamicFormId(savedForm.getId());
                    activityRepository.save(activity);
                });
    }

    private void validateSaveRequest(DynamicFormSaveRequest request) {
        if (request.getPolicyId() == null || request.getPolicyId().isBlank()) {
            throw new IllegalArgumentException("La política de negocio es obligatoria");
        }
        if (request.getActivityName() == null || request.getActivityName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la actividad es obligatorio");
        }
        if (request.getFields() == null || request.getFields().isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un campo al formulario");
        }
        Set<String> fieldNames = new HashSet<>();
        for (int i = 0; i < request.getFields().size(); i++) {
            FormFieldDto field = request.getFields().get(i);
            if (field.getLabel() == null || field.getLabel().trim().isEmpty()) {
                throw new IllegalArgumentException("Todos los campos deben tener una etiqueta");
            }
            if (field.getName() == null || field.getName().trim().isEmpty()) {
                field.setName(FormFieldKeyUtil.generateFromLabel(field.getLabel()));
            }
            FormFieldKeyUtil.validateTechnicalName(field.getName());
            String key = field.getName().trim().toLowerCase(Locale.ROOT);
            if (!fieldNames.add(key)) {
                throw new IllegalArgumentException("Los nombres de campo deben ser únicos: " + field.getName());
            }
            if (field.getType() == null || field.getType().trim().isEmpty()) {
                throw new IllegalArgumentException("Todos los campos deben tener un tipo");
            }
            validateFieldType(field.getType());
            field.setOrder(i);
        }
        if (request.getName() == null || request.getName().isBlank()) {
            request.setName("Formulario - " + request.getActivityName());
        }
    }

    private FormField buildFieldFromLegacyDto(String formId, FormFieldDto fieldDto) {
        FormField field = new FormField();
        field.setFormId(formId);
        field.setLabel(fieldDto.getLabel().trim());
        field.setName(fieldDto.getName().trim());
        field.setType(normalizeFieldType(fieldDto.getType()));
        field.setRequired(fieldDto.isRequired());
        field.setOptions(fieldDto.getOptions());
        field.setValidationRules(fieldDto.getOptions());
        field.setOrder(fieldDto.getOrder());
        field.setActive(true);
        field.setCreatedAt(LocalDateTime.now());
        field.setUpdatedAt(LocalDateTime.now());
        return field;
    }

    private List<FormFieldDto> mapFieldsToLegacyDto(List<FormField> fields) {
        return fields.stream()
                .sorted(Comparator.comparingInt(FormField::getOrder))
                .map(field -> {
                    FormFieldDto dto = new FormFieldDto();
                    dto.setLabel(field.getLabel());
                    dto.setName(field.getName());
                    dto.setType(field.getType().toLowerCase(Locale.ROOT));
                    dto.setRequired(field.isRequired());
                    dto.setOptions(field.resolvedOptions());
                    dto.setOrder(field.getOrder());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void auditFormAction(String action, String verb, DynamicForm form) {
        String actor = bitacoraService.resolveActorDisplay();
        String formName = form.getName() != null ? form.getName() : form.getActivityName();
        bitacoraService.registrar(
                AuditModules.FORMULARIOS,
                action,
                actor + " " + verb + " el formulario \"" + formName + "\"",
                "DynamicForm",
                form.getId()
        );
    }

    private void auditFieldChanges(List<FormField> previous, List<FormField> current, DynamicForm form) {
        String actor = bitacoraService.resolveActorDisplay();
        Map<String, FormField> beforeByName = previous.stream()
                .filter(f -> f.getName() != null)
                .collect(Collectors.toMap(FormField::getName, f -> f, (a, b) -> a));
        Map<String, FormField> afterByName = current.stream()
                .filter(f -> f.getName() != null)
                .collect(Collectors.toMap(FormField::getName, f -> f, (a, b) -> a));

        for (String name : afterByName.keySet()) {
            if (!beforeByName.containsKey(name)) {
                bitacoraService.registrar(
                        AuditModules.FORMULARIOS,
                        AuditActions.CREAR_CAMPO,
                        actor + " creó el campo \"" + name + "\" en formulario " + form.getName(),
                        "FormField",
                        afterByName.get(name).getId()
                );
            } else if (!fieldSnapshot(beforeByName.get(name)).equals(fieldSnapshot(afterByName.get(name)))) {
                bitacoraService.registrar(
                        AuditModules.FORMULARIOS,
                        AuditActions.EDITAR_CAMPO,
                        actor + " editó el campo \"" + name + "\" en formulario " + form.getName(),
                        "FormField",
                        afterByName.get(name).getId()
                );
            }
        }
        for (String name : beforeByName.keySet()) {
            if (!afterByName.containsKey(name)) {
                bitacoraService.registrar(
                        AuditModules.FORMULARIOS,
                        AuditActions.ELIMINAR_CAMPO,
                        actor + " eliminó el campo \"" + name + "\" del formulario " + form.getName(),
                        "FormField",
                        beforeByName.get(name).getId()
                );
            }
        }
    }

    private static String fieldSnapshot(FormField field) {
        return field.getLabel() + "|" + field.getType() + "|" + field.isRequired() + "|" + field.getOrder();
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
