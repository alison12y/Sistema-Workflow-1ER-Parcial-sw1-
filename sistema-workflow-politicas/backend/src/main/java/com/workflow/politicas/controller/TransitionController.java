package com.workflow.politicas.controller;

import com.workflow.politicas.model.Transition;
import com.workflow.politicas.service.TransitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** @deprecated API motor BPM legacy (C). */
@Deprecated(since = "0.0.1-cycle1-f0")
@RestController
@RequestMapping("/api/transitions")
public class TransitionController {

    private final TransitionService transitionService;

    public TransitionController(TransitionService transitionService) {
        this.transitionService = transitionService;
    }

    @GetMapping("/diagram/{diagramId}")
    public List<Transition> getTransitionsByDiagram(@PathVariable String diagramId) {
        return transitionService.findByDiagramId(diagramId);
    }

    @PostMapping
    public Transition createTransition(@RequestBody Transition transition) {
        return transitionService.create(transition);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transition> updateTransition(@PathVariable String id, @RequestBody Transition details) {
        try {
            return ResponseEntity.ok(transitionService.update(id, details));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransition(@PathVariable String id) {
        try {
            transitionService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
