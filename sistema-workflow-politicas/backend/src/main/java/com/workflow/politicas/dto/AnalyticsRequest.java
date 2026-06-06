package com.workflow.politicas.dto;

import java.time.LocalDate;

/**
 * Solicitud base para analítica inteligente (CU24–CU26).
 */
public class AnalyticsRequest {

    private String message;
    private String audioText;
    private String policyId;
    /** INICIADO, EN_PROCESO, ACTIVO, FINALIZADO, CANCELADO, ERROR */
    private String status;
    private LocalDate fromDate;
    private LocalDate toDate;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAudioText() {
        return audioText;
    }

    public void setAudioText(String audioText) {
        this.audioText = audioText;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }
}
