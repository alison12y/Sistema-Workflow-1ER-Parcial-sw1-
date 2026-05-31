package com.workflow.politicas.controller;

import com.workflow.politicas.dto.WorkflowDeleteResponse;
import com.workflow.politicas.dto.WorkflowFlowValidationResponse;
import com.workflow.politicas.dto.WorkflowTransitionCleanupResponse;
import com.workflow.politicas.dto.WorkflowTransitionDedupeResponse;
import com.workflow.politicas.dto.WorkflowTransitionRequest;
import com.workflow.politicas.dto.WorkflowTransitionResponse;
import com.workflow.politicas.service.WorkflowTransitionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow-transitions")
public class WorkflowTransitionController {

    private final WorkflowTransitionService workflowTransitionService;

    public WorkflowTransitionController(WorkflowTransitionService workflowTransitionService) {
        this.workflowTransitionService = workflowTransitionService;
    }

    @GetMapping("/policy/{policyId}")
    public List<WorkflowTransitionResponse> getByPolicy(@PathVariable String policyId) {
        return workflowTransitionService.findByPolicyId(policyId);
    }

    @GetMapping("/policy/{policyId}/validate")
    public WorkflowFlowValidationResponse validatePolicyFlow(@PathVariable String policyId) {
        return workflowTransitionService.validateFlow(policyId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowTransitionResponse> getById(@PathVariable String id) {
        return workflowTransitionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<WorkflowTransitionResponse> create(@RequestBody WorkflowTransitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowTransitionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowTransitionResponse> update(
            @PathVariable String id,
            @RequestBody WorkflowTransitionRequest request
    ) {
        return ResponseEntity.ok(workflowTransitionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WorkflowDeleteResponse> delete(@PathVariable String id) {
        return ResponseEntity.ok(workflowTransitionService.delete(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<WorkflowTransitionResponse> activate(@PathVariable String id) {
        return ResponseEntity.ok(workflowTransitionService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<WorkflowTransitionResponse> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(workflowTransitionService.deactivate(id));
    }

    @PostMapping("/policy/{policyId}/deduplicate")
    public ResponseEntity<WorkflowTransitionDedupeResponse> deduplicate(@PathVariable String policyId) {
        return ResponseEntity.ok(workflowTransitionService.deduplicateByPolicyId(policyId));
    }

    @PostMapping("/policy/{policyId}/cleanup")
    public ResponseEntity<WorkflowTransitionCleanupResponse> cleanup(@PathVariable String policyId) {
        return ResponseEntity.ok(workflowTransitionService.cleanupTransitions(policyId));
    }
}
