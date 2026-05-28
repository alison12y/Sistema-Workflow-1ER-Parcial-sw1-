import { TraceItem, TramiteTask } from './tramite.model';

export interface MonitoringItem {
  id: string;
  code: string;
  policyName: string;
  status: string;
  currentActivity: string;
  responsible: string;
  timeElapsed: string;
  progress: number;
}

export interface MonitoringTrace {
  code: string;
  policyName: string;
  status: string;
  currentActivity: string;
  responsible: string;
  progress: number;
  tasks: TramiteTask[];
  events: TraceItem[];
}
