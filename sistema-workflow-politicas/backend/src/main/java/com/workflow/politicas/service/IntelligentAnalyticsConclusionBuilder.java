package com.workflow.politicas.service;

import com.workflow.politicas.dto.AnalyticsRecommendationItemDto;
import com.workflow.politicas.dto.KpiBottleneckDto;
import com.workflow.politicas.dto.KpiDashboardFullResponse;
import com.workflow.politicas.dto.KpiLoadMetricDto;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Genera la conclusión narrativa que responde directamente la consulta del usuario (CU24–CU26).
 */
public final class IntelligentAnalyticsConclusionBuilder {

    private static final double DEFAULT_SLA_HOURS = 48.0;

    private IntelligentAnalyticsConclusionBuilder() {
    }

    public static String build(
            String combinedText,
            List<Tramite> tramites,
            KpiDashboardFullResponse kpiDashboard
    ) {
        String normalized = normalize(combinedText);
        String conclusionType = detectConclusionType(normalized);

        return switch (conclusionType) {
            case "FUNCIONARIO_CARGA" -> buildEmployeeLoadConclusion(kpiDashboard, tramites);
            case "PRIORIZAR_TRAMITES" -> buildPrioritizeTramitesConclusion(tramites);
            case "ACTIVIDAD_PRIORITARIA" -> buildPriorityActivityConclusion(kpiDashboard);
            case "ACCIONES_RECOMENDADAS" -> buildRecommendedActionsConclusion(tramites, kpiDashboard);
            default -> buildGeneralConclusion(tramites, kpiDashboard);
        };
    }

    private static String buildEmployeeLoadConclusion(
            KpiDashboardFullResponse kpiDashboard,
            List<Tramite> tramites
    ) {
        if (kpiDashboard == null || kpiDashboard.getEmployeeLoad() == null || kpiDashboard.getEmployeeLoad().isEmpty()) {
            return "No hay datos de carga por funcionario en el periodo analizado.";
        }

        KpiLoadMetricDto top = pickTopEmployee(kpiDashboard.getEmployeeLoad());
        if (top == null || top.getTotalActive() <= 0) {
            return "No se registran tareas activas asignadas a funcionarios en el periodo analizado.";
        }

        String displayName = resolveDisplayName(top);
        List<String> assignedCodes = findAssignedTramiteCodes(top, tramites);
        String tramitesText = assignedCodes.isEmpty()
                ? "sin trámites activos identificados en la muestra"
                : formatCodeList(assignedCodes);

        String recommendation = top.getTotalActive() >= 4
                ? "Se recomienda redistribuir tareas o priorizar el cierre de pendientes para equilibrar la bandeja."
                : "Se recomienda monitorear la evolución de su bandeja para evitar acumulación de demoras.";

        return "El funcionario con mayor carga de trabajo es "
                + displayName
                + ", con "
                + top.getTotalActive()
                + " tarea(s) activas ("
                + top.getPendingCount()
                + " pendiente(s), "
                + top.getInProgressCount()
                + " en curso). Trámites asignados: "
                + tramitesText
                + ". "
                + recommendation;
    }

    private static String buildPrioritizeTramitesConclusion(List<Tramite> tramites) {
        LocalDateTime now = LocalDateTime.now();
        List<Tramite> prioritized = tramites.stream()
                .filter(t -> isActive(t.getStatus()))
                .sorted(Comparator
                        .comparingDouble((Tramite t) -> priorityScore(t, now)).reversed()
                        .thenComparing(Tramite::getCode))
                .limit(3)
                .toList();

        if (prioritized.isEmpty()) {
            return "No hay trámites activos que requieran priorización inmediata en el periodo analizado.";
        }

        List<String> codes = prioritized.stream().map(Tramite::getCode).toList();
        String codeList = formatCodeList(codes);
        boolean hasDelay = prioritized.stream().anyMatch(t -> isDelayedTramite(t, now));

        if (hasDelay) {
            return "Debe priorizar " + codeList + " porque presentan demora y requieren seguimiento.";
        }
        return "Debe priorizar " + codeList + " por su prioridad operativa y tiempo de espera acumulado.";
    }

    private static String buildPriorityActivityConclusion(KpiDashboardFullResponse kpiDashboard) {
        if (kpiDashboard == null || kpiDashboard.getBottlenecks() == null || kpiDashboard.getBottlenecks().isEmpty()) {
            return "No se detectaron cuellos de botella significativos; atienda primero las tareas con mayor antigüedad.";
        }

        KpiBottleneckDto top = kpiDashboard.getBottlenecks().stream()
                .max(Comparator
                        .comparingInt((KpiBottleneckDto b) -> bottleneckPriority(b.getLevel()))
                        .thenComparingLong(b -> b.getPendingCount() + b.getInProgressCount() + b.getOverdueCount()))
                .orElse(kpiDashboard.getBottlenecks().get(0));

        String activityName = safe(top.getActivityName());
        long stuck = top.getPendingCount() + top.getInProgressCount();
        String reason = top.getOverdueCount() > 0
                ? "concentra " + top.getOverdueCount() + " tarea(s) demorada(s) y " + stuck + " en curso o pendientes"
                : "concentra " + stuck + " tarea(s) activas con mayor cuello de botella";

        return "La actividad prioritaria es \"" + activityName + "\" porque " + reason + ".";
    }

    private static String buildRecommendedActionsConclusion(
            List<Tramite> tramites,
            KpiDashboardFullResponse kpiDashboard
    ) {
        List<AnalyticsRecommendationItemDto> items =
                IntelligentAnalyticsFallbackMatcher.buildRecommendations(tramites, kpiDashboard).getRecommendations();

        if (items == null || items.isEmpty()) {
            return "No hay acciones urgentes recomendadas; el flujo opera dentro de parámetros normales.";
        }

        StringBuilder sb = new StringBuilder("Acciones recomendadas:");
        int limit = Math.min(3, items.size());
        for (int i = 0; i < limit; i++) {
            AnalyticsRecommendationItemDto item = items.get(i);
            sb.append("\n• ")
                    .append(safe(item.getAction()))
                    .append(" — Motivo: ")
                    .append(safe(item.getRationale()))
                    .append(" — Prioridad: ")
                    .append(safe(item.getPriority()));
        }
        return sb.toString().trim();
    }

    private static String buildGeneralConclusion(
            List<Tramite> tramites,
            KpiDashboardFullResponse kpiDashboard
    ) {
        LocalDateTime now = LocalDateTime.now();
        long delayed = tramites.stream().filter(t -> isDelayedTramite(t, now)).count();
        long active = tramites.stream().filter(t -> isActive(t.getStatus())).count();

        if (kpiDashboard != null && kpiDashboard.getBottlenecks() != null && !kpiDashboard.getBottlenecks().isEmpty()) {
            KpiBottleneckDto top = kpiDashboard.getBottlenecks().get(0);
            return "En el periodo analizado hay "
                    + active
                    + " trámite(s) activo(s) y "
                    + delayed
                    + " con demora. El principal cuello de botella es \""
                    + safe(top.getActivityName())
                    + "\"; revise riesgos y recomendaciones para definir la siguiente acción.";
        }

        if (delayed > 0) {
            return "Se detectaron " + delayed + " trámite(s) demorado(s) sobre " + tramites.size()
                    + " analizados; priorice su seguimiento según la lista de riesgos.";
        }

        return "El flujo analizado muestra " + active + " trámite(s) activo(s) sin demoras críticas en el periodo seleccionado.";
    }

    private static String detectConclusionType(String normalized) {
        if (containsAny(normalized, "actividad")
                && containsAny(normalized, "primero", "primera", "atender primero", "debo atender", "atender primero")) {
            return "ACTIVIDAD_PRIORITARIA";
        }
        if (containsAny(normalized, "acciones recomiendas", "que acciones", "qué acciones",
                "acciones recomendadas", "que me recomiendas", "qué me recomiendas")) {
            return "ACCIONES_RECOMENDADAS";
        }
        if (containsAny(normalized, "priorizar", "debo priorizar", "que tramites debo", "qué trámites debo",
                "tramites debo priorizar", "trámites debo priorizar", "que tramite priorizar")) {
            return "PRIORIZAR_TRAMITES";
        }
        if (containsAny(normalized, "funcionario", "empleado", "carga de trabajo", "mayor carga",
                "quien tiene mas carga", "quién tiene más carga", "mas carga de trabajo", "más carga de trabajo")) {
            return "FUNCIONARIO_CARGA";
        }
        return "GENERAL";
    }

    private static List<String> findAssignedTramiteCodes(KpiLoadMetricDto employee, List<Tramite> tramites) {
        Set<String> codes = new LinkedHashSet<>();
        String key = normalizeKey(employee.getKey());
        String displayName = normalizeKey(employee.getDisplayName());

        for (Tramite tramite : tramites) {
            if (!isActive(tramite.getStatus())) {
                continue;
            }
            if (tramite.getTasks() == null || tramite.getTasks().isEmpty()) {
                continue;
            }
            boolean assigned = tramite.getTasks().stream()
                    .filter(task -> isPendingOrInProgress(task.getStatus()))
                    .anyMatch(task -> matchesEmployee(task, tramite, key, displayName));
            if (assigned && tramite.getCode() != null) {
                codes.add(tramite.getCode());
            }
        }
        return new ArrayList<>(codes);
    }

    private static boolean matchesEmployee(
            TramiteTask task,
            Tramite tramite,
            String employeeKey,
            String employeeDisplayName
    ) {
        if (task.getTakenBy() != null && !task.getTakenBy().isBlank()) {
            String takenBy = normalizeKey(task.getTakenBy());
            if (takenBy.equals(employeeKey) || takenBy.equals(employeeDisplayName)) {
                return true;
            }
        }
        String responsible = task.getResponsible() != null ? task.getResponsible() : tramite.getResponsible();
        if (responsible != null && !responsible.isBlank()) {
            String normalizedResponsible = normalizeKey(responsible);
            return normalizedResponsible.equals(employeeKey)
                    || normalizedResponsible.equals(employeeDisplayName);
        }
        return false;
    }

    private static KpiLoadMetricDto pickTopEmployee(List<KpiLoadMetricDto> loads) {
        if (loads == null || loads.isEmpty()) {
            return null;
        }
        return loads.stream()
                .filter(load -> EmployeeDisplayNameResolver.isRankableMetric(
                        load, Collections.emptyList(), Collections.emptyMap()))
                .max(Comparator.comparingLong(KpiLoadMetricDto::getTotalActive))
                .orElse(null);
    }

    private static String resolveDisplayName(KpiLoadMetricDto load) {
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

    private static String formatCodeList(List<String> codes) {
        if (codes.isEmpty()) {
            return "";
        }
        if (codes.size() == 1) {
            return codes.get(0);
        }
        if (codes.size() == 2) {
            return codes.get(0) + " y " + codes.get(1);
        }
        String head = String.join(", ", codes.subList(0, codes.size() - 1));
        return head + " y " + codes.get(codes.size() - 1);
    }

    private static boolean isDelayedTramite(Tramite tramite, LocalDateTime now) {
        if (!isActive(tramite.getStatus())) {
            return false;
        }
        return hoursSince(tramite.getUpdatedAt(), now) >= DEFAULT_SLA_HOURS;
    }

    private static double priorityScore(Tramite tramite, LocalDateTime now) {
        double score = hoursSince(tramite.getUpdatedAt(), now);
        String priority = safe(tramite.getPriority()).toUpperCase(Locale.ROOT);
        if ("URGENTE".equals(priority)) {
            score += 72;
        } else if ("ALTA".equals(priority)) {
            score += 36;
        } else if ("BAJA".equals(priority)) {
            score -= 12;
        }
        if (tramite.getWorkflowError() != null && !tramite.getWorkflowError().isBlank()) {
            score += 48;
        }
        if (isDelayedTramite(tramite, now)) {
            score += 24;
        }
        return score;
    }

    private static int bottleneckPriority(String level) {
        if ("Alto".equalsIgnoreCase(level)) {
            return 3;
        }
        if ("Medio".equalsIgnoreCase(level)) {
            return 2;
        }
        return 1;
    }

    private static double hoursSince(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        return Duration.between(start, end).toMinutes() / 60.0;
    }

    private static boolean isActive(String status) {
        if (status == null) {
            return false;
        }
        String n = status.trim().toUpperCase(Locale.ROOT);
        return "INICIADO".equals(n) || "EN_PROCESO".equals(n) || "ACTIVO".equals(n)
                || "IN_PROGRESS".equals(n) || "CREATED".equals(n);
    }

    private static boolean isPendingOrInProgress(String status) {
        if (status == null) {
            return false;
        }
        String n = status.trim().toUpperCase(Locale.ROOT);
        return "PENDIENTE".equals(n) || "EN_CURSO".equals(n);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private static String normalizeKey(String value) {
        return normalize(value).replaceAll("\\s+", " ");
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }
}
