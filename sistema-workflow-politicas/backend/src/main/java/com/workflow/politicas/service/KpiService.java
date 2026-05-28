package com.workflow.politicas.service;

import com.workflow.politicas.dto.KpiBottleneckDto;
import com.workflow.politicas.dto.KpiSummaryResponse;
import com.workflow.politicas.model.KpiReport;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import com.workflow.politicas.repository.KpiReportRepository;
import com.workflow.politicas.repository.TramiteRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class KpiService {

    private final TramiteRepository tramiteRepository;
    private final KpiReportRepository kpiReportRepository;

    public KpiService(TramiteRepository tramiteRepository, KpiReportRepository kpiReportRepository) {
        this.tramiteRepository = tramiteRepository;
        this.kpiReportRepository = kpiReportRepository;
    }

    public KpiSummaryResponse getSummary() {
        List<Tramite> tramites = tramiteRepository.findAll();
        List<KpiBottleneckDto> bottlenecks = calculateBottlenecks(tramites);

        KpiSummaryResponse summary = new KpiSummaryResponse();
        summary.setTotalTramites(tramites.size());
        summary.setIniciados(tramites.stream().filter(t -> isIniciado(t.getStatus())).count());
        summary.setEnProceso(tramites.stream().filter(t -> isEnProceso(t.getStatus())).count());
        summary.setFinalizados(tramites.stream().filter(t -> isFinalizado(t.getStatus())).count());
        summary.setCancelados(tramites.stream().filter(t -> isCancelado(t.getStatus())).count());
        summary.setTiempoPromedio(formatDays(calculateAverageAttentionDays(tramites)));

        ActivityMetric topActivity = findActivityWithHighestDelay(tramites);
        summary.setActividadMayorDemora(
                topActivity != null ? topActivity.activityName : "Sin datos"
        );
        summary.setResponsableMayorCarga(findResponsibleWithHighestLoad(tramites));
        summary.setCuelloDeBotellaPrincipal(
                bottlenecks.isEmpty()
                        ? "Sin datos"
                        : bottlenecks.get(0).getActivityName() + " (" + bottlenecks.get(0).getLevel() + ")"
        );

        saveKpiReport("SUMMARY", buildSummaryMetrics(summary, bottlenecks.size()));
        return summary;
    }

    public List<KpiBottleneckDto> getBottlenecks() {
        List<KpiBottleneckDto> bottlenecks = calculateBottlenecks(tramiteRepository.findAll());
        saveKpiReport("BOTTLENECKS", Map.of("count", bottlenecks.size()));
        return bottlenecks;
    }

    private List<KpiBottleneckDto> calculateBottlenecks(List<Tramite> tramites) {
        Map<String, ActivityGroup> groups = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (Tramite tramite : tramites) {
            if (isFinalizado(tramite.getStatus()) || isCancelado(tramite.getStatus())) {
                continue;
            }

            if (tramite.getTasks() != null && !tramite.getTasks().isEmpty()) {
                accumulateTaskGroups(tramite, groups, now);
            } else {
                accumulateTramiteGroup(tramite, groups, now);
            }
        }

        List<KpiBottleneckDto> bottlenecks = new ArrayList<>();
        for (ActivityGroup group : groups.values()) {
            long stuckCount = group.pendingCount + group.inProgressCount;
            if (stuckCount <= 0) {
                continue;
            }

            double averageDays = group.samples == 0 ? 0 : group.totalDays / group.samples;
            String level = determineLevel(stuckCount, averageDays);
            if (level == null) {
                continue;
            }

            KpiBottleneckDto dto = new KpiBottleneckDto();
            dto.setActivityName(group.activityName);
            dto.setResponsible(group.responsible);
            dto.setPendingCount(group.pendingCount);
            dto.setInProgressCount(group.inProgressCount);
            dto.setAverageTime(formatDays(averageDays));
            dto.setLevel(level);
            dto.setObservation(buildObservation(group, level));
            bottlenecks.add(dto);
        }

        bottlenecks.sort(Comparator
                .comparingInt((KpiBottleneckDto dto) -> levelPriority(dto.getLevel()))
                .thenComparing(
                        (KpiBottleneckDto dto) -> dto.getPendingCount() + dto.getInProgressCount(),
                        Comparator.reverseOrder()
                )
                .thenComparing(KpiBottleneckDto::getActivityName));

        return bottlenecks;
    }

    private void accumulateTramiteGroup(Tramite tramite, Map<String, ActivityGroup> groups, LocalDateTime now) {
        String activity = normalizeActivity(tramite.getCurrentActivity());
        String responsible = normalizeResponsible(tramite.getResponsible());
        ActivityGroup group = getOrCreateGroup(groups, activity, responsible);

        if (isIniciado(tramite.getStatus())) {
            group.pendingCount++;
        } else if (isEnProceso(tramite.getStatus())) {
            group.inProgressCount++;
        } else {
            group.pendingCount++;
        }

        addTimeSample(group, resolveAttentionStart(tramite), now);
    }

    private void accumulateTaskGroups(Tramite tramite, Map<String, ActivityGroup> groups, LocalDateTime now) {
        for (TramiteTask task : tramite.getTasks()) {
            if (task.getStatus() == null) {
                continue;
            }
            String status = task.getStatus().trim().toUpperCase(Locale.ROOT);
            if (!"PENDIENTE".equals(status) && !"EN_CURSO".equals(status)) {
                continue;
            }

            String activity = normalizeActivity(task.getName());
            String responsible = normalizeResponsible(
                    task.getResponsible() != null ? task.getResponsible() : tramite.getResponsible()
            );
            ActivityGroup group = getOrCreateGroup(groups, activity, responsible);

            if ("PENDIENTE".equals(status)) {
                group.pendingCount++;
            } else {
                group.inProgressCount++;
            }

            LocalDateTime start = task.getStartedAt() != null ? task.getStartedAt() : tramite.getCreatedAt();
            if (start == null) {
                start = tramite.getUpdatedAt();
            }
            addTimeSample(group, start, now);
        }
    }

    private ActivityGroup getOrCreateGroup(Map<String, ActivityGroup> groups, String activity, String responsible) {
        String key = activity + "||" + responsible;
        return groups.computeIfAbsent(key, ignored -> {
            ActivityGroup group = new ActivityGroup();
            group.activityName = activity;
            group.responsible = responsible;
            return group;
        });
    }

    private void addTimeSample(ActivityGroup group, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return;
        }
        group.totalDays += calculateDaysBetween(start, end);
        group.samples++;
    }

    private double calculateAverageAttentionDays(List<Tramite> tramites) {
        if (tramites.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        double totalDays = 0;
        int count = 0;

        for (Tramite tramite : tramites) {
            if (tramite.getCreatedAt() == null) {
                continue;
            }
            LocalDateTime end = resolveAttentionEnd(tramite, now);
            totalDays += calculateDaysBetween(tramite.getCreatedAt(), end);
            count++;
        }

        return count == 0 ? 0 : totalDays / count;
    }

    private LocalDateTime resolveAttentionEnd(Tramite tramite, LocalDateTime now) {
        if (isFinalizado(tramite.getStatus()) || isCancelado(tramite.getStatus())) {
            return tramite.getUpdatedAt() != null ? tramite.getUpdatedAt() : now;
        }
        return tramite.getUpdatedAt() != null ? tramite.getUpdatedAt() : now;
    }

    private LocalDateTime resolveAttentionStart(Tramite tramite) {
        if (tramite.getUpdatedAt() != null) {
            return tramite.getUpdatedAt();
        }
        return tramite.getCreatedAt();
    }

    private ActivityMetric findActivityWithHighestDelay(List<Tramite> tramites) {
        Map<String, ActivityMetric> metrics = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (Tramite tramite : tramites) {
            if (isFinalizado(tramite.getStatus()) || isCancelado(tramite.getStatus())) {
                continue;
            }

            String activity = normalizeActivity(tramite.getCurrentActivity());
            ActivityMetric metric = metrics.computeIfAbsent(activity, ActivityMetric::new);
            metric.stuckCount++;

            LocalDateTime start = resolveAttentionStart(tramite);
            if (start != null) {
                metric.totalDays += calculateDaysBetween(start, now);
                metric.samples++;
            }
        }

        return metrics.values().stream()
                .max(Comparator
                        .comparingDouble(ActivityMetric::averageDays)
                        .thenComparingInt(metric -> metric.stuckCount))
                .orElse(null);
    }

    private String findResponsibleWithHighestLoad(List<Tramite> tramites) {
        Map<String, Long> loadByResponsible = new HashMap<>();

        for (Tramite tramite : tramites) {
            if (isFinalizado(tramite.getStatus()) || isCancelado(tramite.getStatus())) {
                continue;
            }

            if (tramite.getTasks() != null && !tramite.getTasks().isEmpty()) {
                for (TramiteTask task : tramite.getTasks()) {
                    if (task.getStatus() == null) {
                        continue;
                    }
                    String status = task.getStatus().trim().toUpperCase(Locale.ROOT);
                    if ("PENDIENTE".equals(status) || "EN_CURSO".equals(status)) {
                        String responsible = normalizeResponsible(
                                task.getResponsible() != null ? task.getResponsible() : tramite.getResponsible()
                        );
                        loadByResponsible.merge(responsible, 1L, Long::sum);
                    }
                }
            } else {
                String responsible = normalizeResponsible(tramite.getResponsible());
                loadByResponsible.merge(responsible, 1L, Long::sum);
            }
        }

        return loadByResponsible.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Sin datos");
    }

    private String determineLevel(long stuckCount, double averageDays) {
        if (stuckCount >= 3 || averageDays > 2.0) {
            return "Alto";
        }
        if (stuckCount >= 2 || averageDays >= 1.0) {
            return "Medio";
        }
        if (stuckCount >= 1) {
            return "Bajo";
        }
        return null;
    }

    private int levelPriority(String level) {
        return switch (level) {
            case "Alto" -> 0;
            case "Medio" -> 1;
            case "Bajo" -> 2;
            default -> 3;
        };
    }

    private String buildObservation(ActivityGroup group, String level) {
        long stuckCount = group.pendingCount + group.inProgressCount;
        if ("Alto".equals(level)) {
            return "Existen varios trámites acumulados en esta actividad.";
        }
        if ("Medio".equals(level)) {
            return "La actividad presenta demora moderada con " + stuckCount + " trámite(s) detenido(s).";
        }
        return "Actividad con ligera acumulación de trámites.";
    }

    private boolean isIniciado(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "INICIADO".equals(normalized) || "CREATED".equals(normalized);
    }

    private boolean isEnProceso(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "EN_PROCESO".equals(normalized) || "IN_PROGRESS".equals(normalized);
    }

    private boolean isFinalizado(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "COMPLETADO".equals(normalized)
                || "FINALIZADO".equals(normalized)
                || "COMPLETED".equals(normalized)
                || "DONE".equals(normalized);
    }

    private boolean isCancelado(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "CANCELADO".equals(normalized) || "CANCELLED".equals(normalized);
    }

    private String normalizeActivity(String activity) {
        if (activity == null || activity.isBlank()) {
            return "Sin actividad";
        }
        return activity.trim();
    }

    private String normalizeResponsible(String responsible) {
        if (responsible == null || responsible.isBlank() || "—".equals(responsible.trim())) {
            return "Sin asignar";
        }
        return responsible.trim();
    }

    private double calculateDaysBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        return Duration.between(start, end).toMinutes() / (60.0 * 24.0);
    }

    private String formatDays(double days) {
        if (days <= 0) {
            return "0 días";
        }
        if (days < 1) {
            int hours = Math.max(1, (int) Math.round(days * 24));
            return hours + (hours == 1 ? " hora" : " horas");
        }
        if (Math.abs(days - Math.rint(days)) < 0.05) {
            int wholeDays = (int) Math.round(days);
            return wholeDays + (wholeDays == 1 ? " día" : " días");
        }
        return String.format(Locale.ROOT, "%.1f días", days);
    }

    private void saveKpiReport(String reportType, Map<String, Object> metrics) {
        KpiReport report = new KpiReport();
        report.setPolicyId("ALL");
        report.setReportType(reportType);
        report.setMetrics(metrics);
        report.setGeneratedAt(LocalDateTime.now());
        kpiReportRepository.save(report);
    }

    private Map<String, Object> buildSummaryMetrics(KpiSummaryResponse summary, int bottleneckCount) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalTramites", summary.getTotalTramites());
        metrics.put("iniciados", summary.getIniciados());
        metrics.put("enProceso", summary.getEnProceso());
        metrics.put("finalizados", summary.getFinalizados());
        metrics.put("cancelados", summary.getCancelados());
        metrics.put("tiempoPromedio", summary.getTiempoPromedio());
        metrics.put("actividadMayorDemora", summary.getActividadMayorDemora());
        metrics.put("responsableMayorCarga", summary.getResponsableMayorCarga());
        metrics.put("cuelloDeBotellaPrincipal", summary.getCuelloDeBotellaPrincipal());
        metrics.put("bottleneckCount", bottleneckCount);
        return metrics;
    }

    private static final class ActivityGroup {
        private String activityName;
        private String responsible;
        private long pendingCount;
        private long inProgressCount;
        private double totalDays;
        private int samples;
    }

    private static final class ActivityMetric {
        private final String activityName;
        private int stuckCount;
        private double totalDays;
        private int samples;

        private ActivityMetric(String activityName) {
            this.activityName = activityName;
        }

        private double averageDays() {
            return samples == 0 ? 0 : totalDays / samples;
        }
    }
}
