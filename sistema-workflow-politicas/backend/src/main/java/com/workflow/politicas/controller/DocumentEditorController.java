package com.workflow.politicas.controller;

import com.workflow.politicas.dto.DocumentCollaborationSessionRequest;
import com.workflow.politicas.dto.DocumentCollaborationStateResponse;
import com.workflow.politicas.dto.DocumentEditorSessionResponse;
import com.workflow.politicas.dto.OnlyOfficeCallbackRequest;
import com.workflow.politicas.security.AuthenticatedActorResolver;
import com.workflow.politicas.service.DocumentEditorService;
import com.workflow.politicas.storage.StoredObject;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/document-repositories")
public class DocumentEditorController {

    private final DocumentEditorService documentEditorService;
    private final AuthenticatedActorResolver actorResolver;

    public DocumentEditorController(
            DocumentEditorService documentEditorService,
            AuthenticatedActorResolver actorResolver
    ) {
        this.documentEditorService = documentEditorService;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/documents/{documentId}/editor-session")
    public DocumentEditorSessionResponse getEditorSession(
            @PathVariable String documentId,
            @RequestParam String repositoryId,
            @RequestParam String sessionId
    ) {
        return documentEditorService.buildEditorSession(
                documentId,
                repositoryId,
                sessionId,
                actorResolver.requireCurrentActor()
        );
    }

    @PostMapping("/documents/{documentId}/edit/start")
    public DocumentCollaborationStateResponse startEdit(
            @PathVariable String documentId,
            @RequestParam String repositoryId,
            @RequestBody DocumentCollaborationSessionRequest request
    ) {
        return documentEditorService.startEdit(
                documentId,
                repositoryId,
                request.getSessionId(),
                actorResolver.requireCurrentActor()
        );
    }

    @PostMapping("/documents/{documentId}/edit/close")
    public DocumentCollaborationStateResponse closeEdit(
            @PathVariable String documentId,
            @RequestParam String repositoryId,
            @RequestBody DocumentCollaborationSessionRequest request
    ) {
        return documentEditorService.closeEdit(
                documentId,
                repositoryId,
                request.getSessionId(),
                actorResolver.requireCurrentActor()
        );
    }

    @GetMapping("/documents/{documentId}/onlyoffice/file")
    public void downloadForOnlyOffice(
            @PathVariable String documentId,
            @RequestParam String accessToken,
            HttpServletResponse response
    ) throws Exception {
        StoredObject stored = documentEditorService.loadDocumentForOnlyOffice(documentId, accessToken);
        response.setContentType(stored.getContentType() != null
                ? stored.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");
        if (stored.getContentLength() > 0) {
            response.setContentLengthLong(stored.getContentLength());
        }
        try (InputStream inputStream = stored.getInputStream()) {
            inputStream.transferTo(response.getOutputStream());
        }
    }

    @PostMapping("/documents/{documentId}/onlyoffice/callback")
    public ResponseEntity<Map<String, Object>> onlyOfficeCallback(
            @PathVariable String documentId,
            @RequestParam String accessToken,
            @RequestBody OnlyOfficeCallbackRequest callback
    ) {
        return ResponseEntity.ok(documentEditorService.handleCallback(documentId, accessToken, callback));
    }
}
