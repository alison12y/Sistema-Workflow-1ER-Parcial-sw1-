import { TraceItem, TramiteTask } from './tramite.model';

export interface MonitoringTaskCounts {
  pending: number;
  inProgress: number;
  completed: number;
  total: number;
}

export interface MonitoringResponsible {
  responsible: string;
  pendingCount: number;
  inProgressCount: number;
  completedCount: number;
}

export interface MonitoringItem {
  id: string;
  policyId?: string;
  code: string;
  policyName: string;
  status: string;
  currentActivity: string;
  responsible: string;
  timeElapsed: string;
  progress: number;
  workflowError?: string;
  pendingTaskCount?: number;
  inProgressTaskCount?: number;
  completedTaskCount?: number;
  updatedAt?: string;
}

export interface MonitoringTrace {
  id?: string;
  policyId?: string;
  code: string;
  policyName: string;
  status: string;
  currentActivity: string;
  responsible: string;
  progress: number;
  workflowError?: string;
  createdAt?: string;
  updatedAt?: string;
  taskCounts?: MonitoringTaskCounts;
  pendingTasks?: TramiteTask[];
  inProgressTasks?: TramiteTask[];
  completedTasks?: TramiteTask[];
  tasks: TramiteTask[];
  responsibles?: MonitoringResponsible[];
  events: TraceItem[];
}
