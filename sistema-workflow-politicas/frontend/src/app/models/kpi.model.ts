export interface KpiSummary {
  totalTramites: number;
  iniciados: number;
  enProceso: number;
  finalizados: number;
  cancelados: number;
  tiempoPromedio: string;
  actividadMayorDemora: string;
  responsableMayorCarga: string;
  cuelloDeBotellaPrincipal: string;
}

export interface KpiBottleneck {
  activityName: string;
  responsible: string;
  pendingCount: number;
  inProgressCount: number;
  averageTime: string;
  level: 'Alto' | 'Medio' | 'Bajo' | string;
  observation: string;
}
