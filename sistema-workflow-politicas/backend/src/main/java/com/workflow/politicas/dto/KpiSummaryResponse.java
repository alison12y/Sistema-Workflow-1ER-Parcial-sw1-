package com.workflow.politicas.dto;

public class KpiSummaryResponse {
    private long totalTramites;
    private long iniciados;
    private long enProceso;
    private long finalizados;
    private long cancelados;
    private long tramitesActivos;
    private long tramitesConError;
    private long tareasPendientes;
    private long tareasEnProceso;
    private long tareasCompletadas;
    private String tiempoPromedio;
    private String tiempoPromedioActividad;
    private String actividadMayorDemora;
    private String responsableMayorCarga;
    private String cuelloDeBotellaPrincipal;

    public long getTotalTramites() {
        return totalTramites;
    }

    public void setTotalTramites(long totalTramites) {
        this.totalTramites = totalTramites;
    }

    public long getIniciados() {
        return iniciados;
    }

    public void setIniciados(long iniciados) {
        this.iniciados = iniciados;
    }

    public long getEnProceso() {
        return enProceso;
    }

    public void setEnProceso(long enProceso) {
        this.enProceso = enProceso;
    }

    public long getFinalizados() {
        return finalizados;
    }

    public void setFinalizados(long finalizados) {
        this.finalizados = finalizados;
    }

    public long getCancelados() {
        return cancelados;
    }

    public void setCancelados(long cancelados) {
        this.cancelados = cancelados;
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

    public String getTiempoPromedio() {
        return tiempoPromedio;
    }

    public void setTiempoPromedio(String tiempoPromedio) {
        this.tiempoPromedio = tiempoPromedio;
    }

    public String getTiempoPromedioActividad() {
        return tiempoPromedioActividad;
    }

    public void setTiempoPromedioActividad(String tiempoPromedioActividad) {
        this.tiempoPromedioActividad = tiempoPromedioActividad;
    }

    public String getActividadMayorDemora() {
        return actividadMayorDemora;
    }

    public void setActividadMayorDemora(String actividadMayorDemora) {
        this.actividadMayorDemora = actividadMayorDemora;
    }

    public String getResponsableMayorCarga() {
        return responsableMayorCarga;
    }

    public void setResponsableMayorCarga(String responsableMayorCarga) {
        this.responsableMayorCarga = responsableMayorCarga;
    }

    public String getCuelloDeBotellaPrincipal() {
        return cuelloDeBotellaPrincipal;
    }

    public void setCuelloDeBotellaPrincipal(String cuelloDeBotellaPrincipal) {
        this.cuelloDeBotellaPrincipal = cuelloDeBotellaPrincipal;
    }
}
