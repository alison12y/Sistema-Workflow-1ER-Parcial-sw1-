package com.workflow.politicas.controller;

import com.workflow.politicas.dto.WorkflowDesignerResponse;
import com.workflow.politicas.service.WorkflowDesignerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflow-designer")
public class WorkflowDesignerController {

    private final WorkflowDesignerService workflowDesignerService;

    public WorkflowDesignerController(WorkflowDesignerService workflowDesignerService) {
        this.workflowDesignerService = workflowDesignerService;
    }

    @GetMapping("/policy/{policyId}")
    public ResponseEntity<WorkflowDesignerResponse> getByPolicy(@PathVariable String policyId) {
        return ResponseEntity.ok(workflowDesignerService.getDesignerData(policyId));
    }
}
