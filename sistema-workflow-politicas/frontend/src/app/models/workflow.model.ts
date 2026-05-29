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
  responsibleType?: string;
  responsibleId?: string;
  responsibleName?: string;
  activityType?: string;
  orderIndex?: number;
  estimatedTimeHours?: number;
  status?: string;
  formId?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface WorkflowActivityRequest {
  policyId?: string;
  name: string;
  description?: string;
  responsibleType?: string;
  responsibleId?: string;
  responsibleName?: string;
  activityType?: string;
  status?: string;
  orderIndex?: number;
  estimatedTimeHours?: number;
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
