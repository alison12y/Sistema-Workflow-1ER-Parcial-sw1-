package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.*;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.repository.TramiteRepository;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class IntelligentAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(IntelligentAnalyticsService.class);

    private final RestTemplate restTemplate;
    private final TramiteRepository tramiteRepository;
    private final KpiService kpiService;
    private final BitacoraService bitacoraService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public IntelligentAnalyticsService(
            RestTemplate aiRestTemplate,
            TramiteRepository tramiteRepository,
            KpiService kpiService,
            BitacoraService bitacoraService
    ) {
        this.restTemplate = aiRestTemplate;
        this.tramiteRepository = tramiteRepository;
        this.kpiService = kpiService;
        this.bitacoraService = bitacoraService;
    }

    public AnalyticsReportResponse generateReport(AnalyticsRequest request, String authenticatedUsername) {
        validateMessage(request);
        String combinedText = buildCombinedText(request);
        List<Tramite> tramites = loadFilteredTramites(request);
        KpiDashboardFullResponse kpiDashboard = kpiService.getDashboard(toKpiFilter(request));

        audit(authenticatedUsername, AuditActions.ANALYTICS_REPORT_REQUESTED,
                "Reporte solicitado: " + truncate(combinedText));

        AnalyticsReportResponse response;
        try {
            response = callAiReport(combinedText, request, tramites, kpiDashboard);
            if (response.getSource() == null || response.getSource().isBlank()) {
                response.setSource("AI_SERVICE");
            }
        } catch (RuntimeException ex) {
            log.error("Error llamando al servicio de analítica (reporte): {}", ex.getMessage());
            response = IntelligentAnalyticsFallbackMatcher.buildReport(
                    combinedText, tramites, request, kpiDashboard
            );
        }
        enrichConclusion(response, combinedText, tramites, kpiDashboard);
        return response;
    }

    public AnalyticsRiskResponse analyzeRisks(AnalyticsRequest request, String authenticatedUsername) {
        List<Tramite> tramites = loadFilteredTramites(request);
        KpiDashboardFullResponse kpiDashboard = kpiService.getDashboard(toKpiFilter(request));

        audit(authenticatedUsername, AuditActions.ANALYTICS_RISK_ANALYZED,
                "Análisis de riesgos sobre " + tramites.size() + " trámite(s)");

        AnalyticsRiskResponse response;
        try {
            response = callAiRisks(request, tramites, kpiDashboard);
            if (response.getSource() == null || response.getSource().isBlank()) {
                response.setSource("AI_SERVICE");
            }
        } catch (RuntimeException ex) {
            log.error("Error llamando al servicio de analítica (riesgos): {}", ex.getMessage());
            response = IntelligentAnalyticsFallbackMatcher.buildRisks(tramites, kpiDashboard);
        }
        return response;
    }

    public AnalyticsRecommendationResponse generateRecommendations(
            AnalyticsRequest request,
            String authenticatedUsername
    ) {
        List<Tramite> tramites = loadFilteredTramites(request);
        KpiDashboardFullResponse kpiDashboard = kpiService.getDashboard(toKpiFilter(request));

        audit(authenticatedUsername, AuditActions.ANALYTICS_RECOMMENDATION_GENERATED,
                "Recomendaciones generadas sobre " + tramites.size() + " trámite(s)");

        AnalyticsRecommendationResponse response;
        try {
            response = callAiRecommendations(request, tramites, kpiDashboard);
            if (response.getSource() == null || response.getSource().isBlank()) {
                response.setSource("AI_SERVICE");
            }
        } catch (RuntimeException ex) {
            log.error("Error llamando al servicio de analítica (recomendaciones): {}", ex.getMessage());
            response = IntelligentAnalyticsFallbackMatcher.buildRecommendations(tramites, kpiDashboard);
        }
        return response;
    }

    private AnalyticsReportResponse callAiReport(
            String combinedText,
            AnalyticsRequest request,
            List<Tramite> tramites,
            KpiDashboardFullResponse kpiDashboard
    ) {
        Map<String, Object> body = buildAiPayload(combinedText, request, tramites, kpiDashboard);
        Map<String, Object> raw = postToAiMap("/analytics/report", body);
        return mapReportResponse(raw);
    }

    private AnalyticsRiskResponse callAiRisks(
            AnalyticsRequest request,
            List<Tramite> tramites,
            KpiDashboardFullResponse kpiDashboard
    ) {
        Map<String, Object> body = buildAiPayload(buildCombinedText(request), request, tramites, kpiDashboard);
        Map<String, Object> raw = postToAiMap("/analytics/risks", body);
        return mapRiskResponse(raw);
    }

    private AnalyticsRecommendationResponse callAiRecommendations(
            AnalyticsRequest request,
            List<Tramite> tramites,
            KpiDashboardFullResponse kpiDashboard
    ) {
        Map<String, Object> body = buildAiPayload(buildCombinedText(request), request, tramites, kpiDashboard);
        Map<String, Object> raw = postToAiMap("/analytics/recommendations", body);
        return mapRecommendationResponse(raw);
    }

    private Map<String, Object> buildAiPayload(
            String combinedText,
            AnalyticsRequest request,
            List<Tramite> tramites,
            KpiDashboardFullResponse kpiDashboard
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", combinedText);
        body.put("audioText", request.getAudioText());
        body.put("policyId", request.getPolicyId());
        body.put("status", request.getStatus());
        body.put("fromDate", request.getFromDate() != null ? request.getFromDate().toString() : null);
        body.put("toDate", request.getToDate() != null ? request.getToDate().toString() : null);
        body.put("tramiteCount", tramites.size());
        body.put("kpiSummary", kpiDashboard.getSummary());
        body.put("bottlenecks", kpiDashboard.getBottlenecks());
        body.put("employeeLoad", kpiDashboard.getEmployeeLoad());
        body.put("tramiteSample", tramites.stream().limit(20).map(this::toTramitePayload).toList());
        return body;
    }

    private Map<String, Object> toTramitePayload(Tramite tramite) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", tramite.getId());
        payload.put("code", tramite.getCode());
        payload.put("policyName", tramite.getPolicyName());
        payload.put("status", tramite.getStatus());
        payload.put("priority", tramite.getPriority());
        payload.put("currentActivity", tramite.getCurrentActivity());
        payload.put("createdAt", tramite.getCreatedAt() != null ? tramite.getCreatedAt().toString() : null);
        payload.put("updatedAt", tramite.getUpdatedAt() != null ? tramite.getUpdatedAt().toString() : null);
        payload.put("taskCount", tramite.getTasks() != null ? tramite.getTasks().size() : 0);
        return payload;
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
    private AnalyticsReportResponse mapReportResponse(Map<String, Object> raw) {
        AnalyticsReportResponse response = new AnalyticsReportResponse();
        response.setTitle(asString(raw.get("title")));
        response.setExplanation(asString(raw.get("explanation")));
        response.setConclusion(asString(raw.get("conclusion")));
        response.setReportType(asString(raw.get("reportType")));
        response.setSuggestedFormat(asString(raw.get("suggestedFormat")));
        response.setSource(asString(raw.get("source")));

        Object columnsObj = raw.get("columns");
        List<String> columns = new ArrayList<>();
        if (columnsObj instanceof List<?> list && !list.isEmpty()) {
            columns = list.stream().map(String::valueOf).toList();
        }

        Object rowsObj = raw.get("rows");
        if (rowsObj instanceof List<?> list && !list.isEmpty()) {
            // Si columns está vacío, intentar inferir de la primera fila
            if (columns.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?> firstRow) {
                    columns = firstRow.keySet().stream().map(String::valueOf).toList();
                }
            }
            response.setColumns(columns);

            List<Map<String, Object>> normalizedRows = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> rawRow) {
                    Map<String, Object> normalizedRow = new LinkedHashMap<>();
                    // Normalizar cada fila para que sus llaves coincidan exactamente con las columnas
                    for (String column : columns) {
                        Object value = findValueCaseInsensitive(rawRow, column);
                        normalizedRow.put(column, value);
                    }
                    normalizedRows.add(normalizedRow);
                }
            }
            response.setRows(normalizedRows);
        } else {
            response.setColumns(columns);
        }

        Object filters = raw.get("appliedFilters");
        if (filters instanceof Map<?, ?> map) {
            Map<String, Object> applied = new LinkedHashMap<>();
            map.forEach((k, v) -> applied.put(String.valueOf(k), v));
            response.setAppliedFilters(applied);
        }
        Object warnings = raw.get("warnings");
        if (warnings instanceof List<?> list) {
            response.setWarnings(list.stream().map(String::valueOf).toList());
        }
        Object cards = raw.get("cards");
        if (cards instanceof List<?> list) {
            response.setCards(mapCards(list));
        }
        Object chart = raw.get("chart");
        if (chart instanceof Map<?, ?> map) {
            response.setChart(mapChart(map));
        }
        return response;
    }

    private Object findValueCaseInsensitive(Map<?, ?> map, String targetKey) {
        if (map.containsKey(targetKey)) {
            return map.get(targetKey);
        }
        String normalizedTarget = targetKey.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey()).trim().toLowerCase(Locale.ROOT);
            if (key.equals(normalizedTarget)) {
                return entry.getValue();
            }
        }
        return "—"; // Valor por defecto si no se encuentra
    }

    @SuppressWarnings("unchecked")
    private AnalyticsRiskResponse mapRiskResponse(Map<String, Object> raw) {
        AnalyticsRiskResponse response = new AnalyticsRiskResponse();
        response.setSummary(asString(raw.get("summary")));
        response.setSource(asString(raw.get("source")));

        Object risks = raw.get("risks");
        if (risks instanceof List<?> list) {
            List<AnalyticsRiskItemDto> mapped = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    AnalyticsRiskItemDto dto = new AnalyticsRiskItemDto();
                    dto.setType(asString(map.get("type")));
                    dto.setSeverity(asString(map.get("severity")));
                    dto.setTitle(asString(map.get("title")));
                    dto.setDescription(asString(map.get("description")));
                    dto.setEntityType(asString(map.get("entityType")));
                    dto.setEntityId(asString(map.get("entityId")));
                    dto.setEntityLabel(asString(map.get("entityLabel")));
                    mapped.add(dto);
                }
            }
            response.setRisks(mapped);
        }
        Object cards = raw.get("cards");
        if (cards instanceof List<?> list) {
            response.setCards(mapCards(list));
        }
        Object warnings = raw.get("warnings");
        if (warnings instanceof List<?> list) {
            response.setWarnings(list.stream().map(String::valueOf).toList());
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private AnalyticsRecommendationResponse mapRecommendationResponse(Map<String, Object> raw) {
        AnalyticsRecommendationResponse response = new AnalyticsRecommendationResponse();
        response.setSummary(asString(raw.get("summary")));
        response.setSource(asString(raw.get("source")));

        Object recommendations = raw.get("recommendations");
        if (recommendations instanceof List<?> list) {
            List<AnalyticsRecommendationItemDto> mapped = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    AnalyticsRecommendationItemDto dto = new AnalyticsRecommendationItemDto();
                    dto.setPriority(asString(map.get("priority")));
                    dto.setType(asString(map.get("type")));
                    dto.setTitle(asString(map.get("title")));
                    dto.setAction(asString(map.get("action")));
                    dto.setRationale(asString(map.get("rationale")));
                    dto.setTramiteCode(asString(map.get("tramiteCode")));
                    dto.setActivityName(asString(map.get("activityName")));
                    mapped.add(dto);
                }
            }
            response.setRecommendations(mapped);
        }
        Object cards = raw.get("cards");
        if (cards instanceof List<?> list) {
            response.setCards(mapCards(list));
        }
        Object warnings = raw.get("warnings");
        if (warnings instanceof List<?> list) {
            response.setWarnings(list.stream().map(String::valueOf).toList());
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<AnalyticsSummaryCardDto> mapCards(List<?> list) {
        List<AnalyticsSummaryCardDto> cards = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                AnalyticsSummaryCardDto card = new AnalyticsSummaryCardDto();
                card.setLabel(asString(map.get("label")));
                card.setValue(asString(map.get("value")));
                card.setHint(asString(map.get("hint")));
                card.setSeverity(asString(map.get("severity")));
                cards.add(card);
            }
        }
        return cards;
    }

    @SuppressWarnings("unchecked")
    private AnalyticsChartDto mapChart(Map<?, ?> map) {
        AnalyticsChartDto chart = new AnalyticsChartDto();
        chart.setType(asString(map.get("type")));
        chart.setTitle(asString(map.get("title")));
        Object labels = map.get("labels");
        if (labels instanceof List<?> list) {
            chart.setLabels(list.stream().map(String::valueOf).toList());
        }
        Object values = map.get("values");
        if (values instanceof List<?> list) {
            chart.setValues(list.stream()
                    .map(v -> v instanceof Number n ? n.doubleValue() : parseDouble(String.valueOf(v)))
                    .filter(Objects::nonNull)
                    .toList());
        }
        return chart;
    }

    private List<Tramite> loadFilteredTramites(AnalyticsRequest request) {
        List<Tramite> tramites = tramiteRepository.findAll().stream()
                .filter(t -> !isCancelado(t.getStatus()))
                .toList();
        return filterTramites(tramites, request);
    }

    private List<Tramite> filterTramites(List<Tramite> tramites, AnalyticsRequest request) {
        return tramites.stream()
                .filter(t -> matchesPolicy(t, request.getPolicyId()))
                .filter(t -> matchesStatus(t, request.getStatus()))
                .filter(t -> matchesDateRange(t, request.getFromDate(), request.getToDate()))
                .toList();
    }

    private KpiFilter toKpiFilter(AnalyticsRequest request) {
        KpiFilter filter = new KpiFilter();
        filter.setPolicyId(request.getPolicyId());
        filter.setStatus(request.getStatus());
        filter.setFromDate(request.getFromDate());
        filter.setToDate(request.getToDate());
        return filter;
    }

    private boolean matchesPolicy(Tramite tramite, String policyId) {
        if (policyId == null || policyId.isBlank()) {
            return true;
        }
        return policyId.trim().equals(tramite.getPolicyId());
    }

    private boolean matchesStatus(Tramite tramite, String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return true;
        }
        String status = tramite.getStatus() != null ? tramite.getStatus().trim().toUpperCase(Locale.ROOT) : "";
        String filter = statusFilter.trim().toUpperCase(Locale.ROOT);
        if ("ACTIVO".equals(filter)) {
            return isActivo(status);
        }
        if ("ERROR".equals(filter)) {
            return tramite.getWorkflowError() != null && !tramite.getWorkflowError().isBlank();
        }
        return filter.equals(status)
                || ("EN_PROCESO".equals(filter) && ("IN_PROGRESS".equals(status) || "EN_PROCESO".equals(status)))
                || ("FINALIZADO".equals(filter) && isFinalizado(status))
                || ("INICIADO".equals(filter) && ("CREATED".equals(status) || "INICIADO".equals(status)));
    }

    private boolean matchesDateRange(Tramite tramite, LocalDate from, LocalDate to) {
        LocalDateTime createdAt = tramite.getCreatedAt();
        if (createdAt == null) {
            return from == null && to == null;
        }
        if (from != null && createdAt.isBefore(from.atStartOfDay())) {
            return false;
        }
        if (to != null && createdAt.isAfter(to.atTime(LocalTime.MAX))) {
            return false;
        }
        return true;
    }

    private void validateMessage(AnalyticsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }
        boolean hasMessage = request.getMessage() != null && !request.getMessage().isBlank();
        boolean hasAudio = request.getAudioText() != null && !request.getAudioText().isBlank();
        if (!hasMessage && !hasAudio) {
            throw new IllegalArgumentException("Debe ingresar una consulta de texto o dictado por voz");
        }
    }

    private String buildCombinedText(AnalyticsRequest request) {
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
        return sb.toString().trim();
    }

    private void audit(String actor, String action, String detail) {
        bitacoraService.registrar(
                actor,
                AuditModules.ANALITICA,
                action,
                detail,
                "IntelligentAnalytics",
                null
        );
    }

    private boolean isCancelado(String status) {
        if (status == null) return false;
        String n = status.trim().toUpperCase(Locale.ROOT);
        return "CANCELADO".equals(n) || "CANCELLED".equals(n);
    }

    private boolean isActivo(String status) {
        return "INICIADO".equals(status) || "EN_PROCESO".equals(status) || "ACTIVO".equals(status)
                || "IN_PROGRESS".equals(status) || "CREATED".equals(status);
    }

    private boolean isFinalizado(String status) {
        return "FINALIZADO".equals(status) || "COMPLETADO".equals(status)
                || "COMPLETED".equals(status) || "DONE".equals(status);
    }

    private void enrichConclusion(
            AnalyticsReportResponse response,
            String combinedText,
            List<Tramite> tramites,
            KpiDashboardFullResponse kpiDashboard
    ) {
        if (response == null) {
            return;
        }
        if (response.getConclusion() == null || response.getConclusion().isBlank()) {
            response.setConclusion(
                    IntelligentAnalyticsConclusionBuilder.build(combinedText, tramites, kpiDashboard)
            );
        }
    }

    private String truncate(String text) {
        return text.length() > 180 ? text.substring(0, 180) + "..." : text;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
