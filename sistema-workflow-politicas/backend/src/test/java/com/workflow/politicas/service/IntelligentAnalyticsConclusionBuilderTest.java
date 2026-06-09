package com.workflow.politicas.service;

import com.workflow.politicas.dto.KpiBottleneckDto;
import com.workflow.politicas.dto.KpiDashboardFullResponse;
import com.workflow.politicas.dto.KpiLoadMetricDto;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntelligentAnalyticsConclusionBuilderTest {

    @Test
    void build_employeeLoadQuery_ignoresRoleLabelAndUsesRealPersonName() {
        KpiLoadMetricDto roleBucket = new KpiLoadMetricDto();
        roleBucket.setKey("Funcionario");
        roleBucket.setDisplayName("Funcionario");
        roleBucket.setTotalActive(10);

        KpiLoadMetricDto person = new KpiLoadMetricDto();
        person.setKey("ana.rodriguez");
        person.setDisplayName("Ana Rodríguez Paz");
        person.setPendingCount(2);
        person.setInProgressCount(1);
        person.setTotalActive(3);

        KpiDashboardFullResponse kpi = emptyKpi();
        kpi.setEmployeeLoad(List.of(roleBucket, person));

        String conclusion = IntelligentAnalyticsConclusionBuilder.build(
                "¿Qué funcionario tiene mayor carga de trabajo?",
                List.of(),
                kpi
        );

        assertTrue(conclusion.contains("Ana Rodríguez Paz"));
        assertFalse(conclusion.contains("es Funcionario,"));
        assertFalse(conclusion.endsWith("es Funcionario"));
    }

    @Test
    void build_employeeLoadQuery_mentionsDisplayNameAndActiveTasks() {
        KpiLoadMetricDto load = new KpiLoadMetricDto();
        load.setKey("ana.rodriguez");
        load.setDisplayName("Ana Rodríguez");
        load.setPendingCount(3);
        load.setInProgressCount(2);
        load.setTotalActive(5);

        KpiDashboardFullResponse kpi = dashboardWithLoad(load);
        Tramite tramite = activeTramite("TRM-010", LocalDateTime.now().minusHours(10));
        TramiteTask task = pendingTask("ana.rodriguez");
        tramite.setTasks(new ArrayList<>(List.of(task)));

        String conclusion = IntelligentAnalyticsConclusionBuilder.build(
                "¿Qué funcionario tiene mayor carga de trabajo?",
                List.of(tramite),
                kpi
        );

        assertTrue(conclusion.contains("Ana Rodríguez"));
        assertTrue(conclusion.contains("5 tarea(s) activas"));
        assertTrue(conclusion.contains("TRM-010"));
    }

    @Test
    void build_prioritizeTramitesQuery_listsTopDelayedCodes() {
        Tramite delayed1 = activeTramite("TRM-001", LocalDateTime.now().minusHours(72));
        Tramite delayed2 = activeTramite("TRM-002", LocalDateTime.now().minusHours(60));
        Tramite delayed3 = activeTramite("TRM-003", LocalDateTime.now().minusHours(55));

        String conclusion = IntelligentAnalyticsConclusionBuilder.build(
                "¿Qué trámites debo priorizar?",
                List.of(delayed1, delayed2, delayed3),
                emptyKpi()
        );

        assertTrue(conclusion.contains("Debe priorizar"));
        assertTrue(conclusion.contains("TRM-001"));
        assertTrue(conclusion.contains("TRM-002"));
        assertTrue(conclusion.contains("TRM-003"));
        assertTrue(conclusion.contains("demora"));
    }

    @Test
    void build_priorityActivityQuery_mentionsBottleneckActivity() {
        KpiBottleneckDto bottleneck = new KpiBottleneckDto();
        bottleneck.setActivityName("Aprobación gerencial");
        bottleneck.setLevel("Alto");
        bottleneck.setPendingCount(4);
        bottleneck.setInProgressCount(2);
        bottleneck.setOverdueCount(3);

        KpiDashboardFullResponse kpi = emptyKpi();
        kpi.setBottlenecks(List.of(bottleneck));

        String conclusion = IntelligentAnalyticsConclusionBuilder.build(
                "¿Qué actividad debo atender primero?",
                List.of(),
                kpi
        );

        assertTrue(conclusion.contains("La actividad prioritaria es"));
        assertTrue(conclusion.contains("Aprobación gerencial"));
    }

    @Test
    void build_recommendedActionsQuery_listsActionMotiveAndPriority() {
        Tramite delayed = activeTramite("TRM-099", LocalDateTime.now().minusHours(96));
        delayed.setPriority("URGENTE");
        delayed.setCurrentActivity("Validación");

        String conclusion = IntelligentAnalyticsConclusionBuilder.build(
                "¿Qué acciones recomiendas?",
                List.of(delayed),
                emptyKpi()
        );

        assertTrue(conclusion.contains("Acciones recomendadas"));
        assertTrue(conclusion.contains("Motivo:"));
        assertTrue(conclusion.contains("Prioridad:"));
        assertTrue(conclusion.contains("TRM-099"));
    }

    private static KpiDashboardFullResponse emptyKpi() {
        KpiDashboardFullResponse dashboard = new KpiDashboardFullResponse();
        dashboard.setEmployeeLoad(List.of());
        dashboard.setBottlenecks(List.of());
        return dashboard;
    }

    private static KpiDashboardFullResponse dashboardWithLoad(KpiLoadMetricDto load) {
        KpiDashboardFullResponse dashboard = emptyKpi();
        dashboard.setEmployeeLoad(List.of(load));
        return dashboard;
    }

    private static Tramite activeTramite(String code, LocalDateTime updatedAt) {
        Tramite tramite = new Tramite();
        tramite.setId("id-" + code);
        tramite.setCode(code);
        tramite.setPolicyName("Política demo");
        tramite.setStatus("EN_PROCESO");
        tramite.setPriority("NORMAL");
        tramite.setCurrentActivity("Validación");
        tramite.setCreatedAt(updatedAt.minusDays(2));
        tramite.setUpdatedAt(updatedAt);
        tramite.setTasks(new ArrayList<>());
        return tramite;
    }

    private static TramiteTask pendingTask(String takenBy) {
        TramiteTask task = new TramiteTask();
        task.setName("Revisión");
        task.setStatus("PENDIENTE");
        task.setTakenBy(takenBy);
        return task;
    }
}
