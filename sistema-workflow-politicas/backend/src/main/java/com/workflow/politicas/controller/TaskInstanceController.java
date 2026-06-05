package com.workflow.politicas.controller;

import com.workflow.politicas.dto.TaskCompleteRequest;
import com.workflow.politicas.model.TaskInstance;
import com.workflow.politicas.service.TaskInstanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** @deprecated API motor BPM legacy (C). */
@Deprecated(since = "0.0.1-cycle1-f0")
@RestController
@RequestMapping("/api/tasks")
public class TaskInstanceController {

    private final TaskInstanceService taskInstanceService;

    public TaskInstanceController(TaskInstanceService taskInstanceService) {
        this.taskInstanceService = taskInstanceService;
    }

    @GetMapping("/my/{userId}")
    public List<TaskInstance> getMyTasks(@PathVariable String userId) {
        return taskInstanceService.findByUserId(userId);
    }

    @GetMapping("/role/{roleId}")
    public List<TaskInstance> getTasksByRole(@PathVariable String roleId) {
        return taskInstanceService.findByRoleId(roleId);
    }

    @PostMapping("/{id}/complete")
    public TaskInstance completeTask(@PathVariable String id, @RequestBody(required = false) TaskCompleteRequest request) {
        return taskInstanceService.complete(id, request);
    }
}
