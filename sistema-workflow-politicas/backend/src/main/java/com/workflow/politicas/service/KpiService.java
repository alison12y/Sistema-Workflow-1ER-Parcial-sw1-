package com.workflow.politicas.service;

import com.workflow.politicas.dto.*;
import com.workflow.politicas.model.Activity;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.KpiReport;
import com.workflow.politicas.model.ProcessInstance;
import com.workflow.politicas.model.TaskInstance;
import com.workflow.politicas.repository.ActivityRepository;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.KpiReportRepository;
import com.workflow.politicas.repository.ProcessInstanceRepository;
import com.workflow.politicas.repository.TaskInstanceRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KpiService {

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_PENDING = "PENDING";

    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final ActivityRepository activityRepository;
    private final BusinessPolicyRepository businessPolicyRepository;
    private final KpiReportRepository kpiReportRepository;

    public KpiService(
            ProcessInstanceRepository processInstanceRepository,
            TaskInstanceRepository taskInstanceRepository,
            ActivityRepository activityRepository,
            BusinessPolicyRepository businessPolicyRepository,
            KpiReportRepository kpiReportRepository) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskInstanceRepository = taskInstanceRepository;
        this.activityRepository = activityRepository;
        this.businessPolicyRepository = businessPolicyRepository;
        this.kpiReportRepository = kpiReportRepository;
    }

    public KpiDashboardResponse getDashboard() {
        List<ProcessInstance> processes = processInstanceRepository.findAll();
        List<TaskInstance> tasks = taskInstanceRepository.findAll();

        KpiDashboardResponse dashboard = new KpiDashboardResponse();
        dashboard.setTotalProcesses(processes.size());
        dashboard.setProcessesInProgress(countProcessesByStatus(processes, STATUS_IN_PROGRESS));
        dashboard.setCompletedProcesses(countProcessesByStatus(processes, STATUS_COMPLETED));
        dashboard.setPendingTasks(countTasksByStatus(tasks, STATUS_PENDING));
        dashboard.setCompletedTasks(countTasksByStatus(tasks, STATUS_COMPLETED));
        dashboard.setAverageProcessDurationHours(calculateAverageProcessDurationHours(processes));
        dashboard.setTasksByStatus(groupTasksByStatus(tasks));
        dashboard.setProcessesByStatus(groupProcessesByStatus(processes));

        saveKpiReport("DASHBOARD", buildDashboardMetrics(dashboard));
        return dashboard;
    }

    public KpiBottlenecksResponse getBottlenecks() {
        List<ProcessInstance> processes = processInstanceRepository.findAll();
        List<TaskInstance> tasks = taskInstanceRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        KpiBottlenecksResponse bottlenecks = new KpiBottlenecksResponse();
        bottlenecks.setActivitiesWithHighestDelay(calculateActivityDelays(tasks));
        bottlenecks.setRolesWithMostPendingTasks(calculateRolePendingTasks(tasks));
        bottlenecks.setLongRunningProcesses(calculateLongRunningProcesses(processes, now));
        bottlenecks.setOverdueTasks(calculateOverdueTasks(tasks, now));

        saveKpiReport("BOTTLENECKS", buildBottlenecksMetrics(bottlenecks));
        return bottlenecks;
    }

    private long countProcessesByStatus(List<ProcessInstance> processes, String status) {
        return processes.stream().filter(p -> status.equals(p.getStatus())).count();
    }

    private long countTasksByStatus(List<TaskInstance> tasks, String status) {
        return tasks.stream().filter(t -> status.equals(t.getStatus())).count();
    }

    private Map<String, Long> groupProcessesByStatus(List<ProcessInstance> processes) {
        return processes.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStatus() != null ? p.getStatus() : "UNKNOWN",
                        Collectors.counting()));
    }

    private Map<String, Long> groupTasksByStatus(List<TaskInstance> tasks) {
        return tasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus() != null ? t.getStatus() : "UNKNOWN",
                        Collectors.counting()));
    }

    private double calculateAverageProcessDurationHours(List<ProcessInstance> processes) {
        List<ProcessInstance> completed = processes.stream()
                .filter(p -> STATUS_COMPLETED.equals(p.getStatus())
                        && p.getStartedAt() != null && p.getEndedAt() != null)
                .toList();
        if (completed.isEmpty()) {
            return 0.0;
        }
        double totalHours = completed.stream()
                .mapToDouble(p -> Duration.between(p.getStartedAt(), p.getEndedAt()).toMinutes() / 60.0)
                .sum();
        return Math.round((totalHours / completed.size()) * 100.0) / 100.0;
    }

    private List<ActivityDelayDto> calculateActivityDelays(List<TaskInstance> tasks) {
        Map<String, List<Double>> delaysByActivity = new HashMap<>();
        for (TaskInstance task : tasks) {
            if (!STATUS_COMPLETED.equals(task.getStatus())
                    || task.getCreatedAt() == null || task.getCompletedAt() == null
                    || task.getActivityId() == null) {
                continue;
            }
            double hours = Duration.between(task.getCreatedAt(), task.getCompletedAt()).toMinutes() / 60.0;
            delaysByActivity.computeIfAbsent(task.getActivityId(), k -> new ArrayList<>()).add(hours);
        }

        return delaysByActivity.entrySet().stream()
                .map(entry -> {
                    ActivityDelayDto dto = new ActivityDelayDto();
                    dto.setActivityId(entry.getKey());
                    dto.setActivityName(resolveActivityName(entry.getKey()));
                    double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    dto.setAverageDelayHours(Math.round(avg * 100.0) / 100.0);
                    dto.setTaskCount(entry.getValue().size());
                    return dto;
                })
                .sorted(Comparator.comparingDouble(ActivityDelayDto::getAverageDelayHours).reversed())
                .limit(10)
                .toList();
    }

    private List<RolePendingTasksDto> calculateRolePendingTasks(List<TaskInstance> tasks) {
        Map<String, Long> pendingByRole = tasks.stream()
                .filter(t -> STATUS_PENDING.equals(t.getStatus())
                        && t.getAssignedRoleId() != null && !t.getAssignedRoleId().isBlank())
                .collect(Collectors.groupingBy(TaskInstance::getAssignedRoleId, Collectors.counting()));

        return pendingByRole.entrySet().stream()
                .map(entry -> {
                    RolePendingTasksDto dto = new RolePendingTasksDto();
                    dto.setRoleId(entry.getKey());
                    dto.setPendingTaskCount(entry.getValue());
                    return dto;
                })
                .sorted(Comparator.comparingLong(RolePendingTasksDto::getPendingTaskCount).reversed())
                .limit(10)
                .toList();
    }

    private List<LongRunningProcessDto> calculateLongRunningProcesses(List<ProcessInstance> processes, LocalDateTime now) {
        return processes.stream()
                .filter(p -> STATUS_IN_PROGRESS.equals(p.getStatus()) && p.getStartedAt() != null)
                .map(p -> {
                    LongRunningProcessDto dto = new LongRunningProcessDto();
                    dto.setProcessId(p.getId());
                    dto.setPolicyId(p.getPolicyId());
                    dto.setPolicyName(resolvePolicyName(p.getPolicyId()));
                    dto.setStartedAt(p.getStartedAt());
                    double hours = Duration.between(p.getStartedAt(), now).toMinutes() / 60.0;
                    dto.setHoursInExecution(Math.round(hours * 100.0) / 100.0);
                    return dto;
                })
                .sorted(Comparator.comparingDouble(LongRunningProcessDto::getHoursInExecution).reversed())
                .limit(10)
                .toList();
    }

    private List<OverdueTaskDto> calculateOverdueTasks(List<TaskInstance> tasks, LocalDateTime now) {
        return tasks.stream()
                .filter(t -> STATUS_PENDING.equals(t.getStatus())
                        && t.getDueDate() != null && t.getDueDate().isBefore(now))
                .map(t -> {
                    OverdueTaskDto dto = new OverdueTaskDto();
                    dto.setTaskId(t.getId());
                    dto.setProcessInstanceId(t.getProcessInstanceId());
                    dto.setActivityId(t.getActivityId());
                    dto.setActivityName(resolveActivityName(t.getActivityId()));
                    dto.setDueDate(t.getDueDate());
                    double hours = Duration.between(t.getDueDate(), now).toMinutes() / 60.0;
                    dto.setHoursOverdue(Math.round(hours * 100.0) / 100.0);
                    return dto;
                })
                .sorted(Comparator.comparingDouble(OverdueTaskDto::getHoursOverdue).reversed())
                .toList();
    }

    private String resolveActivityName(String activityId) {
        if (activityId == null) {
            return "Unknown";
        }
        return activityRepository.findById(activityId)
                .map(Activity::getName)
                .orElse("Unknown");
    }

    private String resolvePolicyName(String policyId) {
        if (policyId == null) {
            return "Unknown";
        }
        return businessPolicyRepository.findById(policyId)
                .map(BusinessPolicy::getName)
                .orElse("Unknown");
    }

    private void saveKpiReport(String reportType, Map<String, Object> metrics) {
        KpiReport report = new KpiReport();
        report.setPolicyId("ALL");
        report.setReportType(reportType);
        report.setMetrics(metrics);
        report.setGeneratedAt(LocalDateTime.now());
        kpiReportRepository.save(report);
    }

    private Map<String, Object> buildDashboardMetrics(KpiDashboardResponse dashboard) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalProcesses", dashboard.getTotalProcesses());
        metrics.put("processesInProgress", dashboard.getProcessesInProgress());
        metrics.put("completedProcesses", dashboard.getCompletedProcesses());
        metrics.put("pendingTasks", dashboard.getPendingTasks());
        metrics.put("completedTasks", dashboard.getCompletedTasks());
        metrics.put("averageProcessDurationHours", dashboard.getAverageProcessDurationHours());
        metrics.put("tasksByStatus", dashboard.getTasksByStatus());
        metrics.put("processesByStatus", dashboard.getProcessesByStatus());
        return metrics;
    }

    private Map<String, Object> buildBottlenecksMetrics(KpiBottlenecksResponse bottlenecks) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activitiesWithHighestDelayCount", bottlenecks.getActivitiesWithHighestDelay().size());
        metrics.put("rolesWithMostPendingTasksCount", bottlenecks.getRolesWithMostPendingTasks().size());
        metrics.put("longRunningProcessesCount", bottlenecks.getLongRunningProcesses().size());
        metrics.put("overdueTasksCount", bottlenecks.getOverdueTasks().size());
        metrics.put("topActivityDelay", bottlenecks.getActivitiesWithHighestDelay().isEmpty()
                ? null : bottlenecks.getActivitiesWithHighestDelay().get(0));
        metrics.put("topRolePending", bottlenecks.getRolesWithMostPendingTasks().isEmpty()
                ? null : bottlenecks.getRolesWithMostPendingTasks().get(0));
        return metrics;
    }
}
