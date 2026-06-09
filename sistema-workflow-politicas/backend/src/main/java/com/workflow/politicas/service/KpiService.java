package com.workflow.politicas.service;

import com.workflow.politicas.dto.*;
import com.workflow.politicas.model.Department;
import com.workflow.politicas.model.KpiReport;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import com.workflow.politicas.model.User;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.repository.DepartmentRepository;
import com.workflow.politicas.repository.KpiReportRepository;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * KPI y cuellos de botella sobre runtime oficial: Tramite, TramiteTask, Trace, WorkflowActivity.
 */
@Service
public class KpiService {

    private static final String TASK_PENDIENTE = "PENDIENTE";
    private static final String TASK_EN_CURSO = "EN_CURSO";
    private static final String TASK_COMPLETADA = "COMPLETADA";
    private static final double DEFAULT_SLA_HOURS = 48.0;

    private final TramiteRepository tramiteRepository;
    private final KpiReportRepository kpiReportRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkflowActivityRepository workflowActivityRepository;

    public KpiService(
            TramiteRepository tramiteRepository,
            KpiReportRepository kpiReportRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            WorkflowActivityRepository workflowActivityRepository
    ) {
        this.tramiteRepository = tramiteRepository;
        this.kpiReportRepository = kpiReportRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.workflowActivityRepository = workflowActivityRepository;
    }

    public KpiDashboardFullResponse getDashboard(KpiFilter filter) {
        KpiFilter effective = filter != null ? filter : new KpiFilter();
        List<Tramite> tramites = filterTramites(loadTramitesForKpi(effective), effective);
        KpiContext ctx = buildContext(tramites);

        KpiDashboardFullResponse response = new KpiDashboardFullResponse();
        response.setGeneratedAt(LocalDateTime.now());
        response.setSufficientData(!tramites.isEmpty());
        response.setMessage(tramites.isEmpty()
                ? "No hay trámites en el periodo seleccionado. Cree trámites y complete actividades para generar indicadores."
                : null);

        response.setSummary(buildSummary(ctx));
        response.setTareasPendientes(ctx.taskPending);
        response.setTareasEnProceso(ctx.taskInProgress);
        response.setTareasCompletadas(ctx.taskCompleted);
        response.setTramitesActivos(ctx.tramitesActivos);
        response.setTramitesConError(ctx.tramitesConError);
        response.setSlowActivities(buildSlowActivities(ctx));
        response.setEmployeeLoad(buildEmployeeLoad(ctx));
        response.setDepartmentLoad(buildDepartmentLoad(ctx));
        response.setBottlenecks(calculateBottlenecks(ctx));

        saveKpiReport("DASHBOARD_F5", Map.of(
                "tramites", tramites.size(),
                "bottlenecks", response.getBottlenecks().size()
        ));
        return response;
    }

    public KpiSummaryResponse getSummary(KpiFilter filter) {
        return getDashboard(filter != null ? filter : new KpiFilter()).getSummary();
    }

    public List<KpiBottleneckDto> getBottlenecks(KpiFilter filter) {
        return getDashboard(filter != null ? filter : new KpiFilter()).getBottlenecks();
    }

    private KpiContext buildContext(List<Tramite> tramites) {
        KpiContext ctx = new KpiContext();
        ctx.tramites = tramites;
        ctx.now = LocalDateTime.now();
        ctx.usersByUsername = userRepository.findAll().stream()
                .collect(Collectors.toMap(
                        u -> u.getUsername().toLowerCase(Locale.ROOT),
                        u -> u,
                        (a, b) -> a
                ));
        ctx.usersByFullName = userRepository.findAll().stream()
                .filter(u -> u.getFullName() != null && !u.getFullName().isBlank())
                .collect(Collectors.toMap(
                        u -> normalizeKey(u.getFullName()),
                        u -> u,
                        (a, b) -> a
                ));
        ctx.usersById = userRepository.findAll().stream()
                .filter(u -> u.getId() != null && !u.getId().isBlank())
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        ctx.roleNames = roleRepository.findAll().stream()
                .map(role -> role.getName())
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());
        ctx.departmentsById = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, d -> d, (a, b) -> a));
        ctx.activitiesById = loadActivitiesForTramites(tramites);

        for (Tramite tramite : tramites) {
            if (hasWorkflowError(tramite)) {
                ctx.tramitesConError++;
            }
            if (isActivo(tramite.getStatus())) {
                ctx.tramitesActivos++;
            }

            if (tramite.getTasks() == null || tramite.getTasks().isEmpty()) {
                continue;
            }

            for (TramiteTask task : tramite.getTasks()) {
                String status = normalizeTaskStatus(task.getStatus());
                if (TASK_PENDIENTE.equals(status)) {
                    ctx.taskPending++;
                } else if (TASK_EN_CURSO.equals(status)) {
                    ctx.taskInProgress++;
                } else if (TASK_COMPLETADA.equals(status)) {
                    ctx.taskCompleted++;
                } else {
                    continue;
                }

                String activityKey = activityKey(task, tramite);
                ActivityAccumulator acc = ctx.activityMetrics.computeIfAbsent(activityKey, k -> {
                    ActivityAccumulator a = new ActivityAccumulator();
                    a.workflowActivityId = task.getWorkflowActivityId();
                    a.activityName = normalizeActivity(task.getName());
                    a.policyId = tramite.getPolicyId();
                    a.policyName = tramite.getPolicyName();
                    return a;
                });

                if (TASK_PENDIENTE.equals(status)) {
                    acc.pendingCount++;
                } else if (TASK_EN_CURSO.equals(status)) {
                    acc.inProgressCount++;
                } else {
                    acc.completedCount++;
                }

                LocalDateTime waitStart = taskWaitStart(task, tramite);
                if (TASK_COMPLETADA.equals(status)) {
                    LocalDateTime end = task.getCompletedAt();
                    LocalDateTime start = taskHandlingStart(task);
                    if (start != null && end != null) {
                        acc.completedDurationHours += hoursBetween(start, end);
                        acc.completedSamples++;
                    }
                } else if (waitStart != null) {
                    double hours = hoursBetween(waitStart, ctx.now);
                    acc.activeWaitHours += hours;
                    acc.activeWaitSamples++;
                    if (isOverdue(task, hours, ctx.activitiesById)) {
                        acc.overdueCount++;
                    }
                }

                EmployeeIdentity employee = resolveEmployeeIdentity(task, tramite, ctx);
                String deptKey = resolveDepartmentKey(task, tramite, ctx);
                String deptDisplay = ctx.departmentsById.values().stream()
                        .filter(d -> deptKey.equals(d.getName()))
                        .map(Department::getName)
                        .findFirst()
                        .orElse(deptKey);
                if (employee.rankable()) {
                    accumulateLoad(ctx.employeeLoads, employee.key(), employee.displayName(), deptDisplay, status, task, tramite, ctx.now);
                    acc.responsibleCounts.merge(employee.displayName(), 1L, Long::sum);
                }
                accumulateLoad(ctx.departmentLoads, deptKey, deptKey, deptDisplay, status, task, tramite, ctx.now);
                acc.departmentCounts.merge(deptKey, 1L, Long::sum);
            }
        }

        return ctx;
    }

    private KpiSummaryResponse buildSummary(KpiContext ctx) {
        List<Tramite> tramites = ctx.tramites;
        List<KpiBottleneckDto> bottlenecks = calculateBottlenecks(ctx);

        KpiSummaryResponse summary = new KpiSummaryResponse();
        summary.setTotalTramites(tramites.size());
        summary.setIniciados(tramites.stream().filter(t -> isIniciado(t.getStatus())).count());
        summary.setEnProceso(tramites.stream().filter(t -> isEnProceso(t.getStatus())).count());
        summary.setFinalizados(tramites.stream().filter(t -> isFinalizado(t.getStatus())).count());
        summary.setCancelados(tramites.stream().filter(t -> isCancelado(t.getStatus())).count());
        summary.setTramitesActivos(ctx.tramitesActivos);
        summary.setTramitesConError(ctx.tramitesConError);
        summary.setTareasPendientes(ctx.taskPending);
        summary.setTareasEnProceso(ctx.taskInProgress);
        summary.setTareasCompletadas(ctx.taskCompleted);
        summary.setTiempoPromedio(formatDurationHours(averageTramiteDurationHours(tramites, ctx.now)));

        double avgActivityHours = ctx.activityMetrics.values().stream()
                .filter(a -> a.completedSamples > 0)
                .mapToDouble(a -> a.completedDurationHours / a.completedSamples)
                .average()
                .orElse(0);
        summary.setTiempoPromedioActividad(formatDurationHours(avgActivityHours));

        ActivityAccumulator topSlow = ctx.activityMetrics.values().stream()
                .max(Comparator
                        .comparingDouble(ActivityAccumulator::activeWaitScore)
                        .thenComparingLong(a -> a.pendingCount + a.inProgressCount))
                .orElse(null);
        summary.setActividadMayorDemora(topSlow != null ? topSlow.activityName : "Sin datos");

        KpiLoadMetricDto topEmployee = buildEmployeeLoad(ctx).stream().findFirst().orElse(null);
        summary.setResponsableMayorCarga(topEmployee != null ? topEmployee.getDisplayName() : "Sin datos");

        summary.setCuelloDeBotellaPrincipal(
                bottlenecks.isEmpty()
                        ? "Sin datos"
                        : bottlenecks.get(0).getActivityName() + " (" + bottlenecks.get(0).getLevel() + ")"
        );
        return summary;
    }

    private List<KpiActivityMetricDto> buildSlowActivities(KpiContext ctx) {
        return ctx.activityMetrics.values().stream()
                .filter(a -> a.pendingCount + a.inProgressCount + a.completedCount > 0)
                .sorted(Comparator
                        .comparingDouble(ActivityAccumulator::sortScore).reversed()
                        .thenComparing(a -> a.activityName))
                .limit(20)
                .map(a -> {
                    KpiActivityMetricDto dto = new KpiActivityMetricDto();
                    dto.setWorkflowActivityId(a.workflowActivityId);
                    dto.setActivityName(a.activityName);
                    dto.setPolicyId(a.policyId);
                    dto.setPolicyName(a.policyName);
                    dto.setPendingCount(a.pendingCount);
                    dto.setInProgressCount(a.inProgressCount);
                    dto.setCompletedCount(a.completedCount);
                    dto.setOverdueCount(a.overdueCount);
                    dto.setAverageDuration(a.completedSamples > 0
                            ? formatDurationHours(a.completedDurationHours / a.completedSamples)
                            : "—");
                    dto.setAverageActiveWait(a.activeWaitSamples > 0
                            ? formatDurationHours(a.activeWaitHours / a.activeWaitSamples)
                            : "—");
                    return dto;
                })
                .toList();
    }

    private List<KpiLoadMetricDto> buildEmployeeLoad(KpiContext ctx) {
        return ctx.employeeLoads.values().stream()
                .sorted(Comparator.comparingLong(LoadAccumulator::totalActive).reversed())
                .map(load -> toLoadDto(load, ctx))
                .filter(load -> EmployeeDisplayNameResolver.isRankableMetric(
                        load, ctx.roleNames, ctx.usersByUsername))
                .toList();
    }

    private List<KpiLoadMetricDto> buildDepartmentLoad(KpiContext ctx) {
        return ctx.departmentLoads.values().stream()
                .sorted(Comparator.comparingLong(LoadAccumulator::totalActive).reversed())
                .map(load -> toLoadDto(load, ctx))
                .toList();
    }

    private KpiLoadMetricDto toLoadDto(LoadAccumulator load, KpiContext ctx) {
        KpiLoadMetricDto dto = new KpiLoadMetricDto();
        dto.setKey(load.key);
        String resolvedDisplay = EmployeeDisplayNameResolver.resolvePersonLabel(
                provisionalMetric(load.key, load.displayName),
                ctx.usersByUsername,
                ctx.roleNames
        );
        dto.setDisplayName(resolvedDisplay != null ? resolvedDisplay : load.displayName);
        dto.setDepartmentName(load.departmentName);
        dto.setPendingCount(load.pendingCount);
        dto.setInProgressCount(load.inProgressCount);
        dto.setCompletedCount(load.completedCount);
        dto.setTotalActive(load.pendingCount + load.inProgressCount);
        dto.setAverageHandlingTime(load.handlingSamples > 0
                ? formatDurationHours(load.handlingHours / load.handlingSamples)
                : "—");
        return dto;
    }

    private List<KpiBottleneckDto> calculateBottlenecks(KpiContext ctx) {
        List<KpiBottleneckDto> bottlenecks = new ArrayList<>();

        for (ActivityAccumulator group : ctx.activityMetrics.values()) {
            long stuck = group.pendingCount + group.inProgressCount;
            if (stuck <= 0 && group.overdueCount <= 0) {
                continue;
            }

            double avgWaitDays = group.activeWaitSamples == 0
                    ? 0
                    : (group.activeWaitHours / group.activeWaitSamples) / 24.0;
            String level = determineLevel(stuck, avgWaitDays, group.overdueCount);
            if (level == null) {
                continue;
            }

            KpiBottleneckDto dto = new KpiBottleneckDto();
            dto.setWorkflowActivityId(group.workflowActivityId);
            dto.setActivityName(group.activityName);
            dto.setPolicyId(group.policyId);
            dto.setPolicyName(group.policyName);
            dto.setResponsible(resolveDominantResponsible(group));
            dto.setDepartmentName(resolveDominantDepartment(group, ctx));
            dto.setPendingCount(group.pendingCount);
            dto.setInProgressCount(group.inProgressCount);
            dto.setOverdueCount(group.overdueCount);
            dto.setAverageTime(group.activeWaitSamples > 0
                    ? formatDurationHours(group.activeWaitHours / group.activeWaitSamples)
                    : "—");
            dto.setLevel(level);
            dto.setObservation(buildObservation(group, level));
            bottlenecks.add(dto);
        }

        bottlenecks.sort(Comparator
                .comparingInt((KpiBottleneckDto dto) -> levelPriority(dto.getLevel()))
                .thenComparingLong(dto -> dto.getOverdueCount() + dto.getPendingCount() + dto.getInProgressCount())
                .reversed()
                .thenComparing(KpiBottleneckDto::getActivityName));

        return bottlenecks;
    }

    private void accumulateLoad(
            Map<String, LoadAccumulator> loads,
            String key,
            String displayName,
            String departmentName,
            String status,
            TramiteTask task,
            Tramite tramite,
            LocalDateTime now
    ) {
        LoadAccumulator load = loads.computeIfAbsent(key, k -> {
            LoadAccumulator l = new LoadAccumulator();
            l.key = key;
            l.displayName = displayName;
            l.departmentName = departmentName;
            return l;
        });

        if (TASK_PENDIENTE.equals(status)) {
            load.pendingCount++;
        } else if (TASK_EN_CURSO.equals(status)) {
            load.inProgressCount++;
        } else if (TASK_COMPLETADA.equals(status)) {
            load.completedCount++;
            LocalDateTime start = taskHandlingStart(task);
            LocalDateTime end = task.getCompletedAt();
            if (start != null && end != null) {
                load.handlingHours += hoursBetween(start, end);
                load.handlingSamples++;
            }
        }
    }

    private List<Tramite> loadTramitesForKpi(KpiFilter filter) {
        if (filter.getPolicyId() != null && !filter.getPolicyId().isBlank()) {
            return tramiteRepository.findByPolicyId(filter.getPolicyId().trim());
        }
        LocalDateTime from = filter.getFromDate() != null ? filter.getFromDate().atStartOfDay() : null;
        LocalDateTime to = filter.getToDate() != null ? filter.getToDate().atTime(23, 59, 59) : null;
        if (from != null && to != null) {
            return tramiteRepository.findByCreatedAtBetween(from, to);
        }
        if (from != null) {
            return tramiteRepository.findByCreatedAtGreaterThanEqual(from);
        }
        return tramiteRepository.findByStatusNot("CANCELADO");
    }

    private Map<String, WorkflowActivity> loadActivitiesForTramites(List<Tramite> tramites) {
        Set<String> policyIds = tramites.stream()
                .map(Tramite::getPolicyId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .collect(Collectors.toSet());
        Map<String, WorkflowActivity> map = new HashMap<>();
        for (String policyId : policyIds) {
            for (WorkflowActivity activity : workflowActivityRepository.findByPolicyId(policyId)) {
                if (activity.getId() != null) {
                    map.putIfAbsent(activity.getId(), activity);
                }
            }
        }
        return map;
    }

    private List<Tramite> filterTramites(List<Tramite> all, KpiFilter filter) {
        return all.stream()
                .filter(t -> matchesPolicy(t, filter.getPolicyId()))
                .filter(t -> matchesStatus(t, filter.getStatus()))
                .filter(t -> matchesDateRange(t, filter.getFromDate(), filter.getToDate()))
                .toList();
    }

    private boolean matchesPolicy(Tramite tramite, String policyId) {
        if (policyId == null || policyId.isBlank()) {
            return true;
        }
        return policyId.equals(tramite.getPolicyId());
    }

    private boolean matchesStatus(Tramite tramite, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "INICIADO" -> isIniciado(tramite.getStatus());
            case "EN_PROCESO" -> isEnProceso(tramite.getStatus());
            case "ACTIVO" -> isActivo(tramite.getStatus());
            case "FINALIZADO", "COMPLETADO" -> isFinalizado(tramite.getStatus());
            case "CANCELADO" -> isCancelado(tramite.getStatus());
            case "ERROR" -> hasWorkflowError(tramite);
            default -> true;
        };
    }

    private boolean matchesDateRange(Tramite tramite, LocalDate from, LocalDate to) {
        if (tramite.getCreatedAt() == null) {
            return from == null && to == null;
        }
        LocalDate created = tramite.getCreatedAt().toLocalDate();
        if (from != null && created.isBefore(from)) {
            return false;
        }
        if (to != null && created.isAfter(to)) {
            return false;
        }
        return true;
    }

    private double averageTramiteDurationHours(List<Tramite> tramites, LocalDateTime now) {
        if (tramites.isEmpty()) {
            return 0;
        }
        double total = 0;
        int count = 0;
        for (Tramite tramite : tramites) {
            if (tramite.getCreatedAt() == null) {
                continue;
            }
            LocalDateTime end = isFinalizado(tramite.getStatus()) || isCancelado(tramite.getStatus())
                    ? (tramite.getUpdatedAt() != null ? tramite.getUpdatedAt() : now)
                    : now;
            total += hoursBetween(tramite.getCreatedAt(), end);
            count++;
        }
        return count == 0 ? 0 : total / count;
    }

    private boolean isOverdue(TramiteTask task, double elapsedHours, Map<String, WorkflowActivity> activitiesById) {
        double slaHours = DEFAULT_SLA_HOURS;
        if (task.getWorkflowActivityId() != null) {
            WorkflowActivity activity = activitiesById.get(task.getWorkflowActivityId());
            if (activity != null && activity.getEstimatedTimeHours() != null && activity.getEstimatedTimeHours() > 0) {
                slaHours = activity.getEstimatedTimeHours();
            }
        }
        return elapsedHours > slaHours;
    }

    private LocalDateTime taskWaitStart(TramiteTask task, Tramite tramite) {
        if (task.getStartedAt() != null) {
            return task.getStartedAt();
        }
        return tramite.getCreatedAt();
    }

    private LocalDateTime taskHandlingStart(TramiteTask task) {
        if (task.getTakenAt() != null) {
            return task.getTakenAt();
        }
        if (task.getStartedAt() != null) {
            return task.getStartedAt();
        }
        return null;
    }

    private String activityKey(TramiteTask task, Tramite tramite) {
        if (task.getWorkflowActivityId() != null && !task.getWorkflowActivityId().isBlank()) {
            return task.getWorkflowActivityId();
        }
        return tramite.getPolicyId() + "||" + normalizeActivity(task.getName());
    }

    private record EmployeeIdentity(String key, String displayName, boolean rankable) {
        static EmployeeIdentity fromUser(User user) {
            String displayName = EmployeeDisplayNameResolver.fromUser(user);
            return new EmployeeIdentity(user.getUsername(), displayName, true);
        }

        static EmployeeIdentity unrankable() {
            return new EmployeeIdentity("", "", false);
        }
    }

    private EmployeeIdentity resolveEmployeeIdentity(TramiteTask task, Tramite tramite, KpiContext ctx) {
        if (task.getTakenBy() != null && !task.getTakenBy().isBlank()) {
            String username = task.getTakenBy().trim();
            User user = ctx.usersByUsername.get(username.toLowerCase(Locale.ROOT));
            if (user != null) {
                return EmployeeIdentity.fromUser(user);
            }
            if (!EmployeeDisplayNameResolver.isRoleLabel(username, ctx.roleNames)) {
                return new EmployeeIdentity(username, username, true);
            }
            return EmployeeIdentity.unrankable();
        }

        if (task.getWorkflowActivityId() != null) {
            WorkflowActivity activity = ctx.activitiesById.get(task.getWorkflowActivityId());
            if (activity != null && "USER".equalsIgnoreCase(activity.getResponsibleType())) {
                User user = resolveUserReference(activity.getResponsibleId(), activity.getResponsibleName(), ctx);
                if (user != null) {
                    return EmployeeIdentity.fromUser(user);
                }
            }
        }

        String responsible = task.getResponsible() != null ? task.getResponsible() : tramite.getResponsible();
        if (responsible != null && !responsible.isBlank()) {
            User byUsername = ctx.usersByUsername.get(responsible.trim().toLowerCase(Locale.ROOT));
            if (byUsername != null) {
                return EmployeeIdentity.fromUser(byUsername);
            }

            User byName = ctx.usersByFullName.get(normalizeKey(responsible));
            if (byName != null) {
                return EmployeeIdentity.fromUser(byName);
            }

            if (EmployeeDisplayNameResolver.isRoleLabel(responsible, ctx.roleNames)) {
                return EmployeeIdentity.unrankable();
            }

            String normalized = normalizeResponsible(responsible);
            if (EmployeeDisplayNameResolver.isRoleLabel(normalized, ctx.roleNames)) {
                return EmployeeIdentity.unrankable();
            }
            return new EmployeeIdentity(normalized, normalized, true);
        }

        return EmployeeIdentity.unrankable();
    }

    private User resolveUserReference(String id, String name, KpiContext ctx) {
        if (id != null && !id.isBlank()) {
            String trimmed = id.trim();
            User byUsername = ctx.usersByUsername.get(trimmed.toLowerCase(Locale.ROOT));
            if (byUsername != null) {
                return byUsername;
            }
            User byId = ctx.usersById.get(trimmed);
            if (byId != null) {
                return byId;
            }
        }
        if (name != null && !name.isBlank()) {
            return ctx.usersByFullName.get(normalizeKey(name));
        }
        return null;
    }

    private static KpiLoadMetricDto provisionalMetric(String key, String displayName) {
        KpiLoadMetricDto dto = new KpiLoadMetricDto();
        dto.setKey(key);
        dto.setDisplayName(displayName);
        return dto;
    }

    private String resolveDepartmentKey(TramiteTask task, Tramite tramite, KpiContext ctx) {
        if (task.getTakenBy() != null && !task.getTakenBy().isBlank()) {
            User user = ctx.usersByUsername.get(task.getTakenBy().trim().toLowerCase(Locale.ROOT));
            if (user != null && user.getDepartmentId() != null) {
                Department dept = ctx.departmentsById.get(user.getDepartmentId());
                if (dept != null) {
                    return dept.getName();
                }
            }
        }
        if (task.getWorkflowActivityId() != null) {
            WorkflowActivity wa = ctx.activitiesById.get(task.getWorkflowActivityId());
            if (wa != null && "DEPARTMENT".equalsIgnoreCase(wa.getResponsibleType()) && wa.getResponsibleName() != null) {
                return wa.getResponsibleName().trim();
            }
        }
        String responsible = task.getResponsible() != null ? task.getResponsible() : tramite.getResponsible();
        for (Department dept : ctx.departmentsById.values()) {
            if (responsible != null && normalizeKey(responsible).contains(normalizeKey(dept.getName()))) {
                return dept.getName();
            }
        }
        return "Sin departamento";
    }

    private String resolveDominantResponsible(ActivityAccumulator group) {
        return group.responsibleCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Sin asignar");
    }

    private String resolveDominantDepartment(ActivityAccumulator group, KpiContext ctx) {
        if (group.workflowActivityId != null) {
            WorkflowActivity wa = ctx.activitiesById.get(group.workflowActivityId);
            if (wa != null && "DEPARTMENT".equalsIgnoreCase(wa.getResponsibleType()) && wa.getResponsibleName() != null) {
                return wa.getResponsibleName().trim();
            }
        }
        return group.departmentCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Sin departamento");
    }

    private String determineLevel(long stuckCount, double averageDays, long overdueCount) {
        if (overdueCount >= 2 || stuckCount >= 4 || averageDays > 2.0) {
            return "Alto";
        }
        if (overdueCount >= 1 || stuckCount >= 2 || averageDays >= 1.0) {
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

    private String buildObservation(ActivityAccumulator group, String level) {
        long stuck = group.pendingCount + group.inProgressCount;
        if ("Alto".equals(level)) {
            return "Mayor tiempo de espera y acumulación (" + stuck + " tarea(s) activa(s), "
                    + group.overdueCount + " demorada(s)).";
        }
        if ("Medio".equals(level)) {
            return "Demora moderada con " + stuck + " tarea(s) en bandeja.";
        }
        return "Ligera acumulación en esta actividad.";
    }

    private boolean hasWorkflowError(Tramite tramite) {
        return tramite.getWorkflowError() != null && !tramite.getWorkflowError().isBlank();
    }

    private boolean isActivo(String status) {
        return isIniciado(status) || isEnProceso(status);
    }

    private boolean isIniciado(String status) {
        if (status == null) return false;
        String n = status.trim().toUpperCase(Locale.ROOT);
        return "INICIADO".equals(n) || "CREATED".equals(n);
    }

    private boolean isEnProceso(String status) {
        if (status == null) return false;
        String n = status.trim().toUpperCase(Locale.ROOT);
        return "EN_PROCESO".equals(n) || "IN_PROGRESS".equals(n);
    }

    private boolean isFinalizado(String status) {
        if (status == null) return false;
        String n = status.trim().toUpperCase(Locale.ROOT);
        return "COMPLETADO".equals(n) || "FINALIZADO".equals(n) || "COMPLETED".equals(n) || "DONE".equals(n);
    }

    private boolean isCancelado(String status) {
        if (status == null) return false;
        String n = status.trim().toUpperCase(Locale.ROOT);
        return "CANCELADO".equals(n) || "CANCELLED".equals(n);
    }

    private String normalizeTaskStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeActivity(String activity) {
        return activity == null || activity.isBlank() ? "Sin actividad" : activity.trim();
    }

    private String normalizeResponsible(String responsible) {
        if (responsible == null || responsible.isBlank() || "—".equals(responsible.trim())) {
            return "Sin asignar";
        }
        return responsible.trim();
    }

    private String normalizeKey(String value) {
        return value.trim().toUpperCase(Locale.ROOT)
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace("Ñ", "N");
    }

    private double hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        return Duration.between(start, end).toMinutes() / 60.0;
    }

    private String formatDurationHours(double hours) {
        if (hours <= 0) {
            return "0 h";
        }
        if (hours < 1) {
            int minutes = Math.max(1, (int) Math.round(hours * 60));
            return minutes + " min";
        }
        if (hours < 24) {
            return String.format(Locale.ROOT, "%.1f h", hours);
        }
        double days = hours / 24.0;
        if (Math.abs(days - Math.rint(days)) < 0.05) {
            int whole = (int) Math.round(days);
            return whole + (whole == 1 ? " día" : " días");
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

    private static final class KpiContext {
        List<Tramite> tramites = List.of();
        LocalDateTime now;
        long taskPending;
        long taskInProgress;
        long taskCompleted;
        long tramitesActivos;
        long tramitesConError;
        Map<String, User> usersByUsername = Map.of();
        Map<String, User> usersByFullName = Map.of();
        Map<String, User> usersById = Map.of();
        Set<String> roleNames = Set.of();
        Map<String, Department> departmentsById = Map.of();
        Map<String, WorkflowActivity> activitiesById = Map.of();
        Map<String, ActivityAccumulator> activityMetrics = new HashMap<>();
        Map<String, LoadAccumulator> employeeLoads = new HashMap<>();
        Map<String, LoadAccumulator> departmentLoads = new HashMap<>();
    }

    private static final class ActivityAccumulator {
        String workflowActivityId;
        String activityName;
        String policyId;
        String policyName;
        long pendingCount;
        long inProgressCount;
        long completedCount;
        long overdueCount;
        Map<String, Long> responsibleCounts = new HashMap<>();
        Map<String, Long> departmentCounts = new HashMap<>();
        double completedDurationHours;
        int completedSamples;
        double activeWaitHours;
        int activeWaitSamples;

        double activeWaitScore() {
            double avgWait = activeWaitSamples == 0 ? 0 : activeWaitHours / activeWaitSamples;
            return avgWait + (pendingCount + inProgressCount) * 8.0 + overdueCount * 12.0;
        }

        double sortScore() {
            double completedAvg = completedSamples == 0 ? 0 : completedDurationHours / completedSamples;
            double waitAvg = activeWaitSamples == 0 ? 0 : activeWaitHours / activeWaitSamples;
            return Math.max(completedAvg, waitAvg) + (pendingCount + inProgressCount) * 4.0 + overdueCount * 6.0;
        }
    }

    private static final class LoadAccumulator {
        String key;
        String displayName;
        String departmentName;
        long pendingCount;
        long inProgressCount;
        long completedCount;
        double handlingHours;
        int handlingSamples;

        long totalActive() {
            return pendingCount + inProgressCount;
        }
    }
}
