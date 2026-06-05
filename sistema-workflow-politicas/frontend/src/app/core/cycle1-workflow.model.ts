/**
 * Referencia frontend del modelo oficial Ciclo 1 (F0).
 * Documentación completa: docs/ciclo1-modelo-workflow.md
 */

export const CYCLE1_CONSOLIDATION_VERSION = 'cycle1-f0';

/** APIs oficiales — diseño UML */
export const CYCLE1_OFFICIAL_API = {
  policies: '/api/policies',
  workflowActivities: '/api/workflow-activities',
  workflowTransitions: '/api/workflow-transitions',
  workflowDesigner: '/api/workflow-designer',
  forms: '/api/forms',
  formFields: '/api/form-fields',
  tramites: '/api/tramites',
  myActivities: '/api/my-activities',
  monitoring: '/api/monitoring',
} as const;

/** APIs deprecated — mantener hasta F1; no usar en features nuevas */
export const CYCLE1_DEPRECATED_API = {
  activityDiagrams: '/api/activity-diagrams',
  processInstances: '/api/process',
  taskInstances: '/api/tasks',
  workflowsLegacy: '/api/workflows',
  activitiesLegacy: '/api/activities',
  transitionsLegacy: '/api/transitions',
} as const;

/** Rutas UI oficiales */
export const CYCLE1_OFFICIAL_ROUTES = {
  workflowDesigner: '/workflow-designer',
  activityFormDesigner: '/activities/:activityId/form',
  myActivities: '/mis-actividades',
  tramites: '/tramites',
  monitoring: '/monitoring',
} as const;

/** Rutas UI deprecated */
export const CYCLE1_DEPRECATED_ROUTES = {
  /** @deprecated Redirige a workflow-designer — usar activityFormDesigner */
  formDesigner: '/form-designer',
} as const;

export const WORKFLOW_TRANSITION_TYPES = [
  'SEQUENTIAL',
  'CONDITIONAL',
  'ITERATIVE',
  'PARALLEL_SPLIT',
  'PARALLEL_JOIN',
] as const;

export type WorkflowTransitionTypeCode = (typeof WORKFLOW_TRANSITION_TYPES)[number];

export const WORKFLOW_RESPONSIBLE_TYPES = ['ROLE', 'DEPARTMENT', 'USER'] as const;

export type WorkflowResponsibleTypeCode = (typeof WORKFLOW_RESPONSIBLE_TYPES)[number];

/**
 * Contrato previsto F1: el funcionario no elige la siguiente actividad;
 * el backend resuelve según WorkflowTransition + datos del formulario.
 */
export interface Cycle1RoutingContractNote {
  readonly policyId: string;
  readonly currentWorkflowActivityId: string;
  readonly tramiteId: string;
  readonly stepData?: Record<string, unknown>;
}
