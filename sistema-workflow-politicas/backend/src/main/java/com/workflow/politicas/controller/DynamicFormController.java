package com.workflow.politicas.controller;

import com.workflow.politicas.dto.DynamicFormDetailResponse;
import com.workflow.politicas.dto.DynamicFormSaveRequest;
import com.workflow.politicas.model.DynamicForm;
import com.workflow.politicas.service.DynamicFormService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forms")
public class DynamicFormController {

    private final DynamicFormService dynamicFormService;

    public DynamicFormController(DynamicFormService dynamicFormService) {
        this.dynamicFormService = dynamicFormService;
    }

    @PostMapping
    public DynamicForm createForm(@RequestBody DynamicForm form) {
        return dynamicFormService.create(form);
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

    @GetMapping("/{activityId}")
    public ResponseEntity<DynamicForm> getFormByActivity(@PathVariable String activityId) {
        return dynamicFormService.findByActivityId(activityId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DynamicForm> updateForm(@PathVariable String id, @RequestBody DynamicForm details) {
        try {
            return ResponseEntity.ok(dynamicFormService.update(id, details));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
