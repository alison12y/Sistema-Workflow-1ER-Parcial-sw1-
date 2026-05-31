package com.workflow.politicas.service;

import com.workflow.politicas.dto.WorkflowDeleteResponse;
import com.workflow.politicas.dto.WorkflowFlowValidationResponse;
import com.workflow.politicas.dto.WorkflowTransitionCleanupResponse;
import com.workflow.politicas.dto.WorkflowTransitionDedupeResponse;
import com.workflow.politicas.dto.WorkflowTransitionRequest;
import com.workflow.politicas.dto.WorkflowTransitionResponse;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.model.WorkflowTransition;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.repository.WorkflowTransitionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkflowTransitionService {

    private static final Map<String, String> TRANSITION_TYPE_LABELS = Map.of(
            "SEQUENTIAL", "Secuencial",
            "CONDITIONAL", "Condicional",
            "ITERATIVE", "Iterativa",
            "PARALLEL_SPLIT", "División paralela",
            "PARALLEL_JOIN", "Unión paralela"
    );

    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowActivityRepository workflowActivityRepository;
    private final BusinessPolicyRepository businessPolicyRepository;

    public WorkflowTransitionService(
            WorkflowTransitionRepository workflowTransitionRepository,
            WorkflowActivityRepository workflowActivityRepository,
            BusinessPolicyRepository businessPolicyRepository
    ) {
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.workflowActivityRepository = workflowActivityRepository;
        this.businessPolicyRepository = businessPolicyRepository;
    }

    public List<WorkflowTransitionResponse> findByPolicyId(String policyId) {
        validatePolicyExists(policyId);
        return workflowTransitionRepository.findByPolicyIdOrderByOrderIndexAsc(policyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public Optional<WorkflowTransitionResponse> findById(String id) {
        return workflowTransitionRepository.findById(id).map(this::toResponse);
    }

    public WorkflowTransitionResponse create(WorkflowTransitionRequest request) {
        validateRequest(request, true);
        BusinessPolicy policy = validatePolicyExists(request.getPolicyId());
        WorkflowActivity from = validateActivity(request.getFromActivityId(), policy.getId());
        WorkflowActivity to = validateActivity(request.getToActivityId(), policy.getId());

        List<WorkflowTransition> existing = workflowTransitionRepository
                .findByPolicyIdAndFromActivityIdAndToActivityId(policy.getId(), from.getId(), to.getId());

        if (existing.stream().anyMatch(WorkflowTransition::isActive)) {
            throw new IllegalArgumentException("La conexión ya existe.");
        }

        Optional<WorkflowTransition> inactiveExisting = existing.stream()
                .filter(t -> !t.isActive())
                .max(Comparator.comparing(
                        this::transitionSortTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ));

        if (inactiveExisting.isPresent()) {
            WorkflowTransition transition = inactiveExisting.get();
            applyRequest(transition, request, from, to, policy.getId());
            transition.setActive(true);
            transition.setUpdatedAt(LocalDateTime.now());
            WorkflowTransitionResponse response = toResponse(workflowTransitionRepository.save(transition));
            response.setReactivated(true);
            return response;
        }

        WorkflowTransition transition = new WorkflowTransition();
        applyRequest(transition, request, from, to, policy.getId());
        transition.setOrderIndex(resolveOrderIndex(policy.getId(), request.getOrderIndex()));
        transition.setCreatedAt(LocalDateTime.now());
        transition.setUpdatedAt(LocalDateTime.now());
        if (request.getActive() == null) {
            transition.setActive(true);
        }

        return toResponse(workflowTransitionRepository.save(transition));
    }

    public WorkflowTransitionResponse update(String id, WorkflowTransitionRequest request) {
        WorkflowTransition transition = workflowTransitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conexión no encontrada"));

        validateRequest(request, false);
        String policyId = request.getPolicyId() != null ? request.getPolicyId() : transition.getPolicyId();
        validatePolicyExists(policyId);
        WorkflowActivity from = validateActivity(request.getFromActivityId(), policyId);
        WorkflowActivity to = validateActivity(request.getToActivityId(), policyId);

        List<WorkflowTransition> samePair = workflowTransitionRepository
                .findByPolicyIdAndFromActivityIdAndToActivityId(policyId, from.getId(), to.getId());
        boolean duplicateActive = samePair.stream()
                .filter(WorkflowTransition::isActive)
                .anyMatch(t -> !t.getId().equals(transition.getId()));
        if (duplicateActive) {
            throw new IllegalArgumentException("La conexión ya existe.");
        }

        applyRequest(transition, request, from, to, policyId);
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            transition.setOrderIndex(request.getOrderIndex());
        }
        transition.setUpdatedAt(LocalDateTime.now());
        return toResponse(workflowTransitionRepository.save(transition));
    }

    public WorkflowDeleteResponse delete(String id) {
        WorkflowTransition transition = workflowTransitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conexión no encontrada"));

        workflowTransitionRepository.deleteById(id);

        WorkflowDeleteResponse response = new WorkflowDeleteResponse();
        response.setLogicalDelete(false);
        response.setAffectedConnections(0);
        response.setMessage("Conexión eliminada correctamente.");
        return response;
    }

    public WorkflowTransitionResponse activate(String id) {
        WorkflowTransition transition = workflowTransitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conexión no encontrada"));

        List<WorkflowTransition> samePair = workflowTransitionRepository
                .findByPolicyIdAndFromActivityIdAndToActivityId(
                        transition.getPolicyId(),
                        transition.getFromActivityId(),
                        transition.getToActivityId()
                );
        boolean otherActive = samePair.stream()
                .filter(WorkflowTransition::isActive)
                .anyMatch(t -> !t.getId().equals(transition.getId()));
        if (otherActive) {
            throw new IllegalArgumentException("La conexión ya existe.");
        }

        transition.setActive(true);
        transition.setUpdatedAt(LocalDateTime.now());
        return toResponse(workflowTransitionRepository.save(transition));
    }

    public WorkflowTransitionResponse deactivate(String id) {
        WorkflowTransition transition = workflowTransitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conexión no encontrada"));
        transition.setActive(false);
        transition.setUpdatedAt(LocalDateTime.now());
        return toResponse(workflowTransitionRepository.save(transition));
    }

    public int countByPolicyId(String policyId) {
        return (int) workflowTransitionRepository.countByPolicyId(policyId);
    }

    public WorkflowTransitionDedupeResponse deduplicateByPolicyId(String policyId) {
        WorkflowTransitionCleanupResponse cleanup = cleanupTransitions(policyId);
        WorkflowTransitionDedupeResponse response = new WorkflowTransitionDedupeResponse();
        response.setRemoved(cleanup.getRemovedDuplicates() + cleanup.getRemovedOrphans());
        response.setKept(cleanup.getKept());
        response.setDeactivatedCount(response.getRemoved());
        response.setMessage(cleanup.getMessage());
        return response;
    }

    public WorkflowTransitionCleanupResponse cleanupTransitions(String policyId) {
        validatePolicyExists(policyId);
        List<WorkflowActivity> activities = workflowActivityRepository.findByPolicyIdOrderByOrderIndexAsc(policyId);
        Set<String> validActivityIds = activities.stream()
                .map(WorkflowActivity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        boolean duplicateActivitiesDetected = hasDuplicateActivitiesByName(activities);

        List<WorkflowTransition> remaining = new ArrayList<>(
                workflowTransitionRepository.findByPolicyIdOrderByOrderIndexAsc(policyId));
        LocalDateTime now = LocalDateTime.now();

        int removedOrphans = 0;
        Set<String> pendingDelete = new LinkedHashSet<>();

        for (WorkflowTransition transition : remaining) {
            if (isOrphanTransition(transition, validActivityIds)) {
                pendingDelete.add(transition.getId());
                removedOrphans++;
            }
        }
        deleteTransitions(pendingDelete);
        remaining.removeIf(t -> pendingDelete.contains(t.getId()));
        pendingDelete.clear();

        int removedDuplicates = removeDuplicateTransitionGroups(
                remaining, this::activityIdPairKey, pendingDelete, now);
        deleteTransitions(pendingDelete);
        remaining.removeIf(t -> pendingDelete.contains(t.getId()));
        pendingDelete.clear();

        removedDuplicates += removeDuplicateTransitionGroups(
                remaining, this::normalizedNamePairKey, pendingDelete, now);
        deleteTransitions(pendingDelete);

        int kept = (int) workflowTransitionRepository.countByPolicyId(policyId);

        WorkflowTransitionCleanupResponse response = new WorkflowTransitionCleanupResponse();
        response.setRemovedDuplicates(removedDuplicates);
        response.setRemovedOrphans(removedOrphans);
        response.setKept(kept);
        response.setDuplicateActivitiesDetected(duplicateActivitiesDetected);

        if (removedDuplicates > 0 || removedOrphans > 0) {
            response.setMessage("Conexiones duplicadas limpiadas correctamente.");
        } else {
            response.setMessage("No se encontraron conexiones duplicadas para limpiar.");
        }

        if (duplicateActivitiesDetected) {
            response.setWarning(
                    "Existen actividades duplicadas con nombres similares. Revise las actividades.");
            if (removedDuplicates == 0 && removedOrphans == 0) {
                response.setMessage(
                        "No se pudieron limpiar todas las conexiones porque existen actividades duplicadas con nombres similares.");
            } else {
                response.setMessage(response.getMessage() + " " + response.getWarning());
            }
        }

        return response;
    }

    public WorkflowFlowValidationResponse validateFlow(String policyId) {
        validatePolicyExists(policyId);
        List<WorkflowActivity> activities = workflowActivityRepository.findByPolicyIdOrderByOrderIndexAsc(policyId);
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByPolicyIdOrderByOrderIndexAsc(policyId);

        WorkflowFlowValidationResponse result = new WorkflowFlowValidationResponse();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        boolean hasStart = activities.stream()
                .anyMatch(a -> a.isActive() && "START".equalsIgnoreCase(a.getActivityType()));
        boolean hasEnd = activities.stream()
                .anyMatch(a -> a.isActive() && "END".equalsIgnoreCase(a.getActivityType()));

        if (!hasStart) {
            warnings.add("Debe existir una actividad de inicio.");
        }
        if (!hasEnd) {
            warnings.add("Debe existir una actividad de fin.");
        }
        if (activities.isEmpty()) {
            errors.add("Agregue al menos una actividad para diseñar el flujo.");
        }

        Set<String> activeActivityIds = activities.stream()
                .filter(WorkflowActivity::isActive)
                .map(WorkflowActivity::getId)
                .collect(Collectors.toSet());

        Set<String> duplicatePairs = new HashSet<>();
        for (WorkflowTransition t : transitions) {
            if (!t.isActive()) {
                continue;
            }
            String pair = t.getFromActivityId() + "->" + t.getToActivityId();
            if (!duplicatePairs.add(pair)) {
                warnings.add("Existe una transición duplicada entre \""
                        + t.getFromActivityName() + "\" y \"" + t.getToActivityName() + "\".");
            }
            if ("CONDITIONAL".equalsIgnoreCase(t.getTransitionType())
                    && (t.getConditionLabel() == null || t.getConditionLabel().isBlank())) {
                warnings.add("La conexión condicional de \"" + t.getFromActivityName()
                        + "\" hacia \"" + t.getToActivityName() + "\" no tiene etiqueta de condición.");
            }
        }

        Set<String> withOutgoing = transitions.stream()
                .filter(WorkflowTransition::isActive)
                .map(WorkflowTransition::getFromActivityId)
                .collect(Collectors.toSet());
        Set<String> withIncoming = transitions.stream()
                .filter(WorkflowTransition::isActive)
                .map(WorkflowTransition::getToActivityId)
                .collect(Collectors.toSet());

        for (WorkflowActivity activity : activities) {
            if (!activity.isActive()) {
                continue;
            }
            String activityId = activity.getId();
            boolean isStart = "START".equalsIgnoreCase(activity.getActivityType());
            boolean isEnd = "END".equalsIgnoreCase(activity.getActivityType());
            boolean hasOut = withOutgoing.contains(activityId);
            boolean hasIn = withIncoming.contains(activityId);

            boolean isDecision = "DECISION".equalsIgnoreCase(activity.getActivityType());
            long conditionalOut = transitions.stream()
                    .filter(WorkflowTransition::isActive)
                    .filter(t -> activityId.equals(t.getFromActivityId()))
                    .filter(t -> "CONDITIONAL".equalsIgnoreCase(t.getTransitionType())
                            || (t.getConditionLabel() != null && !t.getConditionLabel().isBlank()))
                    .count();

            if (!isStart && !isEnd && !hasOut && !hasIn) {
                warnings.add("La actividad \"" + activity.getName() + "\" no está conectada.");
            } else if (!isEnd && !hasOut) {
                warnings.add("La actividad \"" + activity.getName() + "\" no tiene salida.");
            } else if (!isStart && !hasIn) {
                warnings.add("La actividad \"" + activity.getName() + "\" no tiene entrada.");
            } else if (isDecision && conditionalOut < 2) {
                warnings.add("La decisión \"" + activity.getName() + "\" necesita al menos dos salidas.");
            }
        }

        if (activeActivityIds.size() > 1 && transitions.stream().noneMatch(WorkflowTransition::isActive)) {
            warnings.add("Debe conectar las actividades para formar el flujo.");
        }

        result.setWarnings(warnings);
        result.setErrors(errors);
        result.setValid(errors.isEmpty() && warnings.isEmpty());
        if (result.isValid()) {
            result.setMessage("Flujo válido.");
        } else if (!errors.isEmpty()) {
            result.setMessage("El flujo tiene errores que deben corregirse.");
        } else {
            result.setMessage("El flujo tiene advertencias.");
        }
        return result;
    }

    public List<String> buildFlowPreview(String policyId) {
        List<WorkflowActivity> activities = workflowActivityRepository.findByPolicyIdOrderByOrderIndexAsc(policyId);
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByPolicyIdOrderByOrderIndexAsc(policyId)
                .stream().filter(WorkflowTransition::isActive).toList();

        if (activities.isEmpty()) {
            return List.of("Sin actividades configuradas.");
        }
        if (transitions.isEmpty()) {
            return List.of("Sin conexiones entre actividades.");
        }

        List<String> lines = new ArrayList<>();
        Map<String, List<WorkflowTransition>> byFrom = transitions.stream()
                .collect(Collectors.groupingBy(WorkflowTransition::getFromActivityId, LinkedHashMap::new, Collectors.toList()));

        for (WorkflowActivity activity : activities) {
            if (!activity.isActive()) {
                continue;
            }
            lines.add(activity.getName());
            List<WorkflowTransition> outs = byFrom.getOrDefault(activity.getId(), List.of());
            if (outs.isEmpty()) {
                continue;
            }
            if (outs.size() == 1 && "SEQUENTIAL".equalsIgnoreCase(outs.get(0).getTransitionType())) {
                lines.add("↓");
            } else {
                for (WorkflowTransition t : outs) {
                    String label = "CONDITIONAL".equalsIgnoreCase(t.getTransitionType())
                            && t.getConditionLabel() != null && !t.getConditionLabel().isBlank()
                            ? t.getConditionLabel()
                            : transitionTypeLabel(t.getTransitionType());
                    lines.add("├─ " + label + " → " + t.getToActivityName());
                }
            }
        }
        return lines;
    }

    private LocalDateTime transitionSortTime(WorkflowTransition transition) {
        return transition.getUpdatedAt() != null ? transition.getUpdatedAt() : transition.getCreatedAt();
    }

    private int compareTransitionPriority(WorkflowTransition a, WorkflowTransition b) {
        if (a.isActive() != b.isActive()) {
            return a.isActive() ? -1 : 1;
        }
        LocalDateTime aTime = transitionSortTime(a);
        LocalDateTime bTime = transitionSortTime(b);
        if (aTime == null && bTime == null) {
            return 0;
        }
        if (aTime == null) {
            return 1;
        }
        if (bTime == null) {
            return -1;
        }
        return bTime.compareTo(aTime);
    }

    private int removeDuplicateTransitionGroups(
            List<WorkflowTransition> transitions,
            Function<WorkflowTransition, String> keyFn,
            Set<String> pendingDelete,
            LocalDateTime now
    ) {
        Map<String, List<WorkflowTransition>> groups = new LinkedHashMap<>();
        for (WorkflowTransition transition : transitions) {
            if (pendingDelete.contains(transition.getId())) {
                continue;
            }
            String key = keyFn.apply(transition);
            if (key == null || key.isBlank()) {
                continue;
            }
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(transition);
        }

        int removed = 0;
        for (List<WorkflowTransition> group : groups.values()) {
            if (group.size() <= 1) {
                continue;
            }
            group.sort(this::compareTransitionPriority);
            WorkflowTransition keeper = group.get(0);
            if (!keeper.isActive()) {
                keeper.setActive(true);
                keeper.setUpdatedAt(now);
                workflowTransitionRepository.save(keeper);
            }
            for (int i = 1; i < group.size(); i++) {
                pendingDelete.add(group.get(i).getId());
                removed++;
            }
        }
        return removed;
    }

    private void deleteTransitions(Set<String> ids) {
        for (String id : ids) {
            workflowTransitionRepository.deleteById(id);
        }
    }

    private boolean isOrphanTransition(WorkflowTransition transition, Set<String> validActivityIds) {
        String fromId = transition.getFromActivityId();
        String toId = transition.getToActivityId();
        return fromId == null || fromId.isBlank()
                || toId == null || toId.isBlank()
                || !validActivityIds.contains(fromId)
                || !validActivityIds.contains(toId);
    }

    private String activityIdPairKey(WorkflowTransition transition) {
        if (transition.getFromActivityId() == null || transition.getToActivityId() == null) {
            return null;
        }
        return transition.getFromActivityId() + "->" + transition.getToActivityId();
    }

    private String normalizedNamePairKey(WorkflowTransition transition) {
        String from = normalizeTransitionName(transition.getFromActivityName());
        String to = normalizeTransitionName(transition.getToActivityName());
        if (from.isEmpty() || to.isEmpty()) {
            return null;
        }
        return from + "->" + to;
    }

    private String normalizeTransitionName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private boolean hasDuplicateActivitiesByName(List<WorkflowActivity> activities) {
        Map<String, Long> counts = activities.stream()
                .map(WorkflowActivity::getName)
                .map(this::normalizeTransitionName)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.values().stream().anyMatch(count -> count > 1);
    }

    private BusinessPolicy validatePolicyExists(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("La política de negocio es obligatoria");
        }
        return businessPolicyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("La política seleccionada no existe"));
    }

    private WorkflowActivity validateActivity(String activityId, String policyId) {
        if (activityId == null || activityId.isBlank()) {
            throw new IllegalArgumentException("La actividad es obligatoria");
        }
        WorkflowActivity activity = workflowActivityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        if (!policyId.equals(activity.getPolicyId())) {
            throw new IllegalArgumentException("Las actividades deben pertenecer a la misma política");
        }
        return activity;
    }

    private void validateRequest(WorkflowTransitionRequest request, boolean creating) {
        if (creating && (request.getPolicyId() == null || request.getPolicyId().isBlank())) {
            throw new IllegalArgumentException("La política de negocio es obligatoria");
        }
        if (request.getFromActivityId() == null || request.getFromActivityId().isBlank()) {
            throw new IllegalArgumentException("La actividad origen es obligatoria");
        }
        if (request.getToActivityId() == null || request.getToActivityId().isBlank()) {
            throw new IllegalArgumentException("La actividad destino es obligatoria");
        }
        if (request.getFromActivityId().equals(request.getToActivityId())) {
            throw new IllegalArgumentException("La actividad origen y destino no pueden ser iguales");
        }

        String type = request.getTransitionType() != null
                ? request.getTransitionType().trim().toUpperCase()
                : "SEQUENTIAL";
        if ("CONDITIONAL".equals(type)
                && (request.getConditionLabel() == null || request.getConditionLabel().isBlank())) {
            throw new IllegalArgumentException("Debe indicar una condición para conexiones condicionales");
        }
        if (request.getOrderIndex() != null && request.getOrderIndex() < 1) {
            throw new IllegalArgumentException("El orden debe ser un número positivo");
        }
    }

    private int resolveOrderIndex(String policyId, Integer requested) {
        if (requested != null && requested > 0) {
            return requested;
        }
        return workflowTransitionRepository.findByPolicyIdOrderByOrderIndexAsc(policyId).stream()
                .mapToInt(WorkflowTransition::getOrderIndex)
                .max()
                .orElse(0) + 1;
    }

    private void applyRequest(
            WorkflowTransition transition,
            WorkflowTransitionRequest request,
            WorkflowActivity from,
            WorkflowActivity to,
            String policyId
    ) {
        transition.setPolicyId(policyId);
        transition.setFromActivityId(from.getId());
        transition.setFromActivityName(from.getName());
        transition.setToActivityId(to.getId());
        transition.setToActivityName(to.getName());
        if (request.getTransitionType() != null) {
            transition.setTransitionType(request.getTransitionType().trim().toUpperCase());
        } else if (transition.getTransitionType() == null) {
            transition.setTransitionType("SEQUENTIAL");
        }
        transition.setConditionLabel(request.getConditionLabel() != null ? request.getConditionLabel().trim() : null);
        transition.setConditionExpression(
                request.getConditionExpression() != null ? request.getConditionExpression().trim() : null
        );
        if (request.getActive() != null) {
            transition.setActive(request.getActive());
        }
    }

    private WorkflowTransitionResponse toResponse(WorkflowTransition transition) {
        WorkflowTransitionResponse response = new WorkflowTransitionResponse();
        response.setId(transition.getId());
        response.setPolicyId(transition.getPolicyId());
        response.setFromActivityId(transition.getFromActivityId());
        response.setFromActivityName(transition.getFromActivityName());
        response.setToActivityId(transition.getToActivityId());
        response.setToActivityName(transition.getToActivityName());
        response.setTransitionType(transition.getTransitionType());
        response.setTransitionTypeLabel(transitionTypeLabel(transition.getTransitionType()));
        response.setConditionLabel(transition.getConditionLabel());
        response.setConditionExpression(transition.getConditionExpression());
        response.setOrderIndex(transition.getOrderIndex());
        response.setActive(transition.isActive());
        response.setCreatedAt(transition.getCreatedAt());
        response.setUpdatedAt(transition.getUpdatedAt());
        return response;
    }

    private String transitionTypeLabel(String type) {
        if (type == null) {
            return "Secuencial";
        }
        return TRANSITION_TYPE_LABELS.getOrDefault(type.toUpperCase(), type);
    }
}
