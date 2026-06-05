const TRACE_EVENT_LABELS: Record<string, string> = {
  PROCESO_CREADO: 'Trámite creado',
  TRAMITE_CREADO: 'Trámite creado',
  TRAMITE_INICIADO: 'Trámite iniciado',
  TAREA_ASIGNADA: 'Tarea asignada',
  TAREA_TOMADA: 'Tarea tomada',
  FORMULARIO_ENVIADO: 'Formulario enviado',
  IA_FORMULARIO_ASISTIDO: 'Asistencia IA en formulario',
  ACTIVIDAD_COMPLETADA: 'Actividad completada',
  TAREA_COMPLETADA: 'Tarea completada',
  PROCESO_AVANZADO: 'Proceso avanzado',
  ESPERA_PARALELO: 'Espera paralelo',
  TRAMITE_FINALIZADO: 'Trámite finalizado',
  TRAMITE_CANCELADO: 'Trámite cancelado',
  ERROR_WORKFLOW: 'Error de workflow',
  WORKFLOW_ABIERTO: 'Diseñador UML abierto',
  WORKFLOW_MODIFICADO: 'Workflow UML modificado',
  CONFLICTO_EDICION: 'Conflicto de edición en diseñador',
};

export function traceEventLabel(eventType?: string, eventLabel?: string): string {
  if (eventLabel?.trim()) {
    return eventLabel.trim();
  }
  if (!eventType) {
    return 'Evento';
  }
  return TRACE_EVENT_LABELS[eventType] ?? eventType;
}

export function traceEventCssClass(eventType?: string): string {
  const type = (eventType ?? '').toUpperCase();
  if (type === 'ERROR_WORKFLOW') return 'trace-event trace-error';
  if (type === 'ESPERA_PARALELO') return 'trace-event trace-wait';
  if (
    type.startsWith('TAREA_') ||
    type === 'ACTIVIDAD_COMPLETADA' ||
    type === 'FORMULARIO_ENVIADO' ||
    type === 'IA_FORMULARIO_ASISTIDO'
  ) {
    return 'trace-event trace-task';
  }
  if (type === 'TRAMITE_FINALIZADO' || type === 'PROCESO_AVANZADO') return 'trace-event trace-advance';
  if (type === 'TRAMITE_CANCELADO') return 'trace-event trace-cancel';
  return 'trace-event trace-default';
}

export function taskStatusCssClass(status?: string): string {
  const normalized = (status ?? '').toUpperCase();
  if (normalized === 'PENDIENTE') return 'task-pill task-pending';
  if (normalized === 'EN_CURSO') return 'task-pill task-progress';
  if (normalized === 'COMPLETADA') return 'task-pill task-done';
  return 'task-pill';
}

export function tramiteHasWorkflowError(workflowError?: string | null): boolean {
  return !!workflowError?.trim();
}
