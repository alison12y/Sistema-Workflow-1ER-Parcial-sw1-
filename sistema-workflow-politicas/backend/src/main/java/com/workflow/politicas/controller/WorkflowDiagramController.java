package com.workflow.politicas.controller;

import com.workflow.politicas.dto.WorkflowValidationResponse;
import com.workflow.politicas.model.WorkflowDiagram;
import com.workflow.politicas.service.WorkflowDiagramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowDiagramController {

    private final WorkflowDiagramService workflowDiagramService;

    public WorkflowDiagramController(WorkflowDiagramService workflowDiagramService) {
        this.workflowDiagramService = workflowDiagramService;
    }

    @PostMapping
    public WorkflowDiagram createWorkflow(@RequestBody WorkflowDiagram diagram) {
        return workflowDiagramService.create(diagram);
    }

    @GetMapping("/{policyId}")
    public List<WorkflowDiagram> getWorkflowsByPolicy(@PathVariable String policyId) {
        return workflowDiagramService.findByPolicyId(policyId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDiagram> updateWorkflow(@PathVariable String id, @RequestBody WorkflowDiagram details) {
        try {
            return ResponseEntity.ok(workflowDiagramService.update(id, details));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<WorkflowValidationResponse> validateWorkflow(@PathVariable String id) {
        try {
            return ResponseEntity.ok(workflowDiagramService.validate(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
