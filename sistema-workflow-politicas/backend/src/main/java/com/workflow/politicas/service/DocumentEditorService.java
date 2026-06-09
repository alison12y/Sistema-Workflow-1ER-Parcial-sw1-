package com.workflow.politicas.service;

import com.workflow.politicas.dto.DocumentAccessResponse;
import com.workflow.politicas.dto.DocumentCollaborationStateResponse;
import com.workflow.politicas.dto.DocumentEditorSessionResponse;
import com.workflow.politicas.dto.DocumentRecordResponse;
import com.workflow.politicas.dto.OnlyOfficeCallbackRequest;
import com.workflow.politicas.model.DocumentPermissionLevel;
import com.workflow.politicas.model.DocumentRecord;
import com.workflow.politicas.security.AuthenticatedActorResolver.Actor;
import com.workflow.politicas.storage.OnlyOfficeProperties;
import com.workflow.politicas.storage.StoredObject;
import com.workflow.politicas.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DocumentEditorService {

    private static final Logger log = LoggerFactory.getLogger(DocumentEditorService.class);

    private static final Set<String> EDITABLE_EXTENSIONS = Set.of("docx", "xlsx");

    private final DocumentRepositoryService documentRepositoryService;
    private final DocumentCollaborationService collaborationService;
    private final OnlyOfficeProperties onlyOfficeProperties;
    private final OnlyOfficeAccessTokenService accessTokenService;
    private final StorageService storageService;
    private final RestTemplate restTemplate;

    public DocumentEditorService(
            DocumentRepositoryService documentRepositoryService,
            DocumentCollaborationService collaborationService,
            OnlyOfficeProperties onlyOfficeProperties,
            OnlyOfficeAccessTokenService accessTokenService,
            StorageService storageService
    ) {
        this.documentRepositoryService = documentRepositoryService;
        this.collaborationService = collaborationService;
        this.onlyOfficeProperties = onlyOfficeProperties;
        this.accessTokenService = accessTokenService;
        this.storageService = storageService;
        this.restTemplate = new RestTemplate();
    }

    public DocumentEditorSessionResponse buildEditorSession(
            String documentId,
            String repositoryId,
            String sessionId,
            Actor actor
    ) {
        DocumentRecord record = documentRepositoryService.requireAvailableDocumentRecord(documentId);
        if (repositoryId != null && !repositoryId.equals(record.getRepositoryId())) {
            throw new IllegalArgumentException("El documento no pertenece a este repositorio");
        }

        collaborationService.requireDocumentPermission(record, DocumentPermissionLevel.READ);
        DocumentAccessResponse access = collaborationService.getDocumentAccess(documentId);
        DocumentCollaborationStateResponse collaboration = collaborationService.getState(record.getRepositoryId(), sessionId);

        DocumentEditorSessionResponse response = new DocumentEditorSessionResponse();
        response.setDocument(documentRepositoryService.toRecordResponsePublic(record));
        response.setAccess(access);
        response.setCollaboration(collaboration);
        response.setRecentActions(collaborationService.listRecentActions(record.getRepositoryId()));
        response.setReadOnly(!access.isCanEdit());

        boolean editableType = isEditableOfficeDocument(record);
        boolean onlyOfficeReady = onlyOfficeProperties.isEnabled() && editableType;
        response.setOnlyOfficeEnabled(onlyOfficeReady);
        response.setFallbackMode(!onlyOfficeReady);

        if (onlyOfficeReady) {
            response.setOnlyOfficeApiScriptUrl(onlyOfficeProperties.getDocumentServerApiScriptUrl());
            response.setOnlyOfficeConfig(buildOnlyOfficeConfig(record, actor, access.isCanEdit()));
        }

        collaborationService.auditDocumentOpened(record, actor.username());
        return response;
    }

    public DocumentCollaborationStateResponse startEdit(
            String documentId,
            String repositoryId,
            String sessionId,
            Actor actor
    ) {
        return collaborationService.registerEditStarted(documentId, repositoryId, sessionId, actor);
    }

    public DocumentCollaborationStateResponse closeEdit(
            String documentId,
            String repositoryId,
            String sessionId,
            Actor actor
    ) {
        return collaborationService.registerEditClosed(documentId, repositoryId, sessionId, actor);
    }

    public StoredObject loadDocumentForOnlyOffice(String documentId, String accessToken) {
        accessTokenService.validateToken(accessToken, documentId, "FILE");
        DocumentRecord record = documentRepositoryService.requireAvailableDocumentRecord(documentId);
        StoredObject stored = storageService.download(record.getS3Key());
        if (!stored.hasContent()) {
            throw new IllegalStateException("No se pudo leer el contenido del documento");
        }
        return stored;
    }

    public Map<String, Object> handleCallback(String documentId, String accessToken, OnlyOfficeCallbackRequest callback) {
        accessTokenService.validateToken(accessToken, documentId, "CALLBACK");
        DocumentRecord record = documentRepositoryService.requireAvailableDocumentRecord(documentId);
        int status = callback != null ? callback.getStatus() : 0;
        log.info("OnlyOffice callback documentId={} status={}", documentId, status);

        if (status == 2 || status == 6) {
            if (callback.getUrl() == null || callback.getUrl().isBlank()) {
                return callbackOk();
            }
            byte[] content = restTemplate.getForObject(callback.getUrl(), byte[].class);
            if (content == null || content.length == 0) {
                return callbackError("Contenido vacío desde OnlyOffice");
            }
            String username = collaborationService.resolveLockOwnerUsername(record.getRepositoryId(), record.getId())
                    .orElse("onlyoffice");
            DocumentRecordResponse saved = documentRepositoryService.saveNewVersionFromEditor(
                    documentId,
                    new ByteArrayInputStream(content),
                    content.length,
                    record.getContentType(),
                    username
            );
            collaborationService.registerEditSaved(record, saved, username);
        } else if (status == 4) {
            collaborationService.resolveLockOwnerUsername(record.getRepositoryId(), record.getId())
                    .ifPresent(username -> collaborationService.registerEditClosedWithoutSave(record, username));
        }
        return callbackOk();
    }

    public boolean isEditableOfficeDocument(DocumentRecord record) {
        return record != null && isEditableOfficeDocument(record.getExtension());
    }

    public boolean isEditableOfficeDocument(String extension) {
        if (extension == null) return false;
        return EDITABLE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    private Map<String, Object> buildOnlyOfficeConfig(DocumentRecord record, Actor actor, boolean canEdit) {
        String fileToken = accessTokenService.createFileAccessToken(record.getId(), actor.username());
        String callbackToken = accessTokenService.createCallbackToken(record.getId(), actor.username());
        String backend = trimSlash(onlyOfficeProperties.getBackendPublicUrl());
        String fileUrl = backend + "/api/document-repositories/documents/" + record.getId()
                + "/onlyoffice/file?accessToken=" + fileToken;
        String callbackUrl = backend + "/api/document-repositories/documents/" + record.getId()
                + "/onlyoffice/callback?accessToken=" + callbackToken;

        String ext = record.getExtension().toLowerCase(Locale.ROOT);
        String documentType = "xlsx".equals(ext) ? "cell" : "word";
        String key = resolveFamilyId(record) + "-v" + record.getVersion() + "-" + record.getId();

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileType", ext);
        document.put("key", key);
        document.put("title", record.getNombreOriginal());
        document.put("url", fileUrl);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", actor.userId() != null ? actor.userId() : actor.username());
        user.put("name", actor.displayName());

        Map<String, Object> customization = new LinkedHashMap<>();
        customization.put("forcesave", true);

        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("callbackUrl", callbackUrl);
        editorConfig.put("mode", canEdit ? "edit" : "view");
        editorConfig.put("lang", "es");
        editorConfig.put("user", user);
        editorConfig.put("customization", customization);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("documentType", documentType);
        config.put("document", document);
        config.put("editorConfig", editorConfig);
        config.put("height", "100%");
        config.put("width", "100%");
        config.put("type", "desktop");
        return config;
    }

    private String resolveFamilyId(DocumentRecord record) {
        if (record.getDocumentFamilyId() != null && !record.getDocumentFamilyId().isBlank()) {
            return record.getDocumentFamilyId();
        }
        return record.getId();
    }

    private String trimSlash(String value) {
        if (value == null) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private Map<String, Object> callbackOk() {
        Map<String, Object> body = new HashMap<>();
        body.put("error", 0);
        return body;
    }

    private Map<String, Object> callbackError(String message) {
        log.warn("OnlyOffice callback error: {}", message);
        Map<String, Object> body = new HashMap<>();
        body.put("error", 1);
        return body;
    }
}
