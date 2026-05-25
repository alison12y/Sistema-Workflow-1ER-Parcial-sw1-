package com.workflow.politicas.controller;

import com.workflow.politicas.dto.ProcessTraceabilityResponse;
import com.workflow.politicas.model.ProcessInstance;
import com.workflow.politicas.model.TaskInstance;
import com.workflow.politicas.service.MonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/processes")
    public List<ProcessInstance> listRunningProcesses() {
        return monitoringService.listRunningProcesses();
    }

    @GetMapping("/processes/{processId}")
    public ResponseEntity<ProcessInstance> getProcessDetail(@PathVariable String processId) {
        return monitoringService.getProcessDetail(processId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/processes/{processId}/tasks")
    public ResponseEntity<List<TaskInstance>> getProcessTasks(@PathVariable String processId) {
        if (monitoringService.getProcessDetail(processId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(monitoringService.getProcessTasks(processId));
    }

    @GetMapping("/traceability/{processId}")
    public ResponseEntity<ProcessTraceabilityResponse> getTraceability(@PathVariable String processId) {
        return monitoringService.getTraceability(processId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
