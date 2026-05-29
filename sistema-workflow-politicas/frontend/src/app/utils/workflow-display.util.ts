const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  START: 'Inicio',
  TASK: 'Tarea',
  DECISION: 'Decisión',
  END: 'Fin',
};

const ACTIVITY_STATUS_LABELS: Record<string, string> = {
  BORRADOR: 'Borrador',
  ACTIVA: 'Activa',
  INACTIVA: 'Inactiva',
};

const RESPONSIBLE_TYPE_LABELS: Record<string, string> = {
  ROLE: 'Rol',
  DEPARTMENT: 'Departamento',
  USER: 'Usuario',
};

export function activityTypeLabel(type?: string): string {
  if (!type) return '—';
  return ACTIVITY_TYPE_LABELS[type.toUpperCase()] ?? type;
}

export function activityStatusLabel(status?: string): string {
  if (!status) return '—';
  return ACTIVITY_STATUS_LABELS[status.toUpperCase()] ?? status;
}

export function responsibleTypeLabel(type?: string): string {
  if (!type) return '—';
  return RESPONSIBLE_TYPE_LABELS[type.toUpperCase()] ?? type;
}

export function activityStatusClass(status?: string): string {
  const s = (status ?? '').toUpperCase();
  if (s === 'ACTIVA') return 'active';
  if (s === 'INACTIVA') return 'inactive';
  return 'draft';
}

export const RESPONSIBLE_OPTIONS = [
  'Atención al cliente',
  'Técnico',
  'Legal',
  'Supervisor',
  'Dueño de proceso',
  'Funcionario',
];

export const ACTIVITY_TYPE_OPTIONS = [
  { value: 'START', label: 'Inicio' },
  { value: 'TASK', label: 'Tarea' },
  { value: 'DECISION', label: 'Decisión' },
  { value: 'END', label: 'Fin' },
];

export const ACTIVITY_STATUS_OPTIONS = [
  { value: 'BORRADOR', label: 'Borrador' },
  { value: 'ACTIVA', label: 'Activa' },
  { value: 'INACTIVA', label: 'Inactiva' },
];

export const RESPONSIBLE_TYPE_OPTIONS = [
  { value: 'ROLE', label: 'Rol' },
  { value: 'DEPARTMENT', label: 'Departamento' },
  { value: 'USER', label: 'Usuario' },
];
