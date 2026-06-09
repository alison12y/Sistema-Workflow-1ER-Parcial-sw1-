package com.workflow.politicas.service;

import com.workflow.politicas.dto.*;
import com.workflow.politicas.model.TraceItem;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor local de reportes, riesgos y recomendaciones (CU24–CU26 fallback).
 */
public final class IntelligentAnalyticsFallbackMatcher {

    private static final double DEFAULT_SLA_HOURS = 48.0;
    private static final int HIGH_LOAD_THRESHOLD = 4;

    private static final Map<String, Month> MONTH_KEYWORDS = Map.ofEntries(
            Map.entry("enero", Month.JANUARY),
            Map.entry("febrero", Month.FEBRUARY),
            Map.entry("marzo", Month.MARCH),
            Map.entry("abril", Month.APRIL),
            Map.entry("mayo", Month.MAY),
            Map.entry("junio", Month.JUNE),
            Map.entry("julio", Month.JULY),
            Map.entry("agosto", Month.AUGUST),
            Map.entry("septiembre", Month.SEPTEMBER),
            Map.entry("octubre", Month.OCTOBER),
            Map.entry("noviembre", Month.NOVEMBER),
            Map.entry("diciembre", Month.DECEMBER)
    );

    private IntelligentAnalyticsFallbackMatcher() {
    }

    public static AnalyticsReportResponse buildReport(
            String combinedText,
            List<Tramite> tramites,
            AnalyticsRequest request,
            KpiDashboardFullResponse kpiDashboard
    ) {
        AnalyticsReportResponse response = new AnalyticsReportResponse();
        response.setSource("LOCAL_FALLBACK");

        String normalized = normalize(combinedText);
        String reportType = detectReportType(normalized);
        response.setReportType(reportType);
        response.setAppliedFilters(buildAppliedFilters(request, reportType));

        List<Tramite> scoped = applyReportScope(tramites, request, reportType, normalized);
        response.setSuggestedFormat(suggestFormat(reportType, scoped.size()));

        switch (reportType) {
            case "TRAMITES_MES" -> buildMonthReport(response, scoped, request, normalized);
            case "POLITICA_MAS_USADA" -> buildPolicyUsageReport(response, scoped, kpiDashboard);
            case "FUNCIONARIO_CARGA" -> buildEmployeeLoadReport(response, scoped, kpiDashboard);
            case "TRAMITES_DEMORADOS" -> buildDelayedReport(response, scoped);
            case "RESUMEN_FINALIZADOS" -> buildFinishedSummaryReport(response, scoped);
            default -> buildGeneralSummaryReport(response, scoped, kpiDashboard);
        }

        response.setConclusion(IntelligentAnalyticsConclusionBuilder.build(combinedText, tramites, kpiDashboard));

        if (scoped.isEmpty()) {
            response.getWarnings().add("No hay datos en el periodo o filtros seleccionados.");
        }
        response.getWarnings().add("Análisis generado con motor local (FastAPI no disponible).");
        return response;
    }

    public static AnalyticsRiskResponse buildRisks(
            List<Tramite> tramites,
            KpiDashboardFullResponse kpiDashboard
    ) {
        AnalyticsRiskResponse response = new AnalyticsRiskResponse();
        response.setSource("LOCAL_FALLBACK");
        LocalDateTime now = LocalDateTime.now();

        List<AnalyticsRiskItemDto> risks = new ArrayList<>();

        for (Tramite tramite : tramites) {
            if (isDelayedTramite(tramite, now)) {
                risks.add(riskItem(
                        "DEMORA",
                        severityForHours(hoursSince(tramite.getUpdatedAt(), now)),
                        "Trámite demorado: " + tramite.getCode(),
                        "El trámite lleva " + formatHours(hoursSince(tramite.getUpdatedAt(), now))
                                + " sin avance reciente en estado " + tramite.getStatus() + ".",
                        "Tramite",
                        tramite.getId(),
                        tramite.getCode()
                ));
            }

            if (tramite.getTasks() != null) {
                for (TramiteTask task : tramite.getTasks()) {
                    if (isOverdueTask(task, tramite, now)) {
                        risks.add(riskItem(
                                "VENCIDA",
                                "ALTO",
                                "Tarea vencida en " + tramite.getCode(),
                                "La actividad \"" + task.getName() + "\" supera el SLA estimado.",
                                "TramiteTask",
                                tramite.getId(),
                                tramite.getCode() + " / " + task.getName()
                        ));
                    }
                }
            }

            if (hasDurationAnomaly(tramite, now)) {
                risks.add(riskItem(
                        "ANOMALIA",
                        "MEDIO",
                        "Duración excesiva: " + tramite.getCode(),
                        "El trámite excede el tiempo promedio esperado para su estado actual.",
                        "Tramite",
                        tramite.getId(),
                        tramite.getCode()
                ));
            }
        }

        if (kpiDashboard != null && kpiDashboard.getBottlenecks() != null) {
            for (KpiBottleneckDto bottleneck : kpiDashboard.getBottlenecks()) {
                risks.add(riskItem(
                        "CUELLO",
                        mapBottleneckLevel(bottleneck.getLevel()),
                        "Cuello de botella: " + bottleneck.getActivityName(),
                        bottleneck.getObservation() != null
                                ? bottleneck.getObservation()
                                : "Acumulación en actividad " + bottleneck.getActivityName() + ".",
                        "WorkflowActivity",
                        bottleneck.getWorkflowActivityId(),
                        bottleneck.getActivityName()
                ));
            }
        }

        if (kpiDashboard != null && kpiDashboard.getEmployeeLoad() != null) {
            for (KpiLoadMetricDto load : kpiDashboard.getEmployeeLoad()) {
                if (load.getTotalActive() >= HIGH_LOAD_THRESHOLD) {
                    risks.add(riskItem(
                            "CARGA",
                            load.getTotalActive() >= HIGH_LOAD_THRESHOLD + 2 ? "ALTO" : "MEDIO",
                            "Sobrecarga: " + resolveEmployeeLabel(load),
                            load.getTotalActive() + " tareas activas (pendientes + en curso).",
                            "User",
                            load.getKey(),
                            resolveEmployeeLabel(load)
                    ));
                }
            }
        }

        Map<String, Long> policyDelays = tramites.stream()
                .filter(t -> isDelayedTramite(t, now))
                .collect(Collectors.groupingBy(
                        t -> t.getPolicyName() != null ? t.getPolicyName() : "Sin política",
                        Collectors.counting()
                ));
        policyDelays.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> risks.add(riskItem(
                        "DEMORA",
                        entry.getValue() >= 3 ? "ALTO" : "MEDIO",
                        "Política con retrasos: " + entry.getKey(),
                        entry.getValue() + " trámite(s) demorado(s) bajo esta política.",
                        "BusinessPolicy",
                        null,
                        entry.getKey()
                )));

        risks.sort(Comparator
                .comparingInt((AnalyticsRiskItemDto r) -> severityPriority(r.getSeverity()))
                .thenComparing(AnalyticsRiskItemDto::getTitle));

        response.setRisks(risks.stream().limit(25).toList());
        response.setSummary(risks.isEmpty()
                ? "No se detectaron riesgos significativos en el periodo analizado."
                : "Se detectaron " + risks.size() + " riesgo(s) o anomalía(s) en trámites y tareas.");
        response.setCards(buildRiskCards(risks, tramites));
        response.getWarnings().add("Análisis de riesgos generado con motor local.");
        return response;
    }

    public static AnalyticsRecommendationResponse buildRecommendations(
            List<Tramite> tramites,
            KpiDashboardFullResponse kpiDashboard
    ) {
        AnalyticsRecommendationResponse response = new AnalyticsRecommendationResponse();
        response.setSource("LOCAL_FALLBACK");
        LocalDateTime now = LocalDateTime.now();
        List<AnalyticsRecommendationItemDto> items = new ArrayList<>();

        tramites.stream()
                .filter(t -> isActive(t.getStatus()))
                .sorted(Comparator
                        .comparingDouble((Tramite t) -> priorityScore(t, now)).reversed()
                        .thenComparing(Tramite::getCode))
                .limit(5)
                .forEach(tramite -> {
                    AnalyticsRecommendationItemDto item = new AnalyticsRecommendationItemDto();
                    item.setPriority(mapPriorityLabel(priorityScore(tramite, now)));
                    item.setType("PRIORIZAR_TRAMITE");
                    item.setTitle("Priorizar trámite " + tramite.getCode());
                    item.setAction("Revisar y avanzar el trámite " + tramite.getCode()
                            + " en actividad \"" + safe(tramite.getCurrentActivity()) + "\".");
                    item.setRationale("Demora acumulada de " + formatHours(hoursSince(tramite.getUpdatedAt(), now))
                            + " con prioridad " + safe(tramite.getPriority()) + ".");
                    item.setTramiteCode(tramite.getCode());
                    item.setActivityName(tramite.getCurrentActivity());
                    items.add(item);
                });

        if (kpiDashboard != null && kpiDashboard.getBottlenecks() != null) {
            kpiDashboard.getBottlenecks().stream().limit(3).forEach(b -> {
                AnalyticsRecommendationItemDto item = new AnalyticsRecommendationItemDto();
                item.setPriority("ALTA".equals(mapBottleneckLevel(b.getLevel())) ? "ALTA" : "MEDIA");
                item.setType("REVISAR_CUELLO");
                item.setTitle("Revisar cuello de botella: " + b.getActivityName());
                item.setAction("Analizar tiempos y reasignar recursos en la actividad \""
                        + b.getActivityName() + "\".");
                item.setRationale(b.getObservation() != null ? b.getObservation()
                        : "Nivel de riesgo " + b.getLevel() + " detectado por KPI.");
                item.setActivityName(b.getActivityName());
                items.add(item);
            });
        }

        if (kpiDashboard != null && kpiDashboard.getEmployeeLoad() != null) {
            kpiDashboard.getEmployeeLoad().stream()
                    .filter(l -> l.getTotalActive() >= HIGH_LOAD_THRESHOLD)
                    .limit(3)
                    .forEach(load -> {
                        AnalyticsRecommendationItemDto item = new AnalyticsRecommendationItemDto();
                        item.setPriority(load.getTotalActive() >= HIGH_LOAD_THRESHOLD + 2 ? "ALTA" : "MEDIA");
                        item.setType("REASIGNAR_TAREA");
                        item.setTitle("Reasignar carga de " + resolveEmployeeLabel(load));
                        item.setAction("Redistribuir tareas pendientes del responsable " + resolveEmployeeLabel(load) + ".");
                        item.setRationale(load.getTotalActive() + " tareas activas superan el umbral recomendado.");
                        items.add(item);
                    });
        }

        Map<String, Long> policyDelays = tramites.stream()
                .filter(t -> isDelayedTramite(t, now))
                .collect(Collectors.groupingBy(
                        t -> t.getPolicyName() != null ? t.getPolicyName() : "Sin política",
                        Collectors.counting()
                ));
        policyDelays.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    AnalyticsRecommendationItemDto item = new AnalyticsRecommendationItemDto();
                    item.setPriority(entry.getValue() >= 3 ? "ALTA" : "MEDIA");
                    item.setType("POLITICA_RIESGO");
                    item.setTitle("Política con riesgo alto de demora");
                    item.setAction("Revisar flujo y SLA de la política \"" + entry.getKey() + "\".");
                    item.setRationale(entry.getValue() + " trámite(s) demorado(s) asociados a esta política.");
                    items.add(item);
                });

        tramites.stream()
                .filter(t -> isActive(t.getStatus()) && t.getTasks() != null)
                .flatMap(t -> t.getTasks().stream()
                        .filter(task -> isPendingOrInProgress(task.getStatus()))
                        .map(task -> Map.entry(t, task)))
                .sorted(Comparator.comparingDouble(e -> -taskWaitHours(e.getValue(), e.getKey(), now)))
                .limit(2)
                .forEach(e -> {
                    Tramite tramite = e.getKey();
                    TramiteTask task = e.getValue();
                    AnalyticsRecommendationItemDto item = new AnalyticsRecommendationItemDto();
                    item.setPriority("MEDIA");
                    item.setType("RUTA_SUGERIDA");
                    item.setTitle("Siguiente acción en " + tramite.getCode());
                    item.setAction("Completar \"" + task.getName() + "\" asignada a "
                            + safe(task.getResponsible()) + ".");
                    item.setRationale("Es la tarea con mayor tiempo de espera en el trámite.");
                    item.setTramiteCode(tramite.getCode());
                    item.setActivityName(task.getName());
                    items.add(item);
                });

        response.setRecommendations(items.stream().limit(15).toList());
        response.setSummary(items.isEmpty()
                ? "No hay recomendaciones urgentes; el flujo opera dentro de parámetros normales."
                : "Se generaron " + items.size() + " recomendación(es) de priorización y mejora.");
        response.setCards(buildRecommendationCards(items));
        response.getWarnings().add("Recomendaciones generadas con motor local.");
        return response;
    }

    private static void buildMonthReport(
            AnalyticsReportResponse response,
            List<Tramite> scoped,
            AnalyticsRequest request,
            String normalized
    ) {
        Month month = detectMonth(normalized);
        String monthLabel = month != null
                ? month.getDisplayName(TextStyle.FULL, new Locale("es", "ES"))
                : "periodo seleccionado";

        response.setTitle("Trámites de " + monthLabel);
        response.setExplanation("Listado de trámites creados en " + monthLabel
                + " según la consulta del usuario.");
        response.setColumns(List.of("Código", "Política", "Estado", "Prioridad", "Solicitante", "Creado"));
        response.setRows(scoped.stream()
                .sorted(Comparator.comparing(Tramite::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(IntelligentAnalyticsFallbackMatcher::tramiteRow)
                .toList());
        response.getCards().add(card("Total trámites", String.valueOf(scoped.size()), monthLabel, "info"));
        response.setChart(buildStatusChart(scoped, "Trámites por estado"));
    }

    private static void buildPolicyUsageReport(
            AnalyticsReportResponse response,
            List<Tramite> scoped,
            KpiDashboardFullResponse kpiDashboard
    ) {
        Map<String, Long> counts = scoped.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPolicyName() != null ? t.getPolicyName() : "Sin política",
                        Collectors.counting()
                ));

        response.setTitle("Políticas más utilizadas");
        response.setExplanation("Ranking de políticas por cantidad de trámites en el periodo filtrado.");
        response.setColumns(List.of("Política", "Trámites", "Porcentaje"));
        long total = scoped.size();
        response.setRows(counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("Política", e.getKey());
                    row.put("Trámites", e.getValue());
                    row.put("Porcentaje", total == 0 ? "0%" : String.format(Locale.ROOT, "%.0f%%", e.getValue() * 100.0 / total));
                    return row;
                })
                .toList());

        counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(top -> response.getCards().add(
                        card("Política más usada", top.getKey(), top.getValue() + " trámite(s)", "success")));

        if (kpiDashboard != null && kpiDashboard.getSummary() != null) {
            response.getCards().add(card(
                    "Trámites activos",
                    String.valueOf(kpiDashboard.getSummary().getTramitesActivos()),
                    "En el universo KPI",
                    "info"
            ));
        }
        response.setChart(buildBarChart("Uso por política", counts));
    }

    private static void buildEmployeeLoadReport(
            AnalyticsReportResponse response,
            List<Tramite> scoped,
            KpiDashboardFullResponse kpiDashboard
    ) {
        if (kpiDashboard == null || kpiDashboard.getEmployeeLoad() == null || kpiDashboard.getEmployeeLoad().isEmpty()) {
            response.setTitle("Carga por funcionario");
            response.setExplanation("No hay datos de carga de funcionarios disponibles.");
            response.setColumns(List.of("Funcionario", "Tareas activas"));
            return;
        }

        List<KpiLoadMetricDto> loadMetrics = kpiDashboard.getEmployeeLoad();

        response.setTitle("Carga por funcionario");
        response.setExplanation("Análisis detallado de la carga de trabajo actual por responsable.");
        response.setColumns(List.of("Funcionario", "Departamento", "Tareas activas", "Completadas", "Promedio atención"));
        List<KpiLoadMetricDto> rankableLoads = loadMetrics.stream()
                .filter(l -> EmployeeDisplayNameResolver.isRankableMetric(
                        l, Collections.emptyList(), Collections.emptyMap()))
                .sorted(Comparator.comparingLong(KpiLoadMetricDto::getTotalActive).reversed())
                .toList();

        response.setRows(rankableLoads.stream()
                .map(l -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("Funcionario", resolveEmployeeLabel(l));
                    row.put("Departamento", l.getDepartmentName());
                    row.put("Tareas activas", l.getTotalActive());
                    row.put("Completadas", l.getCompletedCount());
                    row.put("Promedio atención", l.getAverageHandlingTime());
                    return row;
                })
                .toList());

        rankableLoads.stream()
                .findFirst()
                .ifPresent(top -> response.getCards().add(
                        card("Mayor carga", resolveEmployeeLabel(top), top.getTotalActive() + " tarea(s) activa(s)", "warning")));

        long totalActivas = rankableLoads.stream().mapToLong(KpiLoadMetricDto::getTotalActive).sum();
        response.getCards().add(card("Total tareas", String.valueOf(totalActivas), "En toda la muestra", "info"));

        AnalyticsChartDto chart = new AnalyticsChartDto();
        chart.setType("bar");
        chart.setTitle("Tareas activas por funcionario");
        rankableLoads.stream()
                .limit(8)
                .forEach(l -> {
                    chart.getLabels().add(resolveEmployeeLabel(l));
                    chart.getValues().add((double) l.getTotalActive());
                });
        response.setChart(chart);
    }

    private static void buildDelayedReport(AnalyticsReportResponse response, List<Tramite> scoped) {
        LocalDateTime now = LocalDateTime.now();
        List<Tramite> delayed = scoped.stream()
                .filter(t -> isDelayedTramite(t, now))
                .sorted(Comparator.comparingDouble((Tramite t) -> hoursSince(t.getUpdatedAt(), now)).reversed())
                .toList();

        response.setTitle("Trámites demorados");
        response.setExplanation("Trámites activos que superan el umbral de demora (" + (int) DEFAULT_SLA_HOURS + " h).");
        response.setColumns(List.of("Código", "Política", "Estado", "Actividad actual", "Horas sin avance", "Prioridad"));
        response.setRows(delayed.stream().map(t -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Código", t.getCode());
            row.put("Política", safe(t.getPolicyName()));
            row.put("Estado", safe(t.getStatus()));
            row.put("Actividad actual", safe(t.getCurrentActivity()));
            row.put("Horas sin avance", formatHours(hoursSince(t.getUpdatedAt(), now)));
            row.put("Prioridad", safe(t.getPriority()));
            return row;
        }).toList());

        response.getCards().add(card("Demorados", String.valueOf(delayed.size()), "Sobre " + scoped.size() + " trámites", "danger"));
        response.setChart(buildBarChart("Demoras por política", delayed.stream()
                .collect(Collectors.groupingBy(
                        t -> safe(t.getPolicyName()),
                        Collectors.counting()
                ))));
    }

    private static void buildFinishedSummaryReport(AnalyticsReportResponse response, List<Tramite> scoped) {
        List<Tramite> finished = scoped.stream()
                .filter(t -> isFinalizado(t.getStatus()))
                .sorted(Comparator.comparing(Tramite::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        response.setTitle("Resumen de trámites finalizados");
        response.setExplanation("Consolidado de trámites completados en el periodo y filtros aplicados.");
        response.setColumns(List.of("Código", "Política", "Solicitante", "Prioridad", "Finalizado"));
        response.setRows(finished.stream().map(t -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Código", t.getCode());
            row.put("Política", safe(t.getPolicyName()));
            row.put("Solicitante", safe(t.getRequesterName()));
            row.put("Prioridad", safe(t.getPriority()));
            row.put("Finalizado", t.getUpdatedAt() != null ? t.getUpdatedAt().toLocalDate().toString() : "—");
            return row;
        }).toList());

        response.getCards().add(card("Finalizados", String.valueOf(finished.size()), "En el periodo", "success"));
        response.getCards().add(card("Tasa de cierre", scoped.isEmpty() ? "0%"
                : String.format(Locale.ROOT, "%.0f%%", finished.size() * 100.0 / scoped.size()), "Sobre total filtrado", "info"));
        response.setChart(buildStatusChart(finished, "Finalizados por política"));
    }

    private static void buildGeneralSummaryReport(
            AnalyticsReportResponse response,
            List<Tramite> scoped,
            KpiDashboardFullResponse kpiDashboard
    ) {
        response.setTitle("Resumen operativo de trámites");
        response.setExplanation("Vista general generada a partir de la consulta y los filtros aplicados.");
        response.setColumns(List.of("Código", "Política", "Estado", "Actividad", "Prioridad", "Actualizado"));
        response.setRows(scoped.stream()
                .sorted(Comparator.comparing(Tramite::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(50)
                .map(t -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("Código", t.getCode());
                    row.put("Política", safe(t.getPolicyName()));
                    row.put("Estado", safe(t.getStatus()));
                    row.put("Actividad", safe(t.getCurrentActivity()));
                    row.put("Prioridad", safe(t.getPriority()));
                    row.put("Actualizado", t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : "—");
                    return row;
                })
                .toList());

        response.getCards().add(card("Trámites", String.valueOf(scoped.size()), "En el filtro", "info"));
        if (kpiDashboard != null && kpiDashboard.getSummary() != null) {
            KpiSummaryResponse summary = kpiDashboard.getSummary();
            response.getCards().add(card("Activos", String.valueOf(summary.getTramitesActivos()), "KPI", "warning"));
            response.getCards().add(card("Tareas pendientes", String.valueOf(summary.getTareasPendientes()), "Bandeja", "warning"));
            response.getCards().add(card("Cuello principal", summary.getCuelloDeBotellaPrincipal(), "KPI", "danger"));
        }
        response.setChart(buildStatusChart(scoped, "Distribución por estado"));
    }

    private static String detectReportType(String normalized) {
        if (containsAny(normalized, "mayo", "enero", "febrero", "marzo", "abril", "junio", "julio",
                "agosto", "septiembre", "octubre", "noviembre", "diciembre", "mes", "periodo")) {
            return "TRAMITES_MES";
        }
        if (containsAny(normalized, "politica mas usada", "politica mas utilizada", "mas usada", "mas utilizada")) {
            return "POLITICA_MAS_USADA";
        }
        if (containsAny(normalized, "funcionario", "empleado", "carga", "responsable", "quien tiene mas")) {
            return "FUNCIONARIO_CARGA";
        }
        if (containsAny(normalized, "demorad", "retrasad", "atrasad", "tardan", "lentos")) {
            return "TRAMITES_DEMORADOS";
        }
        if (containsAny(normalized, "finalizad", "cerrad", "completad", "terminad")) {
            return "RESUMEN_FINALIZADOS";
        }
        return "RESUMEN_GENERAL";
    }

    private static List<Tramite> applyReportScope(
            List<Tramite> tramites,
            AnalyticsRequest request,
            String reportType,
            String normalized
    ) {
        List<Tramite> scoped = new ArrayList<>(tramites);
        if ("RESUMEN_FINALIZADOS".equals(reportType)) {
            scoped = scoped.stream().filter(t -> isFinalizado(t.getStatus())).toList();
        } else if ("TRAMITES_DEMORADOS".equals(reportType)) {
            LocalDateTime now = LocalDateTime.now();
            scoped = scoped.stream().filter(t -> isDelayedTramite(t, now)).toList();
        } else if ("TRAMITES_MES".equals(reportType)) {
            Month month = detectMonth(normalized);
            if (month != null) {
                int year = request.getFromDate() != null ? request.getFromDate().getYear()
                        : LocalDate.now().getYear();
                scoped = scoped.stream()
                        .filter(t -> t.getCreatedAt() != null
                                && t.getCreatedAt().getMonth() == month
                                && t.getCreatedAt().getYear() == year)
                        .toList();
            }
        }
        return scoped;
    }

    private static Month detectMonth(String normalized) {
        for (Map.Entry<String, Month> entry : MONTH_KEYWORDS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Map<String, Object> buildAppliedFilters(AnalyticsRequest request, String reportType) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("reportType", reportType);
        if (request.getPolicyId() != null && !request.getPolicyId().isBlank()) {
            filters.put("policyId", request.getPolicyId());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            filters.put("status", request.getStatus());
        }
        if (request.getFromDate() != null) {
            filters.put("fromDate", request.getFromDate().toString());
        }
        if (request.getToDate() != null) {
            filters.put("toDate", request.getToDate().toString());
        }
        return filters;
    }

    private static String suggestFormat(String reportType, int rowCount) {
        if (rowCount > 30) {
            return "EXCEL";
        }
        if ("RESUMEN_FINALIZADOS".equals(reportType) || "TRAMITES_MES".equals(reportType)) {
            return "PDF";
        }
        if ("POLITICA_MAS_USADA".equals(reportType) || "FUNCIONARIO_CARGA".equals(reportType)) {
            return rowCount > 10 ? "EXCEL" : "PANTALLA";
        }
        return "PANTALLA";
    }

    private static Map<String, Object> tramiteRow(Tramite tramite) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Código", tramite.getCode());
        row.put("Política", safe(tramite.getPolicyName()));
        row.put("Estado", safe(tramite.getStatus()));
        row.put("Prioridad", safe(tramite.getPriority()));
        row.put("Solicitante", safe(tramite.getRequesterName()));
        row.put("Creado", tramite.getCreatedAt() != null ? tramite.getCreatedAt().toLocalDate().toString() : "—");
        return row;
    }

    private static AnalyticsChartDto buildStatusChart(List<Tramite> tramites, String title) {
        Map<String, Long> counts = tramites.stream()
                .collect(Collectors.groupingBy(t -> safe(t.getStatus()), Collectors.counting()));
        return buildBarChart(title, counts);
    }

    private static AnalyticsChartDto buildBarChart(String title, Map<String, Long> counts) {
        AnalyticsChartDto chart = new AnalyticsChartDto();
        chart.setType("bar");
        chart.setTitle(title);
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .forEach(e -> {
                    chart.getLabels().add(e.getKey());
                    chart.getValues().add(e.getValue().doubleValue());
                });
        return chart;
    }

    private static List<AnalyticsSummaryCardDto> buildRiskCards(List<AnalyticsRiskItemDto> risks, List<Tramite> tramites) {
        List<AnalyticsSummaryCardDto> cards = new ArrayList<>();
        long alto = risks.stream().filter(r -> "ALTO".equals(r.getSeverity())).count();
        long demoras = risks.stream().filter(r -> "DEMORA".equals(r.getType()) || "VENCIDA".equals(r.getType())).count();
        cards.add(card("Riesgos detectados", String.valueOf(risks.size()), "En el periodo", alto > 0 ? "danger" : "warning"));
        cards.add(card("Trámites analizados", String.valueOf(tramites.size()), "Universo filtrado", "info"));
        cards.add(card("Demoras / vencidas", String.valueOf(demoras), "Requieren atención", demoras > 0 ? "danger" : "success"));
        return cards;
    }

    private static List<AnalyticsSummaryCardDto> buildRecommendationCards(List<AnalyticsRecommendationItemDto> items) {
        List<AnalyticsSummaryCardDto> cards = new ArrayList<>();
        long alta = items.stream().filter(i -> "ALTA".equals(i.getPriority())).count();
        cards.add(card("Recomendaciones", String.valueOf(items.size()), "Acciones sugeridas", "info"));
        cards.add(card("Prioridad alta", String.valueOf(alta), "Atender primero", alta > 0 ? "warning" : "success"));
        return cards;
    }

    private static AnalyticsSummaryCardDto card(String label, String value, String hint, String severity) {
        AnalyticsSummaryCardDto card = new AnalyticsSummaryCardDto();
        card.setLabel(label);
        card.setValue(value);
        card.setHint(hint);
        card.setSeverity(severity);
        return card;
    }

    private static AnalyticsRiskItemDto riskItem(
            String type,
            String severity,
            String title,
            String description,
            String entityType,
            String entityId,
            String entityLabel
    ) {
        AnalyticsRiskItemDto item = new AnalyticsRiskItemDto();
        item.setType(type);
        item.setSeverity(severity);
        item.setTitle(title);
        item.setDescription(description);
        item.setEntityType(entityType);
        item.setEntityId(entityId);
        item.setEntityLabel(entityLabel);
        return item;
    }

    private static boolean isDelayedTramite(Tramite tramite, LocalDateTime now) {
        if (!isActive(tramite.getStatus())) {
            return false;
        }
        return hoursSince(tramite.getUpdatedAt(), now) >= DEFAULT_SLA_HOURS;
    }

    private static boolean isOverdueTask(TramiteTask task, Tramite tramite, LocalDateTime now) {
        if (!isPendingOrInProgress(task.getStatus())) {
            return false;
        }
        return taskWaitHours(task, tramite, now) >= DEFAULT_SLA_HOURS;
    }

    private static boolean hasDurationAnomaly(Tramite tramite, LocalDateTime now) {
        if (tramite.getCreatedAt() == null || !isActive(tramite.getStatus())) {
            return false;
        }
        double totalHours = hoursSince(tramite.getCreatedAt(), now);
        return totalHours >= DEFAULT_SLA_HOURS * 3;
    }

    private static double taskWaitHours(TramiteTask task, Tramite tramite, LocalDateTime now) {
        LocalDateTime start = task.getTakenAt() != null ? task.getTakenAt()
                : task.getStartedAt() != null ? task.getStartedAt()
                : tramite.getUpdatedAt() != null ? tramite.getUpdatedAt()
                : tramite.getCreatedAt();
        return hoursSince(start, now);
    }

    private static double priorityScore(Tramite tramite, LocalDateTime now) {
        double score = hoursSince(tramite.getUpdatedAt(), now);
        String priority = safe(tramite.getPriority()).toUpperCase(Locale.ROOT);
        if ("URGENTE".equals(priority)) score += 72;
        else if ("ALTA".equals(priority)) score += 36;
        else if ("BAJA".equals(priority)) score -= 12;
        if (tramite.getWorkflowError() != null && !tramite.getWorkflowError().isBlank()) score += 48;
        return score;
    }

    private static String mapPriorityLabel(double score) {
        if (score >= 96) return "ALTA";
        if (score >= 48) return "MEDIA";
        return "BAJA";
    }

    private static String severityForHours(double hours) {
        if (hours >= DEFAULT_SLA_HOURS * 2) return "ALTO";
        if (hours >= DEFAULT_SLA_HOURS) return "MEDIO";
        return "BAJO";
    }

    private static String mapBottleneckLevel(String level) {
        if ("Alto".equalsIgnoreCase(level)) return "ALTO";
        if ("Medio".equalsIgnoreCase(level)) return "MEDIO";
        return "BAJO";
    }

    private static int severityPriority(String severity) {
        return switch (safe(severity)) {
            case "ALTO" -> 0;
            case "MEDIO" -> 1;
            case "BAJO" -> 2;
            default -> 3;
        };
    }

    private static double hoursSince(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        return Duration.between(start, end).toMinutes() / 60.0;
    }

    private static String formatHours(double hours) {
        if (hours < 1) {
            return Math.max(1, Math.round(hours * 60)) + " min";
        }
        if (hours < 24) {
            return String.format(Locale.ROOT, "%.1f h", hours);
        }
        return String.format(Locale.ROOT, "%.1f d", hours / 24.0);
    }

    private static boolean isActive(String status) {
        if (status == null) return false;
        String n = status.trim().toUpperCase(Locale.ROOT);
        return "INICIADO".equals(n) || "EN_PROCESO".equals(n) || "ACTIVO".equals(n)
                || "IN_PROGRESS".equals(n) || "CREATED".equals(n);
    }

    private static boolean isFinalizado(String status) {
        if (status == null) return false;
        String n = status.trim().toUpperCase(Locale.ROOT);
        return "FINALIZADO".equals(n) || "COMPLETADO".equals(n) || "COMPLETED".equals(n) || "DONE".equals(n);
    }

    private static boolean isPendingOrInProgress(String status) {
        if (status == null) return false;
        String n = status.trim().toUpperCase(Locale.ROOT);
        return "PENDIENTE".equals(n) || "EN_CURSO".equals(n);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String text) {
        if (text == null) return "";
        String lower = text.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    private static String resolveEmployeeLabel(KpiLoadMetricDto load) {
        String resolved = EmployeeDisplayNameResolver.resolvePersonLabel(
                load,
                Collections.emptyMap(),
                Collections.emptyList()
        );
        if (resolved != null && !EmployeeDisplayNameResolver.isRoleLabel(resolved, Collections.emptyList())) {
            return resolved;
        }
        return safe(load.getKey());
    }
}
