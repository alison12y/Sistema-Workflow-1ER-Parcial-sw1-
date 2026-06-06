package com.workflow.politicas.controller;

import com.workflow.politicas.dto.DocumentDownloadResponse;
import com.workflow.politicas.dto.DocumentRecordResponse;
import com.workflow.politicas.dto.DocumentRepositoryMigrationReport;
import com.workflow.politicas.dto.DocumentRepositoryResponse;
import com.workflow.politicas.service.DocumentRepositoryMigrationService;
import com.workflow.politicas.service.DocumentRepositoryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/document-repositories")
public class DocumentRepositoryController {

    private final DocumentRepositoryService documentRepositoryService;
    private final DocumentRepositoryMigrationService documentRepositoryMigrationService;

    public DocumentRepositoryController(
            DocumentRepositoryService documentRepositoryService,
            DocumentRepositoryMigrationService documentRepositoryMigrationService
    ) {
        this.documentRepositoryService = documentRepositoryService;
        this.documentRepositoryMigrationService = documentRepositoryMigrationService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "document-repositories-ok";
    }

    @GetMapping("/tramite/{tramiteId}")
    public DocumentRepositoryResponse findByTramite(@PathVariable String tramiteId) {
        return documentRepositoryService.findByTramiteId(tramiteId);
    }

    @GetMapping("/{repositoryId}/documents")
    public List<DocumentRecordResponse> listDocuments(@PathVariable String repositoryId) {
        return documentRepositoryService.listDocuments(repositoryId);
    }

    @GetMapping("/documents/{documentId}/versions")
    public List<DocumentRecordResponse> listDocumentVersions(@PathVariable String documentId) {
        return documentRepositoryService.listDocumentVersions(documentId);
    }

    @PostMapping("/migrate-existing")
    public DocumentRepositoryMigrationReport migrateExisting(Authentication authentication) {
        return documentRepositoryMigrationService.migrateExistingTramites(resolveUsername(authentication));
    }

    @PostMapping("/{repositoryId}/upload")
    public DocumentRecordResponse uploadDocument(
            @PathVariable String repositoryId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        return documentRepositoryService.uploadDocument(repositoryId, file, resolveUsername(authentication));
    }

    @GetMapping("/documents/{documentId}")
    public DocumentDownloadResponse downloadDocument(
            @PathVariable String documentId,
            Authentication authentication
    ) {
        return documentRepositoryService.getDocumentDownload(documentId, resolveUsername(authentication));
    }

    @DeleteMapping("/documents/{documentId}")
    public DocumentRecordResponse deleteDocument(
            @PathVariable String documentId,
            Authentication authentication
    ) {
        return documentRepositoryService.deleteDocument(documentId, resolveUsername(authentication));
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
