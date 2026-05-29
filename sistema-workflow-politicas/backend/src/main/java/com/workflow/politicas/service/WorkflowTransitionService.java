package com.workflow.politicas.service;

import com.workflow.politicas.dto.WorkflowFlowValidationResponse;
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

        if (workflowTransitionRepository.existsByPolicyIdAndFromActivityIdAndToActivityId(
                policy.getId(), from.getId(), to.getId())) {
            throw new IllegalArgumentException("La conexión ya existe");
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

        boolean duplicate = workflowTransitionRepository.existsByPolicyIdAndFromActivityIdAndToActivityId(
                policyId, from.getId(), to.getId());
        if (duplicate && !(from.getId().equals(transition.getFromActivityId())
                && to.getId().equals(transition.getToActivityId()))) {
            throw new IllegalArgumentException("La conexión ya existe");
        }

        applyRequest(transition, request, from, to, policyId);
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            transition.setOrderIndex(request.getOrderIndex());
        }
        transition.setUpdatedAt(LocalDateTime.now());
        return toResponse(workflowTransitionRepository.save(transition));
    }

    public void delete(String id) {
        WorkflowTransition transition = workflowTransitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conexión no encontrada"));
        transition.setActive(false);
        transition.setUpdatedAt(LocalDateTime.now());
        workflowTransitionRepository.save(transition);
    }

    public WorkflowTransitionResponse activate(String id) {
        WorkflowTransition transition = workflowTransitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conexión no encontrada"));
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
            warnings.add("La política no tiene actividad de Inicio.");
        }
        if (!hasEnd) {
            warnings.add("La política no tiene actividad de Fin.");
        }
        if (activities.isEmpty()) {
            errors.add("La política no tiene actividades configuradas.");
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

            if (!isStart && !isEnd && !hasOut && !hasIn) {
                warnings.add("La actividad \"" + activity.getName() + "\" está aislada (sin conexiones).");
            } else if (!isEnd && !hasOut) {
                warnings.add("La actividad \"" + activity.getName() + "\" no tiene salida.");
            } else if (!isStart && !hasIn) {
                warnings.add("La actividad \"" + activity.getName() + "\" no tiene entrada.");
            }
        }

        if (activeActivityIds.size() > 1 && transitions.stream().noneMatch(WorkflowTransition::isActive)) {
            warnings.add("La política tiene actividades pero no tiene conexiones activas.");
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
