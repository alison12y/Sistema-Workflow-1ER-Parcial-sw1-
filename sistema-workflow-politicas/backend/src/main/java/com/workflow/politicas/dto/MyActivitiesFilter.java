package com.workflow.politicas.dto;

/**
 * Filtros opcionales de la bandeja del funcionario .
 */
public class MyActivitiesFilter {

    /** PENDIENTE, EN_CURSO, COMPLETADA, OBSERVADA, ERROR */
    private String status;
    private String policyId;
    private String tramiteId;
    private String tramiteCode;
    private String priority;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getTramiteId() {
        return tramiteId;
    }

    public void setTramiteId(String tramiteId) {
        this.tramiteId = tramiteId;
    }

    public String getTramiteCode() {
        return tramiteCode;
    }

    public void setTramiteCode(String tramiteCode) {
        this.tramiteCode = tramiteCode;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
