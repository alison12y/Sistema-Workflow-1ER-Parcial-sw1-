package com.workflow.politicas.controller;

import com.workflow.politicas.dto.FormFieldRequest;
import com.workflow.politicas.dto.FormFieldResponse;
import com.workflow.politicas.dto.WorkflowDeleteResponse;
import com.workflow.politicas.model.FormField;
import com.workflow.politicas.service.FormFieldService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/form-fields")
public class FormFieldController {

    private final FormFieldService formFieldService;

    public FormFieldController(FormFieldService formFieldService) {
        this.formFieldService = formFieldService;
    }

    @GetMapping("/form/{formId}")
    public List<FormFieldResponse> getFieldsByForm(@PathVariable String formId) {
        return formFieldService.findByFormId(formId);
    }

    @PostMapping
    public ResponseEntity<FormFieldResponse> createField(@RequestBody FormFieldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formFieldService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormFieldResponse> updateField(
            @PathVariable String id,
            @RequestBody FormFieldRequest request
    ) {
        return ResponseEntity.ok(formFieldService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WorkflowDeleteResponse> deleteField(@PathVariable String id) {
        return ResponseEntity.ok(formFieldService.delete(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<FormFieldResponse> activateField(@PathVariable String id) {
        return ResponseEntity.ok(formFieldService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<FormFieldResponse> deactivateField(@PathVariable String id) {
        return ResponseEntity.ok(formFieldService.deactivate(id));
    }

    // Compatibilidad legado
    @PostMapping("/legacy")
    public FormField createLegacyField(@RequestBody FormField field) {
        return formFieldService.createLegacy(field);
    }

    @PutMapping("/legacy/{id}")
    public ResponseEntity<FormField> updateLegacyField(@PathVariable String id, @RequestBody FormField details) {
        return ResponseEntity.ok(formFieldService.updateLegacy(id, details));
    }
}
