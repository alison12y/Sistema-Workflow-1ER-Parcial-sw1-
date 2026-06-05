package com.workflow.politicas.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class KpiDashboardFullResponse {
    private KpiSummaryResponse summary = new KpiSummaryResponse();
    private long tareasPendientes;
    private long tareasEnProceso;
    private long tareasCompletadas;
    private long tramitesActivos;
    private long tramitesConError;
    private boolean sufficientData;
    private String message;
    private LocalDateTime generatedAt;
    private List<KpiActivityMetricDto> slowActivities = new ArrayList<>();
    private List<KpiLoadMetricDto> employeeLoad = new ArrayList<>();
    private List<KpiLoadMetricDto> departmentLoad = new ArrayList<>();
    private List<KpiBottleneckDto> bottlenecks = new ArrayList<>();

    public KpiSummaryResponse getSummary() {
        return summary;
    }

    public void setSummary(KpiSummaryResponse summary) {
        this.summary = summary;
    }

    public long getTareasPendientes() {
        return tareasPendientes;
    }

    public void setTareasPendientes(long tareasPendientes) {
        this.tareasPendientes = tareasPendientes;
    }

    public long getTareasEnProceso() {
        return tareasEnProceso;
    }

    public void setTareasEnProceso(long tareasEnProceso) {
        this.tareasEnProceso = tareasEnProceso;
    }

    public long getTareasCompletadas() {
        return tareasCompletadas;
    }

    public void setTareasCompletadas(long tareasCompletadas) {
        this.tareasCompletadas = tareasCompletadas;
    }

    public long getTramitesActivos() {
        return tramitesActivos;
    }

    public void setTramitesActivos(long tramitesActivos) {
        this.tramitesActivos = tramitesActivos;
    }

    public long getTramitesConError() {
        return tramitesConError;
    }

    public void setTramitesConError(long tramitesConError) {
        this.tramitesConError = tramitesConError;
    }

    public boolean isSufficientData() {
        return sufficientData;
    }

    public void setSufficientData(boolean sufficientData) {
        this.sufficientData = sufficientData;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<KpiActivityMetricDto> getSlowActivities() {
        return slowActivities;
    }

    public void setSlowActivities(List<KpiActivityMetricDto> slowActivities) {
        this.slowActivities = slowActivities;
    }

    public List<KpiLoadMetricDto> getEmployeeLoad() {
        return employeeLoad;
    }

    public void setEmployeeLoad(List<KpiLoadMetricDto> employeeLoad) {
        this.employeeLoad = employeeLoad;
    }

    public List<KpiLoadMetricDto> getDepartmentLoad() {
        return departmentLoad;
    }

    public void setDepartmentLoad(List<KpiLoadMetricDto> departmentLoad) {
        this.departmentLoad = departmentLoad;
    }

    public List<KpiBottleneckDto> getBottlenecks() {
        return bottlenecks;
    }

    public void setBottlenecks(List<KpiBottleneckDto> bottlenecks) {
        this.bottlenecks = bottlenecks;
    }
}
