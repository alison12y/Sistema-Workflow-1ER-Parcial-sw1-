package com.workflow.politicas.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.*;
import com.workflow.politicas.model.AiRequest;
import com.workflow.politicas.repository.AiRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;

@Service
public class AiService {

    private final RestTemplate restTemplate;
    private final AiRequestRepository aiRequestRepository;
    private final ObjectMapper objectMapper;
    private final BitacoraService bitacoraService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public AiService(
            RestTemplate aiRestTemplate,
            AiRequestRepository aiRequestRepository,
            ObjectMapper objectMapper,
            BitacoraService bitacoraService
    ) {
        this.restTemplate = aiRestTemplate;
        this.aiRequestRepository = aiRequestRepository;
        this.objectMapper = objectMapper;
        this.bitacoraService = bitacoraService;
    }

    public AiWorkflowSuggestResponse suggestWorkflow(AiWorkflowSuggestRequest request) {
        validatePrompt(request.getPrompt());
        if (request.getPolicyId() == null || request.getPolicyId().isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (WorkflowFullPromptParser.canParse(request.getPrompt())) {
            AiWorkflowSuggestResponse local = WorkflowFullPromptParser.parse(request);
            saveAiRequest(request.getPrompt(), local, "WORKFLOW_SUGGEST_LOCAL_FULL", request.getUserId());
            auditWorkflowGeneration(request.getPolicyId(), "sugerencia local de workflow");
            return local;
        }

        Map<String, Object> body = AiWorkflowSuggestMapper.toAiServiceBody(request);
        try {
            Map<String, Object> raw = postToAiMap("/workflow/suggest", body);
            AiWorkflowSuggestResponse response = AiWorkflowSuggestMapper.fromMap(raw, objectMapper);
            if (response.getRequiresConfirmation() == null) {
                response.setRequiresConfirmation(true);
            }
            saveAiRequest(request.getPrompt(), response, "WORKFLOW_SUGGEST", request.getUserId());
            auditWorkflowGeneration(request.getPolicyId(), "sugerencia de workflow con IA");
            return response;
        } catch (RuntimeException ex) {
            AiWorkflowSuggestResponse fallback = WorkflowSuggestLocalParser.parse(request);
            fallback.setError(
                    (fallback.getError() != null ? fallback.getError() + " " : "")
                            + "Detalle: servicio IA no disponible.");
            saveAiRequest(request.getPrompt(), fallback, "WORKFLOW_SUGGEST_FALLBACK", request.getUserId());
            auditWorkflowGeneration(request.getPolicyId(), "sugerencia de workflow (fallback local)");
            return fallback;
        }
    }

    public AiWorkflowGenerateResponse generateWorkflow(AiWorkflowGenerateRequest request) {
        validatePrompt(request.getPrompt());
        Map<String, Object> body = Map.of("prompt", request.getPrompt());
        AiWorkflowGenerateResponse response = postToAi(
                "/generate-workflow",
                body,
                AiWorkflowGenerateResponse.class
        );
        saveAiRequest(request.getPrompt(), response, "WORKFLOW_GENERATION", request.getUserId());
        auditWorkflowGeneration(null, "generación de workflow con IA");
        return response;
    }

    public AiFormAssistResponse assistForm(AiFormAssistRequest request) {
        String report = request.getReport();
        if (report == null || report.isBlank()) {
            throw new IllegalArgumentException("report is required");
        }
        if (request.getFields() == null || request.getFields().isEmpty()) {
            throw new IllegalArgumentException("fields are required");
        }
        Map<String, Object> body = FormAssistResponseMapper.toAiServiceBody(request);
        try {
            Map<String, Object> raw = postToAiMap("/assist-form", body);
            AiFormAssistResponse response = FormAssistResponseMapper.fromMap(raw, objectMapper);
            String logSummary = "assist-form policy=" + request.getPolicyId()
                    + " activity=" + request.getWorkflowActivityId()
                    + " fields=" + request.getFields().size();
            saveAiRequest(logSummary, response, "FORM_ASSISTANCE", request.getUserId());
            auditFormAssistance(request.getPolicyId(), request.getWorkflowActivityId());
            return response;
        } catch (RuntimeException ex) {
            AiFormAssistResponse fallback = FormAssistLocalParser.parse(request);
            saveAiRequest(
                    "assist-form-fallback activity=" + request.getWorkflowActivityId(),
                    fallback,
                    "FORM_ASSISTANCE_FALLBACK",
                    request.getUserId()
            );
            auditFormAssistance(request.getPolicyId(), request.getWorkflowActivityId());
            return fallback;
        }
    }

    private void auditWorkflowGeneration(String policyId, String detail) {
        String actor = bitacoraService.resolveActorDisplay();
        bitacoraService.registrar(
                AuditModules.IA,
                AuditActions.GENERAR_WORKFLOW_IA,
                actor + " solicitó " + detail + (policyId != null ? " (política " + policyId + ")" : ""),
                "BusinessPolicy",
                policyId
        );
    }

    private void auditFormAssistance(String policyId, String activityId) {
        String actor = bitacoraService.resolveActorDisplay();
        bitacoraService.registrar(
                AuditModules.IA,
                AuditActions.ASISTENCIA_FORMULARIO_IA,
                actor + " usó asistencia IA en formulario"
                        + (activityId != null ? " (actividad " + activityId + ")" : ""),
                "DynamicForm",
                policyId
        );
    }

    public AiAssistantResponse assistant(AiAssistantRequest request) {
        validatePrompt(request.getPrompt());
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", request.getPrompt());
        body.put("module", request.getModule() != null ? request.getModule() : "policies");
        body.put("context", request.getContext() != null ? request.getContext() : Map.of());
        AiAssistantResponse response = postToAi("/assistant", body, AiAssistantResponse.class);
        saveAiRequest(request.getPrompt(), response, "GENERAL_ASSISTANT", request.getUserId());
        return response;
    }

    public AiValidateDiagramResponse validateDiagram(AiValidateDiagramRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("activities", request.getActivities());
        body.put("transitions", request.getTransitions());
        AiValidateDiagramResponse response = postToAi("/validate-diagram", body, AiValidateDiagramResponse.class);
        String promptSummary = "validate-diagram: " + request.getActivities().size() + " activities, "
                + request.getTransitions().size() + " transitions";
        saveAiRequest(promptSummary, response, "DIAGRAM_VALIDATION", request.getUserId());
        return response;
    }

    private Map<String, Object> postToAiMap(String path, Map<String, Object> body) {
        String url = aiServiceUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
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
            return response.getBody();
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("AI service is not available at " + aiServiceUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException("AI service error: " + e.getResponseBodyAsString(), e);
        }
    }

    private <T> T postToAi(String path, Map<String, Object> body, Class<T> responseType) {
        String url = aiServiceUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    responseType
            );
            if (response.getBody() == null) {
                throw new IllegalStateException("AI service returned an empty response");
            }
            return response.getBody();
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("AI service is not available at " + aiServiceUrl, e);
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException("AI service error: " + e.getResponseBodyAsString(), e);
        }
    }

    private void saveAiRequest(String prompt, Object response, String contextType, String userId) {
        AiRequest aiRequest = new AiRequest();
        aiRequest.setPrompt(prompt);
        aiRequest.setResponse(toJson(response));
        aiRequest.setContextType(contextType);
        aiRequest.setUserId(userId);
        aiRequest.setTimestamp(LocalDateTime.now());
        aiRequestRepository.save(aiRequest);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private void validatePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt is required");
        }
    }
}
