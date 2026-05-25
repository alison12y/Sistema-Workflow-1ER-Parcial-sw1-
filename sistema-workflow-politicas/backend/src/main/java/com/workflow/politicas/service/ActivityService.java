package com.workflow.politicas.service;

import com.workflow.politicas.model.Activity;
import com.workflow.politicas.repository.ActivityRepository;
import com.workflow.politicas.repository.WorkflowDiagramRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final WorkflowDiagramRepository workflowDiagramRepository;

    public ActivityService(ActivityRepository activityRepository, WorkflowDiagramRepository workflowDiagramRepository) {
        this.activityRepository = activityRepository;
        this.workflowDiagramRepository = workflowDiagramRepository;
    }

    public List<Activity> findByDiagramId(String diagramId) {
        return activityRepository.findByDiagramId(diagramId);
    }

    public Optional<Activity> findById(String id) {
        return activityRepository.findById(id);
    }

    public Activity create(Activity activity) {
        if (activity.getDiagramId() == null || activity.getDiagramId().isBlank()) {
            throw new IllegalArgumentException("diagramId is required");
        }
        if (!workflowDiagramRepository.existsById(activity.getDiagramId())) {
            throw new IllegalArgumentException("WorkflowDiagram not found: " + activity.getDiagramId());
        }
        if (activity.getName() == null || activity.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (activity.getType() == null || activity.getType().isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        return activityRepository.save(activity);
    }

    public Activity update(String id, Activity details) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + id));
        if (details.getName() != null) {
            activity.setName(details.getName());
        }
        if (details.getType() != null) {
            activity.setType(details.getType());
        }
        if (details.getSwimlaneId() != null) {
            activity.setSwimlaneId(details.getSwimlaneId());
        }
        if (details.getDynamicFormId() != null) {
            activity.setDynamicFormId(details.getDynamicFormId());
        }
        return activityRepository.save(activity);
    }

    public void deleteById(String id) {
        if (!activityRepository.existsById(id)) {
            throw new RuntimeException("Activity not found with id: " + id);
        }
        activityRepository.deleteById(id);
    }
}
