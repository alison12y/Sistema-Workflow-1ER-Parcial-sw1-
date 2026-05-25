package com.workflow.politicas.repository;

import com.workflow.politicas.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    java.util.List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId);
    java.util.List<AuditLog> findByUserId(String userId);
    java.util.List<AuditLog> findByEntityNameAndEntityIdIn(String entityName, java.util.List<String> entityIds);
}
