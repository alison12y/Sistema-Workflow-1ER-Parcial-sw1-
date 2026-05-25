package com.workflow.politicas.service;

import com.workflow.politicas.dto.WorkflowValidationResponse;
import com.workflow.politicas.model.Activity;
import com.workflow.politicas.model.Transition;
import com.workflow.politicas.model.WorkflowDiagram;
import com.workflow.politicas.repository.ActivityRepository;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.TransitionRepository;
import com.workflow.politicas.repository.WorkflowDiagramRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WorkflowDiagramService {

    private final WorkflowDiagramRepository workflowDiagramRepository;
    private final ActivityRepository activityRepository;
    private final TransitionRepository transitionRepository;
    private final BusinessPolicyRepository businessPolicyRepository;

    public WorkflowDiagramService(
            WorkflowDiagramRepository workflowDiagramRepository,
            ActivityRepository activityRepository,
            TransitionRepository transitionRepository,
            BusinessPolicyRepository businessPolicyRepository) {
        this.workflowDiagramRepository = workflowDiagramRepository;
        this.activityRepository = activityRepository;
        this.transitionRepository = transitionRepository;
        this.businessPolicyRepository = businessPolicyRepository;
    }

    public WorkflowDiagram create(WorkflowDiagram diagram) {
        if (diagram.getPolicyId() == null || diagram.getPolicyId().isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (!businessPolicyRepository.existsById(diagram.getPolicyId())) {
            throw new IllegalArgumentException("Policy not found: " + diagram.getPolicyId());
        }
        diagram.setCreatedAt(LocalDateTime.now());
        return workflowDiagramRepository.save(diagram);
    }

    public List<WorkflowDiagram> findByPolicyId(String policyId) {
        return workflowDiagramRepository.findByPolicyId(policyId);
    }

    public Optional<WorkflowDiagram> findById(String id) {
        return workflowDiagramRepository.findById(id);
    }

    public WorkflowDiagram update(String id, WorkflowDiagram details) {
        WorkflowDiagram diagram = workflowDiagramRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkflowDiagram not found with id: " + id));
        if (details.getPolicyId() != null) {
            diagram.setPolicyId(details.getPolicyId());
        }
        if (details.getXmlData() != null) {
            diagram.setXmlData(details.getXmlData());
        }
        if (details.getJsonData() != null) {
            diagram.setJsonData(details.getJsonData());
        }
        if (details.getVersion() != null) {
            diagram.setVersion(details.getVersion());
        }
        return workflowDiagramRepository.save(diagram);
    }

    public WorkflowValidationResponse validate(String diagramId) {
        workflowDiagramRepository.findById(diagramId)
                .orElseThrow(() -> new RuntimeException("WorkflowDiagram not found with id: " + diagramId));

        List<Activity> activities = activityRepository.findByDiagramId(diagramId);
        List<Transition> transitions = transitionRepository.findByDiagramId(diagramId);
        List<String> errors = new ArrayList<>();

        boolean hasStart = activities.stream().anyMatch(a -> "START".equals(a.getType()));
        boolean hasEnd = activities.stream().anyMatch(a -> "END".equals(a.getType()));

        if (!hasStart) {
            errors.add("Workflow must have at least one START activity");
        }
        if (!hasEnd) {
            errors.add("Workflow must have at least one END activity");
        }

        for (Activity activity : activities) {
            if ("TASK".equals(activity.getType())) {
                if (activity.getSwimlaneId() == null || activity.getSwimlaneId().isBlank()) {
                    errors.add("TASK activity '" + activity.getName() + "' must have swimlaneId");
                }
            }
            if ("DECISION".equals(activity.getType())) {
                boolean hasConditionTransition = transitions.stream()
                        .filter(t -> activity.getId().equals(t.getSourceActivityId()))
                        .anyMatch(t -> t.getCondition() != null && !t.getCondition().isBlank());
                if (!hasConditionTransition) {
                    errors.add("DECISION activity '" + activity.getName() + "' must have at least one outgoing transition with condition");
                }
            }
        }

        WorkflowValidationResponse response = new WorkflowValidationResponse();
        response.setValid(errors.isEmpty());
        response.setErrors(errors);
        return response;
    }
}
