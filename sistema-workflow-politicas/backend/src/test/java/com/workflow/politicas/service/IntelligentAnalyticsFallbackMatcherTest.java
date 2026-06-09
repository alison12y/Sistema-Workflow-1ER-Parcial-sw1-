package com.workflow.politicas.service;

import com.workflow.politicas.dto.*;
import com.workflow.politicas.model.TraceItem;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntelligentAnalyticsFallbackMatcherTest {

    @Test
    void buildReport_detectsDelayedTramitesQuery() {
        List<Tramite> tramites = List.of(
                activeTramite("TRM-001", "Política A", LocalDateTime.now().minusHours(60)),
                finishedTramite("TRM-002", "Política B")
        );
        AnalyticsRequest request = new AnalyticsRequest();
        request.setMessage("Cuáles trámites están demorados");

        AnalyticsReportResponse response = IntelligentAnalyticsFallbackMatcher.buildReport(
                request.getMessage(), tramites, request, emptyKpi()
        );

        assertEquals("TRAMITES_DEMORADOS", response.getReportType());
        assertEquals("Trámites demorados", response.getTitle());
        assertEquals(1, response.getRows().size());
        assertEquals("LOCAL_FALLBACK", response.getSource());
    }

    @Test
    void buildReport_detectsMostUsedPolicyQuery() {
        List<Tramite> tramites = List.of(
                activeTramite("TRM-001", "Política A", LocalDateTime.now().minusHours(2)),
                activeTramite("TRM-002", "Política A", LocalDateTime.now().minusHours(3)),
                activeTramite("TRM-003", "Política B", LocalDateTime.now().minusHours(1))
        );
        AnalyticsRequest request = new AnalyticsRequest();
        request.setMessage("Cuál es la política más usada");

        AnalyticsReportResponse response = IntelligentAnalyticsFallbackMatcher.buildReport(
                request.getMessage(), tramites, request, emptyKpi()
        );

        assertEquals("POLITICA_MAS_USADA", response.getReportType());
        assertFalse(response.getRows().isEmpty());
        assertEquals("Política A", response.getRows().get(0).get("Política"));
    }

    @Test
    void buildReport_includesConclusionForEmployeeLoadQuery() {
        KpiLoadMetricDto load = new KpiLoadMetricDto();
        load.setKey("carlos.mendez");
        load.setDisplayName("Carlos Méndez");
        load.setPendingCount(2);
        load.setInProgressCount(3);
        load.setTotalActive(5);

        KpiDashboardFullResponse kpi = emptyKpi();
        kpi.setEmployeeLoad(List.of(load));

        AnalyticsRequest request = new AnalyticsRequest();
        request.setMessage("¿Qué funcionario tiene mayor carga de trabajo?");

        AnalyticsReportResponse response = IntelligentAnalyticsFallbackMatcher.buildReport(
                request.getMessage(), List.of(), request, kpi
        );

        assertNotNull(response.getConclusion());
        assertTrue(response.getConclusion().contains("Carlos Méndez"));
        assertTrue(response.getConclusion().contains("5 tarea(s) activas"));
    }

    @Test
    void buildRisks_detectsDelayedTramiteAndOverdueTask() {
        Tramite tramite = activeTramite("TRM-010", "Política X", LocalDateTime.now().minusHours(72));
        TramiteTask task = new TramiteTask();
        task.setName("Revisión");
        task.setStatus("PENDIENTE");
        task.setStartedAt(LocalDateTime.now().minusHours(80));
        tramite.setTasks(new ArrayList<>(List.of(task)));

        AnalyticsRiskResponse response = IntelligentAnalyticsFallbackMatcher.buildRisks(
                List.of(tramite), emptyKpi()
        );

        assertFalse(response.getRisks().isEmpty());
        assertTrue(response.getRisks().stream().anyMatch(r -> "DEMORA".equals(r.getType())));
        assertTrue(response.getRisks().stream().anyMatch(r -> "VENCIDA".equals(r.getType())));
    }

    @Test
    void buildRecommendations_prioritizesDelayedTramite() {
        Tramite delayed = activeTramite("TRM-099", "Política Z", LocalDateTime.now().minusHours(96));
        delayed.setPriority("URGENTE");
        delayed.setCurrentActivity("Aprobación");

        AnalyticsRecommendationResponse response = IntelligentAnalyticsFallbackMatcher.buildRecommendations(
                List.of(delayed), emptyKpi()
        );

        assertFalse(response.getRecommendations().isEmpty());
        AnalyticsRecommendationItemDto first = response.getRecommendations().get(0);
        assertEquals("PRIORIZAR_TRAMITE", first.getType());
        assertEquals("TRM-099", first.getTramiteCode());
        assertEquals("ALTA", first.getPriority());
    }

    private static KpiDashboardFullResponse emptyKpi() {
        KpiDashboardFullResponse dashboard = new KpiDashboardFullResponse();
        dashboard.setSummary(new KpiSummaryResponse());
        dashboard.setBottlenecks(List.of());
        dashboard.setEmployeeLoad(List.of());
        return dashboard;
    }

    private static Tramite activeTramite(String code, String policyName, LocalDateTime updatedAt) {
        Tramite tramite = new Tramite();
        tramite.setId("id-" + code);
        tramite.setCode(code);
        tramite.setPolicyName(policyName);
        tramite.setStatus("EN_PROCESO");
        tramite.setPriority("NORMAL");
        tramite.setCurrentActivity("Validación");
        tramite.setCreatedAt(updatedAt.minusDays(2));
        tramite.setUpdatedAt(updatedAt);
        tramite.setTasks(new ArrayList<>());
        tramite.setTrace(new ArrayList<>());
        return tramite;
    }

    private static Tramite finishedTramite(String code, String policyName) {
        Tramite tramite = activeTramite(code, policyName, LocalDateTime.now().minusDays(1));
        tramite.setStatus("FINALIZADO");
        TraceItem trace = new TraceItem();
        trace.setEventType("COMPLETADO");
        tramite.setTrace(new ArrayList<>(List.of(trace)));
        return tramite;
    }
}
