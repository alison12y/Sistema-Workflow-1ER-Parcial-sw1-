package com.workflow.politicas.controller;

import com.workflow.politicas.dto.*;
import com.workflow.politicas.service.AiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/assistant")
    public ResponseEntity<AiAssistantResponse> assistant(@RequestBody AiAssistantRequest request) {
        try {
            return ResponseEntity.ok(aiService.assistant(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @PostMapping("/workflow/suggest")
    public ResponseEntity<AiWorkflowSuggestResponse> suggestWorkflow(@RequestBody AiWorkflowSuggestRequest request) {
        try {
            return ResponseEntity.ok(aiService.suggestWorkflow(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/generate-workflow")
    public ResponseEntity<AiWorkflowGenerateResponse> generateWorkflow(@RequestBody AiWorkflowGenerateRequest request) {
        try {
            return ResponseEntity.ok(aiService.generateWorkflow(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @PostMapping("/assist-form")
    public ResponseEntity<AiFormAssistResponse> assistForm(@RequestBody AiFormAssistRequest request) {
        try {
            return ResponseEntity.ok(aiService.assistForm(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @PostMapping("/validate-diagram")
    public ResponseEntity<AiValidateDiagramResponse> validateDiagram(@RequestBody AiValidateDiagramRequest request) {
        try {
            return ResponseEntity.ok(aiService.validateDiagram(request));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
