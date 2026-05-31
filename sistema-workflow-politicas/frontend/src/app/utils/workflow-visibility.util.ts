import { WorkflowActivity, WorkflowTransition } from '../models/workflow.model';

/** Actividad visible en listas que solo muestran elementos activos. */
export function isVisibleActivity(activity: WorkflowActivity): boolean {
  if (activity.active === false) return false;
  const status = (activity.status ?? '').toUpperCase();
  return status !== 'INACTIVA';
}

/** Conexión visible en listas que solo muestran elementos activos. */
export function isVisibleTransition(transition: WorkflowTransition): boolean {
  return transition.active !== false;
}
