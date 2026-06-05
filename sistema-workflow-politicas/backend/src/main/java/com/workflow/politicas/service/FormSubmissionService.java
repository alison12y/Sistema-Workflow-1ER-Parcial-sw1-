package com.workflow.politicas.service;



import com.workflow.politicas.dto.DynamicFormDetailResponse;

import com.workflow.politicas.dto.FormFieldDto;

import com.workflow.politicas.dto.FormSubmissionRequest;

import com.workflow.politicas.dto.FormSubmissionResponse;

import com.workflow.politicas.dto.ResponseItemDto;

import com.workflow.politicas.model.FormSubmission;

import com.workflow.politicas.model.ResponseItem;

import com.workflow.politicas.model.User;

import com.workflow.politicas.repository.FormSubmissionRepository;

import com.workflow.politicas.repository.UserRepository;

import com.workflow.politicas.util.Cu7WorkflowDebugLog;
import org.springframework.stereotype.Service;



import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.Comparator;

import java.util.HashSet;

import java.util.Locale;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

import java.util.Optional;

import java.util.Set;



@Service

public class FormSubmissionService {



    private static final String OBSERVATION_FALLBACK_FIELD = "observacion_cierre";



    private final FormSubmissionRepository formSubmissionRepository;

    private final DynamicFormService dynamicFormService;

    private final UserRepository userRepository;

    private final FormSubmissionFileService formSubmissionFileService;

    private final WorkflowFormConditionValidationService workflowFormConditionValidationService;



    public FormSubmissionService(

            FormSubmissionRepository formSubmissionRepository,

            DynamicFormService dynamicFormService,

            UserRepository userRepository,

            FormSubmissionFileService formSubmissionFileService,

            WorkflowFormConditionValidationService workflowFormConditionValidationService

    ) {

        this.formSubmissionRepository = formSubmissionRepository;

        this.dynamicFormService = dynamicFormService;

        this.userRepository = userRepository;

        this.formSubmissionFileService = formSubmissionFileService;

        this.workflowFormConditionValidationService = workflowFormConditionValidationService;

    }



    public FormSubmissionResponse save(FormSubmissionRequest request, String username) {

        return save(request, username, false);

    }



    public FormSubmissionResponse save(FormSubmissionRequest request, String username, boolean markSubmitted) {

        validateRequest(request);



        User user = userRepository.findByUsername(username)

                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));



        FormSubmission submission = resolveExistingSubmission(request).orElseGet(FormSubmission::new);



        if (submission.getId() == null) {

            submission.setCreatedAt(LocalDateTime.now());

        }



        LocalDateTime now = LocalDateTime.now();

        submission.setTramiteId(request.getTramiteId());

        submission.setPolicyId(request.getPolicyId());

        submission.setWorkflowActivityId(resolveWorkflowActivityId(request));

        submission.setActivityName(resolveActivityName(request));

        submission.setTaskOrder(request.getTaskOrder());

        submission.setTaskKey(buildTaskKey(request.getTramiteId(), request.getTaskOrder()));

        submission.setSubmittedBy(user.getUsername());

        submission.setSubmittedByName(displayName(user));

        submission.setResponses(mapToEntities(request.getResponses(), submission));

        submission.setUpdatedAt(now);

        if (markSubmitted) {

            submission.setSubmittedAt(now);

        }



        return toResponse(formSubmissionRepository.save(submission));

    }



    public List<FormSubmissionResponse> findByTramite(String tramiteId) {

        return formSubmissionRepository.findByTramiteId(tramiteId).stream()

                .sorted(Comparator.comparingInt(FormSubmission::getTaskOrder))

                .map(this::toResponse)

                .toList();

    }



    public Optional<FormSubmissionResponse> findByTramiteAndWorkflowActivity(

            String tramiteId,

            String workflowActivityId,

            int taskOrder

    ) {

        if (workflowActivityId != null && !workflowActivityId.isBlank()) {

            Optional<FormSubmissionResponse> byId = formSubmissionRepository

                    .findByTramiteIdAndWorkflowActivityIdAndTaskOrder(tramiteId, workflowActivityId.trim(), taskOrder)

                    .map(this::toResponse);

            if (byId.isPresent()) {

                return byId;

            }

        }

        return Optional.empty();

    }



    public Optional<FormSubmissionResponse> findByTramiteAndActivity(

            String tramiteId,

            String activityName,

            int taskOrder

    ) {

        if (activityName != null && !activityName.isBlank()) {

            return formSubmissionRepository

                    .findByTramiteIdAndActivityNameAndTaskOrder(tramiteId, activityName.trim(), taskOrder)

                    .map(this::toResponse);

        }

        return Optional.empty();

    }



    public Optional<FormSubmissionResponse> findForTask(

            String tramiteId,

            String workflowActivityId,

            String activityName,

            int taskOrder

    ) {

        return findByTramiteAndWorkflowActivity(tramiteId, workflowActivityId, taskOrder)

                .or(() -> findByTramiteAndActivity(tramiteId, activityName, taskOrder));

    }



    /** @deprecated Usar {@link #validateResponsesForWorkflowActivity(String, String, List, boolean)} */

    public void validateRequiredResponses(

            String policyId,

            String activityName,

            List<ResponseItemDto> responses

    ) {

        DynamicFormDetailResponse form = dynamicFormService.getByPolicyAndActivity(policyId, activityName);

        validateAgainstFormDefinition(null, policyId, form, responses, true);

    }



    public void validateResponsesForWorkflowActivity(

            String workflowActivityId,

            String policyId,

            List<ResponseItemDto> responses,

            boolean completing

    ) {

        if (workflowActivityId == null || workflowActivityId.isBlank()) {

            throw new IllegalArgumentException("La actividad de workflow es obligatoria");

        }

        DynamicFormDetailResponse form = dynamicFormService.getDetailByWorkflowActivityId(workflowActivityId);

        validateAgainstFormDefinition(workflowActivityId, policyId, form, responses, completing);

    }



    public boolean hasConfiguredForm(String workflowActivityId) {

        if (workflowActivityId == null || workflowActivityId.isBlank()) {

            return false;

        }

        DynamicFormDetailResponse form = dynamicFormService.getDetailByWorkflowActivityId(workflowActivityId);

        return form.getId() != null && form.getFields() != null && !form.getFields().isEmpty();

    }



    public Map<String, Object> buildStepDataForWorkflowActivity(

            String workflowActivityId,

            List<ResponseItemDto> responses

    ) {

        DynamicFormDetailResponse form = dynamicFormService.getDetailByWorkflowActivityId(workflowActivityId);

        Map<String, Object> stepData = buildStepDataFromFormAndResponses(form, responses);

        Cu7WorkflowDebugLog.log(
                "buildStepData activityId={} formId={} fields={} responses={} stepData={}",
                workflowActivityId,
                form.getId(),
                summarizeFormFields(form),
                summarizeResponses(responses),
                Cu7WorkflowDebugLog.stepDataSummary(stepData)
        );

        return stepData;

    }



    public Map<String, Object> buildStepDataFromResponses(List<ResponseItemDto> responses) {

        return buildStepDataFromFormAndResponses(null, responses);

    }



    public void validateStepDataForWorkflowCompletion(

            String workflowActivityId,

            String policyId,

            Map<String, Object> stepData

    ) {

        if (policyId == null || policyId.isBlank()) {

            return;

        }

        boolean hasForm = hasConfiguredForm(workflowActivityId);

        workflowFormConditionValidationService.validateStepDataForCompletion(

                workflowActivityId,

                policyId,

                stepData,

                hasForm

        );

    }



    private Map<String, Object> buildStepDataFromFormAndResponses(

            DynamicFormDetailResponse form,

            List<ResponseItemDto> responses

    ) {

        Map<String, Object> stepData = new HashMap<>();

        Map<String, ResponseItemDto> responsesByName = indexResponses(responses);



        List<FormFieldDto> fields = form != null && form.getFields() != null ? form.getFields() : List.of();

        if (!fields.isEmpty()) {

            for (FormFieldDto field : fields) {

                String key = field.getName() != null ? field.getName().trim() : "";

                if (key.isBlank()) {

                    continue;

                }

                ResponseItemDto item = responsesByName.get(key.toLowerCase(Locale.ROOT));

                if (item != null) {

                    stepData.put(key, coerceValue(item));

                } else if ("CHECKBOX".equalsIgnoreCase(normalizeFieldType(field.getType()))) {

                    stepData.put(key, Boolean.FALSE);

                }

            }

            mergeResponsesIntoStepData(stepData, responses);

            return stepData;

        }



        if (responses == null) {

            return stepData;

        }

        mergeResponsesIntoStepData(stepData, responses);

        return stepData;

    }



    private void mergeResponsesIntoStepData(Map<String, Object> stepData, List<ResponseItemDto> responses) {

        if (responses == null) {

            return;

        }

        for (ResponseItemDto item : responses) {

            if (item.getFieldName() != null && !item.getFieldName().isBlank()) {

                String key = item.getFieldName().trim();

                stepData.put(key, coerceValue(item));

            }

        }

    }



    private String summarizeFormFields(DynamicFormDetailResponse form) {

        if (form == null || form.getFields() == null) {

            return "[]";

        }

        return form.getFields().stream()

                .map(f -> (f.getName() != null ? f.getName() : "?") + ":" + f.getType())

                .reduce((a, b) -> a + ", " + b)

                .map(s -> "[" + s + "]")

                .orElse("[]");

    }



    private String summarizeResponses(List<ResponseItemDto> responses) {

        if (responses == null || responses.isEmpty()) {

            return "[]";

        }

        return responses.stream()

                .map(r -> (r.getFieldName() != null ? r.getFieldName() : "?")

                        + "="

                        + r.getValue()

                        + (r.getValue() != null ? "(" + r.getValue().getClass().getSimpleName() + ")" : ""))

                .reduce((a, b) -> a + ", " + b)

                .map(s -> "[" + s + "]")

                .orElse("[]");

    }



    private void validateAgainstFormDefinition(

            String workflowActivityId,

            String policyId,

            DynamicFormDetailResponse form,

            List<ResponseItemDto> responses,

            boolean completing

    ) {

        List<FormFieldDto> fields = form.getFields() == null ? List.of() : form.getFields();

        if (fields.isEmpty()) {

            if (completing) {

                validateNoFormCompletion(workflowActivityId, policyId, responses);

            }

            return;

        }



        validateUniqueResponseKeys(responses);



        Map<String, ResponseItemDto> responsesByName = indexResponses(responses);



        for (FormFieldDto field : fields) {

            String fieldName = field.getName() != null ? field.getName().trim() : "";

            if (fieldName.isBlank()) {

                continue;

            }

            ResponseItemDto item = responsesByName.get(fieldName);

            String fieldType = normalizeFieldType(field.getType());



            if (field.isRequired()) {

                assertRequiredValue(field, fieldType, item);

            }



            if (item != null && hasDisplayValue(item)) {

                assertFieldValue(field, fieldType, item);

            }

        }

    }



    private void validateNoFormCompletion(

            String workflowActivityId,

            String policyId,

            List<ResponseItemDto> responses

    ) {

        Map<String, Object> stepData = buildStepDataFromResponses(responses);

        if (policyId != null && !policyId.isBlank()) {

            workflowFormConditionValidationService.validateStepDataForCompletion(

                    workflowActivityId,

                    policyId,

                    stepData,

                    false

            );

        }

        if (responses == null || responses.isEmpty()) {

            throw new IllegalArgumentException(

                    "No hay formulario configurado para esta actividad. Indique una observación para completar.");

        }

        boolean hasContent = responses.stream().anyMatch(this::hasDisplayValue);

        if (!hasContent) {

            throw new IllegalArgumentException(

                    "No hay formulario configurado para esta actividad. Indique una observación para completar.");

        }

    }



    private void validateUniqueResponseKeys(List<ResponseItemDto> responses) {

        if (responses == null) {

            return;

        }

        Set<String> seen = new HashSet<>();

        for (ResponseItemDto item : responses) {

            if (item.getFieldName() == null || item.getFieldName().isBlank()) {

                continue;

            }

            String key = item.getFieldName().trim().toLowerCase(Locale.ROOT);

            if (!seen.add(key)) {

                throw new IllegalArgumentException("Respuesta duplicada para el campo: " + item.getFieldName());

            }

        }

    }



    private void assertRequiredValue(FormFieldDto field, String fieldType, ResponseItemDto item) {

        if ("file".equalsIgnoreCase(fieldType)) {

            if (item == null || item.getFileId() == null || item.getFileId().isBlank()

                    || !formSubmissionFileService.exists(item.getFileId())) {

                throw new IllegalArgumentException("Complete los campos obligatorios: " + field.getLabel());

            }

            return;

        }

        if ("checkbox".equalsIgnoreCase(fieldType)) {

            if (item == null) {

                throw new IllegalArgumentException("Complete los campos obligatorios: " + field.getLabel());

            }

            return;

        }

        String value = item != null ? item.getValue() : null;

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException("Complete los campos obligatorios: " + field.getLabel());

        }

    }



    private void assertFieldValue(FormFieldDto field, String fieldType, ResponseItemDto item) {

        String value = item.getValue();

        switch (fieldType) {

            case "NUMBER" -> {

                if (value != null && !value.isBlank()) {

                    try {

                        Double.parseDouble(value.trim().replace(",", "."));

                    } catch (NumberFormatException ex) {

                        throw new IllegalArgumentException("Valor numérico inválido en: " + field.getLabel());

                    }

                }

            }

            case "SELECT", "RADIO" -> assertOptionAllowed(field, value);

            case "CHECKBOX" -> {

                if (value != null && !value.isBlank()

                        && !"true".equalsIgnoreCase(value.trim())

                        && !"false".equalsIgnoreCase(value.trim())) {

                    throw new IllegalArgumentException("Valor inválido en: " + field.getLabel());

                }

            }

            case "FILE" -> {

                if (item.getFileId() != null && !item.getFileId().isBlank()

                        && !formSubmissionFileService.exists(item.getFileId())) {

                    throw new IllegalArgumentException("El archivo adjunto ya no está disponible: " + field.getLabel());

                }

            }

            default -> { /* TEXT, TEXTAREA, DATE, OBSERVATION */ }

        }

    }



    private void assertOptionAllowed(FormFieldDto field, String value) {

        if (value == null || value.isBlank()) {

            return;

        }

        List<String> options = parseOptions(field.getOptions());

        if (options.isEmpty()) {

            return;

        }

        String normalizedValue = value.trim();

        boolean match = options.stream().anyMatch(opt -> opt.equalsIgnoreCase(normalizedValue));

        if (!match) {

            throw new IllegalArgumentException("Opción no válida en " + field.getLabel() + ": " + value);

        }

    }



    private List<String> parseOptions(String options) {

        if (options == null || options.isBlank()) {

            return List.of();

        }

        List<String> parsed = new ArrayList<>();

        for (String part : options.split(",")) {

            if (part != null && !part.trim().isBlank()) {

                parsed.add(part.trim());

            }

        }

        return parsed;

    }



    private Map<String, ResponseItemDto> indexResponses(List<ResponseItemDto> responses) {

        Map<String, ResponseItemDto> map = new HashMap<>();

        if (responses == null) {

            return map;

        }

        for (ResponseItemDto item : responses) {

            if (item.getFieldName() != null && !item.getFieldName().isBlank()) {

                map.put(item.getFieldName().trim().toLowerCase(Locale.ROOT), item);

            }

        }

        return map;

    }



    private String normalizeFieldType(String type) {

        if (type == null || type.isBlank()) {

            return "TEXT";

        }

        return type.trim().toUpperCase(Locale.ROOT);

    }



    private Optional<FormSubmission> resolveExistingSubmission(FormSubmissionRequest request) {

        String workflowActivityId = resolveWorkflowActivityId(request);

        if (workflowActivityId != null && !workflowActivityId.isBlank()) {

            Optional<FormSubmission> byWorkflow = formSubmissionRepository

                    .findByTramiteIdAndWorkflowActivityIdAndTaskOrder(

                            request.getTramiteId(),

                            workflowActivityId,

                            request.getTaskOrder()

                    );

            if (byWorkflow.isPresent()) {

                return byWorkflow;

            }

        }

        if (request.getActivityName() != null && !request.getActivityName().isBlank()) {

            return formSubmissionRepository.findByTramiteIdAndActivityNameAndTaskOrder(

                    request.getTramiteId(),

                    request.getActivityName().trim(),

                    request.getTaskOrder()

            );

        }

        return Optional.empty();

    }



    private String resolveWorkflowActivityId(FormSubmissionRequest request) {

        if (request.getWorkflowActivityId() != null && !request.getWorkflowActivityId().isBlank()) {

            return request.getWorkflowActivityId().trim();

        }

        return null;

    }



    private String resolveActivityName(FormSubmissionRequest request) {

        if (request.getActivityName() != null && !request.getActivityName().isBlank()) {

            return request.getActivityName().trim();

        }

        if (request.getWorkflowActivityId() != null && !request.getWorkflowActivityId().isBlank()) {

            return dynamicFormService.getDetailByWorkflowActivityId(request.getWorkflowActivityId().trim())

                    .getActivityName();

        }

        return OBSERVATION_FALLBACK_FIELD;

    }



    public String buildCompletionComment(String activityName, List<ResponseItemDto> responses) {

        StringBuilder comment = new StringBuilder("Formulario completado para " + activityName);

        if (responses != null && !responses.isEmpty()) {

            comment.append(": ");

            comment.append(responses.stream()

                    .filter(this::hasDisplayValue)

                    .limit(3)

                    .map(item -> item.getFieldLabel() + "=" + displayValue(item))

                    .reduce((a, b) -> a + "; " + b)

                    .orElse("sin datos"));

        }

        return comment.toString();

    }



    private Object coerceValue(ResponseItemDto item) {

        if (item == null) {

            return null;

        }

        String value = item.getValue();

        if (value == null) {

            return item.getFileId();

        }

        String trimmed = value.trim();

        if ("true".equalsIgnoreCase(trimmed)) {

            return Boolean.TRUE;

        }

        if ("false".equalsIgnoreCase(trimmed)) {

            return Boolean.FALSE;

        }

        try {

            if (trimmed.contains(".")) {

                return Double.parseDouble(trimmed.replace(",", "."));

            }

            return Long.parseLong(trimmed);

        } catch (NumberFormatException ex) {

            return trimmed;

        }

    }



    private String normalizeKey(String label) {

        return label.trim()

                .toLowerCase(Locale.ROOT)

                .replaceAll("\\s+", "_")

                .replaceAll("[^a-z0-9_]", "");

    }



    private void validateRequest(FormSubmissionRequest request) {

        if (request.getTramiteId() == null || request.getTramiteId().isBlank()) {

            throw new IllegalArgumentException("El trámite es obligatorio");

        }

        if (request.getPolicyId() == null || request.getPolicyId().isBlank()) {

            throw new IllegalArgumentException("La política es obligatoria");

        }

        boolean hasWorkflowId = request.getWorkflowActivityId() != null && !request.getWorkflowActivityId().isBlank();

        boolean hasActivityName = request.getActivityName() != null && !request.getActivityName().isBlank();

        if (!hasWorkflowId && !hasActivityName) {

            throw new IllegalArgumentException("La actividad de workflow es obligatoria");

        }

        if (request.getTaskOrder() <= 0) {

            throw new IllegalArgumentException("La tarea es obligatoria");

        }

    }



    private String buildTaskKey(String tramiteId, int taskOrder) {

        return tramiteId + ":" + taskOrder;

    }



    private List<ResponseItem> mapToEntities(List<ResponseItemDto> items, FormSubmission existingSubmission) {

        if (items == null) {

            return List.of();

        }



        Map<String, ResponseItem> existingByFieldName = new HashMap<>();

        if (existingSubmission.getResponses() != null) {

            for (ResponseItem existingItem : existingSubmission.getResponses()) {

                if (existingItem.getFieldName() != null && !existingItem.getFieldName().isBlank()) {

                    existingByFieldName.put(existingItem.getFieldName().trim(), existingItem);

                }

            }

        }



        return items.stream().map(dto -> {

            ResponseItem item = new ResponseItem();

            item.setFieldId(dto.getFieldId());

            item.setFieldName(dto.getFieldName());

            item.setFieldLabel(dto.getFieldLabel());

            item.setFieldType(normalizeResponseFieldType(dto.getFieldType()));

            item.setFileId(dto.getFileId());

            item.setFileName(dto.getFileName());

            item.setContentType(dto.getContentType());

            item.setSize(dto.getSize());



            ResponseItem previous = dto.getFieldName() == null

                    ? null

                    : existingByFieldName.get(dto.getFieldName().trim());



            if ("file".equalsIgnoreCase(item.getFieldType())) {

                String fileName = dto.getFileName() != null && !dto.getFileName().isBlank()

                        ? dto.getFileName().trim()

                        : dto.getValue();

                item.setFileName(fileName);

                item.setValue(fileName);



                if ((item.getFileId() == null || item.getFileId().isBlank()) && previous != null) {

                    item.setFileId(previous.getFileId());

                    item.setContentType(previous.getContentType());

                    item.setSize(previous.getSize());

                    if (fileName == null || fileName.isBlank()) {

                        item.setFileName(previous.getFileName());

                        item.setValue(previous.getFileName());

                    }

                }



                if (item.getFileId() != null && !item.getFileId().isBlank()

                        && !formSubmissionFileService.exists(item.getFileId())) {

                    throw new IllegalArgumentException(

                            "El archivo adjunto ya no está disponible. Vuelva a seleccionarlo."

                    );

                }

            } else {

                item.setValue(dto.getValue());

            }

            return item;

        }).toList();

    }



    private String normalizeResponseFieldType(String fieldType) {

        if (fieldType == null || fieldType.isBlank()) {

            return fieldType;

        }

        if ("file".equalsIgnoreCase(fieldType.trim())) {

            return "file";

        }

        return fieldType.trim();

    }



    private FormSubmissionResponse toResponse(FormSubmission submission) {

        FormSubmissionResponse response = new FormSubmissionResponse();

        response.setId(submission.getId());

        response.setTramiteId(submission.getTramiteId());

        response.setPolicyId(submission.getPolicyId());

        response.setWorkflowActivityId(submission.getWorkflowActivityId());

        response.setActivityName(submission.getActivityName());

        response.setTaskOrder(submission.getTaskOrder());

        response.setSubmittedBy(submission.getSubmittedBy());

        response.setSubmittedByName(submission.getSubmittedByName());

        response.setSubmittedAt(submission.getSubmittedAt() != null

                ? submission.getSubmittedAt()

                : submission.getUpdatedAt());

        response.setCreatedAt(submission.getCreatedAt());

        response.setUpdatedAt(submission.getUpdatedAt());

        response.setResponses(submission.getResponses() == null ? List.of() : submission.getResponses().stream()

                .map(item -> {

                    ResponseItemDto dto = new ResponseItemDto();

                    dto.setFieldId(item.getFieldId());

                    dto.setFieldName(item.getFieldName());

                    dto.setFieldLabel(item.getFieldLabel());

                    dto.setFieldType(item.getFieldType());

                    dto.setValue(item.getValue());

                    dto.setFileName(item.getFileName());

                    dto.setFileId(item.getFileId());

                    dto.setContentType(item.getContentType());

                    dto.setSize(item.getSize());

                    return dto;

                })

                .toList());

        return response;

    }



    private String displayName(User user) {

        if (user.getFullName() != null && !user.getFullName().isBlank()) {

            return user.getFullName().trim();

        }

        return user.getUsername();

    }



    private boolean hasDisplayValue(ResponseItemDto item) {

        if ("file".equalsIgnoreCase(item.getFieldType())) {

            return item.getFileName() != null && !item.getFileName().isBlank()

                    || item.getFileId() != null && !item.getFileId().isBlank();

        }

        return item.getValue() != null && !item.getValue().isBlank();

    }



    private String displayValue(ResponseItemDto item) {

        if ("file".equalsIgnoreCase(item.getFieldType())

                && item.getFileName() != null

                && !item.getFileName().isBlank()) {

            return item.getFileName().trim();

        }

        return item.getValue();

    }

}

