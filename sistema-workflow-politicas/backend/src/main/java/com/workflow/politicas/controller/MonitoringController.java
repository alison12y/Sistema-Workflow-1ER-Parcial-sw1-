package com.workflow.politicas.controller;

import com.workflow.politicas.dto.MonitoringItemDto;
import com.workflow.politicas.dto.MonitoringResponsibleDto;
import com.workflow.politicas.dto.MonitoringTasksResponse;
import com.workflow.politicas.dto.MonitoringTimelineResponse;
import com.workflow.politicas.dto.MonitoringTraceResponse;
import com.workflow.politicas.service.MonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping
    public List<MonitoringItemDto> listTramites() {
        return monitoringService.listTramites();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitoringTraceResponse> getDetail(@PathVariable String id) {
        return monitoringService.getDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/trace")
    public ResponseEntity<MonitoringTraceResponse> getTrace(@PathVariable String id) {
        return monitoringService.getTrace(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<MonitoringTimelineResponse> getTimeline(@PathVariable String id) {
        return monitoringService.getTimeline(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<MonitoringTasksResponse> getTasks(@PathVariable String id) {
        return monitoringService.getTasks(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/responsibles")
    public ResponseEntity<List<MonitoringResponsibleDto>> getResponsibles(@PathVariable String id) {
        return monitoringService.getResponsibles(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
