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

@Service
public class WorkflowDesignerService {

    private static final int LANE_HEIGHT = 140;
    private static final int LANE_LABEL_WIDTH = 180;
    private static final int NODE_WIDTH = 200;
    private static final int NODE_HORIZONTAL_GAP = 48;
    private static final int NODE_TOP_PADDING = 36;

    private static final Map<String, String> ACTIVITY_TYPE_LABELS = Map.of(
            "START", "Inicio",
            "TASK", "Tarea",
            "DECISION", "Decisión",
            "END", "Fin"
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
        List<WorkflowActivity> activeActivities = allActivities.stream()
                .filter(WorkflowActivity::isActive)
                .toList();

        List<WorkflowTransition> allTransitions = workflowTransitionRepository
                .findByPolicyIdOrderByOrderIndexAsc(policyId);
        List<WorkflowTransition> activeTransitions = allTransitions.stream()
                .filter(WorkflowTransition::isActive)
                .toList();

        List<LaneResponse> lanes = buildLanes(activeActivities);
        List<ActivityNodeResponse> activityNodes = buildActivityNodes(lanes);
        List<TransitionEdgeResponse> transitionEdges = activeTransitions.stream()
                .map(this::toTransitionEdge)
                .toList();

        WorkflowDesignerResponse response = new WorkflowDesignerResponse();
        response.setPolicyId(policy.getId());
        response.setPolicyName(policy.getName());
        response.setPolicyDescription(policy.getDescription());
        response.setPolicyStatus(policyStatusLabel(policy.getStatus()));
        response.setActivities(activityNodes);
        response.setTransitions(transitionEdges);
        response.setLanes(lanes);
        response.setFlowPreview(buildNumberedFlowPreview(activeActivities, activeTransitions));
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

    private List<ActivityNodeResponse> buildActivityNodes(List<LaneResponse> lanes) {
        List<ActivityNodeResponse> nodes = new ArrayList<>();
        int laneIndex = 0;
        for (LaneResponse lane : lanes) {
            int y = laneIndex * LANE_HEIGHT + NODE_TOP_PADDING;
            int x = LANE_LABEL_WIDTH;
            int indexInLane = 0;
            for (ActivityNodeResponse base : lane.getActivities()) {
                ActivityNodeResponse node = copyNode(base);
                node.setX(x + indexInLane * (NODE_WIDTH + NODE_HORIZONTAL_GAP));
                node.setY(y);
                nodes.add(node);
                indexInLane++;
            }
            laneIndex++;
        }
        return nodes;
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
                arrow += " si " + transition.getConditionLabel();
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
