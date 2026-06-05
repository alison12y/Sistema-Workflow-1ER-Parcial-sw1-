package com.workflow.politicas.service;

import com.workflow.politicas.dto.*;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.model.WorkflowTransition;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.repository.WorkflowTransitionRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/** Vista diseñador UML 2.5 (swimlanes) — modelo oficial Ciclo 1. */
@Service
public class WorkflowDesignerService {

    private static final int LANE_HEIGHT = 200;
    private static final int LANE_LABEL_WIDTH = 200;
    private static final int NODE_HORIZONTAL_MIN = 220;
    private static final int NODE_TOP_PADDING = 50;

    private static final Map<String, String> ACTIVITY_TYPE_LABELS = Map.ofEntries(
            Map.entry("START", "Inicio"),
            Map.entry("TASK", "Tarea"),
            Map.entry("DECISION", "Decisión"),
            Map.entry("END", "Fin"),
            Map.entry("FORK", "Fork"),
            Map.entry("JOIN", "Join")
    );

    private static final Map<String, String> TRANSITION_TYPE_LABELS = Map.of(
            "SEQUENTIAL", "Secuencial",
            "CONDITIONAL", "Condicional",
            "ITERATIVE", "Iterativa",
            "PARALLEL_SPLIT", "División paralela",
            "PARALLEL_JOIN", "Unión paralela"
    );

    private static final Map<String, String> POLICY_STATUS_LABELS = Map.of(
            "DRAFT", "Borrador",
            "ACTIVE", "Activa",
            "ARCHIVED", "Archivada",
            "INACTIVE", "Inactiva"
    );

    private final BusinessPolicyRepository businessPolicyRepository;
    private final WorkflowActivityRepository workflowActivityRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowTransitionService workflowTransitionService;

    public WorkflowDesignerService(
            BusinessPolicyRepository businessPolicyRepository,
            WorkflowActivityRepository workflowActivityRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            WorkflowTransitionService workflowTransitionService
    ) {
        this.businessPolicyRepository = businessPolicyRepository;
        this.workflowActivityRepository = workflowActivityRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.workflowTransitionService = workflowTransitionService;
    }

    public WorkflowDesignerResponse getDesignerData(String policyId) {
        BusinessPolicy policy = businessPolicyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("La política seleccionada no existe"));

        List<WorkflowActivity> allActivities = workflowActivityRepository
                .findByPolicyIdOrderByOrderIndexAsc(policyId);
        List<WorkflowTransition> allTransitions = workflowTransitionRepository
                .findByPolicyIdOrderByOrderIndexAsc(policyId);
        List<WorkflowTransition> activeTransitions = allTransitions.stream()
                .filter(WorkflowTransition::isActive)
                .toList();

        Set<String> referencedActivityIds = new LinkedHashSet<>();
        for (WorkflowTransition transition : activeTransitions) {
            if (transition.getFromActivityId() != null && !transition.getFromActivityId().isBlank()) {
                referencedActivityIds.add(transition.getFromActivityId());
            }
            if (transition.getToActivityId() != null && !transition.getToActivityId().isBlank()) {
                referencedActivityIds.add(transition.getToActivityId());
            }
        }

        List<WorkflowActivity> canvasActivities = allActivities.stream()
                .filter(activity -> activity.isActive() || referencedActivityIds.contains(activity.getId()))
                .toList();

        List<LaneResponse> lanes = buildLanes(canvasActivities);
        Map<String, WorkflowActivity> activityById = new LinkedHashMap<>();
        for (WorkflowActivity activity : canvasActivities) {
            activityById.put(activity.getId(), activity);
        }
        List<TransitionEdgeResponse> transitionEdges = activeTransitions.stream()
                .map(this::toTransitionEdge)
                .toList();
        List<ActivityNodeResponse> activityNodes = buildActivityNodes(lanes, activeTransitions, activityById);

        WorkflowDesignerResponse response = new WorkflowDesignerResponse();
        response.setPolicyId(policy.getId());
        response.setPolicyName(policy.getName());
        response.setPolicyDescription(policy.getDescription());
        response.setPolicyStatus(policyStatusLabel(policy.getStatus()));
        response.setActivities(activityNodes);
        response.setTransitions(transitionEdges);
        response.setLanes(lanes);
        response.setFlowPreview(buildNumberedFlowPreview(canvasActivities, activeTransitions));
        response.setFlowValidation(workflowTransitionService.validateFlow(policyId));
        return response;
    }

    private List<LaneResponse> buildLanes(List<WorkflowActivity> activeActivities) {
        if (activeActivities.isEmpty()) {
            return List.of();
        }

        Map<String, List<WorkflowActivity>> byLane = new LinkedHashMap<>();
        for (WorkflowActivity activity : activeActivities) {
            String laneName = resolveLaneName(activity);
            byLane.computeIfAbsent(laneName, k -> new ArrayList<>()).add(activity);
        }

        List<LaneResponse> lanes = new ArrayList<>();
        for (Map.Entry<String, List<WorkflowActivity>> entry : byLane.entrySet()) {
            List<WorkflowActivity> laneActivities = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(WorkflowActivity::getOrderIndex))
                    .toList();

            LaneResponse lane = new LaneResponse();
            lane.setLaneName(entry.getKey());
            lane.setResponsibleType(laneActivities.get(0).getResponsibleType());
            lane.setActivities(laneActivities.stream().map(this::toActivityNodeWithoutPosition).toList());
            lanes.add(lane);
        }
        return lanes;
    }

    private List<ActivityNodeResponse> buildActivityNodes(
            List<LaneResponse> lanes,
            List<WorkflowTransition> activeTransitions,
            Map<String, WorkflowActivity> activityById
    ) {
        Map<String, Integer> incoming = new HashMap<>();
        Map<String, Integer> outgoing = new HashMap<>();
        Map<String, Integer> outgoingConditional = new HashMap<>();

        for (WorkflowTransition transition : activeTransitions) {
            String fromId = transition.getFromActivityId();
            String toId = transition.getToActivityId();
            outgoing.merge(fromId, 1, Integer::sum);
            incoming.merge(toId, 1, Integer::sum);
            if ("CONDITIONAL".equalsIgnoreCase(transition.getTransitionType())) {
                outgoingConditional.merge(fromId, 1, Integer::sum);
            }
        }

        Map<String, Integer> laneIndexByName = new HashMap<>();
        for (int i = 0; i < lanes.size(); i++) {
            laneIndexByName.put(lanes.get(i).getLaneName(), i);
        }

        List<ActivityNodeResponse> nodes = new ArrayList<>();
        for (LaneResponse lane : lanes) {
            int laneIndex = laneIndexByName.getOrDefault(lane.getLaneName(), 0);
            for (ActivityNodeResponse base : lane.getActivities()) {
                ActivityNodeResponse node = copyNode(base);
                WorkflowActivity sourceActivity = activityById.get(node.getId());
                if (hasSavedPosition(sourceActivity)) {
                    node.setX(sourceActivity.getPositionX());
                    node.setY(sourceActivity.getPositionY());
                } else {
                    int orderIndex = Math.max(1, node.getOrderIndex());
                    node.setX(LANE_LABEL_WIDTH + (orderIndex - 1) * NODE_HORIZONTAL_MIN);
                    node.setY(laneIndex * LANE_HEIGHT + NODE_TOP_PADDING);
                }
                String nodeId = node.getId();
                int inCount = incoming.getOrDefault(nodeId, 0);
                int outCount = outgoing.getOrDefault(nodeId, 0);
                int condOut = outgoingConditional.getOrDefault(nodeId, 0);
                node.setIncomingCount(inCount);
                node.setOutgoingCount(outCount);
                node.setOutgoingConditionalCount(condOut);
                node.setDecisionNode(isDecisionNode(node.getActivityType(), condOut));
                nodes.add(node);
            }
        }
        return nodes;
    }

    private boolean hasSavedPosition(WorkflowActivity activity) {
        return activity != null
                && activity.getPositionX() != null
                && activity.getPositionY() != null;
    }

    private boolean isDecisionNode(String activityType, int outgoingConditionalCount) {
        if ("DECISION".equalsIgnoreCase(activityType)) {
            return true;
        }
        return outgoingConditionalCount > 1;
    }

    private ActivityNodeResponse copyNode(ActivityNodeResponse source) {
        ActivityNodeResponse node = new ActivityNodeResponse();
        node.setId(source.getId());
        node.setName(source.getName());
        node.setDescription(source.getDescription());
        node.setResponsibleName(source.getResponsibleName());
        node.setActivityType(source.getActivityType());
        node.setActivityTypeLabel(source.getActivityTypeLabel());
        node.setStatus(source.getStatus());
        node.setOrderIndex(source.getOrderIndex());
        node.setEstimatedTimeHours(source.getEstimatedTimeHours());
        node.setX(source.getX());
        node.setY(source.getY());
        node.setDecisionNode(source.isDecisionNode());
        node.setOutgoingConditionalCount(source.getOutgoingConditionalCount());
        node.setIncomingCount(source.getIncomingCount());
        node.setOutgoingCount(source.getOutgoingCount());
        return node;
    }

    private ActivityNodeResponse toActivityNodeWithoutPosition(WorkflowActivity activity) {
        ActivityNodeResponse node = new ActivityNodeResponse();
        node.setId(activity.getId());
        node.setName(activity.getName());
        node.setDescription(activity.getDescription());
        node.setResponsibleName(resolveLaneName(activity));
        node.setActivityType(activity.getActivityType());
        node.setActivityTypeLabel(activityTypeLabel(activity.getActivityType()));
        node.setStatus(activityStatusLabel(activity.getStatus()));
        node.setOrderIndex(activity.getOrderIndex());
        node.setEstimatedTimeHours(activity.getEstimatedTimeHours());
        return node;
    }

    private TransitionEdgeResponse toTransitionEdge(WorkflowTransition transition) {
        TransitionEdgeResponse edge = new TransitionEdgeResponse();
        edge.setId(transition.getId());
        edge.setFromActivityId(transition.getFromActivityId());
        edge.setFromActivityName(transition.getFromActivityName());
        edge.setToActivityId(transition.getToActivityId());
        edge.setToActivityName(transition.getToActivityName());
        edge.setTransitionType(transition.getTransitionType());
        edge.setTransitionTypeLabel(transitionTypeLabel(transition.getTransitionType()));
        edge.setConditionLabel(transition.getConditionLabel());
        edge.setActive(transition.isActive());
        return edge;
    }

    private List<String> buildNumberedFlowPreview(
            List<WorkflowActivity> activities,
            List<WorkflowTransition> transitions
    ) {
        if (activities.isEmpty()) {
            return List.of("Sin actividades configuradas.");
        }
        if (transitions.isEmpty()) {
            return List.of("Sin conexiones entre actividades.");
        }

        List<String> lines = new ArrayList<>();
        int index = 1;
        for (WorkflowTransition transition : transitions) {
            String arrow = transition.getFromActivityName() + " → " + transition.getToActivityName();
            if ("CONDITIONAL".equalsIgnoreCase(transition.getTransitionType())
                    && transition.getConditionLabel() != null
                    && !transition.getConditionLabel().isBlank()) {
                arrow += " [" + transition.getConditionLabel().trim() + "]";
            } else if (!"SEQUENTIAL".equalsIgnoreCase(transition.getTransitionType())) {
                arrow += " (" + transitionTypeLabel(transition.getTransitionType()) + ")";
            }
            lines.add(index + ". " + arrow);
            index++;
        }
        return lines;
    }

    private String resolveLaneName(WorkflowActivity activity) {
        if (activity.getResponsibleName() != null && !activity.getResponsibleName().isBlank()) {
            return activity.getResponsibleName().trim();
        }
        return "Sin responsable asignado";
    }

    private String activityTypeLabel(String type) {
        if (type == null) {
            return "Tarea";
        }
        return ACTIVITY_TYPE_LABELS.getOrDefault(type.toUpperCase(), type);
    }

    private String activityStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "Borrador";
        }
        return switch (status.toUpperCase()) {
            case "ACTIVA" -> "Activa";
            case "INACTIVA" -> "Inactiva";
            case "BORRADOR" -> "Borrador";
            default -> status;
        };
    }

    private String transitionTypeLabel(String type) {
        if (type == null) {
            return "Secuencial";
        }
        return TRANSITION_TYPE_LABELS.getOrDefault(type.toUpperCase(), type);
    }

    private String policyStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "Borrador";
        }
        return POLICY_STATUS_LABELS.getOrDefault(status.toUpperCase(), status);
    }
}
