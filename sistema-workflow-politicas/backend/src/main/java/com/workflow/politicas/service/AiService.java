package com.workflow.politicas.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class AiService {

    private final RestTemplate restTemplate;
    private final AiRequestRepository aiRequestRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public AiService(RestTemplate aiRestTemplate, AiRequestRepository aiRequestRepository, ObjectMapper objectMapper) {
        this.restTemplate = aiRestTemplate;
        this.aiRequestRepository = aiRequestRepository;
        this.objectMapper = objectMapper;
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
        return response;
    }

    public AiFormAssistResponse assistForm(AiFormAssistRequest request) {
        validatePrompt(request.getPrompt());
        if (request.getFieldName() == null || request.getFieldName().isBlank()) {
            throw new IllegalArgumentException("fieldName is required");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", request.getPrompt());
        body.put("fieldName", request.getFieldName());
        body.put("context", request.getContext() != null ? request.getContext() : Map.of());
        AiFormAssistResponse response = postToAi("/assist-form", body, AiFormAssistResponse.class);
        saveAiRequest(request.getPrompt(), response, "FORM_ASSISTANCE", request.getUserId());
        return response;
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
