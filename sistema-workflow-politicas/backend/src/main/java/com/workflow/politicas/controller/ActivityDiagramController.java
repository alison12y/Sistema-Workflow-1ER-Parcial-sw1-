package com.workflow.politicas.controller;

import com.workflow.politicas.dto.ActivityDiagramSaveRequest;
import com.workflow.politicas.model.ActivityDiagram;
import com.workflow.politicas.service.ActivityDiagramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activity-diagrams")
public class ActivityDiagramController {

    private final ActivityDiagramService activityDiagramService;

    public ActivityDiagramController(ActivityDiagramService activityDiagramService) {
        this.activityDiagramService = activityDiagramService;
    }

    @GetMapping("/policy/{policyId}")
    public ResponseEntity<ActivityDiagram> getByPolicy(@PathVariable String policyId) {
        return ResponseEntity.ok(activityDiagramService.getByPolicyId(policyId));
    }

    @PostMapping("/save")
    public ResponseEntity<ActivityDiagram> save(@RequestBody ActivityDiagramSaveRequest request) {
        return ResponseEntity.ok(activityDiagramService.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActivityDiagram> update(
            @PathVariable String id,
            @RequestBody ActivityDiagramSaveRequest request
    ) {
        return ResponseEntity.ok(activityDiagramService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        activityDiagramService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
