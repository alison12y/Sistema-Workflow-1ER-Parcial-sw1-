export interface KpiFilterParams {
  policyId?: string;
  status?: string;
  from?: string;
  to?: string;
}

export interface KpiSummary {
  totalTramites: number;
  iniciados: number;
  enProceso: number;
  finalizados: number;
  cancelados: number;
  tramitesActivos: number;
  tramitesConError: number;
  tareasPendientes: number;
  tareasEnProceso: number;
  tareasCompletadas: number;
  tiempoPromedio: string;
  tiempoPromedioActividad: string;
  actividadMayorDemora: string;
  responsableMayorCarga: string;
  cuelloDeBotellaPrincipal: string;
}

export interface KpiActivityMetric {
  workflowActivityId?: string;
  activityName: string;
  policyId?: string;
  policyName?: string;
  pendingCount: number;
  inProgressCount: number;
  completedCount: number;
  overdueCount: number;
  averageDuration: string;
  averageActiveWait: string;
}

export interface KpiLoadMetric {
  key: string;
  displayName: string;
  departmentName?: string;
  pendingCount: number;
  inProgressCount: number;
  completedCount: number;
  totalActive: number;
  averageHandlingTime: string;
}

export interface KpiBottleneck {
  workflowActivityId?: string;
  activityName: string;
  policyId?: string;
  policyName?: string;
  responsible: string;
  departmentName?: string;
  pendingCount: number;
  inProgressCount: number;
  overdueCount: number;
  averageTime: string;
  level: 'Alto' | 'Medio' | 'Bajo' | string;
  observation: string;
}

export interface KpiDashboard {
  summary: KpiSummary;
  tareasPendientes: number;
  tareasEnProceso: number;
  tareasCompletadas: number;
  tramitesActivos: number;
  tramitesConError: number;
  sufficientData: boolean;
  message?: string;
  generatedAt?: string;
  slowActivities: KpiActivityMetric[];
  employeeLoad: KpiLoadMetric[];
  departmentLoad: KpiLoadMetric[];
  bottlenecks: KpiBottleneck[];
}
