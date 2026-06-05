package com.workflow.politicas.service;

import com.workflow.politicas.dto.WorkflowCollaborationActiveEditDto;
import com.workflow.politicas.dto.WorkflowCollaborationConnectedUserDto;
import com.workflow.politicas.dto.WorkflowCollaborationEditingRequest;
import com.workflow.politicas.dto.WorkflowCollaborationEditorDto;
import com.workflow.politicas.dto.WorkflowCollaborationModificationRequest;
import com.workflow.politicas.dto.WorkflowCollaborationRecentActionDto;
import com.workflow.politicas.dto.WorkflowCollaborationStateResponse;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.WorkflowActiveEdit;
import com.workflow.politicas.security.AuthenticatedActorResolver;
import com.workflow.politicas.security.AuthenticatedActorResolver.Actor;
import com.workflow.politicas.model.WorkflowCollaborationMeta;
import com.workflow.politicas.model.WorkflowCollaborationRecentAction;
import com.workflow.politicas.model.WorkflowEditorSession;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.WorkflowCollaborationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Presencia y revisión colaborativa del diseñador UML (F8 — sin CRDT). */
@Service
public class WorkflowCollaborationService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCollaborationService.class);

    static final String MODULE = "Colaboración";
    static final String ACTION_OPEN = "ABRIR_WORKFLOW_COLABORATIVO";
    static final String ACTION_MODIFIED = "MODIFICAR_WORKFLOW_COLABORATIVO";
    static final String ACTION_CONFLICT = "CONFLICTO_EDICION";

    private static final int PRESENCE_TTL_SECONDS = 25;
    private static final int ACTIVE_EDIT_TTL_SECONDS = 25;
    private static final int RECENT_ACTIONS_MAX = 10;

    private static final Set<String> VALID_ELEMENT_TYPES = Set.of("ACTIVITY", "TRANSITION");
    private static final Set<String> VALID_ACTIONS = Set.of("SELECTING", "EDITING", "MOVING");

    private final WorkflowCollaborationRepository collaborationRepository;
    private final BusinessPolicyRepository businessPolicyRepository;
    private final BitacoraService bitacoraService;
    private final AuthenticatedActorResolver actorResolver;

    public WorkflowCollaborationService(
            WorkflowCollaborationRepository collaborationRepository,
            BusinessPolicyRepository businessPolicyRepository,
            BitacoraService bitacoraService,
            AuthenticatedActorResolver actorResolver
    ) {
        this.collaborationRepository = collaborationRepository;
        this.businessPolicyRepository = businessPolicyRepository;
        this.bitacoraService = bitacoraService;
        this.actorResolver = actorResolver;
    }

    public WorkflowCollaborationStateResponse getState(String policyId, String sessionId, Long baseRevision) {
        validatePolicy(policyId);
        String username = resolveCurrentUsername();
        WorkflowCollaborationMeta meta = loadOrCreate(policyId);
        pruneStaleSessions(meta);
        pruneStaleActiveEdits(meta);
        collaborationRepository.save(meta);

        WorkflowCollaborationStateResponse response = toResponse(meta, username);
        applyStaleFlag(response, meta, username, baseRevision);
        return response;
    }

    private void applyStaleFlag(
            WorkflowCollaborationStateResponse response,
            WorkflowCollaborationMeta meta,
            String username,
            Long baseRevision
    ) {
        if (baseRevision == null) {
            return;
        }
        if (meta.getRevision() > baseRevision) {
            boolean modifiedByOther = meta.getLastModifiedByUsername() != null
                    && !meta.getLastModifiedByUsername().equalsIgnoreCase(username);
            response.setStaleForClient(modifiedByOther);
        }
    }

    public WorkflowCollaborationStateResponse registerOpen(String policyId, String sessionId) {
        validatePolicy(policyId);
        requireSessionId(sessionId);
        Actor actor = actorResolver.requireCurrentActor();
        log.info(
                "[CU16] registerOpen policyId={} sessionId={} actorUser={} actorDisplay={}",
                policyId,
                sessionId,
                actor.username(),
                actor.displayName()
        );
        WorkflowCollaborationMeta meta = loadOrCreate(policyId);
        pruneStaleSessions(meta);
        pruneStaleActiveEdits(meta);
        upsertSession(meta, sessionId, actor, LocalDateTime.now());
        collaborationRepository.save(meta);

        businessPolicyRepository.findById(policyId).ifPresent(policy ->
                bitacoraService.registrar(
                        actor.username(),
                        MODULE,
                        ACTION_OPEN,
                        actor.displayName() + " abrió el diseñador UML de la política " + policy.getName(),
                        "BusinessPolicy",
                        policyId
                )
        );

        return toResponse(meta, actor.username());
    }

    public WorkflowCollaborationStateResponse heartbeat(String policyId, String sessionId, Long baseRevision) {
        validatePolicy(policyId);
        requireSessionId(sessionId);
        Actor actor = actorResolver.requireCurrentActor();
        log.debug(
                "[CU16] heartbeat policyId={} sessionId={} actorUser={} actorDisplay={}",
                policyId,
                sessionId,
                actor.username(),
                actor.displayName()
        );
        WorkflowCollaborationMeta meta = loadOrCreate(policyId);
        upsertSession(meta, sessionId, actor, LocalDateTime.now());
        touchActiveEditsForSession(meta, sessionId, LocalDateTime.now());
        pruneStaleSessions(meta);
        pruneStaleActiveEdits(meta);
        collaborationRepository.save(meta);
        WorkflowCollaborationStateResponse response = toResponse(meta, actor.username());
        applyStaleFlag(response, meta, actor.username(), baseRevision);
        return response;
    }

    public WorkflowCollaborationStateResponse registerEditing(
            String policyId,
            WorkflowCollaborationEditingRequest request
    ) {
        validatePolicy(policyId);
        requireSessionId(request.getSessionId());
        validateEditingRequest(request);
        Actor actor = actorResolver.requireCurrentActor();
        LocalDateTime now = LocalDateTime.now();
        WorkflowCollaborationMeta meta = loadOrCreate(policyId);
        pruneStaleSessions(meta);
        pruneStaleActiveEdits(meta);
        upsertSession(meta, request.getSessionId(), actor, now);
        upsertActiveEdit(meta, request.getSessionId(), actor, request, now);
        collaborationRepository.save(meta);
        return toResponse(meta, actor.username());
    }

    public WorkflowCollaborationStateResponse clearEditing(
            String policyId,
            String sessionId,
            String elementId
    ) {
        if (policyId == null || policyId.isBlank()) {
            return new WorkflowCollaborationStateResponse();
        }
        requireSessionId(sessionId);
        String username = resolveCurrentUsername();
        collaborationRepository.findById(policyId).ifPresent(meta -> {
            removeActiveEdit(meta, sessionId, elementId);
            pruneStaleActiveEdits(meta);
            collaborationRepository.save(meta);
        });
        return collaborationRepository.findById(policyId)
                .map(meta -> toResponse(meta, username))
                .orElseGet(() -> {
                    WorkflowCollaborationStateResponse empty = new WorkflowCollaborationStateResponse();
                    empty.setPolicyId(policyId);
                    empty.setCurrentUsername(username);
                    return empty;
                });
    }

    public void registerClose(String policyId, String sessionId) {
        if (policyId == null || policyId.isBlank() || sessionId == null || sessionId.isBlank()) {
            return;
        }
        collaborationRepository.findById(policyId).ifPresent(meta -> {
            if (meta.getActiveEditors() == null) {
                return;
            }
            meta.getActiveEditors().removeIf(s -> sessionId.equals(s.getSessionId()));
            if (meta.getActiveEdits() != null) {
                meta.getActiveEdits().removeIf(e -> sessionId.equals(e.getSessionId()));
            }
            collaborationRepository.save(meta);
        });
    }

    public void registerModification(String policyId) {
        registerModification(policyId, null);
    }

    public void registerModification(String policyId, WorkflowCollaborationModificationRequest details) {
        if (policyId == null || policyId.isBlank()) {
            return;
        }
        Actor actor = actorResolver.requireCurrentActor();
        log.info(
                "[CU16] registerModification policyId={} actorUser={} actorDisplay={} actorId={} actionType={} element={}",
                policyId,
                actor.username(),
                actor.displayName(),
                actor.userId(),
                details != null ? details.getActionType() : null,
                details != null ? details.getElementName() : null
        );
        LocalDateTime now = LocalDateTime.now();
        WorkflowCollaborationMeta meta = loadOrCreate(policyId);
        meta.setRevision(meta.getRevision() + 1);
        meta.setLastModifiedAt(now);
        meta.setLastModifiedByUserId(actor.userId());
        meta.setLastModifiedByUsername(actor.username());
        meta.setLastModifiedByDisplayName(actor.displayName());

        String actionType = details != null ? trimOrNull(details.getActionType()) : null;
        String actionLabel = details != null ? trimOrNull(details.getActionLabel()) : null;
        String elementType = details != null ? trimOrNull(details.getElementType()) : null;
        String elementName = details != null ? trimOrNull(details.getElementName()) : null;

        if (actionType == null) {
            actionType = "MODIFY";
        }
        if (actionLabel == null || actionLabel.isBlank()) {
            actionLabel = "modificó el workflow";
        }
        if (elementType != null) {
            elementType = elementType.toUpperCase(Locale.ROOT);
        }

        meta.setLastModifiedActionType(actionType);
        meta.setLastModifiedActionLabel(actionLabel);
        meta.setLastModifiedElementType(elementType);
        meta.setLastModifiedElementName(elementName);
        appendRecentAction(meta, actor, actionType, actionLabel, elementType, elementName, now);

        pruneStaleSessions(meta);
        pruneStaleActiveEdits(meta);
        collaborationRepository.save(meta);

        String summary = buildActionSummary(actor.displayName(), actionLabel, elementType, elementName);
        businessPolicyRepository.findById(policyId).ifPresent(policy ->
                bitacoraService.registrar(
                        actor.username(),
                        MODULE,
                        ACTION_MODIFIED,
                        actor.displayName() + " " + summary + " en la política " + policy.getName()
                                + " (rev. " + meta.getRevision() + ")",
                        "BusinessPolicy",
                        policyId
                )
        );
    }

    private void appendRecentAction(
            WorkflowCollaborationMeta meta,
            Actor actor,
            String actionType,
            String actionLabel,
            String elementType,
            String elementName,
            LocalDateTime now
    ) {
        List<WorkflowCollaborationRecentAction> actions = meta.getRecentActions();
        if (actions == null) {
            actions = new ArrayList<>();
            meta.setRecentActions(actions);
        }
        WorkflowCollaborationRecentAction entry = new WorkflowCollaborationRecentAction();
        entry.setActionType(actionType);
        entry.setActionLabel(actionLabel);
        entry.setElementType(elementType);
        entry.setElementName(elementName);
        entry.setModifiedByUserId(actor.userId());
        entry.setModifiedByUsername(actor.username());
        entry.setModifiedByDisplayName(actor.displayName());
        entry.setModifiedAt(now);
        actions.add(0, entry);
        while (actions.size() > RECENT_ACTIONS_MAX) {
            actions.remove(actions.size() - 1);
        }
        log.info(
                "[CU16] recentAction saved user={} display={} summary={}",
                actor.username(),
                actor.displayName(),
                buildActionSummary(actor.displayName(), actionLabel, elementType, elementName)
        );
    }

    public void registerConflict(String policyId, Long baseRevision) {
        validatePolicy(policyId);
        Actor actor = actorResolver.requireCurrentActor();
        WorkflowCollaborationMeta meta = loadOrCreate(policyId);

        businessPolicyRepository.findById(policyId).ifPresent(policy -> {
            String detail = actor.displayName()
                    + " detectó conflicto de edición en la política " + policy.getName();
            if (baseRevision != null) {
                detail += " (base rev. " + baseRevision + ", actual " + meta.getRevision() + ")";
            }
            bitacoraService.registrar(
                    actor.username(),
                    MODULE,
                    ACTION_CONFLICT,
                    detail,
                    "BusinessPolicy",
                    policyId
            );
        });
    }

    private WorkflowCollaborationMeta loadOrCreate(String policyId) {
        return collaborationRepository.findById(policyId).orElseGet(() -> {
            WorkflowCollaborationMeta meta = new WorkflowCollaborationMeta();
            meta.setPolicyId(policyId);
            meta.setRevision(0L);
            meta.setActiveEditors(new ArrayList<>());
            meta.setActiveEdits(new ArrayList<>());
            meta.setRecentActions(new ArrayList<>());
            return meta;
        });
    }

    private void validateEditingRequest(WorkflowCollaborationEditingRequest request) {
        if (request.getElementId() == null || request.getElementId().isBlank()) {
            throw new IllegalArgumentException("elementId es obligatorio");
        }
        if (request.getElementType() == null
                || !VALID_ELEMENT_TYPES.contains(request.getElementType().trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("elementType debe ser ACTIVITY o TRANSITION");
        }
        if (request.getAction() == null
                || !VALID_ACTIONS.contains(request.getAction().trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("action debe ser SELECTING, EDITING o MOVING");
        }
    }

    private void upsertActiveEdit(
            WorkflowCollaborationMeta meta,
            String sessionId,
            Actor actor,
            WorkflowCollaborationEditingRequest request,
            LocalDateTime now
    ) {
        List<WorkflowActiveEdit> edits = meta.getActiveEdits();
        if (edits == null) {
            edits = new ArrayList<>();
            meta.setActiveEdits(edits);
        }
        String userKey = actor.userId() != null ? actor.userId() : actor.username();
        edits.removeIf(e ->
                sessionId.equals(e.getSessionId())
                        || (userKey != null && userKey.equals(e.getUserId()))
                        || (actor.username() != null
                        && actor.username().equalsIgnoreCase(e.getUsername()))
        );

        WorkflowActiveEdit edit = new WorkflowActiveEdit();
        edit.setSessionId(sessionId);
        edit.setUserId(actor.userId());
        edit.setUsername(actor.username());
        edit.setDisplayName(actor.displayName());
        edit.setElementType(request.getElementType().trim().toUpperCase(Locale.ROOT));
        edit.setElementId(request.getElementId().trim());
        String name = request.getElementName();
        edit.setElementName(name != null && !name.isBlank() ? name.trim() : request.getElementId());
        edit.setAction(request.getAction().trim().toUpperCase(Locale.ROOT));
        edit.setLastSeenAt(now);
        edits.add(edit);
    }

    private void removeActiveEdit(WorkflowCollaborationMeta meta, String sessionId, String elementId) {
        if (meta.getActiveEdits() == null) {
            return;
        }
        meta.getActiveEdits().removeIf(e -> {
            if (!sessionId.equals(e.getSessionId())) {
                return false;
            }
            if (elementId == null || elementId.isBlank()) {
                return true;
            }
            return elementId.equals(e.getElementId());
        });
    }

    private void touchActiveEditsForSession(
            WorkflowCollaborationMeta meta,
            String sessionId,
            LocalDateTime now
    ) {
        if (meta.getActiveEdits() == null) {
            return;
        }
        for (WorkflowActiveEdit edit : meta.getActiveEdits()) {
            if (sessionId.equals(edit.getSessionId())) {
                edit.setLastSeenAt(now);
            }
        }
    }

    private void pruneStaleActiveEdits(WorkflowCollaborationMeta meta) {
        if (meta.getActiveEdits() == null) {
            meta.setActiveEdits(new ArrayList<>());
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(ACTIVE_EDIT_TTL_SECONDS);
        meta.getActiveEdits().removeIf(edit -> {
            LocalDateTime seen = edit.getLastSeenAt();
            return seen == null || seen.isBefore(cutoff);
        });
    }

    private void upsertSession(
            WorkflowCollaborationMeta meta,
            String sessionId,
            Actor actor,
            LocalDateTime now
    ) {
        List<WorkflowEditorSession> editors = meta.getActiveEditors();
        if (editors == null) {
            editors = new ArrayList<>();
            meta.setActiveEditors(editors);
        }

        WorkflowEditorSession session = editors.stream()
                .filter(s -> sessionId.equals(s.getSessionId()))
                .findFirst()
                .orElse(null);

        if (session != null) {
            session.setUserId(actor.userId());
            session.setUsername(actor.username());
            session.setDisplayName(actor.displayName());
            session.setLastSeenAt(now);
            if (session.getOpenedAt() == null) {
                session.setOpenedAt(now);
            }
            return;
        }

        WorkflowEditorSession created = new WorkflowEditorSession();
        created.setSessionId(sessionId);
        created.setUserId(actor.userId());
        created.setUsername(actor.username());
        created.setDisplayName(actor.displayName());
        created.setOpenedAt(now);
        created.setLastSeenAt(now);
        editors.add(created);
    }

    private void pruneStaleSessions(WorkflowCollaborationMeta meta) {
        if (meta.getActiveEditors() == null) {
            meta.setActiveEditors(new ArrayList<>());
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(PRESENCE_TTL_SECONDS);
        Iterator<WorkflowEditorSession> it = meta.getActiveEditors().iterator();
        while (it.hasNext()) {
            WorkflowEditorSession session = it.next();
            LocalDateTime seen = session.getLastSeenAt();
            if (seen == null || seen.isBefore(cutoff)) {
                it.remove();
            }
        }
        dedupeSessionsBySessionId(meta.getActiveEditors());
    }

    /** Conserva una sola entrada por sessionId (la de lastSeenAt más reciente). */
    private void dedupeSessionsBySessionId(List<WorkflowEditorSession> editors) {
        Map<String, WorkflowEditorSession> bestBySession = new LinkedHashMap<>();
        List<WorkflowEditorSession> withoutSessionId = new ArrayList<>();
        boolean duplicate = false;
        for (WorkflowEditorSession session : editors) {
            if (session.getSessionId() == null || session.getSessionId().isBlank()) {
                withoutSessionId.add(session);
                continue;
            }
            String sid = session.getSessionId();
            if (bestBySession.containsKey(sid)) {
                duplicate = true;
            }
            WorkflowEditorSession existing = bestBySession.get(sid);
            if (existing == null || isAfter(session.getLastSeenAt(), existing.getLastSeenAt())) {
                bestBySession.put(sid, session);
            }
        }
        if (!duplicate) {
            return;
        }
        editors.clear();
        editors.addAll(bestBySession.values());
        editors.addAll(withoutSessionId);
    }

    private static boolean isAfter(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return false;
        }
        if (b == null) {
            return true;
        }
        return a.isAfter(b);
    }

    private WorkflowCollaborationStateResponse toResponse(WorkflowCollaborationMeta meta, String currentUsername) {
        WorkflowCollaborationStateResponse response = new WorkflowCollaborationStateResponse();
        response.setPolicyId(meta.getPolicyId());
        response.setRevision(meta.getRevision());
        response.setCurrentRevision(meta.getRevision());
        response.setLastModifiedAt(meta.getLastModifiedAt());
        response.setLastModifiedByUserId(meta.getLastModifiedByUserId());
        response.setLastModifiedByUsername(meta.getLastModifiedByUsername());
        response.setLastModifiedByDisplayName(meta.getLastModifiedByDisplayName());
        response.setLastModifiedByName(meta.getLastModifiedByDisplayName());
        response.setLastModifiedActionType(meta.getLastModifiedActionType());
        response.setLastModifiedActionLabel(meta.getLastModifiedActionLabel());
        response.setLastModifiedElementType(meta.getLastModifiedElementType());
        response.setLastModifiedElementName(meta.getLastModifiedElementName());
        response.setLastModifiedSummary(
                buildActionSummary(
                        meta.getLastModifiedByDisplayName(),
                        meta.getLastModifiedActionLabel(),
                        meta.getLastModifiedElementType(),
                        meta.getLastModifiedElementName()
                )
        );
        response.setCurrentUsername(currentUsername);

        List<WorkflowEditorSession> sessions = meta.getActiveEditors() != null
                ? meta.getActiveEditors()
                : List.of();
        response.setActiveSessionsCount(sessions.size());

        List<WorkflowCollaborationConnectedUserDto> connectedUsers = groupSessionsByUser(sessions, currentUsername);
        response.setConnectedUsers(connectedUsers);
        response.setConnectedEditors(toLegacyEditorDtos(connectedUsers));
        response.setActiveEdits(mapActiveEdits(meta, currentUsername));
        response.setRecentActions(mapRecentActions(meta));
        return response;
    }

    private List<WorkflowCollaborationRecentActionDto> mapRecentActions(WorkflowCollaborationMeta meta) {
        if (meta.getRecentActions() == null || meta.getRecentActions().isEmpty()) {
            return List.of();
        }
        return meta.getRecentActions().stream()
                .sorted(Comparator.comparing(
                        WorkflowCollaborationRecentAction::getModifiedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(RECENT_ACTIONS_MAX)
                .map(action -> {
                    WorkflowCollaborationRecentActionDto dto = new WorkflowCollaborationRecentActionDto();
                    String displayName = action.getModifiedByDisplayName() != null
                            && !action.getModifiedByDisplayName().isBlank()
                            ? action.getModifiedByDisplayName()
                            : action.getModifiedByUsername();
                    dto.setActionType(action.getActionType());
                    dto.setActionLabel(action.getActionLabel());
                    dto.setElementType(action.getElementType());
                    dto.setElementName(action.getElementName());
                    dto.setModifiedByUserId(action.getModifiedByUserId());
                    dto.setModifiedByUsername(action.getModifiedByUsername());
                    dto.setModifiedByDisplayName(action.getModifiedByDisplayName());
                    dto.setModifiedByName(displayName);
                    dto.setModifiedAt(action.getModifiedAt());
                    dto.setSummary(
                            buildActionSummary(
                                    displayName,
                                    action.getActionLabel(),
                                    action.getElementType(),
                                    action.getElementName()
                            )
                    );
                    return dto;
                })
                .toList();
    }

    static String buildActionSummary(
            String actorName,
            String actionLabel,
            String elementType,
            String elementName
    ) {
        String who = actorName != null && !actorName.isBlank() ? actorName.trim() : "Usuario";
        String verb = actionLabel != null && !actionLabel.isBlank() ? actionLabel.trim() : "modificó el workflow";
        if (elementName == null || elementName.isBlank()) {
            return who + " " + verb;
        }
        String kind = "elemento";
        if ("ACTIVITY".equalsIgnoreCase(elementType)) {
            kind = "actividad";
        } else if ("TRANSITION".equalsIgnoreCase(elementType)) {
            kind = "conexión";
        }
        return who + " " + verb + " " + kind + " \"" + elementName.trim() + "\"";
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<WorkflowCollaborationActiveEditDto> mapActiveEdits(
            WorkflowCollaborationMeta meta,
            String currentUsername
    ) {
        if (meta.getActiveEdits() == null || meta.getActiveEdits().isEmpty()) {
            return List.of();
        }
        String currentNorm = normalizeUsername(currentUsername);
        return meta.getActiveEdits().stream()
                .sorted(Comparator.comparing(
                        WorkflowActiveEdit::getLastSeenAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(edit -> {
                    WorkflowCollaborationActiveEditDto dto = new WorkflowCollaborationActiveEditDto();
                    dto.setUserId(edit.getUserId());
                    dto.setUsername(edit.getUsername());
                    dto.setFullName(
                            edit.getDisplayName() != null && !edit.getDisplayName().isBlank()
                                    ? edit.getDisplayName()
                                    : edit.getUsername()
                    );
                    dto.setElementType(edit.getElementType());
                    dto.setElementId(edit.getElementId());
                    dto.setElementName(edit.getElementName());
                    dto.setAction(edit.getAction());
                    dto.setLastSeenAt(edit.getLastSeenAt());
                    dto.setCurrentUser(
                            edit.getUsername() != null
                                    && currentNorm.equals(normalizeUsername(edit.getUsername()))
                    );
                    return dto;
                })
                .toList();
    }

    private List<WorkflowCollaborationConnectedUserDto> groupSessionsByUser(
            List<WorkflowEditorSession> sessions,
            String currentUsername
    ) {
        Map<String, AggregatedUser> grouped = new LinkedHashMap<>();
        for (WorkflowEditorSession session : sessions) {
            String key = userGroupKey(session);
            AggregatedUser agg = grouped.computeIfAbsent(key, k -> new AggregatedUser(session));
            agg.merge(session);
        }

        String currentNorm = normalizeUsername(currentUsername);
        return grouped.values().stream()
                .map(agg -> agg.toDto(currentNorm))
                .sorted(Comparator.comparing(
                        WorkflowCollaborationConnectedUserDto::getLastSeenAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    private static String userGroupKey(WorkflowEditorSession session) {
        if (session.getUserId() != null && !session.getUserId().isBlank()) {
            return "id:" + session.getUserId().trim();
        }
        if (session.getUsername() != null && !session.getUsername().isBlank()) {
            return "user:" + session.getUsername().trim().toLowerCase(Locale.ROOT);
        }
        if (session.getSessionId() != null) {
            return "sess:" + session.getSessionId();
        }
        return "unknown";
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static List<WorkflowCollaborationEditorDto> toLegacyEditorDtos(
            List<WorkflowCollaborationConnectedUserDto> users
    ) {
        return users.stream().map(user -> {
            WorkflowCollaborationEditorDto dto = new WorkflowCollaborationEditorDto();
            dto.setSessionId(user.getUserId() != null ? user.getUserId() : user.getUsername());
            dto.setUsername(user.getUsername());
            dto.setDisplayName(user.getFullName());
            dto.setCurrentUser(user.isCurrentUser());
            dto.setCanEdit(true);
            return dto;
        }).toList();
    }

    private static final class AggregatedUser {
        private String userId;
        private String username;
        private String fullName;
        private LocalDateTime openedAt;
        private LocalDateTime lastSeenAt;
        private int sessionCount;

        AggregatedUser(WorkflowEditorSession seed) {
            userId = seed.getUserId();
            username = seed.getUsername();
            fullName = seed.getDisplayName();
            openedAt = seed.getOpenedAt();
            lastSeenAt = seed.getLastSeenAt();
            sessionCount = 0;
        }

        void merge(WorkflowEditorSession session) {
            sessionCount++;
            if (session.getUserId() != null && !session.getUserId().isBlank()) {
                userId = session.getUserId();
            }
            if (session.getUsername() != null && !session.getUsername().isBlank()) {
                username = session.getUsername();
            }
            if (session.getDisplayName() != null && !session.getDisplayName().isBlank()) {
                fullName = session.getDisplayName();
            }
            LocalDateTime opened = session.getOpenedAt();
            if (opened != null && (openedAt == null || opened.isBefore(openedAt))) {
                openedAt = opened;
            }
            LocalDateTime seen = session.getLastSeenAt();
            if (seen != null && (lastSeenAt == null || seen.isAfter(lastSeenAt))) {
                lastSeenAt = seen;
            }
        }

        WorkflowCollaborationConnectedUserDto toDto(String currentUsernameNorm) {
            WorkflowCollaborationConnectedUserDto dto = new WorkflowCollaborationConnectedUserDto();
            dto.setUserId(userId);
            dto.setUsername(username);
            dto.setFullName(fullName != null && !fullName.isBlank() ? fullName : username);
            dto.setOpenedAt(openedAt);
            dto.setLastSeenAt(lastSeenAt);
            dto.setSessionCount(sessionCount);
            boolean isCurrent = username != null
                    && currentUsernameNorm.equals(normalizeUsername(username));
            dto.setCurrentUser(isCurrent);
            return dto;
        }
    }

    private void validatePolicy(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la política es obligatorio");
        }
        if (!businessPolicyRepository.existsById(policyId)) {
            throw new IllegalArgumentException("La política seleccionada no existe");
        }
    }

    private void requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId es obligatorio");
        }
    }

    private String resolveCurrentUsername() {
        return actorResolver.resolveCurrentUsername().orElse("system");
    }
}
