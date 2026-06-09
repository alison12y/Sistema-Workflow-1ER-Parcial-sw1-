package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.DocumentDownloadResponse;
import com.workflow.politicas.dto.DocumentRecordResponse;
import com.workflow.politicas.dto.DocumentRepositoryResponse;
import com.workflow.politicas.exception.DocumentRepositoryNotFoundException;
import com.workflow.politicas.model.DocumentPermissionLevel;
import com.workflow.politicas.model.DocumentRecord;
import com.workflow.politicas.model.DocumentRepository;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.security.AuthenticatedActorResolver;
import com.workflow.politicas.security.AuthenticatedActorResolver.Actor;
import com.workflow.politicas.repository.DocumentRecordRepository;
import com.workflow.politicas.repository.DocumentRepositoryStore;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.storage.DocumentMimeTypes;
import com.workflow.politicas.storage.DocumentStoragePathBuilder;
import com.workflow.politicas.storage.StorageProperties;
import com.workflow.politicas.storage.StorageService;
import com.workflow.politicas.storage.StoredObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentRepositoryService {

    public static final String REPOSITORY_NOT_FOUND_MESSAGE = "Repositorio documental no encontrado para el trámite";

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private final DocumentRepositoryStore documentRepositoryStore;
    private final DocumentRecordRepository documentRecordRepository;
    private final TramiteRepository tramiteRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final BitacoraService bitacoraService;
    private final DocumentCollaborationService documentCollaborationService;
    private final AuthenticatedActorResolver actorResolver;

    public DocumentRepositoryService(
            DocumentRepositoryStore documentRepositoryStore,
            DocumentRecordRepository documentRecordRepository,
            TramiteRepository tramiteRepository,
            StorageService storageService,
            StorageProperties storageProperties,
            BitacoraService bitacoraService,
            DocumentCollaborationService documentCollaborationService,
            AuthenticatedActorResolver actorResolver
    ) {
        this.documentRepositoryStore = documentRepositoryStore;
        this.documentRecordRepository = documentRecordRepository;
        this.tramiteRepository = tramiteRepository;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.bitacoraService = bitacoraService;
        this.documentCollaborationService = documentCollaborationService;
        this.actorResolver = actorResolver;
    }

    public DocumentRepositoryResponse findByTramiteId(String tramiteId) {
        DocumentRepository repository = documentRepositoryStore.findByTramiteId(tramiteId)
                .orElseThrow(() -> new DocumentRepositoryNotFoundException(REPOSITORY_NOT_FOUND_MESSAGE));
        return toRepositoryResponse(repository);
    }

    /**
     * Lista la versión actual de cada documento del repositorio (un registro ACTIVO por familia).
     */
    public List<DocumentRecordResponse> listDocuments(String repositoryId) {
        DocumentRepository repository = requireActiveRepository(repositoryId);
        return documentRecordRepository
                .findByRepositoryIdAndEstadoOrderByFechaSubidaDesc(
                        repository.getId(),
                        DocumentRecord.STATUS_ACTIVO
                )
                .stream()
                .map(this::toRecordResponse)
                .toList();
    }

    /**
     * Historial de versiones de un documento (familia documental).
     */
    public List<DocumentRecordResponse> listDocumentVersions(String documentId) {
        DocumentRecord anchor = requireAvailableDocument(documentId);
        String familyId = resolveFamilyId(anchor);
        return documentRecordRepository
                .findByDocumentFamilyIdAndEstadoNotOrderByVersionDesc(
                        familyId,
                        DocumentRecord.STATUS_ELIMINADO
                )
                .stream()
                .map(this::toRecordResponse)
                .toList();
    }

    public DocumentRecordResponse uploadDocument(
            String repositoryId,
            MultipartFile file,
            String username
    ) {
        DocumentRepository repository = requireActiveRepository(repositoryId);
        validateUploadFile(file);

        String documentId = UUID.randomUUID().toString();
        String originalName = DocumentStoragePathBuilder.sanitizeFileName(file.getOriginalFilename());
        String extension = extractExtension(originalName);
        String tramiteCodigo = resolveTramiteCodigo(repository);

        VersioningPlan plan = resolveVersioningPlan(repository.getId(), originalName, documentId);

        if (plan.version() > 1) {
            List<DocumentRecord> familyRecords = documentRecordRepository.findByRepositoryIdAndNombreOriginalAndEstadoNot(
                    repository.getId(), originalName, DocumentRecord.STATUS_ELIMINADO
            );
            DocumentRecord anchor = familyRecords.stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Familia documental no encontrada"));
            documentCollaborationService.requireDocumentPermission(anchor, DocumentPermissionLevel.EDIT);
            Actor actor = actorResolver.requireCurrentActor();
            documentCollaborationService.requireLockForVersionUpload(actor, repository.getId(), plan.familyId());
        }

        String storageFileName = DocumentStoragePathBuilder.buildStorageFileName(originalName, plan.version());
        String s3Key = DocumentStoragePathBuilder.buildObjectKey(tramiteCodigo, storageFileName, plan.version());

        try {
            StoredObject stored = storageService.upload(
                    s3Key,
                    file.getInputStream(),
                    file.getSize(),
                    DocumentMimeTypes.resolveContentType(extension, file.getContentType()),
                    buildUploadMetadata(
                            tramiteCodigo,
                            repository,
                            documentId,
                            plan.familyId(),
                            s3Key,
                            plan.version(),
                            username
                    )
            );

            supersedeCurrentVersions(plan.familyId());

            LocalDateTime now = LocalDateTime.now();
            DocumentRecord record = new DocumentRecord();
            record.setId(documentId);
            record.setDocumentFamilyId(plan.familyId());
            record.setRepositoryId(repository.getId());
            record.setTramiteId(repository.getTramiteId());
            record.setTramiteCodigo(tramiteCodigo);
            record.setNombreArchivo(storageFileName);
            record.setNombreOriginal(originalName);
            record.setExtension(extension);
            record.setContentType(stored.getContentType());
            record.setTamano(stored.getContentLength());
            record.setS3Key(s3Key);
            record.setBucket(stored.getBucket());
            record.setVersion(plan.version());
            record.setFechaSubida(now);
            record.setSubidoPor(username);
            record.setEstado(DocumentRecord.STATUS_ACTIVO);

            DocumentRecord saved = documentRecordRepository.save(record);

            documentCollaborationService.grantAdminToUploader(saved, username);
            if (plan.version() > 1) {
                documentCollaborationService.auditVersionUploaded(saved, username, plan.version());
                documentCollaborationService.releaseLockAfterUpload(
                        repository.getId(), plan.familyId(), username);
            } else {
                String versionLabel = plan.version() > 1 ? " (v" + plan.version() + ")" : "";
                bitacoraService.registrar(
                        username,
                        AuditModules.DOCUMENTOS,
                        AuditActions.DOCUMENT_UPLOADED,
                        "Documento subido: " + originalName + versionLabel + " en trámite " + repository.getTramiteCodigo(),
                        "DocumentRecord",
                        saved.getId()
                );
            }

            return toRecordResponse(saved);
        } catch (Exception exception) {
            throw new IllegalArgumentException("No se pudo subir el documento: " + exception.getMessage(), exception);
        }
    }

    public DocumentDownloadResponse getDocumentDownload(String documentId, String username) {
        DocumentRecord record = requireAvailableDocument(documentId);
        documentCollaborationService.requireDocumentPermission(record, DocumentPermissionLevel.READ);

        int expirationMinutes = storageProperties.getPresignedUrlExpirationMinutes();
        Duration expiration = Duration.ofMinutes(expirationMinutes);
        String presignedUrl = storageService.generatePresignedDownloadUrl(record.getS3Key(), expiration).toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        bitacoraService.registrar(
                username,
                AuditModules.DOCUMENTOS,
                AuditActions.DOCUMENT_DOWNLOADED,
                "Descarga solicitada: " + record.getNombreOriginal()
                        + " v" + record.getVersion()
                        + " (trámite " + record.getTramiteId() + ")",
                "DocumentRecord",
                record.getId()
        );
        documentCollaborationService.auditDocumentViewed(record, username);

        DocumentDownloadResponse response = new DocumentDownloadResponse();
        response.setDocumento(toRecordResponse(record));
        response.setPresignedDownloadUrl(presignedUrl);
        response.setUrlExpiraEn(expiresAt);
        response.setUrlExpiraEnMinutos(expirationMinutes);
        response.setStorageProvider(storageService.getProviderName());
        return response;
    }

    public DocumentRecordResponse deleteDocument(String documentId, String username) {
        DocumentRecord record = requireAvailableDocument(documentId);
        documentCollaborationService.requireDocumentPermission(record, DocumentPermissionLevel.ADMIN);

        if (!DocumentRecord.STATUS_ACTIVO.equalsIgnoreCase(record.getEstado())) {
            throw new IllegalArgumentException("Solo puede eliminar la versión actual del documento");
        }

        record.setEstado(DocumentRecord.STATUS_ELIMINADO);
        DocumentRecord saved = documentRecordRepository.save(record);

        bitacoraService.registrar(
                username,
                AuditModules.DOCUMENTOS,
                AuditActions.DOCUMENT_DELETED,
                "Documento eliminado (baja lógica): " + record.getNombreOriginal()
                        + " v" + record.getVersion()
                        + " (trámite " + record.getTramiteId() + ")",
                "DocumentRecord",
                saved.getId()
        );

        return toRecordResponse(saved);
    }

    public DocumentRecord requireAvailableDocumentRecord(String documentId) {
        return requireAvailableDocument(documentId);
    }

    public DocumentRecordResponse toRecordResponsePublic(DocumentRecord record) {
        return toRecordResponse(record);
    }

    public DocumentRecordResponse saveNewVersionFromEditor(
            String sourceDocumentId,
            java.io.InputStream inputStream,
            long size,
            String contentType,
            String username
    ) {
        DocumentRecord source = requireAvailableDocument(sourceDocumentId);
        if (!DocumentRecord.STATUS_ACTIVO.equalsIgnoreCase(source.getEstado())) {
            throw new IllegalArgumentException("Solo puede versionar la versión activa del documento");
        }
        if (size > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo no puede superar 10 MB");
        }

        DocumentRepository repository = requireActiveRepository(source.getRepositoryId());
        String originalName = source.getNombreOriginal();
        String extension = source.getExtension();
        String tramiteCodigo = resolveTramiteCodigo(repository);
        String newDocumentId = UUID.randomUUID().toString();
        VersioningPlan plan = resolveVersioningPlan(repository.getId(), originalName, newDocumentId);

        String storageFileName = DocumentStoragePathBuilder.buildStorageFileName(originalName, plan.version());
        String s3Key = DocumentStoragePathBuilder.buildObjectKey(tramiteCodigo, storageFileName, plan.version());

        try {
            StoredObject stored = storageService.upload(
                    s3Key,
                    inputStream,
                    size,
                    contentType != null ? contentType : DocumentMimeTypes.resolveContentType(extension, null),
                    buildUploadMetadata(
                            tramiteCodigo,
                            repository,
                            newDocumentId,
                            plan.familyId(),
                            s3Key,
                            plan.version(),
                            username
                    )
            );

            supersedeCurrentVersions(plan.familyId());

            LocalDateTime now = LocalDateTime.now();
            DocumentRecord record = new DocumentRecord();
            record.setId(newDocumentId);
            record.setDocumentFamilyId(plan.familyId());
            record.setRepositoryId(repository.getId());
            record.setTramiteId(repository.getTramiteId());
            record.setTramiteCodigo(tramiteCodigo);
            record.setNombreArchivo(storageFileName);
            record.setNombreOriginal(originalName);
            record.setExtension(extension);
            record.setContentType(stored.getContentType());
            record.setTamano(stored.getContentLength());
            record.setS3Key(s3Key);
            record.setBucket(stored.getBucket());
            record.setVersion(plan.version());
            record.setFechaSubida(now);
            record.setSubidoPor(username);
            record.setEstado(DocumentRecord.STATUS_ACTIVO);

            return toRecordResponse(documentRecordRepository.save(record));
        } catch (Exception exception) {
            throw new IllegalArgumentException("No se pudo guardar la nueva versión: " + exception.getMessage(), exception);
        }
    }

    public DocumentRepository createForTramite(Tramite tramite, String createdBy) {
        if (tramite == null || tramite.getId() == null || tramite.getId().isBlank()) {
            throw new IllegalArgumentException("Trámite inválido para crear repositorio documental");
        }

        return documentRepositoryStore.findByTramiteId(tramite.getId()).orElseGet(() -> {
            DocumentRepository repository = new DocumentRepository();
            repository.setTramiteId(tramite.getId());
            repository.setTramiteCodigo(tramite.getCode());
            repository.setNombre("Repositorio documental — " + tramite.getCode());
            repository.setDescripcion(
                    "Documentos asociados al trámite " + tramite.getCode()
                            + (tramite.getPolicyName() != null ? " (" + tramite.getPolicyName() + ")" : "")
            );
            repository.setFechaCreacion(LocalDateTime.now());
            repository.setCreadoPor(createdBy != null && !createdBy.isBlank() ? createdBy : "system");
            repository.setEstado(DocumentRepository.STATUS_ACTIVO);
            return documentRepositoryStore.save(repository);
        });
    }

    private VersioningPlan resolveVersioningPlan(String repositoryId, String originalName, String newDocumentId) {
        List<DocumentRecord> familyRecords = documentRecordRepository.findByRepositoryIdAndNombreOriginalAndEstadoNot(
                repositoryId,
                originalName,
                DocumentRecord.STATUS_ELIMINADO
        );

        if (familyRecords.isEmpty()) {
            return new VersioningPlan(newDocumentId, 1);
        }

        String familyId = familyRecords.stream()
                .map(this::resolveFamilyId)
                .findFirst()
                .orElse(newDocumentId);

        int nextVersion = familyRecords.stream()
                .mapToInt(DocumentRecord::getVersion)
                .max()
                .orElse(0) + 1;

        return new VersioningPlan(familyId, nextVersion);
    }

    private void supersedeCurrentVersions(String familyId) {
        documentRecordRepository.findByDocumentFamilyIdAndEstadoNotOrderByVersionDesc(
                        familyId,
                        DocumentRecord.STATUS_ELIMINADO
                )
                .stream()
                .filter(record -> DocumentRecord.STATUS_ACTIVO.equalsIgnoreCase(record.getEstado()))
                .forEach(record -> {
                    record.setEstado(DocumentRecord.STATUS_HISTORICO);
                    documentRecordRepository.save(record);
                });
    }

    private Map<String, String> buildUploadMetadata(
            String tramiteCodigo,
            DocumentRepository repository,
            String documentId,
            String familyId,
            String s3Key,
            int version,
            String username
    ) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("tramite-codigo", tramiteCodigo);
        metadata.put("tramite-id", repository.getTramiteId());
        metadata.put("repository-id", repository.getId());
        metadata.put("document-id", documentId);
        metadata.put("document-family-id", familyId);
        metadata.put("s3-key", s3Key);
        metadata.put("version", String.valueOf(version));
        metadata.put("uploaded-by", username);
        return metadata;
    }

    private String resolveFamilyId(DocumentRecord record) {
        if (record.getDocumentFamilyId() != null && !record.getDocumentFamilyId().isBlank()) {
            return record.getDocumentFamilyId();
        }
        return record.getId();
    }

    private DocumentRepository requireActiveRepository(String repositoryId) {
        DocumentRepository repository = documentRepositoryStore.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repositorio documental no encontrado"));

        if (!DocumentRepository.STATUS_ACTIVO.equalsIgnoreCase(repository.getEstado())) {
            throw new IllegalArgumentException("El repositorio documental no está activo");
        }

        tramiteRepository.findById(repository.getTramiteId())
                .orElseThrow(() -> new IllegalArgumentException("El trámite asociado al repositorio no existe"));

        return repository;
    }

    private DocumentRecord requireAvailableDocument(String documentId) {
        DocumentRecord record = documentRecordRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        String estado = record.getEstado() != null ? record.getEstado().toUpperCase(Locale.ROOT) : "";
        if (DocumentRecord.STATUS_ELIMINADO.equals(estado)) {
            throw new IllegalArgumentException("El documento no está disponible");
        }

        return record;
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo no puede superar 10 MB");
        }
    }

    private String resolveTramiteCodigo(DocumentRepository repository) {
        if (repository.getTramiteCodigo() != null && !repository.getTramiteCodigo().isBlank()) {
            return DocumentStoragePathBuilder.normalizeTramiteCodigo(repository.getTramiteCodigo());
        }
        return tramiteRepository.findById(repository.getTramiteId())
                .map(Tramite::getCode)
                .map(DocumentStoragePathBuilder::normalizeTramiteCodigo)
                .orElseThrow(() -> new IllegalArgumentException("No se pudo resolver el código del trámite"));
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private DocumentRepositoryResponse toRepositoryResponse(DocumentRepository repository) {
        DocumentRepositoryResponse response = new DocumentRepositoryResponse();
        response.setId(repository.getId());
        response.setTramiteId(repository.getTramiteId());
        response.setTramiteCodigo(repository.getTramiteCodigo());
        response.setNombre(repository.getNombre());
        response.setDescripcion(repository.getDescripcion());
        response.setFechaCreacion(repository.getFechaCreacion());
        response.setCreadoPor(repository.getCreadoPor());
        response.setEstado(repository.getEstado());
        return response;
    }

    private DocumentRecordResponse toRecordResponse(DocumentRecord record) {
        DocumentRecordResponse response = new DocumentRecordResponse();
        response.setId(record.getId());
        response.setDocumentFamilyId(resolveFamilyId(record));
        response.setRepositoryId(record.getRepositoryId());
        response.setTramiteId(record.getTramiteId());
        response.setTramiteCodigo(record.getTramiteCodigo());
        response.setNombreArchivo(record.getNombreArchivo());
        response.setNombreOriginal(record.getNombreOriginal());
        response.setExtension(record.getExtension());
        response.setContentType(record.getContentType());
        response.setTamano(record.getTamano());
        response.setS3Key(record.getS3Key());
        response.setBucket(record.getBucket());
        response.setVersion(record.getVersion());
        response.setFechaSubida(record.getFechaSubida());
        response.setSubidoPor(record.getSubidoPor());
        response.setEstado(record.getEstado());
        return response;
    }

    private record VersioningPlan(String familyId, int version) {
    }
}

