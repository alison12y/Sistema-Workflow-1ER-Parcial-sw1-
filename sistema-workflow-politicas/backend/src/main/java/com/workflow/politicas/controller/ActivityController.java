package com.workflow.politicas.controller;

import com.workflow.politicas.model.Activity;
import com.workflow.politicas.service.ActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/diagram/{diagramId}")
    public List<Activity> getActivitiesByDiagram(@PathVariable String diagramId) {
        return activityService.findByDiagramId(diagramId);
    }

    @PostMapping
    public Activity createActivity(@RequestBody Activity activity) {
        return activityService.create(activity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Activity> updateActivity(@PathVariable String id, @RequestBody Activity details) {
        try {
            return ResponseEntity.ok(activityService.update(id, details));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable String id) {
        try {
            activityService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
