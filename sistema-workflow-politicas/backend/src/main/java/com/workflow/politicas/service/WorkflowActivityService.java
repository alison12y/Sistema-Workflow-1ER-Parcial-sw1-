package com.workflow.politicas.service;

import com.workflow.politicas.dto.WorkflowActivityResponse;
import com.workflow.politicas.model.ActivityDiagram;
import com.workflow.politicas.model.DiagramNode;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.repository.ActivityDiagramRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WorkflowActivityService {

    private final WorkflowActivityRepository workflowActivityRepository;
    private final ActivityDiagramRepository activityDiagramRepository;

    public WorkflowActivityService(
            WorkflowActivityRepository workflowActivityRepository,
            ActivityDiagramRepository activityDiagramRepository
    ) {
        this.workflowActivityRepository = workflowActivityRepository;
        this.activityDiagramRepository = activityDiagramRepository;
    }

    public List<WorkflowActivityResponse> findByPolicyId(String policyId) {
        List<WorkflowActivity> stored = workflowActivityRepository.findByPolicyIdOrderByOrderAsc(policyId);
        if (!stored.isEmpty()) {
            return stored.stream().map(this::toResponse).toList();
        }
        return fromDiagram(policyId);
    }

    public int countByPolicyId(String policyId) {
        long stored = workflowActivityRepository.countByPolicyId(policyId);
        if (stored > 0) {
            return (int) stored;
        }
        return fromDiagram(policyId).size();
    }

    private List<WorkflowActivityResponse> fromDiagram(String policyId) {
        List<WorkflowActivityResponse> result = new ArrayList<>();
        AtomicInteger order = new AtomicInteger(1);
        activityDiagramRepository.findByPolicyId(policyId).ifPresent(diagram -> {
            if (diagram.getNodes() == null) {
                return;
            }
            for (DiagramNode node : diagram.getNodes()) {
                if (node.getType() == null || !"ACTION".equalsIgnoreCase(node.getType())) {
                    continue;
                }
                WorkflowActivityResponse response = new WorkflowActivityResponse();
                response.setPolicyId(policyId);
                response.setName(node.getLabel() != null ? node.getLabel() : "Actividad");
                response.setDescription("Actividad del diagrama de workflow");
                response.setResponsible(node.getLane() != null ? node.getLane() : "Sin asignar");
                response.setResponsibleType("ROLE");
                response.setActivityType("MANUAL");
                response.setOrder(order.getAndIncrement());
                response.setStatus("CONFIGURED");
                result.add(response);
            }
        });
        return result;
    }

    private WorkflowActivityResponse toResponse(WorkflowActivity activity) {
        WorkflowActivityResponse response = new WorkflowActivityResponse();
        response.setId(activity.getId());
        response.setName(activity.getName());
        response.setDescription(activity.getDescription());
        response.setPolicyId(activity.getPolicyId());
        response.setResponsible(activity.getResponsible());
        response.setResponsibleType(activity.getResponsibleType());
        response.setActivityType(activity.getActivityType());
        response.setOrder(activity.getOrder());
        response.setEstimatedMinutes(activity.getEstimatedMinutes());
        response.setStatus(activity.getStatus());
        response.setFormId(activity.getFormId());
        return response;
    }
}
