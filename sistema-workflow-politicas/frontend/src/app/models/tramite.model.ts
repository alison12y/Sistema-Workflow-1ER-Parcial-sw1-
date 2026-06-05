export interface TraceItem {
  eventType?: string;
  eventLabel?: string;
  activityName?: string;
  responsible?: string;
  status?: string;
  userName?: string;
  userFullName?: string;
  previousStatus?: string;
  newStatus?: string;
  occurredAt?: string;
  startedAt?: string;
  completedAt?: string;
  comment?: string;
}

export interface TramiteTask {
  name: string;
  responsible: string;
  status: string;
  order: number;
  startedAt?: string;
  completedAt?: string;
  notes?: string;
}

export interface Tramite {
  id?: string;
  code: string;
  policyName: string;
  policyDescription?: string;
  description?: string;
  priority: string;
  requestedByName?: string;
  requesterName?: string;
  status: string;
  currentActivity: string;
  responsible: string;
  progress: number;
  workflowError?: string;
  createdAt?: string;
  updatedAt?: string;
  tasks?: TramiteTask[];
  trace: TraceItem[];
}

export interface TramiteCreateRequest {
  policyId: string;
  description: string;
  priority: string;
  requestedBy: string;
}

export interface TramiteAdvanceRequest {
  comment?: string;
}

export interface TramiteCancelRequest {
  comment?: string;
}
