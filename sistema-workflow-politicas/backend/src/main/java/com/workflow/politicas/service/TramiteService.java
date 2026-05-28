package com.workflow.politicas.service;

import com.workflow.politicas.dto.TramiteAdvanceRequest;
import com.workflow.politicas.dto.TramiteCancelRequest;
import com.workflow.politicas.dto.TramiteCreateRequest;
import com.workflow.politicas.model.ActivityDiagram;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.DiagramEdge;
import com.workflow.politicas.model.DiagramNode;
import com.workflow.politicas.model.TraceItem;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.ActivityDiagramRepository;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TramiteService {

    private static final String STATUS_INICIADO = "INICIADO";
    private static final String STATUS_EN_PROCESO = "EN_PROCESO";
    private static final String STATUS_COMPLETADO = "COMPLETADO";
    private static final String STATUS_CANCELADO = "CANCELADO";
    private static final String TASK_PENDIENTE = "PENDIENTE";
    private static final String TASK_EN_CURSO = "EN_CURSO";
    private static final String TASK_COMPLETADA = "COMPLETADA";
    private static final String DEFAULT_ACTIVITY = "Registrar solicitud";
    private static final Pattern CODE_PATTERN = Pattern.compile("^TRM-(\\d+)$");

    private static final List<String> DEFAULT_TASK_NAMES = List.of(
            "Registrar solicitud",
            "Revisar solicitud",
            "Validar información",
            "Aprobar permiso",
            "Rechazar permiso",
            "Notificar resultado"
    );

    private final TramiteRepository tramiteRepository;
    private final BusinessPolicyRepository businessPolicyRepository;
    private final ActivityDiagramRepository activityDiagramRepository;
    private final UserRepository userRepository;
    private final BitacoraService bitacoraService;

    public TramiteService(
            TramiteRepository tramiteRepository,
            BusinessPolicyRepository businessPolicyRepository,
            ActivityDiagramRepository activityDiagramRepository,
            UserRepository userRepository,
            BitacoraService bitacoraService
    ) {
        this.tramiteRepository = tramiteRepository;
        this.businessPolicyRepository = businessPolicyRepository;
        this.activityDiagramRepository = activityDiagramRepository;
        this.userRepository = userRepository;
        this.bitacoraService = bitacoraService;
    }

    public List<Tramite> findAll() {
        return tramiteRepository.findAll().stream()
                .map(this::enrichAndPersistIfNeeded)
                .sorted(Comparator.comparing(Tramite::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Optional<Tramite> findById(String id) {
        return tramiteRepository.findById(id).map(this::enrichAndPersistIfNeeded);
    }

    private Tramite enrichAndPersistIfNeeded(Tramite tramite) {
        if (enrichTramite(tramite)) {
            tramite.setUpdatedAt(LocalDateTime.now());
            return tramiteRepository.save(tramite);
        }
        return tramite;
    }

    private boolean enrichTramite(Tramite tramite) {
        boolean changed = false;

        if (tramite.getPriority() == null || tramite.getPriority().isBlank()) {
            tramite.setPriority("NORMAL");
            changed = true;
        }

        if (tramite.getRequestedByName() == null || tramite.getRequestedByName().isBlank()) {
            String display = resolveDisplayForUsername(tramite.getRequestedBy());
            if (display == null) {
                display = resolveDisplayForUsername(tramite.getCreatedBy());
            }
            if (display != null) {
                tramite.setRequestedByName(display);
                changed = true;
            }
        } else if (tramite.getRequesterName() == null || tramite.getRequesterName().isBlank()) {
            tramite.setRequesterName(tramite.getRequestedByName());
            changed = true;
        }

        if (tramite.getTrace() == null) {
            tramite.setTrace(new ArrayList<>());
            changed = true;
        }

        String defaultActor = tramite.getRequesterName() != null && !tramite.getRequesterName().isBlank()
                ? tramite.getRequesterName()
                : resolveDisplayForUsername(tramite.getCreatedBy());

        for (TraceItem item : tramite.getTrace()) {
            if (enrichTraceItem(item, defaultActor, tramite.getRequestedBy())) {
                changed = true;
            }
        }

        if (tramite.getTasks() == null || tramite.getTasks().isEmpty()) {
            if (tramite.getPolicyId() != null) {
                businessPolicyRepository.findById(tramite.getPolicyId()).ifPresent(policy -> {
                    ActivityDiagram diagram = activityDiagramRepository.findByPolicyId(tramite.getPolicyId()).orElse(null);
                    String firstActivity = tramite.getCurrentActivity() != null && !tramite.getCurrentActivity().isBlank()
                            ? tramite.getCurrentActivity()
                            : DEFAULT_ACTIVITY;
                    tramite.setTasks(buildTasks(diagram, policy, firstActivity, tramite.getCreatedAt()));
                });
                changed = true;
            }
        } else {
            for (TramiteTask task : tramite.getTasks()) {
                if (TASK_EN_CURSO.equals(task.getStatus()) && task.getStartedAt() == null) {
                    task.setStartedAt(tramite.getCreatedAt());
                    changed = true;
                }
            }
        }

        return changed;
    }

    private boolean enrichTraceItem(TraceItem item, String defaultActor, String defaultUserId) {
        boolean changed = false;
        if (item.getEventLabel() == null || item.getEventLabel().isBlank()) {
            if (item.getActivityName() != null && !item.getActivityName().isBlank()) {
                item.setEventLabel(item.getActivityName());
            } else {
                item.setEventLabel("Evento del proceso");
            }
            changed = true;
        }
        String display = item.getUserFullName();
        if (display == null || display.isBlank()) {
            display = item.getUserName();
        }
        if (display == null || display.isBlank()) {
            display = defaultActor;
        }
        if (display != null && !display.isBlank()) {
            if (!display.equals(item.getUserFullName())) {
                item.setUserFullName(display);
                changed = true;
            }
            if (item.getUserName() == null || item.getUserName().isBlank()) {
                item.setUserName(display);
                changed = true;
            }
            if (item.getUserId() == null && defaultUserId != null) {
                item.setUserId(defaultUserId);
                changed = true;
            }
        }
        if (item.getOccurredAt() == null && item.getStartedAt() != null) {
            item.setOccurredAt(item.getStartedAt());
            changed = true;
        }
        return changed;
    }

    private String resolveDisplayForUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return userRepository.findByUsername(username.trim())
                .map(this::displayName)
                .orElse(username.trim());
    }

    public Tramite create(TramiteCreateRequest request, String authenticatedUsername) {
        validateCreateRequest(request);

        BusinessPolicy policy = businessPolicyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new IllegalArgumentException("Política no encontrada"));

        if (!"ACTIVE".equalsIgnoreCase(policy.getStatus())) {
            throw new IllegalArgumentException("Solo puede iniciar trámites sobre políticas activas");
        }

        User requester = userRepository.findByUsername(request.getRequestedBy().trim())
                .orElseThrow(() -> new IllegalArgumentException("El solicitante seleccionado no existe"));

        if (!requester.isActive()) {
            throw new IllegalArgumentException("El solicitante seleccionado no está activo");
        }

        ActivityDiagram diagram = activityDiagramRepository.findByPolicyId(policy.getId()).orElse(null);
        ResolvedActivity firstActivity = resolveFirstActivity(diagram, policy);
        LocalDateTime now = LocalDateTime.now();
        String requesterDisplay = displayName(requester);
        String actorDisplay = resolveActorDisplay(authenticatedUsername);
        List<TramiteTask> tasks = buildTasks(diagram, policy, firstActivity.label(), now);

        Tramite tramite = new Tramite();
        tramite.setCode(generateNextCode());
        tramite.setPolicyId(policy.getId());
        tramite.setPolicyName(policy.getName());
        tramite.setPolicyDescription(policy.getDescription());
        tramite.setDescription(request.getDescription().trim());
        tramite.setPriority(normalizePriority(request.getPriority()));
        tramite.setRequestedBy(requester.getUsername());
        tramite.setRequestedByName(requesterDisplay);
        tramite.setRequesterName(requesterDisplay);
        tramite.setStatus(STATUS_INICIADO);
        tramite.setCurrentActivity(firstActivity.label());
        tramite.setCurrentNodeId(firstActivity.nodeId());
        tramite.setResponsible(firstActivity.responsible());
        tramite.setProgress(10);
        tramite.setCreatedBy(authenticatedUsername);
        tramite.setCreatedAt(now);
        tramite.setUpdatedAt(now);
        tramite.setTasks(tasks);

        List<TraceItem> trace = new ArrayList<>();
        trace.add(buildTraceEvent(
                "PROCESO_CREADO",
                "Proceso creado",
                authenticatedUsername,
                actorDisplay,
                null,
                statusLabel(STATUS_INICIADO),
                now,
                "Trámite " + tramite.getCode() + " registrado para la política " + policy.getName()
        ));
        trace.add(buildTraceEvent(
                "TRAMITE_INICIADO",
                "Trámite iniciado",
                requester.getUsername(),
                requesterDisplay,
                null,
                statusLabel(STATUS_INICIADO),
                now,
                request.getDescription().trim()
        ));
        tramite.setTrace(trace);

        Tramite saved = enrichAndPersistIfNeeded(tramiteRepository.save(tramite));
        bitacoraService.registrar(
                authenticatedUsername,
                "Trámites",
                "CREAR_TRAMITE",
                actorDisplay + " creó el trámite " + saved.getCode() + " para la política " + policy.getName(),
                "Tramite",
                saved.getId()
        );
        return saved;
    }

    public Tramite advance(String id, TramiteAdvanceRequest request, String authenticatedUsername) {
        Tramite tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tramite not found with id: " + id));

        ensureNotClosed(tramite);

        String previousStatus = tramite.getStatus();
        String actorDisplay = resolveActorDisplay(authenticatedUsername);
        String comment = request != null ? request.getComment() : null;

        completeCurrentTask(tramite);
        addActivityCompletedTrace(tramite, authenticatedUsername, actorDisplay, comment);

        ActivityDiagram diagram = activityDiagramRepository.findByPolicyId(tramite.getPolicyId()).orElse(null);
        BusinessPolicy policy = businessPolicyRepository.findById(tramite.getPolicyId())
                .orElseThrow(() -> new IllegalArgumentException("Política no encontrada"));

        DiagramNode currentNode = findCurrentNode(tramite, diagram);
        ResolvedActivity nextActivity = resolveNextActivity(currentNode, diagram, policy);

        LocalDateTime now = LocalDateTime.now();
        if (nextActivity == null || nextActivity.finalStep()) {
            tramite.setStatus(STATUS_COMPLETADO);
            tramite.setCurrentActivity("Finalizado");
            tramite.setCurrentNodeId(null);
            tramite.setResponsible("—");
            tramite.setProgress(100);
            markAllTasksCompleted(tramite);
            tramite.getTrace().add(buildTraceEvent(
                    "TRAMITE_FINALIZADO",
                    "Trámite finalizado",
                    authenticatedUsername,
                    actorDisplay,
                    statusLabel(previousStatus),
                    statusLabel(STATUS_COMPLETADO),
                    now,
                    comment != null && !comment.isBlank() ? comment.trim() : "Proceso completado"
            ));
        } else {
            tramite.setStatus(STATUS_EN_PROCESO);
            tramite.setCurrentActivity(nextActivity.label());
            tramite.setCurrentNodeId(nextActivity.nodeId());
            tramite.setResponsible(nextActivity.responsible());
            tramite.setProgress(calculateProgress(tramite, diagram));
            activateTask(tramite, nextActivity.label(), now);
            tramite.getTrace().add(buildTraceEvent(
                    "PROCESO_AVANZADO",
                    "Proceso avanzado",
                    authenticatedUsername,
                    actorDisplay,
                    statusLabel(previousStatus),
                    statusLabel(STATUS_EN_PROCESO),
                    now,
                    "Nueva actividad: " + nextActivity.label()
                            + (comment != null && !comment.isBlank() ? " — " + comment.trim() : "")
            ));
        }

        tramite.setUpdatedAt(now);
        Tramite saved = enrichAndPersistIfNeeded(tramiteRepository.save(tramite));
        bitacoraService.registrar(
                authenticatedUsername,
                "Trámites",
                "AVANZAR_TRAMITE",
                actorDisplay + " avanzó el trámite " + saved.getCode(),
                "Tramite",
                saved.getId()
        );
        return saved;
    }

    public Tramite cancel(String id, TramiteCancelRequest request, String authenticatedUsername) {
        Tramite tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tramite not found with id: " + id));

        ensureNotClosed(tramite);

        String previousStatus = tramite.getStatus();
        String actorDisplay = resolveActorDisplay(authenticatedUsername);
        LocalDateTime now = LocalDateTime.now();
        String comment = request != null && request.getComment() != null && !request.getComment().isBlank()
                ? request.getComment().trim()
                : "Trámite cancelado por el usuario";

        tramite.setStatus(STATUS_CANCELADO);
        tramite.setCurrentActivity("Cancelado");
        tramite.setResponsible("—");
        tramite.setUpdatedAt(now);

        for (TramiteTask task : tramite.getTasks()) {
            if (!TASK_COMPLETADA.equals(task.getStatus())) {
                task.setStatus(TASK_COMPLETADA);
            }
        }

        tramite.getTrace().add(buildTraceEvent(
                "TRAMITE_CANCELADO",
                "Trámite cancelado",
                authenticatedUsername,
                actorDisplay,
                statusLabel(previousStatus),
                statusLabel(STATUS_CANCELADO),
                now,
                comment
        ));

        Tramite saved = enrichAndPersistIfNeeded(tramiteRepository.save(tramite));
        bitacoraService.registrar(
                authenticatedUsername,
                "Trámites",
                "CANCELAR_TRAMITE",
                actorDisplay + " canceló el trámite " + saved.getCode(),
                "Tramite",
                saved.getId()
        );
        return saved;
    }

    private void validateCreateRequest(TramiteCreateRequest request) {
        if (request.getPolicyId() == null || request.getPolicyId().isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar una política");
        }
        if (request.getRequestedBy() == null || request.getRequestedBy().isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar un solicitante");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("La descripción del trámite es obligatoria");
        }
        if (request.getPriority() == null || request.getPriority().isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar una prioridad");
        }
    }

    private void ensureNotClosed(Tramite tramite) {
        if (STATUS_COMPLETADO.equals(tramite.getStatus())) {
            throw new IllegalArgumentException("El trámite ya está finalizado");
        }
        if (STATUS_CANCELADO.equals(tramite.getStatus())) {
            throw new IllegalArgumentException("El trámite ya está cancelado");
        }
    }

    private String resolveActorDisplay(String username) {
        if (username == null || username.isBlank()) {
            return "Sistema";
        }
        return userRepository.findByUsername(username)
                .map(this::displayName)
                .orElse(username);
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return user.getUsername();
    }

    private String normalizePriority(String priority) {
        String p = priority.trim().toUpperCase(Locale.ROOT);
        return switch (p) {
            case "BAJA", "NORMAL", "ALTA", "URGENTE" -> p;
            default -> throw new IllegalArgumentException("Prioridad no válida");
        };
    }

    private String statusLabel(String status) {
        return switch (status) {
            case STATUS_INICIADO -> "Iniciado";
            case STATUS_EN_PROCESO -> "En proceso";
            case STATUS_COMPLETADO -> "Finalizado";
            case STATUS_CANCELADO -> "Cancelado";
            default -> status;
        };
    }

    private TraceItem buildTraceEvent(
            String eventType,
            String eventLabel,
            String userId,
            String userFullName,
            String previousStatus,
            String newStatus,
            LocalDateTime occurredAt,
            String comment
    ) {
        TraceItem item = new TraceItem();
        item.setEventType(eventType);
        item.setEventLabel(eventLabel);
        item.setUserId(userId);
        item.setUserName(userFullName);
        item.setUserFullName(userFullName);
        item.setPreviousStatus(previousStatus);
        item.setNewStatus(newStatus);
        item.setOccurredAt(occurredAt);
        item.setStartedAt(occurredAt);
        item.setComment(comment);
        return item;
    }

    private void addActivityCompletedTrace(Tramite tramite, String userId, String actorDisplay, String comment) {
        TraceItem item = new TraceItem();
        item.setEventType("ACTIVIDAD_COMPLETADA");
        item.setEventLabel("Actividad completada");
        item.setActivityName(tramite.getCurrentActivity());
        item.setResponsible(tramite.getResponsible());
        item.setUserId(userId);
        item.setUserName(actorDisplay);
        item.setUserFullName(actorDisplay);
        item.setPreviousStatus(statusLabel(tramite.getStatus()));
        item.setOccurredAt(LocalDateTime.now());
        item.setStartedAt(item.getOccurredAt());
        item.setCompletedAt(item.getOccurredAt());
        item.setStatus("Completada");
        item.setComment(comment != null && !comment.isBlank() ? comment.trim() : "Actividad " + tramite.getCurrentActivity() + " completada");
        tramite.getTrace().add(item);
    }

    private List<TramiteTask> buildTasks(
            ActivityDiagram diagram,
            BusinessPolicy policy,
            String firstActivityName,
            LocalDateTime startedAt
    ) {
        List<String> names = extractTaskNames(diagram, policy);
        List<TramiteTask> tasks = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            TramiteTask task = new TramiteTask();
            task.setName(names.get(i));
            task.setResponsible(resolveTaskResponsible(names.get(i), diagram, policy));
            if (names.get(i).equalsIgnoreCase(firstActivityName)) {
                task.setStatus(TASK_EN_CURSO);
                task.setStartedAt(startedAt);
                task.setNotes("Actividad inicial del trámite");
            } else {
                task.setStatus(TASK_PENDIENTE);
            }
            task.setOrder(i + 1);
            tasks.add(task);
        }
        return tasks;
    }

    private List<String> extractTaskNames(ActivityDiagram diagram, BusinessPolicy policy) {
        if (diagram != null && diagram.getNodes() != null) {
            List<DiagramNode> actions = diagram.getNodes().stream()
                    .filter(n -> "ACTION".equalsIgnoreCase(n.getType()))
                    .sorted(Comparator.comparingDouble(DiagramNode::getX))
                    .toList();
            if (!actions.isEmpty()) {
                return actions.stream()
                        .map(n -> n.getLabel() != null && !n.getLabel().isBlank() ? n.getLabel().trim() : DEFAULT_ACTIVITY)
                        .distinct()
                        .toList();
            }
        }
        return new ArrayList<>(DEFAULT_TASK_NAMES);
    }

    private String resolveTaskResponsible(String taskName, ActivityDiagram diagram, BusinessPolicy policy) {
        if (diagram != null && diagram.getNodes() != null) {
            Optional<DiagramNode> node = diagram.getNodes().stream()
                    .filter(n -> taskName.equalsIgnoreCase(n.getLabel()))
                    .findFirst();
            if (node.isPresent() && node.get().getLane() != null && !node.get().getLane().isBlank()) {
                return node.get().getLane().trim();
            }
        }
        return defaultResponsible(policy);
    }

    private void completeCurrentTask(Tramite tramite) {
        LocalDateTime now = LocalDateTime.now();
        for (TramiteTask task : tramite.getTasks()) {
            if (TASK_EN_CURSO.equals(task.getStatus())) {
                task.setStatus(TASK_COMPLETADA);
                task.setCompletedAt(now);
                return;
            }
        }
    }

    private void activateTask(Tramite tramite, String activityName, LocalDateTime startedAt) {
        for (TramiteTask task : tramite.getTasks()) {
            if (activityName.equalsIgnoreCase(task.getName()) && TASK_PENDIENTE.equals(task.getStatus())) {
                task.setStatus(TASK_EN_CURSO);
                task.setStartedAt(startedAt);
                return;
            }
        }
    }

    private void markAllTasksCompleted(Tramite tramite) {
        LocalDateTime now = LocalDateTime.now();
        for (TramiteTask task : tramite.getTasks()) {
            task.setStatus(TASK_COMPLETADA);
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(now);
            }
        }
    }

    private DiagramNode findCurrentNode(Tramite tramite, ActivityDiagram diagram) {
        if (diagram == null || diagram.getNodes() == null) {
            return null;
        }
        if (tramite.getCurrentNodeId() != null) {
            Optional<DiagramNode> byId = diagram.getNodes().stream()
                    .filter(n -> tramite.getCurrentNodeId().equals(n.getId()))
                    .findFirst();
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        return diagram.getNodes().stream()
                .filter(n -> tramite.getCurrentActivity().equalsIgnoreCase(n.getLabel()))
                .findFirst()
                .orElse(null);
    }

    private ResolvedActivity resolveFirstActivity(ActivityDiagram diagram, BusinessPolicy policy) {
        if (diagram != null && diagram.getNodes() != null && !diagram.getNodes().isEmpty()) {
            Optional<DiagramNode> initial = diagram.getNodes().stream()
                    .filter(n -> "INITIAL".equalsIgnoreCase(n.getType()))
                    .findFirst();

            if (initial.isPresent()) {
                DiagramNode workNode = findNextWorkNode(initial.get(), diagram);
                if (workNode != null) {
                    return toResolvedActivity(workNode, policy);
                }
            }

            Optional<DiagramNode> firstAction = diagram.getNodes().stream()
                    .filter(n -> "ACTION".equalsIgnoreCase(n.getType()))
                    .min(Comparator.comparingDouble(DiagramNode::getX));
            if (firstAction.isPresent()) {
                return toResolvedActivity(firstAction.get(), policy);
            }
        }

        return new ResolvedActivity(DEFAULT_ACTIVITY, defaultResponsible(policy), null, false);
    }

    private ResolvedActivity resolveNextActivity(DiagramNode currentNode, ActivityDiagram diagram, BusinessPolicy policy) {
        if (currentNode == null || diagram == null || diagram.getNodes() == null || diagram.getEdges() == null) {
            return null;
        }

        DiagramNode cursor = currentNode;
        Set<String> visited = new HashSet<>();

        while (cursor != null && visited.add(cursor.getId())) {
            final DiagramNode nodeRef = cursor;
            List<DiagramEdge> outgoing = diagram.getEdges().stream()
                    .filter(e -> nodeRef.getId().equals(e.getSourceId()))
                    .toList();

            if (outgoing.isEmpty()) {
                return null;
            }

            String targetId = outgoing.get(0).getTargetId();
            DiagramNode target = diagram.getNodes().stream()
                    .filter(n -> n.getId().equals(targetId))
                    .findFirst()
                    .orElse(null);

            if (target == null) {
                return null;
            }

            if ("FINAL".equalsIgnoreCase(target.getType())) {
                return new ResolvedActivity("Finalizado", "—", target.getId(), true);
            }

            if ("ACTION".equalsIgnoreCase(target.getType())) {
                return toResolvedActivity(target, policy);
            }

            cursor = target;
        }

        return null;
    }

    private DiagramNode findNextWorkNode(DiagramNode from, ActivityDiagram diagram) {
        DiagramNode cursor = from;
        Set<String> visited = new HashSet<>();

        while (cursor != null && visited.add(cursor.getId())) {
            if ("ACTION".equalsIgnoreCase(cursor.getType())) {
                return cursor;
            }
            if ("FINAL".equalsIgnoreCase(cursor.getType())) {
                return null;
            }

            final DiagramNode nodeRef = cursor;
            List<DiagramEdge> outgoing = diagram.getEdges().stream()
                    .filter(e -> nodeRef.getId().equals(e.getSourceId()))
                    .toList();
            if (outgoing.isEmpty()) {
                return null;
            }

            String targetId = outgoing.get(0).getTargetId();
            cursor = diagram.getNodes().stream()
                    .filter(n -> n.getId().equals(targetId))
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    private ResolvedActivity toResolvedActivity(DiagramNode node, BusinessPolicy policy) {
        return new ResolvedActivity(
                node.getLabel() != null && !node.getLabel().isBlank() ? node.getLabel().trim() : DEFAULT_ACTIVITY,
                node.getLane() != null && !node.getLane().isBlank() ? node.getLane().trim() : defaultResponsible(policy),
                node.getId(),
                false
        );
    }

    private String defaultResponsible(BusinessPolicy policy) {
        if (policy.getResponsible() != null && !policy.getResponsible().isBlank()) {
            return policy.getResponsible().trim();
        }
        return "Sin asignar";
    }

    private int calculateProgress(Tramite tramite, ActivityDiagram diagram) {
        int totalSteps = countWorkActivities(diagram);
        if (totalSteps <= 0) {
            totalSteps = tramite.getTasks().isEmpty() ? DEFAULT_TASK_NAMES.size() : tramite.getTasks().size();
        }
        long completed = tramite.getTasks().stream()
                .filter(t -> TASK_COMPLETADA.equals(t.getStatus()))
                .count();
        int progress = (int) Math.round((completed * 100.0) / totalSteps);
        return Math.min(95, Math.max(10, progress));
    }

    private int countWorkActivities(ActivityDiagram diagram) {
        if (diagram == null || diagram.getNodes() == null) {
            return 0;
        }
        return (int) diagram.getNodes().stream()
                .filter(n -> "ACTION".equalsIgnoreCase(n.getType()))
                .count();
    }

    private String generateNextCode() {
        int max = tramiteRepository.findAll().stream()
                .map(Tramite::getCode)
                .mapToInt(this::parseCodeNumber)
                .max()
                .orElse(0);
        return String.format("TRM-%03d", max + 1);
    }

    private int parseCodeNumber(String code) {
        if (code == null) {
            return 0;
        }
        Matcher matcher = CODE_PATTERN.matcher(code.trim());
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private record ResolvedActivity(String label, String responsible, String nodeId, boolean finalStep) {
    }
}
