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
  transitionCount?: number;
  tramiteCount?: number;
}

export interface PolicyDetail extends PolicySummary {
  updatedAt?: string;
  activities?: WorkflowActivity[];
  transitions?: WorkflowTransition[];
  flowPreview?: string[];
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
  positionX?: number;
  positionY?: number;
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

export interface WorkflowTransition {
  id?: string;
  policyId?: string;
  fromActivityId?: string;
  fromActivityName?: string;
  toActivityId?: string;
  toActivityName?: string;
  transitionType?: string;
  transitionTypeLabel?: string;
  conditionLabel?: string;
  conditionExpression?: string;
  orderIndex?: number;
  active?: boolean;
  reactivated?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface WorkflowTransitionDedupeResponse {
  removed: number;
  kept: number;
  deactivatedCount?: number;
  message: string;
}

export interface WorkflowTransitionCleanupResponse {
  removedDuplicates: number;
  removedOrphans: number;
  kept: number;
  message: string;
  duplicateActivitiesDetected?: boolean;
  warning?: string;
}

export interface WorkflowDeleteResponse {
  message: string;
  logicalDelete: boolean;
  affectedConnections?: number;
}

export interface WorkflowTransitionRequest {
  policyId?: string;
  fromActivityId?: string;
  toActivityId?: string;
  transitionType?: string;
  conditionLabel?: string;
  conditionExpression?: string;
  orderIndex?: number;
  active?: boolean;
}

export interface WorkflowFlowValidationResponse {
  valid: boolean;
  message: string;
  warnings: string[];
  errors: string[];
}

export interface ActivityNode {
  id?: string;
  name: string;
  description?: string;
  responsibleName?: string;
  activityType?: string;
  activityTypeLabel?: string;
  status?: string;
  orderIndex?: number;
  estimatedTimeHours?: number;
  x?: number;
  y?: number;
  positionX?: number;
  positionY?: number;
  decisionNode?: boolean;
  outgoingConditionalCount?: number;
  incomingCount?: number;
  outgoingCount?: number;
}

export interface TransitionEdge {
  id?: string;
  fromActivityId?: string;
  fromActivityName?: string;
  toActivityId?: string;
  toActivityName?: string;
  transitionType?: string;
  transitionTypeLabel?: string;
  conditionLabel?: string;
  active?: boolean;
}

export interface WorkflowLane {
  laneName: string;
  responsibleType?: string;
  activities: ActivityNode[];
}

export interface WorkflowDesignerData {
  policyId?: string;
  policyName: string;
  policyDescription?: string;
  policyStatus?: string;
  activities: ActivityNode[];
  transitions: TransitionEdge[];
  lanes: WorkflowLane[];
  flowPreview: string[];
  flowValidation?: WorkflowFlowValidationResponse;
}
