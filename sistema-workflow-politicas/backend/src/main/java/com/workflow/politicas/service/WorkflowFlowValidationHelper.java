package com.workflow.politicas.service;

import com.workflow.politicas.dto.WorkflowFlowValidationResponse;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.model.WorkflowTransition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validaciones de diseño UML — usado por {@link WorkflowTransitionService#validateFlow}.
 */
final class WorkflowFlowValidationHelper {

    private WorkflowFlowValidationHelper() {
    }

    static WorkflowFlowValidationResponse validate(
            String policyId,
            List<WorkflowActivity> activities,
            List<WorkflowTransition> transitions,
            WorkflowRoutingService routingService
    ) {
        WorkflowFlowValidationResponse result = new WorkflowFlowValidationResponse();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        List<WorkflowActivity> activeActivities = activities.stream()
                .filter(WorkflowActivity::isActive)
                .toList();
        List<WorkflowTransition> activeTransitions = transitions.stream()
                .filter(WorkflowTransition::isActive)
                .toList();

        if (activeActivities.isEmpty()) {
            errors.add("Agregue al menos una actividad para diseñar el flujo.");
        }

        long startCount = activeActivities.stream()
                .filter(a -> "START".equalsIgnoreCase(a.getActivityType()))
                .count();
        long endCount = activeActivities.stream()
                .filter(a -> "END".equalsIgnoreCase(a.getActivityType()))
                .count();

        if (startCount == 0) {
            errors.add("Debe existir exactamente una actividad de inicio (START).");
        } else if (startCount > 1) {
            errors.add("Existen múltiples actividades START; solo debe haber una.");
        }
        if (endCount == 0) {
            errors.add("Debe existir al menos una actividad de fin (END).");
        }

        Set<String> activeActivityIds = activeActivities.stream()
                .map(WorkflowActivity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, WorkflowActivity> byId = activeActivities.stream()
                .filter(a -> a.getId() != null)
                .collect(Collectors.toMap(WorkflowActivity::getId, a -> a, (a, b) -> a));

        validateTransitions(activeTransitions, activeActivityIds, byId, warnings, errors);
        validateActivitiesConnectivity(activeActivities, activeTransitions, warnings);
        validateTaskResponsibles(activeActivities, errors);
        validateParallelJoin(policyId, activeTransitions, activeActivityIds, routingService, errors, warnings);
        validateForkJoinGateways(activeActivities, activeTransitions, warnings, errors);
        validateIterativeCycles(activeActivities, activeTransitions, warnings, errors);

        result.setWarnings(warnings);
        result.setErrors(errors);
        result.setValid(errors.isEmpty());
        if (errors.isEmpty() && warnings.isEmpty()) {
            result.setMessage("Flujo válido.");
        } else if (!errors.isEmpty()) {
            result.setMessage("El flujo tiene errores que deben corregirse.");
        } else {
            result.setMessage("El flujo tiene advertencias.");
        }
        return result;
    }

    private static void validateTransitions(
            List<WorkflowTransition> transitions,
            Set<String> activeActivityIds,
            Map<String, WorkflowActivity> byId,
            List<String> warnings,
            List<String> errors
    ) {
        Set<String> duplicatePairs = new HashSet<>();
        for (WorkflowTransition t : transitions) {
            String from = t.getFromActivityId();
            String to = t.getToActivityId();
            if (from == null || to == null) {
                errors.add("Hay una conexión sin origen o destino definido.");
                continue;
            }
            if (!activeActivityIds.contains(from)) {
                errors.add("Conexión con origen inexistente o inactivo: " + label(t));
            }
            if (!activeActivityIds.contains(to)) {
                errors.add("Conexión con destino inexistente o inactivo: " + label(t));
            }
            String pair = from + "->" + to;
            if (!duplicatePairs.add(pair)) {
                warnings.add("Existe una transición duplicada: " + label(t));
            }
            if (isConditional(t)) {
                boolean hasExpression = t.getConditionExpression() != null && !t.getConditionExpression().isBlank();
                boolean hasLabel = t.getConditionLabel() != null && !t.getConditionLabel().isBlank();
                if (!hasExpression && !hasLabel) {
                    errors.add("La conexión condicional " + label(t) + " debe tener expresión o etiqueta de condición.");
                }
            }
        }
    }

    private static void validateActivitiesConnectivity(
            List<WorkflowActivity> activities,
            List<WorkflowTransition> transitions,
            List<String> warnings
    ) {
        Set<String> withOutgoing = transitions.stream()
                .map(WorkflowTransition::getFromActivityId)
                .collect(Collectors.toSet());
        Set<String> withIncoming = transitions.stream()
                .map(WorkflowTransition::getToActivityId)
                .collect(Collectors.toSet());

        for (WorkflowActivity activity : activities) {
            String activityId = activity.getId();
            boolean isStart = "START".equalsIgnoreCase(activity.getActivityType());
            boolean isEnd = "END".equalsIgnoreCase(activity.getActivityType());
            boolean hasOut = withOutgoing.contains(activityId);
            boolean hasIn = withIncoming.contains(activityId);
            boolean isDecision = "DECISION".equalsIgnoreCase(activity.getActivityType());
            boolean isFork = "FORK".equalsIgnoreCase(activity.getActivityType());
            boolean isJoin = "JOIN".equalsIgnoreCase(activity.getActivityType());
            boolean isGateway = isFork || isJoin;

            long conditionalOut = transitions.stream()
                    .filter(t -> activityId.equals(t.getFromActivityId()))
                    .filter(WorkflowFlowValidationHelper::isConditional)
                    .count();

            if (!isStart && !isEnd && !isGateway && !hasOut && !hasIn) {
                warnings.add("La actividad \"" + activity.getName() + "\" está aislada (sin conexiones).");
            } else if (!isEnd && !isGateway && !hasOut) {
                warnings.add("La actividad \"" + activity.getName() + "\" no tiene salida.");
            } else if (!isStart && !isGateway && !hasIn) {
                warnings.add("La actividad \"" + activity.getName() + "\" no tiene entrada.");
            } else if (isDecision && conditionalOut < 2) {
                warnings.add("La decisión \"" + activity.getName() + "\" necesita al menos dos salidas condicionales.");
            }
        }

        if (activities.size() > 1 && transitions.isEmpty()) {
            warnings.add("Debe conectar las actividades para formar el flujo.");
        }
    }

    private static void validateTaskResponsibles(List<WorkflowActivity> activities, List<String> errors) {
        for (WorkflowActivity activity : activities) {
            if (!"TASK".equalsIgnoreCase(activity.getActivityType())) {
                continue;
            }
            if (!hasResponsible(activity)) {
                errors.add("La actividad TASK \"" + activity.getName() + "\" no tiene responsable configurado.");
            }
        }
    }

    private static void validateParallelJoin(
            String policyId,
            List<WorkflowTransition> transitions,
            Set<String> activeActivityIds,
            WorkflowRoutingService routingService,
            List<String> errors,
            List<String> warnings
    ) {
        Map<String, List<WorkflowTransition>> splitsBySource = transitions.stream()
                .filter(t -> "PARALLEL_SPLIT".equalsIgnoreCase(t.getTransitionType()))
                .collect(Collectors.groupingBy(WorkflowTransition::getFromActivityId));

        for (Map.Entry<String, List<WorkflowTransition>> entry : splitsBySource.entrySet()) {
            List<String> branchTargets = entry.getValue().stream()
                    .map(WorkflowTransition::getToActivityId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (branchTargets.isEmpty()) {
                errors.add("División paralela sin ramas de destino.");
                continue;
            }
            Optional<String> joinId = routingService.resolveJoinActivityAfterParallelSplit(policyId, branchTargets);
            if (joinId.isEmpty()) {
                errors.add(
                        "División paralela desde actividad \""
                                + entry.getKey()
                                + "\" sin PARALLEL_JOIN que una todas las ramas ("
                                + branchTargets.size()
                                + " ramas).");
            }
        }

        for (WorkflowTransition join : transitions.stream()
                .filter(t -> "PARALLEL_JOIN".equalsIgnoreCase(t.getTransitionType()))
                .toList()) {
            if (join.getFromActivityId() == null || !activeActivityIds.contains(join.getFromActivityId())) {
                warnings.add("Unión paralela (JOIN) con origen inválido: " + label(join));
            }
        }
    }

    private static void validateForkJoinGateways(
            List<WorkflowActivity> activities,
            List<WorkflowTransition> transitions,
            List<String> warnings,
            List<String> errors
    ) {
        for (WorkflowActivity activity : activities) {
            String id = activity.getId();
            if (id == null) {
                continue;
            }
            String type = activity.getActivityType() != null
                    ? activity.getActivityType().trim().toUpperCase(Locale.ROOT)
                    : "";

            if ("FORK".equals(type)) {
                long incoming = transitions.stream()
                        .filter(t -> id.equals(t.getToActivityId()))
                        .count();
                List<WorkflowTransition> splitOut = transitions.stream()
                        .filter(t -> id.equals(t.getFromActivityId()))
                        .filter(t -> "PARALLEL_SPLIT".equalsIgnoreCase(t.getTransitionType()))
                        .toList();
                long otherOut = transitions.stream()
                        .filter(t -> id.equals(t.getFromActivityId()))
                        .filter(t -> !"PARALLEL_SPLIT".equalsIgnoreCase(t.getTransitionType()))
                        .count();

                if (incoming < 1) {
                    warnings.add("Fork \"" + activity.getName() + "\" debería tener al menos una conexión de entrada.");
                }
                if (splitOut.size() < 2) {
                    errors.add(
                            "Fork \""
                                    + activity.getName()
                                    + "\" requiere al menos dos salidas de tipo división paralela (PARALLEL_SPLIT).");
                }
                if (otherOut > 0) {
                    warnings.add(
                            "Fork \""
                                    + activity.getName()
                                    + "\": las salidas hacia ramas paralelas deben ser PARALLEL_SPLIT.");
                }
            }

            if ("JOIN".equals(type)) {
                long joinIn = transitions.stream()
                        .filter(t -> id.equals(t.getToActivityId()))
                        .filter(t -> "PARALLEL_JOIN".equalsIgnoreCase(t.getTransitionType()))
                        .count();
                long incoming = transitions.stream()
                        .filter(t -> id.equals(t.getToActivityId()))
                        .count();
                long outgoing = transitions.stream()
                        .filter(t -> id.equals(t.getFromActivityId()))
                        .count();

                if (joinIn < 2) {
                    errors.add(
                            "Join \""
                                    + activity.getName()
                                    + "\" requiere al menos dos entradas de tipo unión paralela (PARALLEL_JOIN).");
                } else if (incoming > joinIn) {
                    warnings.add(
                            "Join \""
                                    + activity.getName()
                                    + "\": las entradas desde ramas deben ser PARALLEL_JOIN.");
                }
                if (outgoing < 1) {
                    warnings.add("Join \"" + activity.getName() + "\" debería tener al menos una salida.");
                }
            }
        }
    }

    private static void validateIterativeCycles(
            List<WorkflowActivity> activities,
            List<WorkflowTransition> transitions,
            List<String> warnings,
            List<String> errors
    ) {
        List<WorkflowTransition> iterative = transitions.stream()
                .filter(t -> "ITERATIVE".equalsIgnoreCase(t.getTransitionType()))
                .toList();
        for (WorkflowTransition t : iterative) {
            if (t.getFromActivityId() != null && t.getFromActivityId().equals(t.getToActivityId())) {
                errors.add("Transición iterativa inválida en la misma actividad: " + label(t));
            }
        }
        if (iterative.isEmpty()) {
            return;
        }
        Map<String, List<String>> graph = new HashMap<>();
        for (WorkflowTransition t : transitions) {
            if (t.getFromActivityId() == null || t.getToActivityId() == null) {
                continue;
            }
            graph.computeIfAbsent(t.getFromActivityId(), k -> new ArrayList<>()).add(t.getToActivityId());
        }
        Set<String> endIds = activities.stream()
                .filter(a -> "END".equalsIgnoreCase(a.getActivityType()))
                .map(WorkflowActivity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (WorkflowTransition it : iterative) {
            String to = it.getToActivityId();
            if (to == null) {
                continue;
            }
            if (!canReachEnd(to, endIds, graph, new HashSet<>())) {
                warnings.add(
                        "La transición iterativa " + label(it)
                                + " podría no tener salida hacia END (revise rutas de salida).");
            }
        }
    }

    private static boolean canReachEnd(
            String node,
            Set<String> endIds,
            Map<String, List<String>> graph,
            Set<String> visiting
    ) {
        if (endIds.contains(node)) {
            return true;
        }
        if (!visiting.add(node)) {
            return false;
        }
        for (String next : graph.getOrDefault(node, List.of())) {
            if (canReachEnd(next, endIds, graph, visiting)) {
                return true;
            }
        }
        visiting.remove(node);
        return false;
    }

    private static boolean hasResponsible(WorkflowActivity activity) {
        if (activity.getResponsibleName() != null && !activity.getResponsibleName().isBlank()) {
            return true;
        }
        return activity.getResponsibleType() != null
                && !activity.getResponsibleType().isBlank()
                && activity.getResponsibleId() != null
                && !activity.getResponsibleId().isBlank();
    }

    private static boolean isConditional(WorkflowTransition t) {
        if (t.getTransitionType() == null) {
            return false;
        }
        String type = t.getTransitionType().trim().toUpperCase(Locale.ROOT);
        return "CONDITIONAL".equals(type) || type.contains("COND");
    }

    private static String label(WorkflowTransition t) {
        String from = t.getFromActivityName() != null ? t.getFromActivityName() : t.getFromActivityId();
        String to = t.getToActivityName() != null ? t.getToActivityName() : t.getToActivityId();
        return "\"" + from + "\" → \"" + to + "\"";
    }
}
