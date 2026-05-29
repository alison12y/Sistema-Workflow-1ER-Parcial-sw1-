export interface DashboardStats {
  politicasActivas: number;
  politicasBorrador: number;
  tramitesEnProceso: number;
  tareasPendientes: number;
  tareasFinalizadas: number;
  tramitesObservados: number;
  posiblesCuellosDeBotella: number;
}

export interface PolicySummary {
  id?: string;
  name: string;
  description?: string;
  type?: string;
  status?: string;
  version?: string;
  responsible?: string;
  createdBy?: string;
  createdAt?: string;
  activityCount?: number;
  tramiteCount?: number;
}

export interface PolicyDetail extends PolicySummary {
  updatedAt?: string;
  activities?: WorkflowActivity[];
  tramites?: TramiteSummary[];
}

export interface WorkflowActivity {
  id?: string;
  name: string;
  description?: string;
  policyId?: string;
  responsible?: string;
  responsibleType?: string;
  activityType?: string;
  order?: number;
  estimatedMinutes?: number;
  status?: string;
  formId?: string;
}

export interface TramiteSummary {
  id?: string;
  code?: string;
  policyName?: string;
  description?: string;
  requesterName?: string;
  status?: string;
  currentActivity?: string;
  responsible?: string;
  createdAt?: string;
  updatedAt?: string;
}
