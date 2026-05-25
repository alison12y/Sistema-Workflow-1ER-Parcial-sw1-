package com.workflow.politicas.controller;

import com.workflow.politicas.dto.ProcessStartRequest;
import com.workflow.politicas.model.ProcessInstance;
import com.workflow.politicas.service.ProcessInstanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/process")
public class ProcessInstanceController {

    private final ProcessInstanceService processInstanceService;

    public ProcessInstanceController(ProcessInstanceService processInstanceService) {
        this.processInstanceService = processInstanceService;
    }

    @PostMapping("/start")
    public ProcessInstance startProcess(@RequestBody ProcessStartRequest request) {
        return processInstanceService.start(request);
    }

    @GetMapping
    public List<ProcessInstance> getAllProcesses() {
        return processInstanceService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessInstance> getProcessById(@PathVariable String id) {
        return processInstanceService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
