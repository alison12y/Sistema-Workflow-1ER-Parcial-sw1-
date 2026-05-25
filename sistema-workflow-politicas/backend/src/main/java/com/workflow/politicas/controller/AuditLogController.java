package com.workflow.politicas.controller;

import com.workflow.politicas.model.AuditLog;
import com.workflow.politicas.service.AuditLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<AuditLog> getAllAuditLogs() {
        return auditLogService.findAll();
    }

    @GetMapping("/entity/{entityName}/{entityId}")
    public List<AuditLog> getAuditByEntity(@PathVariable String entityName, @PathVariable String entityId) {
        return auditLogService.findByEntity(entityName, entityId);
    }

    @GetMapping("/user/{userId}")
    public List<AuditLog> getAuditByUser(@PathVariable String userId) {
        return auditLogService.findByUserId(userId);
    }
}
