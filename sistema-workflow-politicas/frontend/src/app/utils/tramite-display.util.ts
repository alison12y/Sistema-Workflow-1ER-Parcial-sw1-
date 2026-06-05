export type TramiteStatus = 'INICIADO' | 'EN_PROCESO' | 'COMPLETADO' | 'CANCELADO';
export type TramitePriority = 'BAJA' | 'NORMAL' | 'ALTA' | 'URGENTE';

const STATUS_LABELS: Record<string, string> = {
  INICIADO: 'Iniciado',
  EN_PROCESO: 'En proceso',
  COMPLETADO: 'Finalizado',
  CANCELADO: 'Cancelado',
};

const PRIORITY_LABELS: Record<string, string> = {
  BAJA: 'Baja',
  NORMAL: 'Normal',
  ALTA: 'Alta',
  URGENTE: 'Urgente',
};

const TASK_STATUS_LABELS: Record<string, string> = {
  PENDIENTE: 'Pendiente',
  EN_CURSO: 'En curso',
  COMPLETADA: 'Completada',
};

export function tramiteStatusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status;
}

export function tramiteStatusClass(status: string): string {
  if (status === 'COMPLETADO') return 'active';
  if (status === 'EN_PROCESO') return 'versioned';
  if (status === 'INICIADO') return 'draft';
  if (status === 'CANCELADO') return 'inactive';
  return 'inactive';
}

export function tramitePriorityLabel(priority?: string | null): string {
  if (!priority) return 'Normal';
  return PRIORITY_LABELS[priority.toUpperCase()] ?? priority;
}

/** Clases dedicadas para prioridad (visibles en tabla y detalle). */
export function tramitePriorityClass(priority?: string | null): string {
  const p = (priority ?? 'NORMAL').toUpperCase();
  if (p === 'URGENTE') return 'priority-pill priority-urgente';
  if (p === 'ALTA') return 'priority-pill priority-alta';
  if (p === 'BAJA') return 'priority-pill priority-baja';
  return 'priority-pill priority-normal';
}

export function tramiteTaskStatusLabel(status: string): string {
  return TASK_STATUS_LABELS[status] ?? status;
}

export function tramiteRequesterName(t: {
  requesterName?: string;
  requestedByName?: string;
}): string {
  const name = t.requesterName?.trim() || t.requestedByName?.trim();
  return name || '—';
}

export function tramiteDescription(t: { description?: string }): string {
  const d = t.description?.trim();
  return d || '—';
}

export function traceUserName(item: {
  userFullName?: string;
  userName?: string;
}): string {
  return item.userFullName?.trim() || item.userName?.trim() || '—';
}

export function isDemoUsername(username: string): boolean {
  const u = username.trim().toLowerCase();
  return u === 'test' || u === 'demo' || /^testtram\d*$/i.test(u);
}

export function httpErrorMessage(err: unknown, fallback: string): string {
  const e = err as { status?: number; error?: { message?: string; details?: string } };
  if (e?.status === 401) {
    return 'Su sesión expiró. Inicie sesión nuevamente';
  }
  const details = e?.error?.details?.trim();
  const message = e?.error?.message?.trim();
  if (details && message && details !== message) {
    return `${message} ${details}`;
  }
  return details || message || fallback;
}
