package com.workflow.politicas.service;

import com.workflow.politicas.dto.ProcessTraceabilityResponse;
import com.workflow.politicas.model.ProcessInstance;
import com.workflow.politicas.model.TaskInstance;
import com.workflow.politicas.repository.ProcessInstanceRepository;
import com.workflow.politicas.repository.TaskInstanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MonitoringService {

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final AuditLogService auditLogService;

    public MonitoringService(
            ProcessInstanceRepository processInstanceRepository,
            TaskInstanceRepository taskInstanceRepository,
            AuditLogService auditLogService) {
        this.processInstanceRepository = processInstanceRepository;
        this.taskInstanceRepository = taskInstanceRepository;
        this.auditLogService = auditLogService;
    }

    public List<ProcessInstance> listRunningProcesses() {
        return processInstanceRepository.findByStatus(STATUS_IN_PROGRESS);
    }

    public Optional<ProcessInstance> getProcessDetail(String processId) {
        return processInstanceRepository.findById(processId);
    }

    public List<TaskInstance> getProcessTasks(String processId) {
        return taskInstanceRepository.findByProcessInstanceId(processId);
    }

    public Optional<ProcessTraceabilityResponse> getTraceability(String processId) {
        return processInstanceRepository.findById(processId).map(process -> {
            List<TaskInstance> tasks = taskInstanceRepository.findByProcessInstanceId(processId);
            List<String> taskIds = tasks.stream().map(TaskInstance::getId).toList();

            ProcessTraceabilityResponse response = new ProcessTraceabilityResponse();
            response.setProcess(process);
            response.setTasks(tasks);
            response.setAuditTrail(auditLogService.findTraceabilityForProcess(processId, taskIds));
            return response;
        });
    }
}
