package com.workflow.politicas.controller;

import com.workflow.politicas.dto.AiFormAssistTraceRequest;
import com.workflow.politicas.dto.CompleteActivityRequest;
import com.workflow.politicas.dto.MyActivitiesFilter;
import com.workflow.politicas.dto.MyActivityDto;
import com.workflow.politicas.dto.TaskAssistantResponseDto;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.service.MyActivitiesService;
import com.workflow.politicas.service.TaskAssistantLocalFallback;
import com.workflow.politicas.service.TaskAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/my-activities")
public class MyActivitiesController {

    private final MyActivitiesService myActivitiesService;
    private final TaskAssistantService taskAssistantService;

    public MyActivitiesController(
            MyActivitiesService myActivitiesService,
            TaskAssistantService taskAssistantService
    ) {
        this.myActivitiesService = myActivitiesService;
        this.taskAssistantService = taskAssistantService;
    }

    @GetMapping
    public List<MyActivityDto> list(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String policyId,
            @RequestParam(required = false) String tramiteId,
            @RequestParam(required = false) String tramiteCode,
            @RequestParam(required = false) String priority
    ) {
        MyActivitiesFilter filter = new MyActivitiesFilter();
        filter.setStatus(status);
        filter.setPolicyId(policyId);
        filter.setTramiteId(tramiteId);
        filter.setTramiteCode(tramiteCode);
        filter.setPriority(priority);
        return myActivitiesService.listInbox(resolveUsername(authentication), filter);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MyActivityDto> getById(
            @PathVariable String id,
            @RequestParam(required = false) Integer taskOrder,
            Authentication authentication
    ) {
        String username = resolveUsername(authentication);
        if (taskOrder != null && taskOrder > 0) {
            return myActivitiesService.findForUser(id, taskOrder, username)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        return myActivitiesService.findForUser(id, username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/tasks/{taskOrder}/take")
    public Tramite takeTask(
            @PathVariable String id,
            @PathVariable int taskOrder,
            Authentication authentication
    ) {
        return myActivitiesService.takeTask(id, taskOrder, resolveUsername(authentication));
    }

    @PostMapping("/{activityId}/tasks/{taskId}/ai-assistant")
    public TaskAssistantResponseDto taskAssistant(
            @PathVariable String activityId,
            @PathVariable int taskId,
            Authentication authentication
    ) {
        String username = resolveUsername(authentication);
        try {
            return taskAssistantService.assist(activityId, taskId, username);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            return TaskAssistantLocalFallback.build(Map.of(
                    "tramiteId", activityId,
                    "tramiteName", activityId,
                    "activityName", "Actividad",
                    "taskStatus", "PENDIENTE",
                    "assignedTo", username
            ));
        }
    }

    @PostMapping("/{id}/ai-form-assisted")
    public ResponseEntity<Void> recordAiFormAssisted(
            @PathVariable String id,
            @RequestBody AiFormAssistTraceRequest request,
            Authentication authentication
    ) {
        try {
            myActivitiesService.recordAiFormAssisted(id, request, resolveUsername(authentication));
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PutMapping("/{id}/complete")
    public Tramite complete(
            @PathVariable String id,
            @RequestBody CompleteActivityRequest request,
            Authentication authentication
    ) {
        return myActivitiesService.completeActivity(id, request, resolveUsername(authentication));
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        return authentication.getName();
    }
}
