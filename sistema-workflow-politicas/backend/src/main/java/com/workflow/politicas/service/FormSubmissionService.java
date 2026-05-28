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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FormSubmissionService {

    private final FormSubmissionRepository formSubmissionRepository;
    private final DynamicFormService dynamicFormService;
    private final UserRepository userRepository;
    private final FormSubmissionFileService formSubmissionFileService;

    public FormSubmissionService(
            FormSubmissionRepository formSubmissionRepository,
            DynamicFormService dynamicFormService,
            UserRepository userRepository,
            FormSubmissionFileService formSubmissionFileService
    ) {
        this.formSubmissionRepository = formSubmissionRepository;
        this.dynamicFormService = dynamicFormService;
        this.userRepository = userRepository;
        this.formSubmissionFileService = formSubmissionFileService;
    }

    public FormSubmissionResponse save(FormSubmissionRequest request, String username) {
        validateRequest(request);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        FormSubmission submission = formSubmissionRepository
                .findByTramiteIdAndActivityNameAndTaskOrder(
                        request.getTramiteId(),
                        request.getActivityName().trim(),
                        request.getTaskOrder()
                )
                .orElseGet(FormSubmission::new);

        if (submission.getId() == null) {
            submission.setCreatedAt(LocalDateTime.now());
        }

        submission.setTramiteId(request.getTramiteId());
        submission.setPolicyId(request.getPolicyId());
        submission.setActivityName(request.getActivityName().trim());
        submission.setTaskOrder(request.getTaskOrder());
        submission.setTaskKey(buildTaskKey(request.getTramiteId(), request.getTaskOrder()));
        submission.setSubmittedBy(user.getUsername());
        submission.setSubmittedByName(displayName(user));
        submission.setResponses(mapToEntities(request.getResponses(), submission));
        submission.setUpdatedAt(LocalDateTime.now());

        return toResponse(formSubmissionRepository.save(submission));
    }

    public List<FormSubmissionResponse> findByTramite(String tramiteId) {
        return formSubmissionRepository.findByTramiteId(tramiteId).stream()
                .sorted(Comparator.comparingInt(FormSubmission::getTaskOrder))
                .map(this::toResponse)
                .toList();
    }

    public Optional<FormSubmissionResponse> findByTramiteAndActivity(
            String tramiteId,
            String activityName,
            int taskOrder
    ) {
        return formSubmissionRepository
                .findByTramiteIdAndActivityNameAndTaskOrder(tramiteId, activityName.trim(), taskOrder)
                .map(this::toResponse);
    }

    public void validateRequiredResponses(
            String policyId,
            String activityName,
            List<ResponseItemDto> responses
    ) {
        DynamicFormDetailResponse form = dynamicFormService.getByPolicyAndActivity(policyId, activityName);
        if (form.getFields() == null || form.getFields().isEmpty()) {
            throw new IllegalArgumentException("No existe formulario configurado para esta actividad");
        }

        Map<String, ResponseItemDto> responsesByName = new HashMap<>();
        if (responses != null) {
            for (ResponseItemDto item : responses) {
                if (item.getFieldName() != null && !item.getFieldName().isBlank()) {
                    responsesByName.put(item.getFieldName().trim(), item);
                }
            }
        }

        for (FormFieldDto field : form.getFields()) {
            if (!field.isRequired()) {
                continue;
            }

            ResponseItemDto item = responsesByName.get(field.getName());
            if ("file".equalsIgnoreCase(field.getType())) {
                if (item == null || item.getFileId() == null || item.getFileId().isBlank()
                        || !formSubmissionFileService.exists(item.getFileId())) {
                    throw new IllegalArgumentException("Complete los campos obligatorios");
                }
                continue;
            }

            String value = item != null ? item.getValue() : null;
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Complete los campos obligatorios");
            }
        }
    }

    public String buildCompletionComment(String activityName, List<ResponseItemDto> responses) {
        StringBuilder comment = new StringBuilder("Formulario completado para " + activityName);
        if (responses != null && !responses.isEmpty()) {
            comment.append(": ");
            comment.append(responses.stream()
                    .filter(item -> hasDisplayValue(item))
                    .limit(3)
                    .map(item -> item.getFieldLabel() + "=" + displayValue(item))
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("sin datos"));
        }
        return comment.toString();
    }

    private void validateRequest(FormSubmissionRequest request) {
        if (request.getTramiteId() == null || request.getTramiteId().isBlank()) {
            throw new IllegalArgumentException("El trámite es obligatorio");
        }
        if (request.getPolicyId() == null || request.getPolicyId().isBlank()) {
            throw new IllegalArgumentException("La política es obligatoria");
        }
        if (request.getActivityName() == null || request.getActivityName().isBlank()) {
            throw new IllegalArgumentException("La actividad es obligatoria");
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
        response.setActivityName(submission.getActivityName());
        response.setTaskOrder(submission.getTaskOrder());
        response.setSubmittedByName(submission.getSubmittedByName());
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
            return item.getFileName() != null && !item.getFileName().isBlank();
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
