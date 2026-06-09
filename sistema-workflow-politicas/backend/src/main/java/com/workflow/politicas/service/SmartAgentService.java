package com.workflow.politicas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.DocumentRecordResponse;
import com.workflow.politicas.dto.SmartAgentAnalyzeRequest;
import com.workflow.politicas.dto.SmartAgentAnalyzeResponse;
import com.workflow.politicas.dto.SmartAgentStartTramiteRequest;
import com.workflow.politicas.dto.SmartAgentStartTramiteResponse;
import com.workflow.politicas.dto.SmartAgentSuggestedField;
import com.workflow.politicas.dto.TramiteCreateRequest;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.DocumentRecord;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.DocumentRecordRepository;
import com.workflow.politicas.repository.DocumentRepositoryStore;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SmartAgentService {

    private static final String IA_UNAVAILABLE_WARNING =
            "Servicio IA no disponible; se usó recomendación local.";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BusinessPolicyRepository businessPolicyRepository;
    private final DocumentRecordRepository documentRecordRepository;
    private final DocumentRepositoryStore documentRepositoryStore;
    private final DocumentRepositoryService documentRepositoryService;
    private final TramiteService tramiteService;
    private final BitacoraService bitacoraService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public SmartAgentService(
            @Qualifier("smartAgentRestTemplate") RestTemplate smartAgentRestTemplate,
            ObjectMapper objectMapper,
            BusinessPolicyRepository businessPolicyRepository,
            DocumentRecordRepository documentRecordRepository,
            DocumentRepositoryStore documentRepositoryStore,
            DocumentRepositoryService documentRepositoryService,
            TramiteService tramiteService,
            BitacoraService bitacoraService
    ) {
        this.restTemplate = smartAgentRestTemplate;
        this.objectMapper = objectMapper;
        this.businessPolicyRepository = businessPolicyRepository;
        this.documentRecordRepository = documentRecordRepository;
        this.documentRepositoryStore = documentRepositoryStore;
        this.documentRepositoryService = documentRepositoryService;
        this.tramiteService = tramiteService;
        this.bitacoraService = bitacoraService;
    }

    public SmartAgentAnalyzeResponse analyze(
            SmartAgentAnalyzeRequest request,
            MultipartFile attachment,
            String authenticatedUsername
    ) {
        validateAnalyzeRequest(request);

        String attachmentFileName = attachment != null && !attachment.isEmpty()
                ? attachment.getOriginalFilename()
                : null;
        String combinedText = buildCombinedText(request, attachmentFileName);
        List<BusinessPolicy> activePolicies = loadActivePolicies();

        auditAgentRequested(authenticatedUsername, combinedText, attachmentFileName);

        SmartAgentAnalyzeResponse response;
        try {
            response = callAiService(combinedText, request, activePolicies, attachmentFileName);
            response.setSource("AI_SERVICE");
        } catch (Exception ex) {
            response = SmartAgentFallbackMatcher.match(
                    combinedText,
                    activePolicies,
                    request.getRequesterName(),
                    attachmentFileName
            );
            addFallbackWarning(response);
        }

        enrichResponseWithPolicy(response, activePolicies);
        try {
            auditPolicyRecommended(authenticatedUsername, response);
        } catch (Exception ignored) {
            // La auditoría no debe impedir devolver la recomendación al cliente.
        }
        return response;
    }

    public SmartAgentStartTramiteResponse startTramite(
            SmartAgentStartTramiteRequest request,
            MultipartFile attachment,
            String authenticatedUsername
    ) {
        if (request.getPolicyId() == null || request.getPolicyId().isBlank()) {
            throw new IllegalArgumentException("policyId es obligatorio");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("description es obligatoria");
        }
        if (request.getRequestedBy() == null || request.getRequestedBy().isBlank()) {
            throw new IllegalArgumentException("requestedBy es obligatorio");
        }

        TramiteCreateRequest createRequest = new TramiteCreateRequest();
        createRequest.setPolicyId(request.getPolicyId().trim());
        createRequest.setDescription(request.getDescription().trim());
        createRequest.setRequestedBy(request.getRequestedBy().trim());
        createRequest.setPriority(
                request.getPriority() != null && !request.getPriority().isBlank()
                        ? request.getPriority().trim()
                        : "NORMAL"
        );

        Tramite tramite = tramiteService.create(createRequest, authenticatedUsername);

        DocumentRecordResponse attachedDocument = null;
        if (attachment != null && !attachment.isEmpty()) {
            String repositoryId = documentRepositoryStore.findByTramiteId(tramite.getId())
                    .orElseThrow(() -> new IllegalStateException("Repositorio documental no creado para el trámite"))
                    .getId();
            attachedDocument = documentRepositoryService.uploadDocument(
                    repositoryId,
                    attachment,
                    authenticatedUsername
            );
        }

        SmartAgentStartTramiteResponse response = new SmartAgentStartTramiteResponse();
        response.setTramite(tramite);
        response.setAttachedDocument(attachedDocument);
        response.setMessage("Trámite " + tramite.getCode() + " iniciado con la política recomendada.");

        auditTramiteStarted(authenticatedUsername, tramite, request);
        return response;
    }

    private SmartAgentAnalyzeResponse callAiService(
            String combinedText,
            SmartAgentAnalyzeRequest request,
            List<BusinessPolicy> activePolicies,
            String attachmentFileName
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", combinedText);
        body.put("audioText", request.getAudioText());
        body.put("requesterName", request.getRequesterName());
        body.put("attachmentFileName", attachmentFileName);
        body.put("documentContext", resolveDocumentContext(request.getDocumentId()));
        body.put("policies", activePolicies.stream().map(this::toPolicyPayload).collect(Collectors.toList()));

        Map<String, Object> raw = postToAiMap("/agent/analyze", body);
        return mapAiResponse(raw, attachmentFileName);
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

    @SuppressWarnings("unchecked")
    private SmartAgentAnalyzeResponse mapAiResponse(Map<String, Object> raw, String attachmentFileName) {
        SmartAgentAnalyzeResponse response = new SmartAgentAnalyzeResponse();
        response.setDetectedIntent(asString(raw.get("detectedIntent")));
        response.setRecommendedPolicyId(asString(raw.get("recommendedPolicyId")));
        response.setRecommendedPolicyName(asString(raw.get("recommendedPolicyName")));
        response.setConfidenceScore(asDouble(raw.get("confidenceScore")));
        response.setExplanation(asString(raw.get("explanation")));
        response.setAttachmentFileName(attachmentFileName);

        Object docs = raw.get("requiredDocuments");
        if (docs instanceof List<?> list) {
            response.setRequiredDocuments(list.stream().map(String::valueOf).collect(Collectors.toList()));
        }

        Object fields = raw.get("suggestedFields");
        if (fields instanceof List<?> list) {
            List<SmartAgentSuggestedField> mapped = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    SmartAgentSuggestedField field = new SmartAgentSuggestedField();
                    field.setName(asString(map.get("name")));
                    field.setLabel(asString(map.get("label")));
                    field.setType(asString(map.get("type")));
                    field.setRequired(Boolean.TRUE.equals(map.get("required")));
                    field.setSuggestedValue(asString(map.get("suggestedValue")));
                    mapped.add(field);
                }
            }
            response.setSuggestedFields(mapped);
        }

        Object warnings = raw.get("warnings");
        if (warnings instanceof List<?> list) {
            response.setWarnings(list.stream().map(String::valueOf).collect(Collectors.toList()));
        }

        return response;
    }

    private void enrichResponseWithPolicy(SmartAgentAnalyzeResponse response, List<BusinessPolicy> activePolicies) {
        if (response.getRecommendedPolicyId() != null) {
            activePolicies.stream()
                    .filter(p -> Objects.equals(p.getId(), response.getRecommendedPolicyId()))
                    .findFirst()
                    .ifPresent(p -> {
                        if (response.getRecommendedPolicyName() == null) {
                            response.setRecommendedPolicyName(p.getName());
                        }
                    });
        }

        if (response.getRecommendedPolicyId() == null || response.getRecommendedPolicyName() == null) {
            SmartAgentAnalyzeResponse fallback = SmartAgentFallbackMatcher.match(
                    response.getExplanation() != null ? response.getExplanation() : "",
                    activePolicies,
                    null,
                    response.getAttachmentFileName()
            );
            if (response.getRecommendedPolicyId() == null) {
                response.setRecommendedPolicyId(fallback.getRecommendedPolicyId());
            }
            if (response.getRecommendedPolicyName() == null) {
                response.setRecommendedPolicyName(fallback.getRecommendedPolicyName());
            }
            if (response.getConfidenceScore() == null) {
                response.setConfidenceScore(fallback.getConfidenceScore());
            }
            if (response.getRequiredDocuments() == null || response.getRequiredDocuments().isEmpty()) {
                response.setRequiredDocuments(fallback.getRequiredDocuments());
            }
            if (response.getSuggestedFields() == null || response.getSuggestedFields().isEmpty()) {
                response.setSuggestedFields(fallback.getSuggestedFields());
            }
        }
    }

    private Map<String, Object> toPolicyPayload(BusinessPolicy policy) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", policy.getId());
        payload.put("name", policy.getName());
        payload.put("description", policy.getDescription());
        payload.put("type", policy.getType());
        payload.put("status", policy.getStatus());
        return payload;
    }

    private Map<String, Object> resolveDocumentContext(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return Map.of();
        }
        return documentRecordRepository.findById(documentId.trim())
                .map(this::toDocumentContext)
                .orElse(Map.of("warning", "Documento no encontrado: " + documentId));
    }

    private Map<String, Object> toDocumentContext(DocumentRecord record) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("documentId", record.getId());
        ctx.put("fileName", record.getNombreOriginal());
        ctx.put("extension", record.getExtension());
        ctx.put("contentType", record.getContentType());
        ctx.put("s3Key", record.getS3Key());
        return ctx;
    }

    private List<BusinessPolicy> loadActivePolicies() {
        return businessPolicyRepository.findAll().stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
    }

    private String buildCombinedText(SmartAgentAnalyzeRequest request, String attachmentFileName) {
        StringBuilder sb = new StringBuilder();
        if (request.getMessage() != null) {
            sb.append(request.getMessage().trim());
        }
        if (request.getAudioText() != null && !request.getAudioText().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(request.getAudioText().trim());
        }
        if (attachmentFileName != null && !attachmentFileName.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append("Documento adjunto: ").append(attachmentFileName);
        }
        if (request.getDocumentId() != null && !request.getDocumentId().isBlank()) {
            documentRecordRepository.findById(request.getDocumentId().trim()).ifPresent(record -> {
                sb.append(" Referencia documental: ").append(record.getNombreOriginal());
            });
        }
        return sb.toString().trim();
    }

    private void validateAnalyzeRequest(SmartAgentAnalyzeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }
        boolean hasMessage = request.getMessage() != null && !request.getMessage().isBlank();
        boolean hasAudio = request.getAudioText() != null && !request.getAudioText().isBlank();
        if (!hasMessage && !hasAudio) {
            throw new IllegalArgumentException("Debe ingresar un mensaje de texto o dictado por voz");
        }
    }

    private void auditAgentRequested(String actor, String combinedText, String attachmentFileName) {
        String summary = combinedText.length() > 180 ? combinedText.substring(0, 180) + "..." : combinedText;
        bitacoraService.registrar(
                actor,
                AuditModules.IA,
                AuditActions.AGENT_REQUESTED,
                "Consulta al agente inteligente: " + summary
                        + (attachmentFileName != null ? " [adjunto: " + attachmentFileName + "]" : ""),
                "SmartAgent",
                null
        );
    }

    private void auditPolicyRecommended(String actor, SmartAgentAnalyzeResponse response) {
        bitacoraService.registrar(
                actor,
                AuditModules.IA,
                AuditActions.AGENT_POLICY_RECOMMENDED,
                "Política recomendada: "
                        + response.getRecommendedPolicyName()
                        + " (confianza "
                        + String.format(Locale.US, "%.0f%%", (response.getConfidenceScore() != null ? response.getConfidenceScore() : 0) * 100)
                        + ", fuente "
                        + response.getSource()
                        + ")",
                "BusinessPolicy",
                response.getRecommendedPolicyId()
        );
    }

    private void auditTramiteStarted(String actor, Tramite tramite, SmartAgentStartTramiteRequest request) {
        bitacoraService.registrar(
                actor,
                AuditModules.IA,
                AuditActions.AGENT_TRAMITE_STARTED,
                "Trámite iniciado desde agente inteligente: "
                        + tramite.getCode()
                        + " — "
                        + tramite.getPolicyName()
                        + (request.getDetectedIntent() != null ? " (intención: " + request.getDetectedIntent() + ")" : ""),
                "Tramite",
                tramite.getId()
        );
    }

    private void addFallbackWarning(SmartAgentAnalyzeResponse response) {
        if (response.getSource() == null || response.getSource().isBlank()) {
            response.setSource("LOCAL_FALLBACK");
        }
        if (response.getWarnings().stream().noneMatch(IA_UNAVAILABLE_WARNING::equals)) {
            response.getWarnings().add(IA_UNAVAILABLE_WARNING);
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
