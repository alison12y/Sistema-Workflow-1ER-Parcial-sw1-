package com.workflow.politicas.service;

import com.workflow.politicas.dto.TaskCompleteRequest;
import com.workflow.politicas.model.TaskInstance;
import com.workflow.politicas.repository.TaskInstanceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** @deprecated Motor BPM legacy (C). */
@Deprecated(since = "0.0.1-cycle1-f0")
@Service
public class TaskInstanceService {

    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_COMPLETED = "COMPLETED";

    private final TaskInstanceRepository taskInstanceRepository;
    private final ProcessInstanceService processInstanceService;
    private final AuditLogService auditLogService;

    public TaskInstanceService(
            TaskInstanceRepository taskInstanceRepository,
            ProcessInstanceService processInstanceService,
            AuditLogService auditLogService) {
        this.taskInstanceRepository = taskInstanceRepository;
        this.processInstanceService = processInstanceService;
        this.auditLogService = auditLogService;
    }

    public List<TaskInstance> findByUserId(String userId) {
        return taskInstanceRepository.findByAssignedUserId(userId).stream()
                .filter(t -> TASK_PENDING.equals(t.getStatus()))
                .toList();
    }

    public List<TaskInstance> findByRoleId(String roleId) {
        return taskInstanceRepository.findByAssignedRoleId(roleId).stream()
                .filter(t -> TASK_PENDING.equals(t.getStatus()))
                .toList();
    }

    public TaskInstance complete(String taskId, TaskCompleteRequest request) {
        TaskInstance task = taskInstanceRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("TaskInstance not found with id: " + taskId));

        if (TASK_COMPLETED.equals(task.getStatus())) {
            throw new IllegalStateException("Task is already completed");
        }

        String previousStatus = task.getStatus();
        String userId = task.getAssignedUserId() != null && !task.getAssignedUserId().isBlank()
                ? task.getAssignedUserId()
                : processInstanceService.findById(task.getProcessInstanceId())
                        .map(p -> p.getInitiatorId())
                        .orElse("system");

        task.setStatus(TASK_COMPLETED);
        task.setStepData(request != null ? request.getStepData() : null);
        task.setCompletedAt(LocalDateTime.now());
        task = taskInstanceRepository.save(task);

        auditLogService.register(
                "TaskInstance",
                task.getId(),
                "COMPLETE_TASK",
                userId,
                previousStatus,
                TASK_COMPLETED,
                "Task completed for process: " + task.getProcessInstanceId() + ", activity: " + task.getActivityId()
        );

        processInstanceService.advanceAfterTaskCompletion(task, task.getStepData());
        return task;
    }
}
