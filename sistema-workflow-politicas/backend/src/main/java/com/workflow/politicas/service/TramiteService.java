package com.workflow.politicas.service;



import com.workflow.politicas.dto.TramiteAdvanceRequest;

import com.workflow.politicas.dto.TramiteCancelRequest;

import com.workflow.politicas.dto.TramiteCreateRequest;

import com.workflow.politicas.dto.WorkflowRoutingResult;

import com.workflow.politicas.model.BusinessPolicy;

import com.workflow.politicas.model.FormSubmission;

import com.workflow.politicas.model.ResponseItem;

import com.workflow.politicas.model.TraceItem;

import com.workflow.politicas.model.Tramite;

import com.workflow.politicas.model.TramiteTask;

import com.workflow.politicas.model.User;

import com.workflow.politicas.model.WorkflowActivity;

import com.workflow.politicas.repository.BusinessPolicyRepository;

import com.workflow.politicas.repository.FormSubmissionRepository;

import com.workflow.politicas.repository.RoleRepository;

import com.workflow.politicas.repository.TramiteRepository;

import com.workflow.politicas.repository.UserRepository;

import com.workflow.politicas.repository.WorkflowActivityRepository;

import org.springframework.stereotype.Service;



import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.Comparator;

import java.util.List;

import java.util.Locale;

import java.util.Map;

import java.util.Objects;

import java.util.Optional;

import java.util.UUID;

import java.util.regex.Matcher;

import java.util.regex.Pattern;



/**

 * Ejecución de trámites Ciclo 1 — motor sobre {@link WorkflowActivity} / {@link WorkflowTransition} (F1).

 */

@Service

public class TramiteService {



    private static final String STATUS_INICIADO = "INICIADO";

    private static final String STATUS_EN_PROCESO = "EN_PROCESO";

    private static final String STATUS_COMPLETADO = "COMPLETADO";

    private static final String STATUS_CANCELADO = "CANCELADO";

    private static final String TASK_PENDIENTE = "PENDIENTE";

    private static final String TASK_EN_CURSO = "EN_CURSO";

    private static final String TASK_COMPLETADA = "COMPLETADA";

    private static final int MAX_TASK_VISITS_PER_ACTIVITY = 20;

    private static final Pattern CODE_PATTERN = Pattern.compile("^TRM-(\\d+)$");



    private final TramiteRepository tramiteRepository;

    private final BusinessPolicyRepository businessPolicyRepository;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final WorkflowActivityRepository workflowActivityRepository;

    private final WorkflowRoutingService workflowRoutingService;

    private final BitacoraService bitacoraService;

    private final FormSubmissionRepository formSubmissionRepository;

    private final FormSubmissionFileService formSubmissionFileService;

    private final DocumentRepositoryService documentRepositoryService;



    public TramiteService(

            TramiteRepository tramiteRepository,

            BusinessPolicyRepository businessPolicyRepository,

            UserRepository userRepository,

            RoleRepository roleRepository,

            WorkflowActivityRepository workflowActivityRepository,

            WorkflowRoutingService workflowRoutingService,

            BitacoraService bitacoraService,

            FormSubmissionRepository formSubmissionRepository,

            FormSubmissionFileService formSubmissionFileService,

            DocumentRepositoryService documentRepositoryService

    ) {

        this.tramiteRepository = tramiteRepository;

        this.businessPolicyRepository = businessPolicyRepository;

        this.userRepository = userRepository;

        this.roleRepository = roleRepository;

        this.workflowActivityRepository = workflowActivityRepository;

        this.workflowRoutingService = workflowRoutingService;

        this.bitacoraService = bitacoraService;

        this.formSubmissionRepository = formSubmissionRepository;

        this.formSubmissionFileService = formSubmissionFileService;

        this.documentRepositoryService = documentRepositoryService;

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



        workflowRoutingService.requireStartActivity(policy.getId());

        WorkflowRoutingResult routing = workflowRoutingService.resolveFirstWorkTasks(policy.getId());

        if (routing.getOutcome() == WorkflowRoutingResult.Outcome.WORKFLOW_ERROR) {

            throw new IllegalStateException(routing.getErrorDetail());

        }



        LocalDateTime now = LocalDateTime.now();

        String requesterDisplay = displayName(requester);

        String actorDisplay = resolveActorDisplay(authenticatedUsername);



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

        tramite.setCreatedBy(authenticatedUsername);

        tramite.setCreatedAt(now);

        tramite.setUpdatedAt(now);

        tramite.setTasks(new ArrayList<>());

        tramite.setTrace(new ArrayList<>());

        tramite.setWorkflowError(null);



        if (routing.getOutcome() == WorkflowRoutingResult.Outcome.COMPLETED) {

            finalizeTramite(tramite, now);

            tramite.getTrace().add(traceEvent(

                    "TRAMITE_INICIADO",

                    "Trámite iniciado y finalizado",

                    requester.getUsername(),

                    requesterDisplay,

                    null,

                    statusLabel(STATUS_COMPLETADO),

                    now,

                    request.getDescription().trim(),

                    null,

                    null,

                    null

            ));

        } else {

            applyRoutingActivation(tramite, policy.getId(), routing, now, true);

            boolean hasActiveTask = tramite.getTasks().stream()
                    .anyMatch(t -> TASK_EN_CURSO.equals(t.getStatus()) || TASK_PENDIENTE.equals(t.getStatus()));
            tramite.setStatus(hasActiveTask ? STATUS_EN_PROCESO : STATUS_INICIADO);

            tramite.getTrace().add(traceEvent(

                    "PROCESO_CREADO",

                    "Proceso creado",

                    authenticatedUsername,

                    actorDisplay,

                    null,

                    statusLabel(STATUS_INICIADO),

                    now,

                    "Trámite " + tramite.getCode() + " registrado para la política " + policy.getName(),

                    null,

                    null,

                    null

            ));

            tramite.getTrace().add(traceEvent(

                    "TRAMITE_INICIADO",

                    "Trámite iniciado",

                    requester.getUsername(),

                    requesterDisplay,

                    null,

                    statusLabel(STATUS_INICIADO),

                    now,

                    request.getDescription().trim(),

                    null,

                    tramite.getCurrentWorkflowActivityId(),

                    tramite.getCurrentActivity()

            ));

        }



        Tramite saved = tramiteRepository.save(tramite);

        documentRepositoryService.createForTramite(saved, authenticatedUsername);

        bitacoraService.registrar(

                authenticatedUsername,

                "Trámites",

                "INICIAR_TRAMITE",

                actorDisplay + " creó el trámite " + saved.getCode() + " para la política " + policy.getName(),

                "Tramite",

                saved.getId()

        );

        return enrichAndPersistIfNeeded(saved);

    }



    /**

     * Avance automático tras completar una actividad (vía Mis actividades / formulario).

     */

    public Tramite advanceWithTaskCompletion(

            String tramiteId,

            String completedWorkflowActivityId,

            int completedTaskOrder,

            Map<String, Object> stepData,

            String authenticatedUsername,

            String comment

    ) {

        Tramite tramite = tramiteRepository.findById(tramiteId)

                .orElseThrow(() -> new IllegalArgumentException("Trámite no encontrado"));



        ensureNotClosed(tramite);



        TramiteTask completedTask = findTaskByOrder(tramite, completedTaskOrder)

                .orElseThrow(() -> new IllegalArgumentException("La tarea indicada no existe"));



        if (!TASK_EN_CURSO.equals(completedTask.getStatus())) {

            throw new IllegalArgumentException("La tarea no está en curso");

        }



        if (completedWorkflowActivityId != null

                && completedTask.getWorkflowActivityId() != null

                && !completedWorkflowActivityId.equals(completedTask.getWorkflowActivityId())) {

            throw new IllegalArgumentException("La actividad no corresponde a la tarea en curso");

        }



        String activityId = completedTask.getWorkflowActivityId() != null

                ? completedTask.getWorkflowActivityId()

                : completedWorkflowActivityId;



        if (activityId == null || activityId.isBlank()) {

            activityId = workflowRoutingService.resolveCanonicalActivityId(

                    tramite.getPolicyId(), null, completedTask.getName());

        } else {

            activityId = workflowRoutingService.resolveCanonicalActivityId(

                    tramite.getPolicyId(), activityId, completedTask.getName());

        }

        if (activityId == null || activityId.isBlank()) {

            throw new IllegalStateException("La tarea no tiene actividad de workflow asociada");

        }

        if (!activityId.equals(completedTask.getWorkflowActivityId())) {

            completedTask.setWorkflowActivityId(activityId);

        }

        com.workflow.politicas.util.Cu7WorkflowDebugLog.advance(

                "advanceWithTaskCompletion tramiteId={} codigo={} currentWorkflowActivityId={} taskOrder={} task.workflowActivityId={} task.status={} task.takenBy={} fromActivity={}",

                tramite.getId(),

                tramite.getCode(),

                tramite.getCurrentWorkflowActivityId(),

                completedTaskOrder,

                activityId,

                completedTask.getStatus(),

                completedTask.getTakenBy(),

                completedTask.getName()

        );



        return processTaskCompletion(tramite, completedTask, activityId, stepData, authenticatedUsername, comment);

    }



    /**

     * Avance manual — solo administradores (depuración). El funcionario debe usar Mis actividades.

     */

    public Tramite advance(String id, TramiteAdvanceRequest request, String authenticatedUsername) {

        if (!isWorkflowAdmin(authenticatedUsername)) {

            throw new IllegalArgumentException(

                    "El avance manual está deshabilitado. Complete su actividad asignada; el sistema enrutará automáticamente.");

        }



        Tramite tramite = tramiteRepository.findById(id)

                .orElseThrow(() -> new RuntimeException("Tramite not found with id: " + id));



        ensureNotClosed(tramite);



        TramiteTask current = findFirstInProgressTask(tramite)

                .orElseThrow(() -> new IllegalArgumentException("No hay tarea en curso"));



        String activityId = current.getWorkflowActivityId();

        if (activityId == null || activityId.isBlank()) {

            activityId = workflowRoutingService.resolveCanonicalActivityId(

                    tramite.getPolicyId(), null, current.getName());

        } else {

            activityId = workflowRoutingService.resolveCanonicalActivityId(

                    tramite.getPolicyId(), activityId, current.getName());

        }

        if (activityId == null || activityId.isBlank()) {

            throw new IllegalStateException("El trámite no tiene actividad de workflow en curso");

        }

        if (!activityId.equals(current.getWorkflowActivityId())) {

            current.setWorkflowActivityId(activityId);

        }



        Map<String, Object> stepData = request != null && request.getStepData() != null

                ? request.getStepData()

                : Map.of();



        return processTaskCompletion(

                tramite,

                current,

                activityId,

                stepData,

                authenticatedUsername,

                request != null ? request.getComment() : null

        );

    }



    /**
     * Trazabilidad F4 — formulario enviado al completar actividad.
     */
    public void recordFormSubmittedTrace(
            String tramiteId,
            String authenticatedUsername,
            String workflowActivityId,
            String activityName,
            int taskOrder
    ) {
        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new IllegalArgumentException("Trámite no encontrado"));

        LocalDateTime now = LocalDateTime.now();
        String actorDisplay = resolveActorDisplay(authenticatedUsername);

        tramite.getTrace().add(traceEvent(
                "COMPLETAR_ACTIVIDAD",
                "Formulario enviado",
                authenticatedUsername,
                actorDisplay,
                statusLabel(tramite.getStatus()),
                statusLabel(tramite.getStatus()),
                now,
                "Formulario de la actividad " + activityName + " (tarea #" + taskOrder + ")",
                workflowActivityId,
                null,
                activityName
        ));
        tramite.setUpdatedAt(now);
        tramiteRepository.save(tramite);

        bitacoraService.registrar(
                authenticatedUsername,
                "Mis actividades",
                "COMPLETAR_ACTIVIDAD",
                actorDisplay + " envió el formulario de " + activityName + " en " + tramite.getCode(),
                "Tramite",
                tramite.getId()
        );
    }

    public void recordAiFormAssistedTrace(
            String tramiteId,
            String authenticatedUsername,
            String workflowActivityId,
            String activityName,
            int taskOrder,
            int fieldsSuggested,
            int fieldsApplied
    ) {
        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new IllegalArgumentException("Trámite no encontrado"));

        LocalDateTime now = LocalDateTime.now();
        String actorDisplay = resolveActorDisplay(authenticatedUsername);
        String summary = "Asistencia IA en formulario: "
                + fieldsApplied + " campo(s) aplicado(s) de "
                + fieldsSuggested + " sugerido(s). Actividad " + activityName + " (tarea #" + taskOrder + ").";

        tramite.getTrace().add(traceEvent(
                "ASISTENCIA_FORMULARIO_IA",
                "Asistencia IA en formulario",
                authenticatedUsername,
                actorDisplay,
                statusLabel(tramite.getStatus()),
                statusLabel(tramite.getStatus()),
                now,
                summary,
                workflowActivityId,
                null,
                activityName
        ));
        tramite.setUpdatedAt(now);
        tramiteRepository.save(tramite);

        bitacoraService.registrar(
                authenticatedUsername,
                "Mis actividades",
                "ASISTENCIA_FORMULARIO_IA",
                actorDisplay + " usó asistencia IA en formulario de " + activityName + " (" + tramite.getCode() + ")",
                "Tramite",
                tramite.getId()
        );
    }

    /**
     * Tomar una tarea PENDIENTE (bandeja F2).
     */
    public Tramite takeWorkflowTask(String tramiteId, int taskOrder, String authenticatedUsername) {
        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new IllegalArgumentException("Trámite no encontrado"));

        ensureNotClosed(tramite);

        TramiteTask task = findTaskByOrder(tramite, taskOrder)
                .orElseThrow(() -> new IllegalArgumentException("La tarea indicada no existe"));

        boolean unclaimedInProgress = TASK_EN_CURSO.equals(task.getStatus())
                && (task.getTakenBy() == null || task.getTakenBy().isBlank());
        if (!TASK_PENDIENTE.equals(task.getStatus()) && !unclaimedInProgress) {
            throw new IllegalArgumentException("La tarea no está disponible para tomar");
        }

        LocalDateTime now = LocalDateTime.now();
        String actorDisplay = resolveActorDisplay(authenticatedUsername);

        task.setStatus(TASK_EN_CURSO);
        task.setTakenBy(authenticatedUsername);
        task.setTakenAt(now);
        task.setStartedAt(now);

        tramite.setStatus(STATUS_EN_PROCESO);
        if (task.getWorkflowActivityId() != null) {
            tramite.setCurrentWorkflowActivityId(task.getWorkflowActivityId());
        }
        tramite.setCurrentActivity(task.getName());
        tramite.setResponsible(task.getResponsible());
        tramite.setUpdatedAt(now);

        tramite.getTrace().add(traceEvent(
                "TAREA_TOMADA",
                "Tarea tomada",
                authenticatedUsername,
                actorDisplay,
                statusLabel(tramite.getStatus()),
                statusLabel(STATUS_EN_PROCESO),
                now,
                "El usuario tomó la tarea " + task.getName(),
                task.getWorkflowActivityId(),
                null,
                task.getName()
        ));

        Tramite saved = tramiteRepository.save(tramite);
        bitacoraService.registrar(
                authenticatedUsername,
                "Mis actividades",
                "TOMAR_TAREA",
                actorDisplay + " tomó la tarea " + task.getName() + " del trámite " + saved.getCode(),
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

        tramite.setCurrentWorkflowActivityId(null);

        tramite.setPendingJoinActivityId(null);

        tramite.setActiveParallelGroupId(null);

        tramite.setResponsible("—");

        tramite.setUpdatedAt(now);



        for (TramiteTask task : tramite.getTasks()) {

            if (!TASK_COMPLETADA.equals(task.getStatus())) {

                task.setStatus(TASK_COMPLETADA);

                task.setCompletedAt(now);

            }

        }



        tramite.getTrace().add(traceEvent(

                "TRAMITE_CANCELADO",

                "Trámite cancelado",

                authenticatedUsername,

                actorDisplay,

                statusLabel(previousStatus),

                statusLabel(STATUS_CANCELADO),

                now,

                comment,

                null,

                null,

                null

        ));



        Tramite saved = tramiteRepository.save(tramite);

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



    /**

     * Elimina un trámite cerrado (COMPLETADO o CANCELADO) y sus datos asociados.

     */

    public void delete(String id, String authenticatedUsername) {

        Tramite tramite = tramiteRepository.findById(id)

                .orElseThrow(() -> new IllegalArgumentException("Trámite no encontrado"));



        if (!isDeletableStatus(tramite.getStatus())) {

            throw new IllegalArgumentException(

                    "Solo se pueden eliminar trámites finalizados o cancelados. El trámite está en estado: "

                            + statusLabel(tramite.getStatus()));

        }



        String actorDisplay = resolveActorDisplay(authenticatedUsername);

        String tramiteCode = tramite.getCode() != null ? tramite.getCode() : id;



        deleteFormSubmissionsForTramite(id);

        bitacoraService.deleteByEntity("Tramite", id);



        bitacoraService.registrar(

                authenticatedUsername,

                "Trámites",

                "ELIMINAR_TRAMITE",

                actorDisplay + " eliminó el trámite " + tramiteCode,

                "Tramite",

                id

        );



        tramiteRepository.deleteById(id);

    }



    private boolean isDeletableStatus(String status) {

        if (status == null || status.isBlank()) {

            return false;

        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);

        return STATUS_COMPLETADO.equals(normalized)

                || STATUS_CANCELADO.equals(normalized)

                || "CANCELLED".equals(normalized)

                || "FINALIZADO".equals(normalized)

                || "COMPLETED".equals(normalized);

    }



    private void deleteFormSubmissionsForTramite(String tramiteId) {

        List<FormSubmission> submissions = formSubmissionRepository.findByTramiteId(tramiteId);

        for (FormSubmission submission : submissions) {

            if (submission.getResponses() != null) {

                for (ResponseItem item : submission.getResponses()) {

                    if (item.getFileId() != null && !item.getFileId().isBlank()) {

                        formSubmissionFileService.deleteIfExists(item.getFileId());

                    }

                }

            }

        }

        formSubmissionRepository.deleteByTramiteId(tramiteId);

    }



    private Tramite processTaskCompletion(

            Tramite tramite,

            TramiteTask completedTask,

            String completedActivityId,

            Map<String, Object> stepData,

            String authenticatedUsername,

            String comment

    ) {

        String previousStatus = tramite.getStatus();

        String actorDisplay = resolveActorDisplay(authenticatedUsername);

        LocalDateTime now = LocalDateTime.now();



        completeTask(completedTask, now);

        tramite.getTrace().add(traceEvent(
                "TAREA_COMPLETADA",
                "Tarea completada",
                authenticatedUsername,
                actorDisplay,
                statusLabel(tramite.getStatus()),
                statusLabel(tramite.getStatus()),
                now,
                "Tarea " + completedTask.getName() + " completada",
                completedActivityId,
                null,
                completedTask.getName()
        ));

        tramite.getTrace().add(traceEvent(

                "ACTIVIDAD_COMPLETADA",

                "Actividad completada",

                authenticatedUsername,

                actorDisplay,

                statusLabel(tramite.getStatus()),

                statusLabel(tramite.getStatus()),

                now,

                comment != null && !comment.isBlank() ? comment.trim() : "Actividad " + completedTask.getName() + " completada",

                completedActivityId,

                null,

                completedTask.getName()

        ));



        if (completedTask.getParallelGroupId() != null && !completedTask.getParallelGroupId().isBlank()) {

            boolean pendingParallel = tramite.getTasks().stream()

                    .filter(t -> completedTask.getParallelGroupId().equals(t.getParallelGroupId()))

                    .anyMatch(t -> TASK_EN_CURSO.equals(t.getStatus()) || TASK_PENDIENTE.equals(t.getStatus()));



            if (pendingParallel) {

                tramite.setUpdatedAt(now);

                tramite.getTrace().add(traceEvent(

                        "ESPERA_PARALELO",

                        "Esperando ramas paralelas",

                        authenticatedUsername,

                        actorDisplay,

                        null,

                        statusLabel(STATUS_EN_PROCESO),

                        now,

                        "Pendiente completar otras tareas del grupo paralelo",

                        completedActivityId,

                        tramite.getPendingJoinActivityId(),

                        null

                ));

                return tramiteRepository.save(tramite);

            }



            tramite.setActiveParallelGroupId(null);

            String joinId = tramite.getPendingJoinActivityId();

            if (joinId == null || joinId.isBlank()) {
                List<String> branchActivityIds = tramite.getTasks().stream()
                        .filter(t -> completedTask.getParallelGroupId().equals(t.getParallelGroupId()))
                        .map(TramiteTask::getWorkflowActivityId)
                        .filter(Objects::nonNull)
                        .filter(id -> !id.isBlank())
                        .distinct()
                        .toList();
                joinId = workflowRoutingService
                        .resolveJoinActivityAfterParallelSplit(tramite.getPolicyId(), branchActivityIds)
                        .orElse(null);
            }

            tramite.setPendingJoinActivityId(null);



            if (joinId != null && !joinId.isBlank()) {

                WorkflowRoutingResult joinRouting = workflowRoutingService.resolveActivationAtActivity(

                        tramite.getPolicyId(),

                        joinId,

                        stepData

                );

                return applyRoutingResult(tramite, joinRouting, stepData, authenticatedUsername, actorDisplay, previousStatus, now);

            }

        }



        WorkflowRoutingResult routing = workflowRoutingService.routeAfterCompletedActivity(

                tramite.getPolicyId(),

                completedActivityId,

                completedTask.getName(),

                stepData

        );

        String nextActivitySummary = routing.getNextActivities() == null || routing.getNextActivities().isEmpty()
                ? "—"
                : routing.getNextActivities().stream()
                        .map(a -> a.getName() + "(" + a.getActivityType() + ")")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("—");

        com.workflow.politicas.util.Cu7WorkflowDebugLog.advance(
                "routingResult tramiteId={} codigo={} outcome={} message={} nextActivities={} stepData={}",
                tramite.getId(),
                tramite.getCode(),
                routing.getOutcome(),
                routing.getMessage() != null ? routing.getMessage() : routing.getErrorDetail(),
                nextActivitySummary,
                com.workflow.politicas.util.Cu7WorkflowDebugLog.stepDataSummary(stepData)
        );



        return applyRoutingResult(tramite, routing, stepData, authenticatedUsername, actorDisplay, previousStatus, now);

    }



    private Tramite applyRoutingResult(

            Tramite tramite,

            WorkflowRoutingResult routing,

            Map<String, Object> stepData,

            String authenticatedUsername,

            String actorDisplay,

            String previousStatus,

            LocalDateTime now

    ) {

        switch (routing.getOutcome()) {

            case COMPLETED -> {

                finalizeTramite(tramite, now);

                tramite.getTrace().add(traceEvent(

                        "TRAMITE_FINALIZADO",

                        "Trámite finalizado",

                        authenticatedUsername,

                        actorDisplay,

                        statusLabel(previousStatus),

                        statusLabel(STATUS_COMPLETADO),

                        now,

                        routing.getMessage(),

                        tramite.getCurrentWorkflowActivityId(),

                        null,

                        "Finalizado"

                ));

            }

            case ACTIVATE_TASKS -> {

                tramite.setStatus(STATUS_EN_PROCESO);

                tramite.setWorkflowError(null);

                assertIterationLimits(tramite, routing.getNextActivities());

                int openBefore = countOpenTasks(tramite);

                applyRoutingActivation(tramite, tramite.getPolicyId(), routing, now, false);

                if (countOpenTasks(tramite) <= openBefore
                        && routing.getNextActivities() != null
                        && routing.getNextActivities().stream()
                                .anyMatch(a -> "TASK".equalsIgnoreCase(a.getActivityType()))) {
                    String detail = "El motor no pudo asignar tareas ejecutables. Revise el diagrama UML (actividades TASK con responsable).";
                    tramite.setWorkflowError(detail);
                    throw new IllegalStateException(detail);
                }

                String nextNames = routing.getNextActivities().stream()

                        .map(WorkflowActivity::getName)

                        .reduce((a, b) -> a + ", " + b)

                        .orElse("—");

                String nextIds = routing.getNextActivities().stream()

                        .map(WorkflowActivity::getId)

                        .findFirst()

                        .orElse(null);

                tramite.getTrace().add(traceEvent(

                        "PROCESO_AVANZADO",

                        "Proceso avanzado",

                        authenticatedUsername,

                        actorDisplay,

                        statusLabel(previousStatus),

                        statusLabel(STATUS_EN_PROCESO),

                        now,

                        routing.getMessage() + " → " + nextNames,

                        tramite.getCurrentWorkflowActivityId(),

                        nextIds,

                        nextNames

                ));

            }

            case WAIT_PARALLEL -> {

                tramite.setStatus(STATUS_EN_PROCESO);

                tramite.setUpdatedAt(now);

            }

            case WORKFLOW_ERROR -> {

                tramite.setWorkflowError(routing.getErrorDetail());

                tramite.setUpdatedAt(now);

                tramite.getTrace().add(traceEvent(

                        "ERROR_WORKFLOW",

                        "Error de workflow",

                        authenticatedUsername,

                        actorDisplay,

                        statusLabel(previousStatus),

                        statusLabel(tramite.getStatus()),

                        now,

                        routing.getErrorDetail(),

                        tramite.getCurrentWorkflowActivityId(),

                        null,

                        null

                ));

                throw new IllegalStateException(routing.getErrorDetail());

            }

        }



        tramite.setUpdatedAt(now);

        tramite.setProgress(calculateProgress(tramite));

        Tramite saved = tramiteRepository.save(tramite);

        bitacoraService.registrar(

                authenticatedUsername,

                "Trámites",

                "AVANZAR_TRAMITE",

                actorDisplay + " avanzó el trámite " + saved.getCode() + " (enrutamiento automático)",

                "Tramite",

                saved.getId()

        );

        return saved;

    }



    private void applyRoutingActivation(

            Tramite tramite,

            String policyId,

            WorkflowRoutingResult routing,

            LocalDateTime now,

            boolean initial

    ) {

        List<WorkflowActivity> next = routing.getNextActivities();

        if (next == null || next.isEmpty()) {

            return;

        }



        String parallelGroupId = null;

        if (next.size() > 1) {

            parallelGroupId = UUID.randomUUID().toString();

            tramite.setActiveParallelGroupId(parallelGroupId);

            tramite.setPendingJoinActivityId(routing.getPendingJoinActivityId());

        } else {

            tramite.setActiveParallelGroupId(null);

            if (routing.getPendingJoinActivityId() != null) {

                tramite.setPendingJoinActivityId(routing.getPendingJoinActivityId());

            }

        }



        WorkflowActivity primary = next.get(0);

        tramite.setCurrentWorkflowActivityId(primary.getId());

        tramite.setCurrentActivity(primary.getName());

        tramite.setResponsible(workflowRoutingService.formatResponsible(primary));

        tramite.setStatus(STATUS_EN_PROCESO);



        for (WorkflowActivity activity : next) {

            if (!"TASK".equalsIgnoreCase(activity.getActivityType())) {

                continue;

            }

            if (activity.getId() != null && tramite.getTasks().stream().anyMatch(t ->
                    activity.getId().equals(t.getWorkflowActivityId())
                            && (TASK_PENDIENTE.equals(t.getStatus()) || TASK_EN_CURSO.equals(t.getStatus())))) {
                continue;
            }

            if (!hasResponsibleConfigured(activity)) {
                throw new IllegalStateException(
                        "La actividad \"" + activity.getName() + "\" no tiene responsable configurado.");
            }

            TramiteTask task = new TramiteTask();

            task.setWorkflowActivityId(activity.getId());

            task.setName(activity.getName());

            task.setResponsible(workflowRoutingService.formatResponsible(activity));

            task.setStatus(TASK_PENDIENTE);

            task.setOrder(nextTaskOrder(tramite));

            task.setParallelGroupId(parallelGroupId);

            task.setNotes(initial ? "Actividad inicial del trámite" : "Actividad asignada por el motor de workflow");

            tramite.getTrace().add(traceEvent(
                    "TAREA_ASIGNADA",
                    "Tarea asignada",
                    null,
                    task.getResponsible(),
                    null,
                    statusLabel(STATUS_EN_PROCESO),
                    now,
                    "Tarea " + task.getName() + " asignada (" + task.getStatus() + ")",
                    task.getWorkflowActivityId(),
                    null,
                    task.getName()
            ));

            tramite.getTasks().add(task);

            com.workflow.politicas.util.Cu7WorkflowDebugLog.advance(
                    "tareaCreada tramiteId={} codigo={} taskOrder={} workflowActivityId={} name={} status={}",
                    tramite.getId(),
                    tramite.getCode(),
                    task.getOrder(),
                    task.getWorkflowActivityId(),
                    task.getName(),
                    task.getStatus()
            );

        }

    }



    private void finalizeTramite(Tramite tramite, LocalDateTime now) {

        tramite.setStatus(STATUS_COMPLETADO);

        tramite.setCurrentActivity("Finalizado");

        tramite.setCurrentWorkflowActivityId(null);

        tramite.setPendingJoinActivityId(null);

        tramite.setActiveParallelGroupId(null);

        tramite.setResponsible("—");

        tramite.setProgress(100);

        tramite.setWorkflowError(null);

        for (TramiteTask task : tramite.getTasks()) {

            task.setStatus(TASK_COMPLETADA);

            if (task.getCompletedAt() == null) {

                task.setCompletedAt(now);

            }

        }

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



        if (tramite.getCurrentWorkflowActivityId() == null

                && tramite.getPolicyId() != null

                && tramite.getCurrentActivity() != null) {

            workflowActivityRepository.findByPolicyId(tramite.getPolicyId()).stream()

                    .filter(WorkflowActivity::isActive)

                    .filter(a -> tramite.getCurrentActivity().equalsIgnoreCase(a.getName()))

                    .findFirst()

                    .ifPresent(a -> {

                        tramite.setCurrentWorkflowActivityId(a.getId());

                    });

            if (tramite.getCurrentWorkflowActivityId() != null) {

                changed = true;

            }

        }



        if (tramite.getTasks() == null) {

            tramite.setTasks(new ArrayList<>());

            changed = true;

        }



        for (TramiteTask task : tramite.getTasks()) {

            if (TASK_EN_CURSO.equals(task.getStatus()) && task.getStartedAt() == null) {

                task.setStartedAt(tramite.getCreatedAt());

                changed = true;

            }

            if (task.getWorkflowActivityId() == null

                    && tramite.getPolicyId() != null

                    && task.getName() != null) {

                workflowActivityRepository.findByPolicyId(tramite.getPolicyId()).stream()

                        .filter(WorkflowActivity::isActive)

                        .filter(a -> task.getName().equalsIgnoreCase(a.getName()))

                        .findFirst()

                        .ifPresent(a -> task.setWorkflowActivityId(a.getId()));

                if (task.getWorkflowActivityId() != null) {

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



    private int nextTaskOrder(Tramite tramite) {

        return tramite.getTasks().stream()

                .mapToInt(TramiteTask::getOrder)

                .max()

                .orElse(0) + 1;

    }



    private int calculateProgress(Tramite tramite) {

        int total = workflowRoutingService.countExecutableTasks(tramite.getPolicyId());

        if (total <= 0) {

            total = Math.max(1, tramite.getTasks().size());

        }

        long completed = tramite.getTasks().stream()

                .filter(t -> TASK_COMPLETADA.equals(t.getStatus()))

                .count();

        int progress = (int) Math.round((completed * 100.0) / total);

        return Math.min(99, Math.max(5, progress));

    }



    private Optional<TramiteTask> findTaskByOrder(Tramite tramite, int order) {

        return tramite.getTasks().stream()

                .filter(t -> t.getOrder() == order)

                .findFirst();

    }



    private Optional<TramiteTask> findFirstInProgressTask(Tramite tramite) {

        return tramite.getTasks().stream()

                .filter(t -> TASK_EN_CURSO.equals(t.getStatus()))

                .findFirst();

    }



    private void completeTask(TramiteTask task, LocalDateTime now) {

        task.setStatus(TASK_COMPLETADA);

        task.setCompletedAt(now);

    }



    private boolean isWorkflowAdmin(String username) {

        if (username == null || username.isBlank()) {

            return false;

        }

        return userRepository.findByUsername(username)

                .map(user -> {

                    if (user.getRoleIds() == null) {

                        return false;

                    }

                    for (String roleId : user.getRoleIds()) {

                        Optional<String> name = roleRepository.findById(roleId).map(r -> r.getName())

                                .or(() -> roleRepository.findByNameIgnoreCase(roleId).map(r -> r.getName()));

                        if (name.isPresent() && name.get().toLowerCase(Locale.ROOT).contains("administrador")) {

                            return true;

                        }

                    }

                    return false;

                })

                .orElse(false);

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



    private String resolveDisplayForUsername(String username) {

        if (username == null || username.isBlank()) {

            return null;

        }

        return userRepository.findByUsername(username.trim())

                .map(this::displayName)

                .orElse(username.trim());

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



    private TraceItem traceEvent(

            String eventType,

            String eventLabel,

            String userId,

            String userFullName,

            String previousStatus,

            String newStatus,

            LocalDateTime occurredAt,

            String comment,

            String workflowActivityId,

            String nextWorkflowActivityId,

            String activityName

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

        item.setWorkflowActivityId(workflowActivityId);

        item.setNextWorkflowActivityId(nextWorkflowActivityId);

        if (activityName != null) {

            item.setActivityName(activityName);

        }

        return item;

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

    private int countOpenTasks(Tramite tramite) {
        if (tramite.getTasks() == null) {
            return 0;
        }
        return (int) tramite.getTasks().stream()
                .filter(t -> TASK_PENDIENTE.equals(t.getStatus()) || TASK_EN_CURSO.equals(t.getStatus()))
                .count();
    }

    private void assertIterationLimits(Tramite tramite, List<WorkflowActivity> next) {
        if (next == null || tramite.getTasks() == null) {
            return;
        }
        for (WorkflowActivity activity : next) {
            if (activity.getId() == null || activity.getId().isBlank()) {
                continue;
            }
            long visits = tramite.getTasks().stream()
                    .filter(t -> activity.getId().equals(t.getWorkflowActivityId()))
                    .count();
            if (visits >= MAX_TASK_VISITS_PER_ACTIVITY) {
                throw new IllegalStateException(
                        "Se alcanzó el límite de repeticiones para la actividad \""
                                + activity.getName()
                                + "\". Revise transiciones iterativas en el diagrama.");
            }
        }
    }

    private boolean hasResponsibleConfigured(WorkflowActivity activity) {
        if (activity.getResponsibleName() != null && !activity.getResponsibleName().isBlank()) {
            return true;
        }
        return activity.getResponsibleType() != null
                && !activity.getResponsibleType().isBlank()
                && activity.getResponsibleId() != null
                && !activity.getResponsibleId().isBlank();
    }

}


