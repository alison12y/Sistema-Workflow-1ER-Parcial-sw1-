package com.workflow.politicas.service;

import com.workflow.politicas.model.Activity;
import com.workflow.politicas.model.DynamicForm;
import com.workflow.politicas.repository.ActivityRepository;
import com.workflow.politicas.repository.DynamicFormRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DynamicFormService {

    private final DynamicFormRepository dynamicFormRepository;
    private final ActivityRepository activityRepository;

    public DynamicFormService(DynamicFormRepository dynamicFormRepository, ActivityRepository activityRepository) {
        this.dynamicFormRepository = dynamicFormRepository;
        this.activityRepository = activityRepository;
    }

    public DynamicForm create(DynamicForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        form.setCreatedAt(LocalDateTime.now());
        form.setUpdatedAt(LocalDateTime.now());
        return dynamicFormRepository.save(form);
    }

    public Optional<DynamicForm> findByActivityId(String activityId) {
        Optional<Activity> activity = activityRepository.findById(activityId);
        if (activity.isEmpty()) {
            return Optional.empty();
        }
        String formId = activity.get().getDynamicFormId();
        if (formId == null || formId.isBlank()) {
            return Optional.empty();
        }
        return dynamicFormRepository.findById(formId);
    }

    public Optional<DynamicForm> findById(String id) {
        return dynamicFormRepository.findById(id);
    }

    public DynamicForm update(String id, DynamicForm details) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DynamicForm not found with id: " + id));
        if (details.getName() != null) {
            form.setName(details.getName());
        }
        if (details.getDescription() != null) {
            form.setDescription(details.getDescription());
        }
        form.setUpdatedAt(LocalDateTime.now());
        return dynamicFormRepository.save(form);
    }
}
