package com.workflow.politicas.service;

import com.workflow.politicas.model.Transition;
import com.workflow.politicas.repository.ActivityRepository;
import com.workflow.politicas.repository.TransitionRepository;
import com.workflow.politicas.repository.WorkflowDiagramRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransitionService {

    private final TransitionRepository transitionRepository;
    private final WorkflowDiagramRepository workflowDiagramRepository;
    private final ActivityRepository activityRepository;

    public TransitionService(
            TransitionRepository transitionRepository,
            WorkflowDiagramRepository workflowDiagramRepository,
            ActivityRepository activityRepository) {
        this.transitionRepository = transitionRepository;
        this.workflowDiagramRepository = workflowDiagramRepository;
        this.activityRepository = activityRepository;
    }

    public List<Transition> findByDiagramId(String diagramId) {
        return transitionRepository.findByDiagramId(diagramId);
    }

    public Optional<Transition> findById(String id) {
        return transitionRepository.findById(id);
    }

    public Transition create(Transition transition) {
        validateTransition(transition);
        return transitionRepository.save(transition);
    }

    public Transition update(String id, Transition details) {
        Transition transition = transitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transition not found with id: " + id));
        if (details.getSourceActivityId() != null) {
            transition.setSourceActivityId(details.getSourceActivityId());
        }
        if (details.getTargetActivityId() != null) {
            transition.setTargetActivityId(details.getTargetActivityId());
        }
        if (details.getCondition() != null) {
            transition.setCondition(details.getCondition());
        }
        validateTransition(transition);
        return transitionRepository.save(transition);
    }

    public void deleteById(String id) {
        if (!transitionRepository.existsById(id)) {
            throw new RuntimeException("Transition not found with id: " + id);
        }
        transitionRepository.deleteById(id);
    }

    private void validateTransition(Transition transition) {
        if (transition.getDiagramId() == null || transition.getDiagramId().isBlank()) {
            throw new IllegalArgumentException("diagramId is required");
        }
        if (!workflowDiagramRepository.existsById(transition.getDiagramId())) {
            throw new IllegalArgumentException("WorkflowDiagram not found: " + transition.getDiagramId());
        }
        if (transition.getSourceActivityId() == null || transition.getSourceActivityId().isBlank()) {
            throw new IllegalArgumentException("sourceActivityId is required");
        }
        if (transition.getTargetActivityId() == null || transition.getTargetActivityId().isBlank()) {
            throw new IllegalArgumentException("targetActivityId is required");
        }
        if (!activityRepository.existsById(transition.getSourceActivityId())) {
            throw new IllegalArgumentException("Source activity not found: " + transition.getSourceActivityId());
        }
        if (!activityRepository.existsById(transition.getTargetActivityId())) {
            throw new IllegalArgumentException("Target activity not found: " + transition.getTargetActivityId());
        }
    }
}
