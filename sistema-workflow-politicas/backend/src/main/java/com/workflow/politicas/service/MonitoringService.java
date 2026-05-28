package com.workflow.politicas.service;

import com.workflow.politicas.dto.MonitoringItemDto;
import com.workflow.politicas.dto.MonitoringTraceResponse;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.repository.TramiteRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class MonitoringService {

    private final TramiteRepository tramiteRepository;

    public MonitoringService(TramiteRepository tramiteRepository) {
        this.tramiteRepository = tramiteRepository;
    }

    public List<MonitoringItemDto> listTramites() {
        return tramiteRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        Tramite::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(this::toMonitoringItem)
                .toList();
    }

    public Optional<MonitoringTraceResponse> getTrace(String tramiteId) {
        return tramiteRepository.findById(tramiteId).map(this::toTraceResponse);
    }

    private MonitoringItemDto toMonitoringItem(Tramite tramite) {
        MonitoringItemDto item = new MonitoringItemDto();
        item.setId(tramite.getId());
        item.setCode(tramite.getCode());
        item.setPolicyName(tramite.getPolicyName());
        item.setStatus(tramite.getStatus());
        item.setCurrentActivity(normalizeText(tramite.getCurrentActivity(), "Sin actividad"));
        item.setResponsible(normalizeResponsible(tramite.getResponsible()));
        item.setProgress(tramite.getProgress());
        item.setTimeElapsed(formatElapsed(tramite.getCreatedAt(), resolveEndTime(tramite)));
        return item;
    }

    private MonitoringTraceResponse toTraceResponse(Tramite tramite) {
        MonitoringTraceResponse response = new MonitoringTraceResponse();
        response.setCode(tramite.getCode());
        response.setPolicyName(tramite.getPolicyName());
        response.setStatus(tramite.getStatus());
        response.setCurrentActivity(normalizeText(tramite.getCurrentActivity(), "Sin actividad"));
        response.setResponsible(normalizeResponsible(tramite.getResponsible()));
        response.setProgress(tramite.getProgress());
        response.setTasks(tramite.getTasks() != null ? tramite.getTasks() : List.of());
        response.setEvents(tramite.getTrace() != null ? tramite.getTrace() : List.of());
        return response;
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
}
