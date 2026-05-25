package com.workflow.politicas.service;

import com.workflow.politicas.model.AuditLog;
import com.workflow.politicas.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog register(
            String entityName,
            String entityId,
            String action,
            String userId,
            String previousState,
            String newState,
            String details) {
        AuditLog log = new AuditLog();
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setUserId(userId);
        log.setPreviousState(previousState);
        log.setNewState(newState);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        return auditLogRepository.save(log);
    }

    public List<AuditLog> findAll() {
        return auditLogRepository.findAll().stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .toList();
    }

    public List<AuditLog> findByEntity(String entityName, String entityId) {
        return auditLogRepository.findByEntityNameAndEntityId(entityName, entityId).stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .toList();
    }

    public List<AuditLog> findByUserId(String userId) {
        return auditLogRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .toList();
    }

    public List<AuditLog> findTraceabilityForProcess(String processId, List<String> taskIds) {
        List<AuditLog> processLogs = auditLogRepository.findByEntityNameAndEntityId("ProcessInstance", processId);
        List<AuditLog> taskLogs = taskIds.isEmpty()
                ? List.of()
                : auditLogRepository.findByEntityNameAndEntityIdIn("TaskInstance", taskIds);
        return java.util.stream.Stream.concat(processLogs.stream(), taskLogs.stream())
                .sorted(Comparator.comparing(AuditLog::getTimestamp))
                .toList();
    }
}
