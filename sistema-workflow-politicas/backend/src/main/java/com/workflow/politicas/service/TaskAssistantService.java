package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.TaskAssistantResponseDto;
import com.workflow.politicas.model.DocumentRecord;
import com.workflow.politicas.model.FormSubmission;
import com.workflow.politicas.model.ResponseItem;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import com.workflow.politicas.model.User;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.repository.DocumentRecordRepository;
import com.workflow.politicas.repository.DocumentRepositoryStore;
import com.workflow.politicas.repository.FormSubmissionRepository;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class TaskAssistantService {

    private final RestTemplate restTemplate;
    private final TramiteRepository tramiteRepository;
    private final UserRepository userRepository;
    private final WorkflowActivityRepository workflowActivityRepository;
    private final FormSubmissionRepository formSubmissionRepository;
    private final DocumentRepositoryStore documentRepositoryStore;
    private final DocumentRecordRepository documentRecordRepository;
    private final MyActivitiesService myActivitiesService;
    private final BitacoraService bitacoraService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public TaskAssistantService(
            @Qualifier("taskAssistantRestTemplate") RestTemplate taskAssistantRestTemplate,
            TramiteRepository tramiteRepository,
            UserRepository userRepository,
            WorkflowActivityRepository workflowActivityRepository,
            FormSubmissionRepository formSubmissionRepository,
            DocumentRepositoryStore documentRepositoryStore,
            DocumentRecordRepository documentRecordRepository,
            MyActivitiesService myActivitiesService,
            BitacoraService bitacoraService
    ) {
        this.restTemplate = taskAssistantRestTemplate;
        this.tramiteRepository = tramiteRepository;
        this.userRepository = userRepository;
        this.workflowActivityRepository = workflowActivityRepository;
        this.formSubmissionRepository = formSubmissionRepository;
        this.documentRepositoryStore = documentRepositoryStore;
        this.documentRecordRepository = documentRecordRepository;
        this.myActivitiesService = myActivitiesService;
        this.bitacoraService = bitacoraService;
    }

    public TaskAssistantResponseDto assist(String tramiteId, int taskOrder, String username) {
        myActivitiesService.findForUser(tramiteId, taskOrder, username)
                .orElseThrow(() -> new IllegalArgumentException("No tiene acceso a esta tarea"));

        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new IllegalArgumentException("Trámite no encontrado"));

        TramiteTask task = findTaskByOrder(tramite, taskOrder)
                .orElseThrow(() -> new IllegalArgumentException("La tarea indicada no existe"));

        Map<String, Object> context = buildContext(tramite, task);
        TaskAssistantResponseDto response;
        try {
            response = callAiService(context);
        } catch (RuntimeException ex) {
            response = TaskAssistantLocalFallback.build(context);
        }

        auditUsage(username, tramiteId, taskOrder, response.getSource());
        return response;
    }

    private TaskAssistantResponseDto callAiService(Map<String, Object> context) {
        String url = aiServiceUrl + "/ai/task-assistant";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(context, headers);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
            if (response.getBody() == null) {
                throw new IllegalStateException("AI service returned an empty response");
            }
            return mapResponse(response.getBody());
        } catch (ResourceAccessException | HttpStatusCodeException ex) {
            throw new IllegalStateException("AI service unavailable", ex);
        }
    }

    private TaskAssistantResponseDto mapResponse(Map<String, Object> raw) {
        TaskAssistantResponseDto dto = new TaskAssistantResponseDto();
        dto.setSummary(asString(raw.get("summary")));
        dto.setImportantData(asStringList(raw.get("importantData")));
        dto.setMissingData(asStringList(raw.get("missingData")));
        dto.setRecommendedAction(asString(raw.get("recommendedAction")));
        String source = asString(raw.get("source"));
        dto.setSource(source != null && !source.isBlank() ? source : "AI");
        if (dto.getSummary() == null || dto.getSummary().isBlank()) {
            throw new IllegalStateException("Invalid AI response");
        }
        return dto;
    }

    private Map<String, Object> buildContext(Tramite tramite, TramiteTask task) {
        WorkflowActivity activity = resolveWorkflowActivity(task);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("taskId", tramite.getId() + ":" + task.getOrder());
        context.put("tramiteId", tramite.getId());
        context.put("tramiteName", firstNonBlank(tramite.getCode(), tramite.getPolicyName(), "Trámite"));
        context.put("activityName", firstNonBlank(task.getName(), tramite.getCurrentActivity(), "Actividad"));
        context.put(
                "activityDescription",
                activity != null ? firstNonBlank(activity.getDescription(), activity.getName(), "") : ""
        );
        context.put("taskStatus", task.getStatus());
        context.put("assignedTo", resolveAssignedDisplayName(task, tramite));
        context.put("formData", loadFormData(tramite, task));
        context.put("documents", loadDocuments(tramite.getId()));
        context.put("observations", buildObservations(tramite, task));
        context.put("createdAt", formatDateTime(task.getStartedAt() != null ? task.getStartedAt() : tramite.getCreatedAt()));
        return context;
    }

    private Map<String, Object> loadFormData(Tramite tramite, TramiteTask task) {
        Optional<FormSubmission> submission = Optional.empty();
        if (task.getWorkflowActivityId() != null && !task.getWorkflowActivityId().isBlank()) {
            submission = formSubmissionRepository.findByTramiteIdAndWorkflowActivityIdAndTaskOrder(
                    tramite.getId(),
                    task.getWorkflowActivityId(),
                    task.getOrder()
            );
        }
        if (submission.isEmpty() && task.getName() != null) {
            submission = formSubmissionRepository.findByTramiteIdAndActivityNameAndTaskOrder(
                    tramite.getId(),
                    task.getName(),
                    task.getOrder()
            );
        }
        Map<String, Object> formData = new LinkedHashMap<>();
        submission.ifPresent(form -> {
            if (form.getResponses() == null) {
                return;
            }
            for (ResponseItem item : form.getResponses()) {
                String key = firstNonBlank(item.getFieldLabel(), item.getFieldName(), item.getFieldId());
                if (key == null) {
                    continue;
                }
                String value = firstNonBlank(item.getValue(), item.getFileName());
                if (value != null) {
                    formData.put(key, value);
                }
            }
        });
        return formData;
    }

    private List<Map<String, String>> loadDocuments(String tramiteId) {
        List<Map<String, String>> documents = new ArrayList<>();
        documentRepositoryStore.findByTramiteId(tramiteId).ifPresent(repository -> {
            List<DocumentRecord> records = documentRecordRepository
                    .findByRepositoryIdAndEstadoOrderByFechaSubidaDesc(
                            repository.getId(),
                            DocumentRecord.STATUS_ACTIVO
                    );
            for (DocumentRecord record : records) {
                Map<String, String> doc = new LinkedHashMap<>();
                doc.put("id", record.getId());
                doc.put("name", record.getNombreOriginal());
                doc.put("contentType", record.getContentType());
                documents.add(doc);
            }
        });
        return documents;
    }

    private String buildObservations(Tramite tramite, TramiteTask task) {
        StringBuilder sb = new StringBuilder();
        if (tramite.getDescription() != null && !tramite.getDescription().isBlank()) {
            sb.append(tramite.getDescription().trim());
        }
        if (task.getNotes() != null && !task.getNotes().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append(task.getNotes().trim());
        }
        if (tramite.getWorkflowError() != null && !tramite.getWorkflowError().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append("Error workflow: ").append(tramite.getWorkflowError().trim());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String resolveAssignedDisplayName(TramiteTask task, Tramite tramite) {
        if (task.getTakenBy() != null && !task.getTakenBy().isBlank()) {
            return userRepository.findByUsername(task.getTakenBy().trim())
                    .map(EmployeeDisplayNameResolver::fromUser)
                    .orElse(task.getTakenBy().trim());
        }
        String responsible = firstNonBlank(task.getResponsible(), tramite.getResponsible());
        if (responsible == null) {
            return "Sin asignar";
        }
        User byUsername = userRepository.findByUsername(responsible.trim()).orElse(null);
        if (byUsername != null) {
            return EmployeeDisplayNameResolver.fromUser(byUsername);
        }
        return responsible;
    }

    private WorkflowActivity resolveWorkflowActivity(TramiteTask task) {
        if (task.getWorkflowActivityId() != null && !task.getWorkflowActivityId().isBlank()) {
            return workflowActivityRepository.findById(task.getWorkflowActivityId()).orElse(null);
        }
        return null;
    }

    private Optional<TramiteTask> findTaskByOrder(Tramite tramite, int taskOrder) {
        if (tramite.getTasks() == null) {
            return Optional.empty();
        }
        return tramite.getTasks().stream()
                .filter(task -> task.getOrder() == taskOrder)
                .findFirst();
    }

    private void auditUsage(String username, String tramiteId, int taskOrder, String source) {
        bitacoraService.registrar(
                username,
                AuditModules.IA,
                AuditActions.AI_TASK_ASSISTANT_USED,
                "Asistente IA de tarea consultado (taskOrder="
                        + taskOrder
                        + ", source="
                        + (source != null ? source : "—")
                        + ")",
                "Tramite",
                tramiteId
        );
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.toString() : null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
    }
}
