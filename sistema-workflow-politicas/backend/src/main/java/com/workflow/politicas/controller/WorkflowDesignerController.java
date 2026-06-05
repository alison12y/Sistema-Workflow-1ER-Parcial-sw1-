package com.workflow.politicas.controller;

import com.workflow.politicas.dto.WorkflowCollaborationSessionRequest;
import com.workflow.politicas.dto.WorkflowCollaborationStateResponse;
import com.workflow.politicas.dto.WorkflowDesignerResponse;
import com.workflow.politicas.service.WorkflowCollaborationService;
import com.workflow.politicas.service.WorkflowDesignerService;
import org.springframework.http.ResponseEntity;
import com.workflow.politicas.dto.WorkflowCollaborationEditingRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflow-designer")
public class WorkflowDesignerController {

    private final WorkflowDesignerService workflowDesignerService;
    private final WorkflowCollaborationService workflowCollaborationService;

    public WorkflowDesignerController(
            WorkflowDesignerService workflowDesignerService,
            WorkflowCollaborationService workflowCollaborationService
    ) {
        this.workflowDesignerService = workflowDesignerService;
        this.workflowCollaborationService = workflowCollaborationService;
    }

    @GetMapping("/policy/{policyId}")
    public ResponseEntity<WorkflowDesignerResponse> getByPolicy(@PathVariable String policyId) {
        return ResponseEntity.ok(workflowDesignerService.getDesignerData(policyId));
    }

    @GetMapping("/policy/{policyId}/collaboration")
    public ResponseEntity<WorkflowCollaborationStateResponse> getCollaborationState(
            @PathVariable String policyId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Long baseRevision
    ) {
        if (sessionId != null && !sessionId.isBlank()) {
            return ResponseEntity.ok(workflowCollaborationService.heartbeat(policyId, sessionId, baseRevision));
        }
        return ResponseEntity.ok(workflowCollaborationService.getState(policyId, null, baseRevision));
    }

    @PostMapping("/policy/{policyId}/collaboration/open")
    public ResponseEntity<WorkflowCollaborationStateResponse> openCollaboration(
            @PathVariable String policyId,
            @RequestBody WorkflowCollaborationSessionRequest request
    ) {
        return ResponseEntity.ok(
                workflowCollaborationService.registerOpen(policyId, request.getSessionId())
        );
    }

    @PostMapping("/policy/{policyId}/collaboration/heartbeat")
    public ResponseEntity<WorkflowCollaborationStateResponse> heartbeatCollaboration(
            @PathVariable String policyId,
            @RequestBody WorkflowCollaborationSessionRequest request
    ) {
        return ResponseEntity.ok(
                workflowCollaborationService.heartbeat(
                        policyId,
                        request.getSessionId(),
                        request.getBaseRevision()
                )
        );
    }

    @PostMapping("/policy/{policyId}/collaboration/close")
    public ResponseEntity<Void> closeCollaboration(
            @PathVariable String policyId,
            @RequestBody WorkflowCollaborationSessionRequest request
    ) {
        workflowCollaborationService.registerClose(policyId, request.getSessionId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/policy/{policyId}/collaboration/conflict")
    public ResponseEntity<Void> reportConflict(
            @PathVariable String policyId,
            @RequestBody WorkflowCollaborationSessionRequest request
    ) {
        workflowCollaborationService.registerConflict(policyId, request.getBaseRevision());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/policy/{policyId}/collaboration/editing")
    public ResponseEntity<WorkflowCollaborationStateResponse> registerEditing(
            @PathVariable String policyId,
            @RequestBody WorkflowCollaborationEditingRequest request
    ) {
        return ResponseEntity.ok(workflowCollaborationService.registerEditing(policyId, request));
    }

    @DeleteMapping("/policy/{policyId}/collaboration/editing/{elementId}")
    public ResponseEntity<WorkflowCollaborationStateResponse> clearEditing(
            @PathVariable String policyId,
            @PathVariable String elementId,
            @RequestParam String sessionId
    ) {
        return ResponseEntity.ok(
                workflowCollaborationService.clearEditing(policyId, sessionId, elementId)
        );
    }
}
