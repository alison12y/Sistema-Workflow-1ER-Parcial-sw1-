package com.workflow.politicas.controller;

import com.workflow.politicas.model.FormField;
import com.workflow.politicas.service.FormFieldService;
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
    public List<FormField> getFieldsByForm(@PathVariable String formId) {
        return formFieldService.findByFormId(formId);
    }

    @PostMapping
    public FormField createField(@RequestBody FormField field) {
        return formFieldService.create(field);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormField> updateField(@PathVariable String id, @RequestBody FormField details) {
        try {
            return ResponseEntity.ok(formFieldService.update(id, details));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteField(@PathVariable String id) {
        try {
            formFieldService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
