import { MyActivity } from '../models/my-activities.model';
import { TaskAssistantResponse } from '../models/task-assistant.model';
import { tramiteTaskStatusLabel } from './tramite-display.util';

export function buildTaskAssistantFallback(activity: MyActivity): TaskAssistantResponse {
  const assignedTo = activity.takenBy?.trim() || activity.responsible?.trim() || 'Sin asignar';
  return {
    summary:
      'El funcionario debe revisar la actividad actual del trámite y continuar con el flujo correspondiente.',
    importantData: [
      `Trámite: ${activity.code || activity.policyName || 'Trámite'}`,
      `Actividad actual: ${activity.activityName || 'Actividad'}`,
      `Estado de la tarea: ${tramiteTaskStatusLabel(activity.status)}`,
      `Responsable asignado: ${assignedTo}`,
    ],
    missingData: [
      'Verificar si el formulario fue completado',
      'Verificar si existen documentos requeridos',
    ],
    recommendedAction:
      'Revisar la información disponible y completar la tarea si los datos son correctos. ' +
      'Si falta información, registrar una observación antes de continuar.',
    source: 'LOCAL_FALLBACK',
  };
}
