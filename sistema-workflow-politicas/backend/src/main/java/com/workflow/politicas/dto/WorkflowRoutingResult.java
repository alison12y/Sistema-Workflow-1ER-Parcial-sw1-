package com.workflow.politicas.dto;

import com.workflow.politicas.model.WorkflowActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado del motor de enrutamiento Ciclo 1 (F1).
 */
public class WorkflowRoutingResult {

    public enum Outcome {
        /** Activar una o más tareas (paralelo o secuencial). */
        ACTIVATE_TASKS,
        /** Trámite finalizado (END alcanzado). */
        COMPLETED,
        /** Esperar otras ramas paralelas. */
        WAIT_PARALLEL,
        /** Error de definición del workflow. */
        WORKFLOW_ERROR
    }

    private Outcome outcome = Outcome.WORKFLOW_ERROR;
    private List<WorkflowActivity> nextActivities = new ArrayList<>();
    private String pendingJoinActivityId;
    private String message;
    private String errorDetail;

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public List<WorkflowActivity> getNextActivities() {
        return nextActivities;
    }

    public void setNextActivities(List<WorkflowActivity> nextActivities) {
        this.nextActivities = nextActivities != null ? nextActivities : new ArrayList<>();
    }

    public String getPendingJoinActivityId() {
        return pendingJoinActivityId;
    }

    public void setPendingJoinActivityId(String pendingJoinActivityId) {
        this.pendingJoinActivityId = pendingJoinActivityId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public static WorkflowRoutingResult completed(String message) {
        WorkflowRoutingResult r = new WorkflowRoutingResult();
        r.setOutcome(Outcome.COMPLETED);
        r.setMessage(message);
        return r;
    }

    public static WorkflowRoutingResult waitParallel(String message) {
        WorkflowRoutingResult r = new WorkflowRoutingResult();
        r.setOutcome(Outcome.WAIT_PARALLEL);
        r.setMessage(message);
        return r;
    }

    public static WorkflowRoutingResult error(String detail) {
        WorkflowRoutingResult r = new WorkflowRoutingResult();
        r.setOutcome(Outcome.WORKFLOW_ERROR);
        r.setErrorDetail(detail);
        r.setMessage(detail);
        return r;
    }

    public static WorkflowRoutingResult activate(List<WorkflowActivity> activities, String joinId, String message) {
        WorkflowRoutingResult r = new WorkflowRoutingResult();
        r.setOutcome(Outcome.ACTIVATE_TASKS);
        r.setNextActivities(activities);
        r.setPendingJoinActivityId(joinId);
        r.setMessage(message);
        return r;
    }
}
