package com.workflow.politicas.controller;

import com.workflow.politicas.dto.DocumentAccessResponse;
import com.workflow.politicas.dto.DocumentCollaborationLockRequest;
import com.workflow.politicas.dto.DocumentCollaborationSessionRequest;
import com.workflow.politicas.dto.DocumentCollaborationStateResponse;
import com.workflow.politicas.dto.DocumentPermissionRequest;
import com.workflow.politicas.dto.DocumentPermissionResponse;
import com.workflow.politicas.service.DocumentCollaborationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/document-repositories")
public class DocumentCollaborationController {

    private final DocumentCollaborationService collaborationService;

    public DocumentCollaborationController(DocumentCollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @GetMapping("/{repositoryId}/collaboration")
    public DocumentCollaborationStateResponse getState(
            @PathVariable String repositoryId,
            @RequestParam(required = false) String sessionId
    ) {
        return collaborationService.getState(repositoryId, sessionId);
    }

    @PostMapping("/{repositoryId}/collaboration/open")
    public DocumentCollaborationStateResponse open(
            @PathVariable String repositoryId,
            @RequestBody DocumentCollaborationSessionRequest request
    ) {
        return collaborationService.registerOpen(repositoryId, request.getSessionId());
    }

    @PostMapping("/{repositoryId}/collaboration/heartbeat")
    public DocumentCollaborationStateResponse heartbeat(
            @PathVariable String repositoryId,
            @RequestBody DocumentCollaborationSessionRequest request
    ) {
        return collaborationService.heartbeat(repositoryId, request.getSessionId());
    }

    @PostMapping("/{repositoryId}/collaboration/close")
    public ResponseEntity<Void> close(
            @PathVariable String repositoryId,
            @RequestBody DocumentCollaborationSessionRequest request
    ) {
        collaborationService.registerClose(repositoryId, request.getSessionId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{repositoryId}/collaboration/lock")
    public DocumentCollaborationStateResponse acquireLock(
            @PathVariable String repositoryId,
            @RequestBody DocumentCollaborationLockRequest request
    ) {
        return collaborationService.acquireLock(repositoryId, request);
    }

    @DeleteMapping("/{repositoryId}/collaboration/lock/{documentFamilyId}")
    public DocumentCollaborationStateResponse releaseLock(
            @PathVariable String repositoryId,
            @PathVariable String documentFamilyId,
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        return collaborationService.releaseLock(repositoryId, documentFamilyId, sessionId, force);
    }

    @GetMapping("/documents/{documentId}/access")
    public DocumentAccessResponse getAccess(@PathVariable String documentId) {
        return collaborationService.getDocumentAccess(documentId);
    }

    @GetMapping("/documents/{documentId}/permissions")
    public List<DocumentPermissionResponse> listPermissions(@PathVariable String documentId) {
        return collaborationService.listPermissions(documentId);
    }

    @PostMapping("/documents/{documentId}/permissions")
    public DocumentPermissionResponse grantPermission(
            @PathVariable String documentId,
            @RequestBody DocumentPermissionRequest request
    ) {
        return collaborationService.grantOrUpdatePermission(documentId, request);
    }

    @PutMapping("/documents/{documentId}/permissions")
    public DocumentPermissionResponse updatePermission(
            @PathVariable String documentId,
            @RequestBody DocumentPermissionRequest request
    ) {
        return collaborationService.grantOrUpdatePermission(documentId, request);
    }

    @DeleteMapping("/documents/{documentId}/permissions")
    public ResponseEntity<Void> removePermission(
            @PathVariable String documentId,
            @RequestBody DocumentPermissionRequest request
    ) {
        collaborationService.removePermission(documentId, request);
        return ResponseEntity.noContent().build();
    }
}
