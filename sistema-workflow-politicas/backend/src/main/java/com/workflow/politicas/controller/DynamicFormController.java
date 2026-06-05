package com.workflow.politicas.controller;

import com.workflow.politicas.dto.*;
import com.workflow.politicas.model.DynamicForm;
import com.workflow.politicas.service.DynamicFormService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forms")
public class DynamicFormController {

    private final DynamicFormService dynamicFormService;

    public DynamicFormController(DynamicFormService dynamicFormService) {
        this.dynamicFormService = dynamicFormService;
    }

    @GetMapping("/activity/{activityId}")
    public DynamicFormResponse getFormByActivity(@PathVariable String activityId) {
        return dynamicFormService.getByActivityId(activityId);
    }

    @GetMapping("/activity/{activityId}/detail")
    public DynamicFormDetailResponse getDetailByActivity(@PathVariable String activityId) {
        return dynamicFormService.getDetailByWorkflowActivityId(activityId);
    }

    @PostMapping
    public ResponseEntity<DynamicFormResponse> createForm(@RequestBody DynamicFormRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dynamicFormService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DynamicFormResponse> updateForm(
            @PathVariable String id,
            @RequestBody DynamicFormRequest request
    ) {
        return ResponseEntity.ok(dynamicFormService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WorkflowDeleteResponse> deleteForm(@PathVariable String id) {
        return ResponseEntity.ok(dynamicFormService.delete(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<DynamicFormResponse> activateForm(@PathVariable String id) {
        return ResponseEntity.ok(dynamicFormService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<DynamicFormResponse> deactivateForm(@PathVariable String id) {
        return ResponseEntity.ok(dynamicFormService.deactivate(id));
    }

    // ——— Compatibilidad diseño anterior ———

    @PostMapping("/legacy")
    public DynamicForm createLegacyForm(@RequestBody DynamicForm form) {
        return dynamicFormService.createLegacy(form);
    }

    @PostMapping("/save")
    public ResponseEntity<DynamicFormDetailResponse> saveForm(@RequestBody DynamicFormSaveRequest request) {
        return ResponseEntity.ok(dynamicFormService.saveFull(request));
    }

    @GetMapping("/policy/{policyId}")
    public ResponseEntity<DynamicFormDetailResponse> getFormByPolicyAndActivity(
            @PathVariable String policyId,
            @RequestParam String activity
    ) {
        return ResponseEntity.ok(dynamicFormService.getByPolicyAndActivity(policyId, activity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DynamicFormResponse> getFormById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(dynamicFormService.getById(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/legacy/{id}")
    public ResponseEntity<DynamicForm> updateLegacyForm(@PathVariable String id, @RequestBody DynamicForm details) {
        return ResponseEntity.ok(dynamicFormService.updateLegacy(id, details));
    }
}
