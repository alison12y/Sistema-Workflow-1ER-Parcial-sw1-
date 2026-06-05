package com.workflow.politicas.service;

import com.workflow.politicas.dto.ProcessStartRequest;
import com.workflow.politicas.model.Activity;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.ProcessInstance;
import com.workflow.politicas.model.TaskInstance;
import com.workflow.politicas.model.Transition;
import com.workflow.politicas.model.WorkflowDiagram;
import com.workflow.politicas.repository.ActivityRepository;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.ProcessInstanceRepository;
import com.workflow.politicas.repository.TaskInstanceRepository;
import com.workflow.politicas.repository.TransitionRepository;
import com.workflow.politicas.repository.WorkflowDiagramRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @deprecated Motor BPM legacy (C). Ciclo 1 usa {@link TramiteService}.
 */
@Deprecated(since = "0.0.1-cycle1-f0")
@Service
public class ProcessInstanceService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_COMPLETED = "COMPLETED";

    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final BusinessPolicyRepository businessPolicyRepository;
    private final WorkflowDiagramRepository workflowDiagramRepository;
    private final ActivityRepository activityRepository;
    private final TransitionRepository transitionRepository;
    private final AuditLogService auditLogService;

    public ProcessInstanceService(
            ProcessInstanceRepository processInstanceRepository,
            TaskInstanceRepository taskInstanceRepository,
            BusinessPolicyRepository businessPolicyRepository,
            WorkflowDiagramRepository workflowDiagramRepository,
            ActivityRepository activityRepository,
            TransitionRepository transitionRepository,
            AuditLogService auditLogService) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskInstanceRepository = taskInstanceRepository;
        this.businessPolicyRepository = businessPolicyRepository;
        this.workflowDiagramRepository = workflowDiagramRepository;
        this.activityRepository = activityRepository;
        this.transitionRepository = transitionRepository;
        this.auditLogService = auditLogService;
    }

    public ProcessInstance start(ProcessStartRequest request) {
        if (request.getPolicyId() == null || request.getPolicyId().isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (request.getInitiatorId() == null || request.getInitiatorId().isBlank()) {
            throw new IllegalArgumentException("initiatorId is required");
        }

        BusinessPolicy policy = businessPolicyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + request.getPolicyId()));

        if (!STATUS_ACTIVE.equals(policy.getStatus())) {
            throw new IllegalStateException("Process can only be started from an ACTIVE policy");
        }

        String diagramId = resolveDiagramId(policy);
        List<Activity> activities = activityRepository.findByDiagramId(diagramId);
        List<Transition> transitions = transitionRepository.findByDiagramId(diagramId);

        Activity startActivity = activities.stream()
                .filter(a -> "START".equals(a.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Workflow has no START activity"));

        Activity nextActivity = resolveNextTaskOrEnd(startActivity, transitions, activities, null);

        ProcessInstance process = new ProcessInstance();
        process.setPolicyId(policy.getId());
        process.setInitiatorId(request.getInitiatorId());
        process.setFormData(request.getFormData());
        process.setStatus(STATUS_IN_PROGRESS);
        process.setStartedAt(LocalDateTime.now());
        process = processInstanceRepository.save(process);

        if ("END".equals(nextActivity.getType())) {
            finalizeProcess(process, nextActivity.getId());
            process = processInstanceRepository.save(process);
            auditLogService.register(
                    "ProcessInstance",
                    process.getId(),
                    "START_PROCESS",
                    request.getInitiatorId(),
                    null,
                    process.getStatus(),
                    "Process started and completed immediately for policy: " + policy.getId()
            );
            return process;
        }

        process.setCurrentActivityId(nextActivity.getId());
        process = processInstanceRepository.save(process);
        createPendingTask(process, nextActivity);
        auditLogService.register(
                "ProcessInstance",
                process.getId(),
                "START_PROCESS",
                request.getInitiatorId(),
                null,
                STATUS_IN_PROGRESS,
                "Process started for policy: " + policy.getId() + ", first activity: " + nextActivity.getName()
        );
        return process;
    }

    public List<ProcessInstance> findAll() {
        return processInstanceRepository.findAll();
    }

    public Optional<ProcessInstance> findById(String id) {
        return processInstanceRepository.findById(id);
    }

    public ProcessInstance advanceAfterTaskCompletion(TaskInstance task, java.util.Map<String, Object> stepData) {
        ProcessInstance process = processInstanceRepository.findById(task.getProcessInstanceId())
                .orElseThrow(() -> new RuntimeException("ProcessInstance not found"));

        if (!STATUS_IN_PROGRESS.equals(process.getStatus())) {
            throw new IllegalStateException("Process is not in progress");
        }

        String previousProcessStatus = process.getStatus();

        BusinessPolicy policy = businessPolicyRepository.findById(process.getPolicyId())
                .orElseThrow(() -> new RuntimeException("Policy not found"));
        String diagramId = resolveDiagramId(policy);
        List<Activity> activities = activityRepository.findByDiagramId(diagramId);
        List<Transition> transitions = transitionRepository.findByDiagramId(diagramId);

        Activity currentActivity = activityRepository.findById(task.getActivityId())
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        Activity nextActivity = resolveNextTaskOrEnd(currentActivity, transitions, activities, stepData);

        if ("END".equals(nextActivity.getType())) {
            finalizeProcess(process, nextActivity.getId());
            process = processInstanceRepository.save(process);
            auditLogService.register(
                    "ProcessInstance",
                    process.getId(),
                    "COMPLETE_PROCESS",
                    resolveAuditUserId(task),
                    previousProcessStatus,
                    STATUS_COMPLETED,
                    "Process completed at activity: " + nextActivity.getName()
            );
            return process;
        }

        process.setCurrentActivityId(nextActivity.getId());
        process = processInstanceRepository.save(process);
        createPendingTask(process, nextActivity);
        auditLogService.register(
                "ProcessInstance",
                process.getId(),
                "ADVANCE_PROCESS",
                resolveAuditUserId(task),
                previousProcessStatus,
                STATUS_IN_PROGRESS,
                "Process advanced to activity: " + nextActivity.getName()
        );
        return process;
    }

    private String resolveAuditUserId(TaskInstance task) {
        if (task.getAssignedUserId() != null && !task.getAssignedUserId().isBlank()) {
            return task.getAssignedUserId();
        }
        return processInstanceRepository.findById(task.getProcessInstanceId())
                .map(ProcessInstance::getInitiatorId)
                .orElse("system");
    }

    private String resolveDiagramId(BusinessPolicy policy) {
        if (policy.getCurrentDiagramId() != null && !policy.getCurrentDiagramId().isBlank()) {
            return policy.getCurrentDiagramId();
        }
        List<WorkflowDiagram> diagrams = workflowDiagramRepository.findByPolicyId(policy.getId());
        if (diagrams.isEmpty()) {
            throw new IllegalStateException("Policy has no workflow diagram");
        }
        return diagrams.get(diagrams.size() - 1).getId();
    }

    private Activity resolveNextTaskOrEnd(
            Activity from,
            List<Transition> transitions,
            List<Activity> activities,
            java.util.Map<String, Object> stepData) {
        Activity current = from;
        while (current != null && !"TASK".equals(current.getType()) && !"END".equals(current.getType())) {
            current = resolveSingleNextActivity(current, transitions, activities, stepData);
        }
        if (current == null) {
            throw new IllegalStateException("No reachable TASK or END activity after: " + from.getId());
        }
        return current;
    }

    private Activity resolveSingleNextActivity(
            Activity from,
            List<Transition> transitions,
            List<Activity> activities,
            java.util.Map<String, Object> stepData) {
        List<Transition> outgoing = transitions.stream()
                .filter(t -> from.getId().equals(t.getSourceActivityId()))
                .toList();

        if (outgoing.isEmpty()) {
            throw new IllegalStateException("No transitions from activity: " + from.getName());
        }

        Transition chosen = outgoing.get(0);
        if ("DECISION".equals(from.getType()) && stepData != null && outgoing.size() > 1) {
            for (Transition t : outgoing) {
                if (t.getCondition() != null && stepData.containsKey(t.getCondition())) {
                    Object value = stepData.get(t.getCondition());
                    if (Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value))) {
                        chosen = t;
                        break;
                    }
                }
            }
        }

        String targetId = chosen.getTargetActivityId();
        return activities.stream()
                .filter(a -> a.getId().equals(targetId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Target activity not found: " + targetId));
    }

    private void finalizeProcess(ProcessInstance process, String endActivityId) {
        process.setStatus(STATUS_COMPLETED);
        process.setCurrentActivityId(endActivityId);
        process.setEndedAt(LocalDateTime.now());
    }

    private void createPendingTask(ProcessInstance process, Activity activity) {
        TaskInstance task = new TaskInstance();
        task.setProcessInstanceId(process.getId());
        task.setActivityId(activity.getId());
        task.setAssignedRoleId(activity.getSwimlaneId());
        task.setStatus(TASK_PENDING);
        task.setCreatedAt(LocalDateTime.now());
        taskInstanceRepository.save(task);
    }
}
