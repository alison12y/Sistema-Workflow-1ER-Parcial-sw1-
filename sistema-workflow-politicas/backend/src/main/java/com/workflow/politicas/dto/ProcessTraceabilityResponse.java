package com.workflow.politicas.dto;

import com.workflow.politicas.model.AuditLog;
import com.workflow.politicas.model.ProcessInstance;
import com.workflow.politicas.model.TaskInstance;

import java.util.ArrayList;
import java.util.List;

public class ProcessTraceabilityResponse {
    private ProcessInstance process;
    private List<TaskInstance> tasks = new ArrayList<>();
    private List<AuditLog> auditTrail = new ArrayList<>();

    public ProcessTraceabilityResponse() {}

    public ProcessInstance getProcess() { return process; }
    public void setProcess(ProcessInstance process) { this.process = process; }

    public List<TaskInstance> getTasks() { return tasks; }
    public void setTasks(List<TaskInstance> tasks) { this.tasks = tasks; }

    public List<AuditLog> getAuditTrail() { return auditTrail; }
    public void setAuditTrail(List<AuditLog> auditTrail) { this.auditTrail = auditTrail; }
}
