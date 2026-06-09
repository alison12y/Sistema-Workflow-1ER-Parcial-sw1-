package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.DocumentAccessResponse;
import com.workflow.politicas.dto.DocumentCollaborationActiveLockDto;
import com.workflow.politicas.dto.DocumentCollaborationConnectedUserDto;
import com.workflow.politicas.dto.DocumentCollaborationLockRequest;
import com.workflow.politicas.dto.DocumentCollaborationStateResponse;
import com.workflow.politicas.dto.DocumentPermissionRequest;
import com.workflow.politicas.dto.DocumentPermissionResponse;
import com.workflow.politicas.dto.DocumentRecordResponse;
import com.workflow.politicas.exception.DocumentAccessDeniedException;
import com.workflow.politicas.model.DocumentActiveLock;
import com.workflow.politicas.model.DocumentCollaborationMeta;
import com.workflow.politicas.model.DocumentEditorSession;
import com.workflow.politicas.model.DocumentPermission;
import com.workflow.politicas.model.DocumentPermissionLevel;
import com.workflow.politicas.model.DocumentRecord;
import com.workflow.politicas.model.DocumentRepository;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.DocumentCollaborationRepository;
import com.workflow.politicas.repository.DocumentPermissionRepository;
import com.workflow.politicas.repository.DocumentRecordRepository;
import com.workflow.politicas.repository.DocumentRepositoryStore;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.security.AuthenticatedActorResolver;
import com.workflow.politicas.dto.DocumentCollaborationRecentActionDto;
import com.workflow.politicas.model.DocumentCollaborationRecentAction;
import com.workflow.politicas.security.AuthenticatedActorResolver.Actor;
import com.workflow.politicas.security.SystemPermissions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentCollaborationService {

    static final int PRESENCE_TTL_SECONDS = 30;
    private static final int RECENT_ACTIONS_MAX = 10;

    private static final Set<String> VALID_GRANTEE_TYPES = Set.of(
            DocumentPermission.GRANTEE_USER,
            DocumentPermission.GRANTEE_ROLE,
            DocumentPermission.GRANTEE_DEPARTMENT
    );

    private final DocumentCollaborationRepository collaborationRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final DocumentRepositoryStore documentRepositoryStore;
    private final DocumentRecordRepository documentRecordRepository;
    private final TramiteRepository tramiteRepository;
    private final UserRepository userRepository;
    private final BitacoraService bitacoraService;
    private final AuthenticatedActorResolver actorResolver;

    public DocumentCollaborationService(
            DocumentCollaborationRepository collaborationRepository,
            DocumentPermissionRepository permissionRepository,
            DocumentRepositoryStore documentRepositoryStore,
            DocumentRecordRepository documentRecordRepository,
            TramiteRepository tramiteRepository,
            UserRepository userRepository,
            BitacoraService bitacoraService,
            AuthenticatedActorResolver actorResolver
    ) {
        this.collaborationRepository = collaborationRepository;
        this.permissionRepository = permissionRepository;
        this.documentRepositoryStore = documentRepositoryStore;
        this.documentRecordRepository = documentRecordRepository;
        this.tramiteRepository = tramiteRepository;
        this.userRepository = userRepository;
        this.bitacoraService = bitacoraService;
        this.actorResolver = actorResolver;
    }

    public DocumentCollaborationStateResponse getState(String repositoryId, String sessionId) {
        DocumentRepository repository = requireRepository(repositoryId);
        Actor actor = actorResolver.requireCurrentActor();
        DocumentCollaborationMeta meta = loadOrCreate(repository);
        pruneStale(meta);
        if (sessionId != null && !sessionId.isBlank()) {
            touchSession(meta, sessionId, actor, LocalDateTime.now());
            refreshLockHeartbeat(meta, sessionId, LocalDateTime.now());
        }
        collaborationRepository.save(meta);
        return toResponse(meta, actor.username());
    }

    public DocumentCollaborationStateResponse registerOpen(String repositoryId, String sessionId) {
        DocumentRepository repository = requireRepository(repositoryId);
        requireSessionId(sessionId);
        Actor actor = actorResolver.requireCurrentActor();
        DocumentCollaborationMeta meta = loadOrCreate(repository);
        pruneStale(meta);
        upsertSession(meta, sessionId, actor, LocalDateTime.now());
        collaborationRepository.save(meta);
        audit(actor.username(), AuditActions.DOCUMENT_OPENED,
                "Abrió documentos del trámite " + repository.getTramiteId(), repository.getTramiteId(), null);
        return toResponse(meta, actor.username());
    }

    public DocumentCollaborationStateResponse heartbeat(String repositoryId, String sessionId) {
        requireRepository(repositoryId);
        requireSessionId(sessionId);
        Actor actor = actorResolver.requireCurrentActor();
        DocumentCollaborationMeta meta = loadOrCreate(requireRepository(repositoryId));
        pruneStale(meta);
        LocalDateTime now = LocalDateTime.now();
        touchSession(meta, sessionId, actor, now);
        refreshLockHeartbeat(meta, sessionId, now);
        collaborationRepository.save(meta);
        return toResponse(meta, actor.username());
    }

    public void registerClose(String repositoryId, String sessionId) {
        if (repositoryId == null || sessionId == null || sessionId.isBlank()) return;
        collaborationRepository.findById(repositoryId).ifPresent(meta -> {
            meta.getActiveSessions().removeIf(s -> sessionId.equals(s.getSessionId()));
            meta.getActiveLocks().removeIf(l -> sessionId.equals(l.getSessionId()));
            collaborationRepository.save(meta);
        });
    }

    public DocumentCollaborationStateResponse acquireLock(String repositoryId, DocumentCollaborationLockRequest request) {
        requireRepository(repositoryId);
        requireSessionId(request.getSessionId());
        DocumentRecord record = requireAvailableDocument(request.getDocumentId());
        assertSameRepository(record, repositoryId);
        Actor actor = actorResolver.requireCurrentActor();
        requireDocumentPermission(record, DocumentPermissionLevel.EDIT);

        DocumentCollaborationMeta meta = loadOrCreate(requireRepository(repositoryId));
        pruneStale(meta);
        LocalDateTime now = LocalDateTime.now();
        touchSession(meta, request.getSessionId(), actor, now);

        String familyId = resolveFamilyId(record);
        Optional<DocumentActiveLock> existing = findLock(meta, familyId);
        if (existing.isPresent() && !sameUser(existing.get(), actor)) {
            throw new IllegalStateException("Documento en edición por "
                    + label(existing.get().getDisplayName(), existing.get().getUsername()));
        }

        meta.getActiveLocks().removeIf(l -> familyId.equals(l.getDocumentFamilyId()));
        DocumentActiveLock lock = new DocumentActiveLock();
        lock.setSessionId(request.getSessionId());
        lock.setUserId(actor.userId());
        lock.setUsername(actor.username());
        lock.setDisplayName(actor.displayName());
        lock.setDocumentFamilyId(familyId);
        lock.setDocumentId(record.getId());
        lock.setDocumentName(record.getNombreOriginal());
        lock.setLockedAt(now);
        lock.setLastSeenAt(now);
        meta.getActiveLocks().add(lock);
        collaborationRepository.save(meta);

        audit(actor.username(), AuditActions.DOCUMENT_LOCK_ACQUIRED,
                "Tomó edición de " + record.getNombreOriginal(), record.getTramiteId(), record.getId());
        return toResponse(meta, actor.username());
    }

    public DocumentCollaborationStateResponse releaseLock(
            String repositoryId, String documentFamilyId, String sessionId, boolean force
    ) {
        requireRepository(repositoryId);
        requireSessionId(sessionId);
        Actor actor = actorResolver.requireCurrentActor();
        DocumentCollaborationMeta meta = collaborationRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Colaboración no iniciada"));
        DocumentActiveLock lock = findLock(meta, documentFamilyId)
                .orElseThrow(() -> new IllegalArgumentException("No hay bloqueo activo"));
        DocumentRecord record = documentRecordRepository.findById(lock.getDocumentId()).orElse(null);

        if (force) {
            if (record != null) requireDocumentPermission(record, DocumentPermissionLevel.ADMIN);
        } else if (!sameUser(lock, actor)) {
            if (record == null || !resolveEffectivePermission(actor, record).satisfies(DocumentPermissionLevel.ADMIN)) {
                deny(actor, record, "Solo quien tiene el bloqueo o un ADMIN puede liberarlo");
            }
        }

        meta.getActiveLocks().removeIf(l -> documentFamilyId.equals(l.getDocumentFamilyId()));
        collaborationRepository.save(meta);
        audit(actor.username(), AuditActions.DOCUMENT_LOCK_RELEASED,
                "Liberó edición de " + lock.getDocumentName(), meta.getTramiteId(), lock.getDocumentId());
        return toResponse(meta, actor.username());
    }

    public void requireLockForVersionUpload(Actor actor, String repositoryId, String documentFamilyId) {
        DocumentCollaborationMeta meta = collaborationRepository.findById(repositoryId).orElse(null);
        if (meta == null) throw new IllegalStateException("Debe tomar edición antes de subir una nueva versión");
        pruneStale(meta);
        collaborationRepository.save(meta);
        DocumentActiveLock lock = findLock(meta, documentFamilyId).orElse(null);
        if (lock == null || !sameUser(lock, actor)) {
            if (lock != null) {
                throw new IllegalStateException("Documento en edición por "
                        + label(lock.getDisplayName(), lock.getUsername()));
            }
            throw new IllegalStateException("Debe tomar edición antes de subir una nueva versión");
        }
    }

    public void releaseLockAfterUpload(String repositoryId, String documentFamilyId, String username) {
        collaborationRepository.findById(repositoryId).ifPresent(meta -> {
            meta.getActiveLocks().removeIf(l ->
                    documentFamilyId.equals(l.getDocumentFamilyId())
                            && username.equalsIgnoreCase(l.getUsername()));
            collaborationRepository.save(meta);
        });
    }

    public void requireDocumentPermission(DocumentRecord record, DocumentPermissionLevel required) {
        Actor actor = actorResolver.requireCurrentActor();
        if (!resolveEffectivePermission(actor, record).satisfies(required)) {
            deny(actor, record, "Permiso " + required.name() + " requerido");
        }
    }

    public DocumentPermissionLevel resolveEffectivePermission(Actor actor, DocumentRecord record) {
        if (isSystemAdmin()) return DocumentPermissionLevel.ADMIN;

        DocumentPermissionLevel level = resolveSystemDocumentPermission();
        DocumentPermissionLevel explicit = resolveExplicitPermissions(actor, resolveFamilyId(record));
        if (explicit != null) level = DocumentPermissionLevel.max(level, explicit);

        if (actor.username() != null && record.getSubidoPor() != null
                && actor.username().equalsIgnoreCase(record.getSubidoPor())) {
            level = DocumentPermissionLevel.max(level, DocumentPermissionLevel.ADMIN);
        }
        if (isTramiteParticipant(actor.username(), record.getTramiteId())) {
            level = DocumentPermissionLevel.max(level, DocumentPermissionLevel.READ);
        }
        return level;
    }

    public DocumentAccessResponse getDocumentAccess(String documentId) {
        DocumentRecord record = requireAvailableDocument(documentId);
        Actor actor = actorResolver.requireCurrentActor();
        DocumentPermissionLevel level = resolveEffectivePermission(actor, record);
        String familyId = resolveFamilyId(record);

        DocumentAccessResponse response = new DocumentAccessResponse();
        response.setDocumentId(record.getId());
        response.setDocumentFamilyId(familyId);
        response.setPermissionLevel(level.name());
        response.setCanRead(level.satisfies(DocumentPermissionLevel.READ));
        response.setCanEdit(level.satisfies(DocumentPermissionLevel.EDIT));
        response.setCanAdmin(level.satisfies(DocumentPermissionLevel.ADMIN));

        collaborationRepository.findById(record.getRepositoryId()).ifPresent(meta -> {
            pruneStale(meta);
            findLock(meta, familyId).ifPresent(lock -> {
                response.setLocked(true);
                response.setLockedByUsername(lock.getUsername());
                response.setLockedByDisplayName(lock.getDisplayName());
                response.setLockHeldByCurrentUser(sameUser(lock, actor));
            });
            collaborationRepository.save(meta);
        });

        boolean lockOk = !response.isLocked() || response.isLockHeldByCurrentUser();
        response.setCanUploadVersion(level.satisfies(DocumentPermissionLevel.EDIT) && lockOk);
        return response;
    }

    public List<DocumentPermissionResponse> listPermissions(String documentId) {
        DocumentRecord record = requireAvailableDocument(documentId);
        requireDocumentPermission(record, DocumentPermissionLevel.ADMIN);
        return permissionRepository.findByDocumentFamilyId(resolveFamilyId(record)).stream()
                .map(this::toPermissionResponse).toList();
    }

    public DocumentPermissionResponse grantOrUpdatePermission(String documentId, DocumentPermissionRequest request) {
        DocumentRecord record = requireAvailableDocument(documentId);
        Actor actor = actorResolver.requireCurrentActor();
        requireDocumentPermission(record, DocumentPermissionLevel.ADMIN);
        validatePermissionRequest(request);

        String familyId = resolveFamilyId(record);
        String granteeType = request.getGranteeType().trim().toUpperCase(Locale.ROOT);
        String granteeKey = request.getGranteeKey().trim();
        DocumentPermissionLevel newLevel = DocumentPermissionLevel.fromString(request.getPermissionLevel());

        Optional<DocumentPermission> existing = permissionRepository
                .findByDocumentFamilyIdAndGranteeTypeAndGranteeKey(familyId, granteeType, granteeKey);
        DocumentPermission permission;
        String action;
        if (existing.isPresent()) {
            permission = existing.get();
            permission.setPermissionLevel(newLevel.name());
            permission.setGranteeLabel(request.getGranteeLabel());
            permission.setGrantedBy(actor.username());
            permission.setGrantedAt(LocalDateTime.now());
            action = AuditActions.DOCUMENT_PERMISSION_CHANGED;
        } else {
            permission = new DocumentPermission();
            permission.setId(UUID.randomUUID().toString());
            permission.setDocumentFamilyId(familyId);
            permission.setRepositoryId(record.getRepositoryId());
            permission.setTramiteId(record.getTramiteId());
            permission.setGranteeType(granteeType);
            permission.setGranteeKey(granteeKey);
            permission.setGranteeLabel(request.getGranteeLabel());
            permission.setPermissionLevel(newLevel.name());
            permission.setGrantedBy(actor.username());
            permission.setGrantedAt(LocalDateTime.now());
            action = AuditActions.DOCUMENT_PERMISSION_GRANTED;
        }
        DocumentPermission saved = permissionRepository.save(permission);
        audit(actor.username(), action, newLevel.name() + " para " + granteeType + " " + granteeKey
                + " en " + record.getNombreOriginal(), record.getTramiteId(), record.getId());
        return toPermissionResponse(saved);
    }

    public void removePermission(String documentId, DocumentPermissionRequest request) {
        DocumentRecord record = requireAvailableDocument(documentId);
        Actor actor = actorResolver.requireCurrentActor();
        requireDocumentPermission(record, DocumentPermissionLevel.ADMIN);
        validatePermissionRequest(request);
        permissionRepository.deleteByDocumentFamilyIdAndGranteeTypeAndGranteeKey(
                resolveFamilyId(record),
                request.getGranteeType().trim().toUpperCase(Locale.ROOT),
                request.getGranteeKey().trim()
        );
        audit(actor.username(), AuditActions.DOCUMENT_PERMISSION_REMOVED,
                "Permiso removido para " + request.getGranteeKey(), record.getTramiteId(), record.getId());
    }

    public void grantAdminToUploader(DocumentRecord record, String username) {
        String familyId = resolveFamilyId(record);
        Optional<DocumentPermission> existing = permissionRepository
                .findByDocumentFamilyIdAndGranteeTypeAndGranteeKey(
                        familyId, DocumentPermission.GRANTEE_USER, username);
        if (existing.isPresent()) {
            DocumentPermission p = existing.get();
            if (!DocumentPermissionLevel.fromString(p.getPermissionLevel()).satisfies(DocumentPermissionLevel.ADMIN)) {
                p.setPermissionLevel(DocumentPermissionLevel.ADMIN.name());
                p.setGrantedBy(username);
                p.setGrantedAt(LocalDateTime.now());
                permissionRepository.save(p);
            }
            return;
        }
        DocumentPermission p = new DocumentPermission();
        p.setId(UUID.randomUUID().toString());
        p.setDocumentFamilyId(familyId);
        p.setRepositoryId(record.getRepositoryId());
        p.setTramiteId(record.getTramiteId());
        p.setGranteeType(DocumentPermission.GRANTEE_USER);
        p.setGranteeKey(username);
        p.setGranteeLabel(username);
        p.setPermissionLevel(DocumentPermissionLevel.ADMIN.name());
        p.setGrantedBy(username);
        p.setGrantedAt(LocalDateTime.now());
        permissionRepository.save(p);
        audit(username, AuditActions.DOCUMENT_PERMISSION_GRANTED,
                "ADMIN automático al subir " + record.getNombreOriginal(), record.getTramiteId(), record.getId());
    }

    public void auditVersionUploaded(DocumentRecord record, String username, int version) {
        audit(username, AuditActions.DOCUMENT_VERSION_UPLOADED,
                "Nueva versión v" + version + " de " + record.getNombreOriginal(),
                record.getTramiteId(), record.getId());
    }

    public void auditDocumentViewed(DocumentRecord record, String username) {
        audit(username, AuditActions.DOCUMENT_VIEWED,
                "Consultó " + record.getNombreOriginal() + " v" + record.getVersion(),
                record.getTramiteId(), record.getId());
    }

    public void auditDocumentOpened(DocumentRecord record, String username) {
        audit(username, AuditActions.DOCUMENT_OPENED,
                "Abrió edición documental de " + record.getNombreOriginal(),
                record.getTramiteId(), record.getId());
        appendRecentAction(record.getRepositoryId(), username, username, AuditActions.DOCUMENT_OPENED,
                "Abrió edición de " + record.getNombreOriginal(), record.getId(), record.getNombreOriginal());
    }

    public DocumentCollaborationStateResponse registerEditStarted(
            String documentId,
            String repositoryId,
            String sessionId,
            Actor actor
    ) {
        DocumentRecord record = requireAvailableDocument(documentId);
        assertSameRepository(record, repositoryId);
        requireDocumentPermission(record, DocumentPermissionLevel.EDIT);

        DocumentCollaborationLockRequest lockRequest = new DocumentCollaborationLockRequest();
        lockRequest.setSessionId(sessionId);
        lockRequest.setDocumentFamilyId(resolveFamilyId(record));
        lockRequest.setDocumentId(record.getId());
        lockRequest.setDocumentName(record.getNombreOriginal());
        DocumentCollaborationStateResponse state = acquireLock(repositoryId, lockRequest);

        audit(actor.username(), AuditActions.DOCUMENT_EDIT_STARTED,
                "Inició edición de " + record.getNombreOriginal(), record.getTramiteId(), record.getId());
        appendRecentAction(record.getRepositoryId(), actor.username(), actor.displayName(),
                AuditActions.DOCUMENT_EDIT_STARTED,
                actor.displayName() + " inició edición de " + record.getNombreOriginal(),
                record.getId(), record.getNombreOriginal());
        return state;
    }

    public DocumentCollaborationStateResponse registerEditClosed(
            String documentId,
            String repositoryId,
            String sessionId,
            Actor actor
    ) {
        DocumentRecord record = requireAvailableDocument(documentId);
        DocumentCollaborationStateResponse state = releaseLock(
                repositoryId, resolveFamilyId(record), sessionId, false);
        audit(actor.username(), AuditActions.DOCUMENT_LOCK_RELEASED,
                "Cerró edición de " + record.getNombreOriginal(), record.getTramiteId(), record.getId());
        appendRecentAction(record.getRepositoryId(), actor.username(), actor.displayName(),
                AuditActions.DOCUMENT_LOCK_RELEASED,
                actor.displayName() + " cerró edición de " + record.getNombreOriginal(),
                record.getId(), record.getNombreOriginal());
        return state;
    }

    public void registerEditSaved(DocumentRecord previous, DocumentRecordResponse saved, String username) {
        audit(username, AuditActions.DOCUMENT_EDIT_SAVED,
                "Guardó cambios de " + previous.getNombreOriginal() + " → v" + saved.getVersion(),
                previous.getTramiteId(), saved.getId());
        auditVersionUploaded(requireAvailableDocument(saved.getId()), username, saved.getVersion());
        releaseLockAfterUpload(previous.getRepositoryId(), resolveFamilyId(previous), username);
        appendRecentAction(previous.getRepositoryId(), username, username, AuditActions.DOCUMENT_EDIT_SAVED,
                "Nueva versión v" + saved.getVersion() + " de " + saved.getNombreOriginal(),
                saved.getId(), saved.getNombreOriginal());
        appendRecentAction(previous.getRepositoryId(), username, username, AuditActions.DOCUMENT_LOCK_RELEASED,
                "Edición liberada tras guardar " + saved.getNombreOriginal(),
                saved.getId(), saved.getNombreOriginal());
    }

    public void registerEditClosedWithoutSave(DocumentRecord record, String username) {
        releaseLockAfterUpload(record.getRepositoryId(), resolveFamilyId(record), username);
        appendRecentAction(record.getRepositoryId(), username, username, AuditActions.DOCUMENT_LOCK_RELEASED,
                "Edición cerrada sin cambios: " + record.getNombreOriginal(),
                record.getId(), record.getNombreOriginal());
    }

    public Optional<String> resolveLockOwnerUsername(String repositoryId, String documentId) {
        DocumentRecord record = requireAvailableDocument(documentId);
        return collaborationRepository.findById(repositoryId)
                .flatMap(meta -> findLock(meta, resolveFamilyId(record)))
                .map(DocumentActiveLock::getUsername);
    }

    public List<DocumentCollaborationRecentActionDto> listRecentActions(String repositoryId) {
        return collaborationRepository.findById(repositoryId)
                .map(meta -> meta.getRecentActions().stream().map(this::toRecentActionDto).toList())
                .orElse(List.of());
    }

    private DocumentCollaborationRecentActionDto toRecentActionDto(DocumentCollaborationRecentAction action) {
        DocumentCollaborationRecentActionDto dto = new DocumentCollaborationRecentActionDto();
        dto.setActionType(action.getActionType());
        dto.setActionLabel(action.getActionLabel());
        dto.setDocumentId(action.getDocumentId());
        dto.setDocumentName(action.getDocumentName());
        dto.setUsername(action.getUsername());
        dto.setDisplayName(action.getDisplayName());
        dto.setOccurredAt(action.getOccurredAt());
        return dto;
    }

    private void appendRecentAction(
            String repositoryId,
            String username,
            String displayName,
            String actionType,
            String actionLabel,
            String documentId,
            String documentName
    ) {
        collaborationRepository.findById(repositoryId).ifPresent(meta -> {
            if (meta.getRecentActions() == null) {
                meta.setRecentActions(new ArrayList<>());
            }
            DocumentCollaborationRecentAction action = new DocumentCollaborationRecentAction();
            action.setActionType(actionType);
            action.setActionLabel(actionLabel);
            action.setDocumentId(documentId);
            action.setDocumentName(documentName);
            action.setUsername(username);
            action.setDisplayName(displayName);
            action.setOccurredAt(LocalDateTime.now());
            meta.getRecentActions().add(0, action);
            while (meta.getRecentActions().size() > RECENT_ACTIONS_MAX) {
                meta.getRecentActions().remove(meta.getRecentActions().size() - 1);
            }
            collaborationRepository.save(meta);
        });
    }

    private DocumentCollaborationMeta loadOrCreate(DocumentRepository repository) {
        return collaborationRepository.findById(repository.getId()).orElseGet(() -> {
            DocumentCollaborationMeta meta = new DocumentCollaborationMeta();
            meta.setRepositoryId(repository.getId());
            meta.setTramiteId(repository.getTramiteId());
            return collaborationRepository.save(meta);
        });
    }

    private DocumentRepository requireRepository(String repositoryId) {
        DocumentRepository repository = documentRepositoryStore.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repositorio documental no encontrado"));
        if (!DocumentRepository.STATUS_ACTIVO.equalsIgnoreCase(repository.getEstado())) {
            throw new IllegalArgumentException("El repositorio documental no está activo");
        }
        return repository;
    }

    private DocumentRecord requireAvailableDocument(String documentId) {
        DocumentRecord record = documentRecordRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
        if (DocumentRecord.STATUS_ELIMINADO.equalsIgnoreCase(
                record.getEstado() != null ? record.getEstado() : "")) {
            throw new IllegalArgumentException("El documento no está disponible");
        }
        return record;
    }

    private void assertSameRepository(DocumentRecord record, String repositoryId) {
        if (!repositoryId.equals(record.getRepositoryId())) {
            throw new IllegalArgumentException("El documento no pertenece a este repositorio");
        }
    }

    private void pruneStale(DocumentCollaborationMeta meta) {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(PRESENCE_TTL_SECONDS);
        meta.getActiveSessions().removeIf(s -> s.getLastSeenAt() == null || s.getLastSeenAt().isBefore(threshold));
        meta.getActiveLocks().removeIf(l -> l.getLastSeenAt() == null || l.getLastSeenAt().isBefore(threshold));
    }

    private void upsertSession(DocumentCollaborationMeta meta, String sessionId, Actor actor, LocalDateTime now) {
        for (DocumentEditorSession session : meta.getActiveSessions()) {
            if (sessionId.equals(session.getSessionId())) {
                session.setLastSeenAt(now);
                session.setDisplayName(actor.displayName());
                return;
            }
        }
        DocumentEditorSession session = new DocumentEditorSession();
        session.setSessionId(sessionId);
        session.setUserId(actor.userId());
        session.setUsername(actor.username());
        session.setDisplayName(actor.displayName());
        session.setOpenedAt(now);
        session.setLastSeenAt(now);
        meta.getActiveSessions().add(session);
    }

    private void touchSession(DocumentCollaborationMeta meta, String sessionId, Actor actor, LocalDateTime now) {
        boolean found = false;
        for (DocumentEditorSession session : meta.getActiveSessions()) {
            if (sessionId.equals(session.getSessionId())) {
                session.setLastSeenAt(now);
                session.setDisplayName(actor.displayName());
                found = true;
                break;
            }
        }
        if (!found) upsertSession(meta, sessionId, actor, now);
    }

    private void refreshLockHeartbeat(DocumentCollaborationMeta meta, String sessionId, LocalDateTime now) {
        for (DocumentActiveLock lock : meta.getActiveLocks()) {
            if (sessionId.equals(lock.getSessionId())) lock.setLastSeenAt(now);
        }
    }

    private Optional<DocumentActiveLock> findLock(DocumentCollaborationMeta meta, String familyId) {
        return meta.getActiveLocks().stream().filter(l -> familyId.equals(l.getDocumentFamilyId())).findFirst();
    }

    private DocumentCollaborationStateResponse toResponse(DocumentCollaborationMeta meta, String currentUsername) {
        DocumentCollaborationStateResponse response = new DocumentCollaborationStateResponse();
        response.setRepositoryId(meta.getRepositoryId());
        response.setTramiteId(meta.getTramiteId());
        response.setCurrentUsername(currentUsername);
        response.setConnectedUsers(buildConnectedUsers(meta.getActiveSessions()));
        response.setActiveLocks(meta.getActiveLocks().stream().map(this::toLockDto).toList());
        response.setRecentActions(meta.getRecentActions() != null
                ? meta.getRecentActions().stream().map(this::toRecentActionDto).toList()
                : List.of());
        return response;
    }

    private List<DocumentCollaborationConnectedUserDto> buildConnectedUsers(List<DocumentEditorSession> sessions) {
        Map<String, DocumentCollaborationConnectedUserDto> grouped = new LinkedHashMap<>();
        for (DocumentEditorSession session : sessions) {
            String key = session.getUsername() != null
                    ? session.getUsername().toLowerCase(Locale.ROOT) : session.getSessionId();
            DocumentCollaborationConnectedUserDto dto = grouped.computeIfAbsent(key, k -> {
                DocumentCollaborationConnectedUserDto c = new DocumentCollaborationConnectedUserDto();
                c.setUserId(session.getUserId());
                c.setUsername(session.getUsername());
                c.setDisplayName(session.getDisplayName());
                c.setSessionCount(0);
                return c;
            });
            dto.setSessionCount(dto.getSessionCount() + 1);
        }
        return grouped.values().stream()
                .sorted(Comparator.comparing(DocumentCollaborationConnectedUserDto::getDisplayName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private DocumentCollaborationActiveLockDto toLockDto(DocumentActiveLock lock) {
        DocumentCollaborationActiveLockDto dto = new DocumentCollaborationActiveLockDto();
        dto.setDocumentFamilyId(lock.getDocumentFamilyId());
        dto.setDocumentId(lock.getDocumentId());
        dto.setDocumentName(lock.getDocumentName());
        dto.setUserId(lock.getUserId());
        dto.setUsername(lock.getUsername());
        dto.setDisplayName(lock.getDisplayName());
        dto.setLockedAt(lock.getLockedAt());
        return dto;
    }

    private DocumentPermissionResponse toPermissionResponse(DocumentPermission permission) {
        DocumentPermissionResponse response = new DocumentPermissionResponse();
        response.setId(permission.getId());
        response.setDocumentFamilyId(permission.getDocumentFamilyId());
        response.setGranteeType(permission.getGranteeType());
        response.setGranteeKey(permission.getGranteeKey());
        response.setGranteeLabel(permission.getGranteeLabel());
        response.setPermissionLevel(permission.getPermissionLevel());
        response.setGrantedBy(permission.getGrantedBy());
        response.setGrantedAt(permission.getGrantedAt());
        return response;
    }

    private DocumentPermissionLevel resolveExplicitPermissions(Actor actor, String familyId) {
        DocumentPermissionLevel level = null;
        Optional<User> userOpt = userRepository.findByUsername(actor.username());
        for (DocumentPermission permission : permissionRepository.findByDocumentFamilyId(familyId)) {
            if (matchesGrantee(permission, actor, userOpt.orElse(null))) {
                level = DocumentPermissionLevel.max(level,
                        DocumentPermissionLevel.fromString(permission.getPermissionLevel()));
            }
        }
        return level;
    }

    private boolean matchesGrantee(DocumentPermission permission, Actor actor, User user) {
        if (DocumentPermission.GRANTEE_USER.equals(permission.getGranteeType())) {
            return permission.getGranteeKey() != null && actor.username() != null
                    && permission.getGranteeKey().equalsIgnoreCase(actor.username());
        }
        if (DocumentPermission.GRANTEE_ROLE.equals(permission.getGranteeType()) && user != null && user.getRoleIds() != null) {
            return user.getRoleIds().stream().anyMatch(r -> r != null && r.equals(permission.getGranteeKey()));
        }
        if (DocumentPermission.GRANTEE_DEPARTMENT.equals(permission.getGranteeType()) && user != null) {
            return user.getDepartmentId() != null && user.getDepartmentId().equals(permission.getGranteeKey());
        }
        return false;
    }

    private DocumentPermissionLevel resolveSystemDocumentPermission() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return DocumentPermissionLevel.READ;
        DocumentPermissionLevel level = null;
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String value = authority.getAuthority();
            if (SystemPermissions.DOCUMENTS_DELETE.equals(value)) {
                level = DocumentPermissionLevel.max(level, DocumentPermissionLevel.ADMIN);
            } else if (SystemPermissions.DOCUMENTS_UPLOAD.equals(value)) {
                level = DocumentPermissionLevel.max(level, DocumentPermissionLevel.EDIT);
            } else if (SystemPermissions.DOCUMENTS_VIEW.equals(value)) {
                level = DocumentPermissionLevel.max(level, DocumentPermissionLevel.READ);
            }
        }
        return level != null ? level : DocumentPermissionLevel.READ;
    }

    private boolean isSystemAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private boolean isTramiteParticipant(String username, String tramiteId) {
        if (username == null || tramiteId == null) return false;
        return tramiteRepository.findById(tramiteId).map(t -> matchesParticipant(username, t)).orElse(false);
    }

    private boolean matchesParticipant(String username, Tramite tramite) {
        if (eq(username, tramite.getResponsible()) || eq(username, tramite.getRequestedBy())
                || eq(username, tramite.getCreatedBy())) return true;
        if (tramite.getTasks() == null) return false;
        for (TramiteTask task : tramite.getTasks()) {
            if (eq(username, task.getResponsible()) || eq(username, task.getTakenBy())) return true;
        }
        return false;
    }

    private boolean eq(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private boolean sameUser(DocumentActiveLock lock, Actor actor) {
        return lock.getUsername() != null && actor.username() != null
                && lock.getUsername().equalsIgnoreCase(actor.username());
    }

    private String resolveFamilyId(DocumentRecord record) {
        if (record.getDocumentFamilyId() != null && !record.getDocumentFamilyId().isBlank()) {
            return record.getDocumentFamilyId();
        }
        return record.getId();
    }

    private void deny(Actor actor, DocumentRecord record, String detail) {
        audit(actor.username(), AuditActions.DOCUMENT_PERMISSION_DENIED, detail,
                record != null ? record.getTramiteId() : null, record != null ? record.getId() : null);
        throw new DocumentAccessDeniedException(detail);
    }

    private void audit(String username, String action, String detail, String tramiteId, String documentId) {
        bitacoraService.registrar(username, AuditModules.DOCUMENTOS, action,
                detail + (tramiteId != null ? " [trámite=" + tramiteId + "]" : ""),
                "DocumentRecord", documentId);
    }

    private void requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) throw new IllegalArgumentException("sessionId requerido");
    }

    private void validatePermissionRequest(DocumentPermissionRequest request) {
        if (request.getGranteeType() == null
                || !VALID_GRANTEE_TYPES.contains(request.getGranteeType().trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("granteeType inválido");
        }
        if (request.getGranteeKey() == null || request.getGranteeKey().isBlank()) {
            throw new IllegalArgumentException("granteeKey requerido");
        }
        DocumentPermissionLevel.fromString(request.getPermissionLevel());
    }

    private String label(String displayName, String username) {
        return displayName != null && !displayName.isBlank() ? displayName : username;
    }
}
