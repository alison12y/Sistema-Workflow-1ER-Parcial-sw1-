package com.workflow.politicas.dto;

public class DashboardStatsResponse {
    private long politicasActivas;
    private long politicasBorrador;
    private long tramitesEnProceso;
    private long tareasPendientes;
    private long tareasFinalizadas;
    private long tramitesObservados;
    private long posiblesCuellosDeBotella;

    public long getPoliticasActivas() { return politicasActivas; }
    public void setPoliticasActivas(long politicasActivas) { this.politicasActivas = politicasActivas; }

    public long getPoliticasBorrador() { return politicasBorrador; }
    public void setPoliticasBorrador(long politicasBorrador) { this.politicasBorrador = politicasBorrador; }

    public long getTramitesEnProceso() { return tramitesEnProceso; }
    public void setTramitesEnProceso(long tramitesEnProceso) { this.tramitesEnProceso = tramitesEnProceso; }

    public long getTareasPendientes() { return tareasPendientes; }
    public void setTareasPendientes(long tareasPendientes) { this.tareasPendientes = tareasPendientes; }

    public long getTareasFinalizadas() { return tareasFinalizadas; }
    public void setTareasFinalizadas(long tareasFinalizadas) { this.tareasFinalizadas = tareasFinalizadas; }

    public long getTramitesObservados() { return tramitesObservados; }
    public void setTramitesObservados(long tramitesObservados) { this.tramitesObservados = tramitesObservados; }

    public long getPosiblesCuellosDeBotella() { return posiblesCuellosDeBotella; }
    public void setPosiblesCuellosDeBotella(long posiblesCuellosDeBotella) {
        this.posiblesCuellosDeBotella = posiblesCuellosDeBotella;
    }
}
