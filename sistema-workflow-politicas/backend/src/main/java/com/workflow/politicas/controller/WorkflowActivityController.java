package com.workflow.politicas.controller;

import com.workflow.politicas.dto.WorkflowActivityResponse;
import com.workflow.politicas.service.WorkflowActivityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
