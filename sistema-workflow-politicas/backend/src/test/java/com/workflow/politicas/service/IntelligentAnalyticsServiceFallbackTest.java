package com.workflow.politicas.service;

import com.workflow.politicas.dto.AnalyticsReportResponse;
import com.workflow.politicas.dto.AnalyticsRequest;
import com.workflow.politicas.dto.KpiDashboardFullResponse;
import com.workflow.politicas.dto.KpiSummaryResponse;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.repository.TramiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntelligentAnalyticsServiceFallbackTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private TramiteRepository tramiteRepository;
    @Mock
    private KpiService kpiService;
    @Mock
    private BitacoraService bitacoraService;

    private IntelligentAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new IntelligentAnalyticsService(
                restTemplate,
                tramiteRepository,
                kpiService,
                bitacoraService
        );
        ReflectionTestUtils.setField(service, "aiServiceUrl", "http://localhost:59999");
    }

    @Test
    void generateReport_whenAiServiceDown_usesLocalFallback() {
        Tramite tramite = new Tramite();
        tramite.setId("t1");
        tramite.setCode("TRM-001");
        tramite.setPolicyName("Política A");
        tramite.setStatus("EN_PROCESO");
        tramite.setCreatedAt(LocalDateTime.now().minusDays(1));
        tramite.setUpdatedAt(LocalDateTime.now().minusHours(60));
        tramite.setTasks(new ArrayList<>());

        when(tramiteRepository.findAll()).thenReturn(List.of(tramite));
        when(kpiService.getDashboard(any())).thenReturn(emptyDashboard());
        when(restTemplate.exchange(
                eq("http://localhost:59999/analytics/report"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new ResourceAccessException("down"));

        AnalyticsRequest request = new AnalyticsRequest();
        request.setMessage("Cuáles trámites están demorados");

        AnalyticsReportResponse response = service.generateReport(request, "supervisor.user");

        assertNotNull(response);
        assertEquals("LOCAL_FALLBACK", response.getSource());
        assertEquals("TRAMITES_DEMORADOS", response.getReportType());
        assertTrue(response.getWarnings().stream().anyMatch(w -> w.contains("motor local")));
        verify(bitacoraService).registrar(
                eq("supervisor.user"),
                any(),
                eq("ANALYTICS_REPORT_REQUESTED"),
                any(),
                any(),
                isNull()
        );
    }

    private static KpiDashboardFullResponse emptyDashboard() {
        KpiDashboardFullResponse dashboard = new KpiDashboardFullResponse();
        dashboard.setSummary(new KpiSummaryResponse());
        dashboard.setBottlenecks(List.of());
        dashboard.setEmployeeLoad(List.of());
        return dashboard;
    }
}
