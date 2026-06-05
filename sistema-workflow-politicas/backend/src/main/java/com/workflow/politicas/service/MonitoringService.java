package com.workflow.politicas.service;



import com.workflow.politicas.dto.*;

import com.workflow.politicas.model.TraceItem;

import com.workflow.politicas.model.Tramite;

import com.workflow.politicas.model.TramiteTask;

import com.workflow.politicas.repository.TramiteRepository;

import org.springframework.stereotype.Service;



import java.time.Duration;

import java.time.LocalDateTime;

import java.util.*;

import java.util.stream.Collectors;



/** Monitoreo de trámites (runtime oficial Ciclo 1) sobre {@link Tramite}. */

@Service

public class MonitoringService {



    private static final String TASK_PENDIENTE = "PENDIENTE";

    private static final String TASK_EN_CURSO = "EN_CURSO";

    private static final String TASK_COMPLETADA = "COMPLETADA";

    private static final String STATUS_CANCELADO = "CANCELADO";



    private final TramiteRepository tramiteRepository;



    public MonitoringService(TramiteRepository tramiteRepository) {

        this.tramiteRepository = tramiteRepository;

    }



    public List<MonitoringItemDto> listTramites() {

        return tramiteRepository.findByStatusNot(STATUS_CANCELADO).stream()

                .sorted(Comparator.comparing(

                        Tramite::getCreatedAt,

                        Comparator.nullsLast(Comparator.reverseOrder())

                ))

                .map(this::toMonitoringItem)

                .toList();

    }



    public Optional<MonitoringTraceResponse> getDetail(String tramiteId) {

        return tramiteRepository.findById(tramiteId).map(this::toDetailResponse);

    }



    public Optional<MonitoringTraceResponse> getTrace(String tramiteId) {

        return getDetail(tramiteId);

    }



    public Optional<MonitoringTimelineResponse> getTimeline(String tramiteId) {

        return tramiteRepository.findById(tramiteId).map(tramite -> {

            MonitoringTimelineResponse response = new MonitoringTimelineResponse();

            response.setTramiteId(tramite.getId());

            response.setCode(tramite.getCode());

            response.setEvents(sortEvents(tramite.getTrace()));

            return response;

        });

    }



    public Optional<MonitoringTasksResponse> getTasks(String tramiteId) {

        return tramiteRepository.findById(tramiteId).map(tramite -> {

            TaskGroups groups = groupTasks(tramite.getTasks());

            MonitoringTasksResponse response = new MonitoringTasksResponse();

            response.setTramiteId(tramite.getId());

            response.setCode(tramite.getCode());

            response.setCounts(groups.counts());

            response.setPending(groups.pending());

            response.setInProgress(groups.inProgress());

            response.setCompleted(groups.completed());

            return response;

        });

    }



    public Optional<List<MonitoringResponsibleDto>> getResponsibles(String tramiteId) {

        return tramiteRepository.findById(tramiteId)

                .map(tramite -> buildResponsibleSummary(tramite.getTasks()));

    }



    private MonitoringItemDto toMonitoringItem(Tramite tramite) {

        TaskGroups groups = groupTasks(tramite.getTasks());

        MonitoringItemDto item = new MonitoringItemDto();

        item.setId(tramite.getId());

        item.setPolicyId(tramite.getPolicyId());

        item.setCode(tramite.getCode());

        item.setPolicyName(tramite.getPolicyName());

        item.setStatus(tramite.getStatus());

        item.setCurrentActivity(normalizeText(tramite.getCurrentActivity(), "Sin actividad"));

        item.setResponsible(normalizeResponsible(tramite.getResponsible()));

        item.setProgress(tramite.getProgress());

        item.setWorkflowError(tramite.getWorkflowError());

        item.setPendingTaskCount(groups.counts().getPending());

        item.setInProgressTaskCount(groups.counts().getInProgress());

        item.setCompletedTaskCount(groups.counts().getCompleted());

        item.setUpdatedAt(tramite.getUpdatedAt());

        item.setTimeElapsed(formatElapsed(tramite.getCreatedAt(), resolveEndTime(tramite)));

        return item;

    }



    private MonitoringTraceResponse toDetailResponse(Tramite tramite) {

        TaskGroups groups = groupTasks(tramite.getTasks());

        List<TramiteTask> allTasks = sortedTasks(tramite.getTasks());



        MonitoringTraceResponse response = new MonitoringTraceResponse();

        response.setId(tramite.getId());

        response.setPolicyId(tramite.getPolicyId());

        response.setCode(tramite.getCode());

        response.setPolicyName(tramite.getPolicyName());

        response.setStatus(tramite.getStatus());

        response.setCurrentActivity(normalizeText(tramite.getCurrentActivity(), "Sin actividad"));

        response.setResponsible(normalizeResponsible(tramite.getResponsible()));

        response.setProgress(tramite.getProgress());

        response.setWorkflowError(tramite.getWorkflowError());

        response.setCreatedAt(tramite.getCreatedAt());

        response.setUpdatedAt(tramite.getUpdatedAt());

        response.setTaskCounts(groups.counts());

        response.setPendingTasks(groups.pending());

        response.setInProgressTasks(groups.inProgress());

        response.setCompletedTasks(groups.completed());

        response.setTasks(allTasks);

        response.setResponsibles(buildResponsibleSummary(tramite.getTasks()));

        response.setEvents(sortEvents(tramite.getTrace()));

        return response;

    }



    private TaskGroups groupTasks(List<TramiteTask> tasks) {

        List<TramiteTask> sorted = sortedTasks(tasks);

        List<TramiteTask> pending = new ArrayList<>();

        List<TramiteTask> inProgress = new ArrayList<>();

        List<TramiteTask> completed = new ArrayList<>();



        for (TramiteTask task : sorted) {

            String status = normalizeTaskStatus(task.getStatus());

            if (TASK_PENDIENTE.equals(status)) {

                pending.add(task);

            } else if (TASK_EN_CURSO.equals(status)) {

                inProgress.add(task);

            } else if (TASK_COMPLETADA.equals(status)) {

                completed.add(task);

            }

        }



        MonitoringTaskCountsDto counts = new MonitoringTaskCountsDto();

        counts.setPending(pending.size());

        counts.setInProgress(inProgress.size());

        counts.setCompleted(completed.size());

        counts.setTotal(sorted.size());

        return new TaskGroups(pending, inProgress, completed, counts);

    }



    private List<TramiteTask> sortedTasks(List<TramiteTask> tasks) {

        if (tasks == null || tasks.isEmpty()) {

            return List.of();

        }

        return tasks.stream()

                .sorted(Comparator.comparingInt(TramiteTask::getOrder))

                .toList();

    }



    private List<MonitoringResponsibleDto> buildResponsibleSummary(List<TramiteTask> tasks) {

        if (tasks == null || tasks.isEmpty()) {

            return List.of();

        }



        Map<String, MonitoringResponsibleDto> byResponsible = new LinkedHashMap<>();

        for (TramiteTask task : tasks) {

            String key = normalizeResponsible(task.getResponsible());

            MonitoringResponsibleDto dto = byResponsible.computeIfAbsent(key, k -> {

                MonitoringResponsibleDto item = new MonitoringResponsibleDto();

                item.setResponsible(k);

                return item;

            });

            String status = normalizeTaskStatus(task.getStatus());

            if (TASK_PENDIENTE.equals(status)) {

                dto.setPendingCount(dto.getPendingCount() + 1);

            } else if (TASK_EN_CURSO.equals(status)) {

                dto.setInProgressCount(dto.getInProgressCount() + 1);

            } else if (TASK_COMPLETADA.equals(status)) {

                dto.setCompletedCount(dto.getCompletedCount() + 1);

            }

        }

        return new ArrayList<>(byResponsible.values());

    }



    private List<TraceItem> sortEvents(List<TraceItem> events) {

        if (events == null || events.isEmpty()) {

            return List.of();

        }

        return events.stream()

                .sorted(Comparator.comparing(

                        this::eventTimestamp,

                        Comparator.nullsLast(Comparator.reverseOrder())

                ))

                .toList();

    }



    private LocalDateTime eventTimestamp(TraceItem event) {

        if (event.getOccurredAt() != null) {

            return event.getOccurredAt();

        }

        return event.getStartedAt();

    }



    private String normalizeTaskStatus(String status) {

        if (status == null) {

            return "";

        }

        return status.trim().toUpperCase(Locale.ROOT);

    }



    private LocalDateTime resolveEndTime(Tramite tramite) {

        LocalDateTime now = LocalDateTime.now();

        if (isClosed(tramite.getStatus())) {

            return tramite.getUpdatedAt() != null ? tramite.getUpdatedAt() : now;

        }

        return now;

    }



    private boolean isClosed(String status) {

        if (status == null) {

            return false;

        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);

        return "COMPLETADO".equals(normalized)

                || "FINALIZADO".equals(normalized)

                || "COMPLETED".equals(normalized)

                || "DONE".equals(normalized)

                || "CANCELADO".equals(normalized)

                || "CANCELLED".equals(normalized);

    }



    private String normalizeText(String value, String fallback) {

        if (value == null || value.isBlank()) {

            return fallback;

        }

        return value.trim();

    }



    private String normalizeResponsible(String responsible) {

        if (responsible == null || responsible.isBlank() || "—".equals(responsible.trim())) {

            return "Sin asignar";

        }

        return responsible.trim();

    }



    private String formatElapsed(LocalDateTime start, LocalDateTime end) {

        if (start == null || end == null || end.isBefore(start)) {

            return "—";

        }



        Duration duration = Duration.between(start, end);

        long minutes = duration.toMinutes();



        if (minutes < 1) {

            return "1 min";

        }

        if (minutes < 60) {

            return minutes + " min";

        }



        long hours = duration.toHours();

        if (hours < 24) {

            return hours + (hours == 1 ? " hora" : " horas");

        }



        long days = duration.toDays();

        return days + (days == 1 ? " día" : " días");

    }



    private record TaskGroups(

            List<TramiteTask> pending,

            List<TramiteTask> inProgress,

            List<TramiteTask> completed,

            MonitoringTaskCountsDto counts

    ) {}

}


