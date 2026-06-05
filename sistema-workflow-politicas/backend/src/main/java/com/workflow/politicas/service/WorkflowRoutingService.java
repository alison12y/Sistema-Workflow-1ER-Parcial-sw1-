package com.workflow.politicas.service;

import com.workflow.politicas.dto.WorkflowRoutingResult;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.model.WorkflowTransition;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.repository.WorkflowTransitionRepository;
import com.workflow.politicas.workflow.cycle1.WorkflowTransitionType;
import com.workflow.politicas.util.Cu7WorkflowDebugLog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Motor de enrutamiento automático Ciclo 1 sobre {@link WorkflowActivity} y {@link WorkflowTransition}.
 */
@Service
public class WorkflowRoutingService {

    private static final int MAX_TRAVERSE = 50;

    private final WorkflowActivityRepository workflowActivityRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowConditionEvaluator conditionEvaluator;

    public WorkflowRoutingService(
            WorkflowActivityRepository workflowActivityRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            WorkflowConditionEvaluator conditionEvaluator
    ) {
        this.workflowActivityRepository = workflowActivityRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.conditionEvaluator = conditionEvaluator;
    }

    public WorkflowActivity requireStartActivity(String policyId) {
        List<WorkflowActivity> activities = loadActiveActivities(policyId);
        if (activities.isEmpty()) {
            throw new IllegalStateException(
                    "La política no tiene actividades de workflow configuradas. Diseñe el diagrama UML primero.");
        }
        List<WorkflowActivity> starts = activities.stream()
                .filter(a -> "START".equalsIgnoreCase(a.getActivityType()))
                .toList();
        if (starts.isEmpty()) {
            throw new IllegalStateException("El workflow debe incluir una actividad de inicio (START).");
        }
        if (starts.size() > 1) {
            throw new IllegalStateException(
                    "El workflow tiene múltiples actividades de inicio (START). Debe existir exactamente una.");
        }
        return starts.get(0);
    }

    public WorkflowRoutingResult resolveFirstWorkTasks(String policyId) {
        WorkflowActivity start = requireStartActivity(policyId);
        return resolveFromGateway(policyId, start.getId(), Map.of());
    }

    /** Activa tareas al llegar a una actividad (p. ej. tras PARALLEL_JOIN). */
    public WorkflowRoutingResult resolveActivationAtActivity(
            String policyId,
            String activityId,
            Map<String, Object> stepData
    ) {
        return resolveFromGateway(policyId, activityId, stepData != null ? stepData : Map.of());
    }

    public WorkflowRoutingResult routeAfterCompletedActivity(
            String policyId,
            String completedActivityId,
            Map<String, Object> stepData
    ) {
        return routeAfterCompletedActivity(policyId, completedActivityId, null, stepData);
    }

    public WorkflowRoutingResult routeAfterCompletedActivity(
            String policyId,
            String completedActivityId,
            String activityNameHint,
            Map<String, Object> stepData
    ) {
        if (completedActivityId == null || completedActivityId.isBlank()) {
            return WorkflowRoutingResult.error("No se identificó la actividad completada");
        }
        Map<String, Object> data = stepData != null ? stepData : Map.of();
        String effectiveId = resolveCanonicalActivityId(policyId, completedActivityId, activityNameHint);
        WorkflowActivity completed = workflowActivityRepository.findById(effectiveId).orElse(null);
        if (completed == null || !completed.isActive()) {
            return WorkflowRoutingResult.error(
                    "Actividad completada no encontrada en el workflow (id="
                            + effectiveId
                            + (activityNameHint != null ? ", nombre=" + activityNameHint : "")
                            + ")");
        }
        logAdvanceContext(policyId, effectiveId, completed.getName(), completed.getActivityType(), data);
        if ("END".equalsIgnoreCase(completed.getActivityType())) {
            return WorkflowRoutingResult.completed("Actividad de fin alcanzada");
        }
        return resolveAfterTask(policyId, effectiveId, completed.getName(), data);
    }

    /**
     * Resuelve el id de actividad vigente cuando el trámite conserva ids antiguos o solo el nombre.
     */
    public String resolveCanonicalActivityId(String policyId, String activityId, String activityNameHint) {
        if (activityId != null && !activityId.isBlank()) {
            WorkflowActivity byId = workflowActivityRepository.findById(activityId).orElse(null);
            if (byId != null && byId.isActive() && policyId.equals(byId.getPolicyId())) {
                if (!outgoingFrom(policyId, activityId).isEmpty() || "TASK".equalsIgnoreCase(byId.getActivityType())) {
                    return activityId;
                }
            }
            if (byId != null && byId.isActive() && byId.getName() != null) {
                String byName = findActiveActivityIdByName(policyId, byId.getName()).orElse(null);
                if (byName != null && !outgoingFrom(policyId, byName).isEmpty()) {
                    Cu7WorkflowDebugLog.advance(
                            "resolveCanonicalActivityId policy={} staleId={} resolvedByName={} name={}",
                            policyId,
                            activityId,
                            byName,
                            byId.getName()
                    );
                    return byName;
                }
            }
        }
        if (activityNameHint != null && !activityNameHint.isBlank()) {
            Optional<String> byHint = findActiveActivityIdByName(policyId, activityNameHint);
            if (byHint.isPresent()) {
                Cu7WorkflowDebugLog.advance(
                        "resolveCanonicalActivityId policy={} hintName={} resolvedId={}",
                        policyId,
                        activityNameHint,
                        byHint.get()
                );
                return byHint.get();
            }
        }
        return activityId;
    }

    public Optional<String> resolveJoinActivityAfterParallelSplit(String policyId, List<String> branchActivityIds) {
        if (branchActivityIds == null || branchActivityIds.isEmpty()) {
            return Optional.empty();
        }
        List<WorkflowTransition> transitions = loadActiveTransitions(policyId);
        Set<String> branchSet = new HashSet<>(branchActivityIds);

        Map<String, Long> joinVote = new HashMap<>();
        for (WorkflowTransition transition : transitions) {
            if (!"PARALLEL_JOIN".equalsIgnoreCase(transition.getTransitionType())) {
                continue;
            }
            if (branchSet.contains(transition.getFromActivityId())) {
                joinVote.merge(transition.getToActivityId(), 1L, Long::sum);
            }
        }
        return joinVote.entrySet().stream()
                .filter(e -> e.getValue() >= branchSet.size())
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public String formatResponsible(WorkflowActivity activity) {
        if (activity.getResponsibleName() != null && !activity.getResponsibleName().isBlank()) {
            return activity.getResponsibleName().trim();
        }
        if (activity.getResponsibleType() != null && activity.getResponsibleId() != null) {
            return activity.getResponsibleType() + ":" + activity.getResponsibleId();
        }
        return "Sin asignar";
    }

    public int countExecutableTasks(String policyId) {
        return (int) loadActiveActivities(policyId).stream()
                .filter(a -> "TASK".equalsIgnoreCase(a.getActivityType()))
                .count();
    }

    private WorkflowRoutingResult resolveAfterTask(
            String policyId,
            String fromActivityId,
            String fromActivityName,
            Map<String, Object> stepData
    ) {
        List<WorkflowTransition> outgoing = outgoingFrom(policyId, fromActivityId);
        if (outgoing.isEmpty() && fromActivityName != null && !fromActivityName.isBlank()) {
            String altId = findActiveActivityIdByName(policyId, fromActivityName).orElse(null);
            if (altId != null && !altId.equals(fromActivityId)) {
                outgoing = outgoingFrom(policyId, altId);
                if (!outgoing.isEmpty()) {
                    Cu7WorkflowDebugLog.advance(
                            "resolveAfterTask policy={} sin salidas para id={} reintento por nombre={} altId={}",
                            policyId,
                            fromActivityId,
                            fromActivityName,
                            altId
                    );
                    fromActivityId = altId;
                }
            }
        }

        Cu7WorkflowDebugLog.advance(
                "resolveAfterTask policy={} fromActivityId={} fromActivity={} outgoing={} stepData={}",
                policyId,
                fromActivityId,
                fromActivityName != null ? fromActivityName : activityName(fromActivityId),
                summarizeTransitions(outgoing),
                Cu7WorkflowDebugLog.stepDataSummary(stepData)
        );

        if (outgoing.isEmpty()) {
            return tryCompleteOrError(policyId, fromActivityId);
        }

        List<WorkflowTransition> parallelSplits = outgoing.stream()
                .filter(t -> "PARALLEL_SPLIT".equalsIgnoreCase(t.getTransitionType()))
                .toList();
        if (!parallelSplits.isEmpty()) {
            return buildParallelActivation(policyId, parallelSplits, stepData);
        }

        WorkflowTransition chosen = chooseTransition(outgoing, stepData);
        if (chosen == null) {
            return WorkflowRoutingResult.error(
                    "No hay transición válida desde \"" + activityName(fromActivityId)
                            + "\". Revise condiciones o datos del formulario.");
        }

        Cu7WorkflowDebugLog.advance(
                "resolveAfterTask chosen type={} to={} ({})",
                chosen.getTransitionType(),
                chosen.getToActivityId(),
                chosen.getToActivityName()
        );

        return routeThroughTransitionTarget(policyId, chosen, stepData);
    }

    private WorkflowRoutingResult routeThroughTransitionTarget(
            String policyId,
            WorkflowTransition chosen,
            Map<String, Object> stepData
    ) {
        WorkflowActivity target = workflowActivityRepository.findById(chosen.getToActivityId()).orElse(null);
        if (target != null
                && target.isActive()
                && isPassthroughGateway(target.getActivityType())
                && !"JOIN".equalsIgnoreCase(target.getActivityType())) {
            List<WorkflowTransition> gatewayOutgoing = outgoingFrom(policyId, target.getId());
            List<WorkflowTransition> parallelSplits = gatewayOutgoing.stream()
                    .filter(t -> "PARALLEL_SPLIT".equalsIgnoreCase(t.getTransitionType()))
                    .toList();
            if (!parallelSplits.isEmpty()) {
                return buildParallelActivation(policyId, parallelSplits, stepData);
            }
            Cu7WorkflowDebugLog.advance(
                    "routeThroughTransitionTarget pasarela {} ({}) -> resolveFromGateway",
                    target.getName(),
                    target.getActivityType()
            );
            return resolveFromGateway(policyId, target.getId(), stepData);
        }
        return followTransition(policyId, chosen, stepData);
    }

    private WorkflowRoutingResult buildParallelActivation(
            String policyId,
            List<WorkflowTransition> parallelSplits,
            Map<String, Object> stepData
    ) {
        List<WorkflowActivity> branches = new ArrayList<>();
        for (WorkflowTransition split : parallelSplits) {
            WorkflowActivity branch = resolveExecutableTarget(policyId, split.getToActivityId(), stepData);
            if (branch == null) {
                WorkflowActivity raw = workflowActivityRepository.findById(split.getToActivityId()).orElse(null);
                if (raw != null
                        && raw.isActive()
                        && isPassthroughGateway(raw.getActivityType())
                        && !"JOIN".equalsIgnoreCase(raw.getActivityType())) {
                    List<WorkflowTransition> nestedSplits = outgoingFrom(policyId, raw.getId()).stream()
                            .filter(t -> "PARALLEL_SPLIT".equalsIgnoreCase(t.getTransitionType()))
                            .toList();
                    if (!nestedSplits.isEmpty()) {
                        WorkflowRoutingResult nested = buildParallelActivation(policyId, nestedSplits, stepData);
                        if (nested.getOutcome() == WorkflowRoutingResult.Outcome.ACTIVATE_TASKS
                                && nested.getNextActivities() != null) {
                            for (WorkflowActivity nestedBranch : nested.getNextActivities()) {
                                if (branches.stream().noneMatch(b -> b.getId().equals(nestedBranch.getId()))) {
                                    branches.add(nestedBranch);
                                }
                            }
                        }
                        continue;
                    }
                }
            }
            if (branch != null && branches.stream().noneMatch(b -> b.getId().equals(branch.getId()))) {
                branches.add(branch);
            }
        }
        if (branches.isEmpty()) {
            return WorkflowRoutingResult.error("División paralela sin actividades de trabajo válidas");
        }
        List<String> branchIds = branches.stream().map(WorkflowActivity::getId).toList();
        String joinId = resolveJoinActivityAfterParallelSplit(policyId, branchIds).orElse(null);
        return WorkflowRoutingResult.activate(
                branches,
                joinId,
                "División paralela: " + branches.size() + " tareas activadas"
        );
    }

    private WorkflowRoutingResult followTransition(
            String policyId,
            WorkflowTransition transition,
            Map<String, Object> stepData
    ) {
        String type = transition.getTransitionType() != null
                ? transition.getTransitionType().trim().toUpperCase(Locale.ROOT)
                : WorkflowTransitionType.defaultCode();

        if ("ITERATIVE".equals(type)) {
            WorkflowActivity loopTarget = resolveExecutableTarget(policyId, transition.getToActivityId(), stepData);
            if (loopTarget == null) {
                return WorkflowRoutingResult.error("Transición iterativa sin actividad destino válida");
            }
            return WorkflowRoutingResult.activate(
                    List.of(loopTarget),
                    null,
                    "Retorno iterativo a " + loopTarget.getName()
            );
        }

        WorkflowActivity target = resolveExecutableTarget(policyId, transition.getToActivityId(), stepData);
        if (target == null) {
            WorkflowActivity raw = workflowActivityRepository.findById(transition.getToActivityId()).orElse(null);
            if (raw != null && raw.isActive() && isPassthroughGateway(raw.getActivityType())) {
                return WorkflowRoutingResult.error(
                        "No se pudo evaluar \""
                                + raw.getName()
                                + "\". Revise condiciones del diagrama o complete el formulario con los campos requeridos.");
            }
            return tryCompleteOrError(policyId, transition.getToActivityId());
        }
        if ("END".equalsIgnoreCase(target.getActivityType())) {
            return WorkflowRoutingResult.completed("Fin del proceso: " + target.getName());
        }
        return WorkflowRoutingResult.activate(
                List.of(target),
                null,
                "Siguiente actividad: " + target.getName()
        );
    }

    private WorkflowRoutingResult resolveFromGateway(
            String policyId,
            String activityId,
            Map<String, Object> stepData
    ) {
        Map<String, WorkflowActivity> byId = indexActivities(policyId);
        String cursorId = activityId;
        int hops = 0;

        while (cursorId != null && hops++ < MAX_TRAVERSE) {
            WorkflowActivity current = byId.get(cursorId);
            if (current == null || !current.isActive()) {
                return WorkflowRoutingResult.error("Actividad de workflow inválida: " + cursorId);
            }
            String type = current.getActivityType() != null
                    ? current.getActivityType().trim().toUpperCase(Locale.ROOT)
                    : "TASK";

            if ("END".equals(type)) {
                return WorkflowRoutingResult.completed("Fin del proceso");
            }

            if ("TASK".equals(type)) {
                return WorkflowRoutingResult.activate(
                        List.of(current),
                        null,
                        "Actividad de trabajo: " + current.getName()
                );
            }

            if ("JOIN".equals(type)) {
                List<WorkflowTransition> outgoing = outgoingFrom(policyId, cursorId);
                if (outgoing.isEmpty()) {
                    return WorkflowRoutingResult.error(
                            "Join \"" + current.getName() + "\" no tiene transición de salida");
                }
                WorkflowTransition chosen = chooseTransition(outgoing, stepData);
                if (chosen == null) {
                    return WorkflowRoutingResult.error(
                            "No se pudo continuar desde Join \"" + current.getName() + "\"");
                }
                return routeThroughTransitionTarget(policyId, chosen, stepData);
            }

            if ("START".equals(type) || "DECISION".equals(type) || "FORK".equals(type)) {
                List<WorkflowTransition> outgoing = outgoingFrom(policyId, cursorId);
                if (outgoing.isEmpty()) {
                    return WorkflowRoutingResult.error(
                            "La actividad \"" + current.getName() + "\" no tiene transiciones de salida");
                }
                if ("DECISION".equals(type) || "FORK".equals(type)) {
                    List<WorkflowTransition> parallelSplits = outgoing.stream()
                            .filter(t -> "PARALLEL_SPLIT".equalsIgnoreCase(t.getTransitionType()))
                            .toList();
                    if (!parallelSplits.isEmpty()) {
                        return buildParallelActivation(policyId, parallelSplits, stepData);
                    }
                    if ("FORK".equals(type)) {
                        return WorkflowRoutingResult.error(
                                "Fork \"" + current.getName() + "\" sin salidas PARALLEL_SPLIT hacia las ramas");
                    }
                }
                WorkflowTransition chosen = chooseTransition(outgoing, stepData);
                if (chosen == null) {
                    return WorkflowRoutingResult.error(
                            "No se pudo resolver la decisión en \"" + current.getName() + "\"");
                }
                cursorId = chosen.getToActivityId();
                continue;
            }

            return WorkflowRoutingResult.error("Tipo de actividad no soportado: " + type);
        }

        return WorkflowRoutingResult.error("Se excedió el límite de recorrido del workflow");
    }

    private WorkflowTransition chooseTransition(List<WorkflowTransition> outgoing, Map<String, Object> stepData) {
        List<WorkflowTransition> ordered = outgoing.stream()
                .sorted(Comparator.comparingInt(WorkflowTransition::getOrderIndex))
                .toList();

        List<WorkflowTransition> conditional = ordered.stream()
                .filter(t -> isConditionalType(t.getTransitionType()))
                .toList();
        if (!conditional.isEmpty()) {
            Cu7WorkflowDebugLog.log(
                    "chooseTransition conditional count={} from={} stepData={}",
                    conditional.size(),
                    transitionFromName(ordered),
                    Cu7WorkflowDebugLog.stepDataSummary(stepData)
            );
            for (WorkflowTransition transition : conditional) {
                boolean match = conditionEvaluator.evaluate(
                        transition.getConditionExpression(),
                        transition.getConditionLabel(),
                        stepData
                );
                Cu7WorkflowDebugLog.log(
                        "chooseTransition -> {} expr='{}' label='{}' type={} match={}",
                        transition.getToActivityName(),
                        transition.getConditionExpression(),
                        transition.getConditionLabel(),
                        transition.getTransitionType(),
                        match
                );
                if (match) {
                    return transition;
                }
            }
            List<WorkflowTransition> fallback = ordered.stream()
                    .filter(t -> !isConditionalType(t.getTransitionType()))
                    .toList();
            if (!fallback.isEmpty()) {
                Cu7WorkflowDebugLog.advance(
                        "chooseTransition sin match condicional from={} -> fallback secuencial to={}",
                        transitionFromName(ordered),
                        fallback.get(0).getToActivityName()
                );
                return fallback.get(0);
            }
            Cu7WorkflowDebugLog.log(
                    "chooseTransition ninguna condicional coincidió from={} stepData={}",
                    transitionFromName(ordered),
                    Cu7WorkflowDebugLog.stepDataSummary(stepData)
            );
            return null;
        }

        List<WorkflowTransition> sequential = ordered.stream()
                .filter(t -> t.getTransitionType() == null
                        || t.getTransitionType().isBlank()
                        || "SEQUENTIAL".equalsIgnoreCase(t.getTransitionType())
                        || "ITERATIVE".equalsIgnoreCase(t.getTransitionType()))
                .toList();
        if (!sequential.isEmpty()) {
            return sequential.get(0);
        }

        return ordered.isEmpty() ? null : ordered.get(0);
    }

    private WorkflowRoutingResult tryCompleteOrError(String policyId, String activityId) {
        WorkflowActivity activity = workflowActivityRepository.findById(activityId).orElse(null);
        if (activity != null && "END".equalsIgnoreCase(activity.getActivityType())) {
            return WorkflowRoutingResult.completed("Fin del workflow");
        }
        boolean hasEnd = loadActiveActivities(policyId).stream()
                .anyMatch(a -> "END".equalsIgnoreCase(a.getActivityType()));
        if (hasEnd) {
            return WorkflowRoutingResult.error(
                    "Sin transición válida hacia fin desde \"" + activityName(activityId) + "\"");
        }
        return WorkflowRoutingResult.completed("No hay más actividades; trámite finalizado");
    }

    private WorkflowActivity resolveExecutableTarget(
            String policyId,
            String targetActivityId,
            Map<String, Object> stepData
    ) {
        Map<String, WorkflowActivity> byId = indexActivities(policyId);
        String cursorId = targetActivityId;
        int hops = 0;

        while (cursorId != null && hops++ < MAX_TRAVERSE) {
            WorkflowActivity current = byId.get(cursorId);
            if (current == null || !current.isActive()) {
                return null;
            }
            String type = current.getActivityType() != null
                    ? current.getActivityType().trim().toUpperCase(Locale.ROOT)
                    : "TASK";

            if ("TASK".equals(type)) {
                return current;
            }
            if ("END".equals(type)) {
                return current;
            }
            if ("JOIN".equals(type)) {
                List<WorkflowTransition> outgoing = outgoingFrom(policyId, cursorId);
                if (outgoing.isEmpty()) {
                    return null;
                }
                WorkflowTransition chosen = chooseTransition(outgoing, stepData);
                if (chosen == null) {
                    return null;
                }
                cursorId = chosen.getToActivityId();
                continue;
            }
            if ("START".equals(type) || "DECISION".equals(type) || "FORK".equals(type)) {
                List<WorkflowTransition> outgoing = outgoingFrom(policyId, cursorId);
                if (outgoing.isEmpty()) {
                    return null;
                }
                if ("DECISION".equals(type) || "FORK".equals(type)) {
                    List<WorkflowTransition> parallelSplits = outgoing.stream()
                            .filter(t -> "PARALLEL_SPLIT".equalsIgnoreCase(t.getTransitionType()))
                            .toList();
                    if (!parallelSplits.isEmpty()) {
                        return null;
                    }
                }
                WorkflowTransition chosen = chooseTransition(outgoing, stepData);
                if (chosen == null) {
                    return null;
                }
                cursorId = chosen.getToActivityId();
                continue;
            }
            return null;
        }
        return null;
    }

    private List<WorkflowTransition> outgoingFrom(String policyId, String fromActivityId) {
        if (fromActivityId == null || fromActivityId.isBlank()) {
            return List.of();
        }
        List<WorkflowTransition> byId = loadActiveTransitions(policyId).stream()
                .filter(t -> fromActivityId.equals(t.getFromActivityId()))
                .sorted(Comparator.comparingInt(WorkflowTransition::getOrderIndex))
                .toList();
        if (!byId.isEmpty()) {
            return byId;
        }
        return workflowActivityRepository.findById(fromActivityId)
                .map(WorkflowActivity::getName)
                .flatMap(name -> findActiveActivityIdByName(policyId, name))
                .filter(resolvedId -> !resolvedId.equals(fromActivityId))
                .map(resolvedId -> loadActiveTransitions(policyId).stream()
                        .filter(t -> resolvedId.equals(t.getFromActivityId()))
                        .sorted(Comparator.comparingInt(WorkflowTransition::getOrderIndex))
                        .toList())
                .filter(list -> !list.isEmpty())
                .orElse(List.of());
    }

    private Optional<String> findActiveActivityIdByName(String policyId, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizeName(name);
        return loadActiveActivities(policyId).stream()
                .filter(a -> a.getName() != null && normalizeName(a.getName()).equals(normalized))
                .map(WorkflowActivity::getId)
                .findFirst();
    }

    private static String normalizeName(String value) {
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ñ", "N");
    }

    private void logAdvanceContext(
            String policyId,
            String activityId,
            String activityName,
            String activityType,
            Map<String, Object> stepData
    ) {
        List<WorkflowTransition> outgoing = outgoingFrom(policyId, activityId);
        Cu7WorkflowDebugLog.advance(
                "routeAfterCompletedActivity policy={} fromActivityId={} fromActivity={} type={} outgoing={} stepData={}",
                policyId,
                activityId,
                activityName,
                activityType,
                summarizeTransitions(outgoing),
                Cu7WorkflowDebugLog.stepDataSummary(stepData)
        );
    }

    private String summarizeTransitions(List<WorkflowTransition> transitions) {
        if (transitions == null || transitions.isEmpty()) {
            return "[]";
        }
        return transitions.stream()
                .map(t -> (t.getTransitionType() != null ? t.getTransitionType() : "SEQUENTIAL")
                        + ":"
                        + (t.getToActivityName() != null ? t.getToActivityName() : t.getToActivityId()))
                .reduce((a, b) -> a + ", " + b)
                .map(s -> "[" + s + "]")
                .orElse("[]");
    }

    private List<WorkflowActivity> loadActiveActivities(String policyId) {
        return workflowActivityRepository.findByPolicyIdOrderByOrderIndexAsc(policyId).stream()
                .filter(WorkflowActivity::isActive)
                .toList();
    }

    private List<WorkflowTransition> loadActiveTransitions(String policyId) {
        return workflowTransitionRepository.findByPolicyIdOrderByOrderIndexAsc(policyId).stream()
                .filter(WorkflowTransition::isActive)
                .toList();
    }

    private Map<String, WorkflowActivity> indexActivities(String policyId) {
        return loadActiveActivities(policyId).stream()
                .collect(Collectors.toMap(WorkflowActivity::getId, a -> a, (a, b) -> a, LinkedHashMap::new));
    }

    private static boolean isPassthroughGateway(String activityType) {
        if (activityType == null || activityType.isBlank()) {
            return false;
        }
        String t = activityType.trim().toUpperCase(Locale.ROOT);
        return "DECISION".equals(t) || "FORK".equals(t) || "JOIN".equals(t);
    }

    private boolean isConditionalType(String transitionType) {
        if (transitionType == null || transitionType.isBlank()) {
            return false;
        }
        String type = transitionType.trim().toUpperCase(Locale.ROOT);
        return "CONDITIONAL".equals(type) || type.contains("COND");
    }

    private String activityName(String activityId) {
        return workflowActivityRepository.findById(activityId)
                .map(WorkflowActivity::getName)
                .orElse(activityId);
    }

    private String transitionFromName(List<WorkflowTransition> transitions) {
        if (transitions == null || transitions.isEmpty()) {
            return "?";
        }
        String fromId = transitions.get(0).getFromActivityId();
        return fromId != null ? activityName(fromId) : "?";
    }
}
