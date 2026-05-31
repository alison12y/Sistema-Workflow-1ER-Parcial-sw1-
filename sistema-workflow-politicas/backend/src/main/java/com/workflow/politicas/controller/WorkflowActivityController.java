package com.workflow.politicas.controller;

import com.workflow.politicas.dto.WorkflowDeleteResponse;
import com.workflow.politicas.dto.WorkflowActivityPositionRequest;
import com.workflow.politicas.dto.WorkflowActivityRequest;
import com.workflow.politicas.dto.WorkflowActivityResponse;
import com.workflow.politicas.service.WorkflowActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow-activities")
public class WorkflowActivityController {

    private final WorkflowActivityService workflowActivityService;

    public WorkflowActivityController(WorkflowActivityService workflowActivityService) {
        this.workflowActivityService = workflowActivityService;
    }

    @GetMapping("/policy/{policyId}")
    public List<WorkflowActivityResponse> getByPolicy(@PathVariable String policyId) {
        return workflowActivityService.findByPolicyId(policyId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowActivityResponse> getById(@PathVariable String id) {
        return workflowActivityService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<WorkflowActivityResponse> create(@RequestBody WorkflowActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowActivityService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowActivityResponse> update(
            @PathVariable String id,
            @RequestBody WorkflowActivityRequest request
    ) {
        return ResponseEntity.ok(workflowActivityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WorkflowDeleteResponse> delete(@PathVariable String id) {
        return ResponseEntity.ok(workflowActivityService.delete(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<WorkflowActivityResponse> activate(@PathVariable String id) {
        return ResponseEntity.ok(workflowActivityService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<WorkflowActivityResponse> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(workflowActivityService.deactivate(id));
    }

    @PatchMapping("/{id}/position")
    public ResponseEntity<WorkflowActivityResponse> updatePosition(
            @PathVariable String id,
            @RequestBody WorkflowActivityPositionRequest request
    ) {
        return ResponseEntity.ok(workflowActivityService.updatePosition(id, request));
    }

    @DeleteMapping("/{id}/position")
    public ResponseEntity<WorkflowActivityResponse> clearPosition(@PathVariable String id) {
        return ResponseEntity.ok(workflowActivityService.clearPosition(id));
    }
}
