export const TRANSITION_TYPE_OPTIONS = [
  { value: 'SEQUENTIAL', label: 'Secuencial', enabled: true },
  { value: 'CONDITIONAL', label: 'Condicional', enabled: true },
  { value: 'ITERATIVE', label: 'Iterativa', enabled: false, hint: 'Disponible en fase posterior' },
  { value: 'PARALLEL_SPLIT', label: 'División paralela', enabled: false, hint: 'Disponible en fase posterior' },
  { value: 'PARALLEL_JOIN', label: 'Unión paralela', enabled: false, hint: 'Disponible en fase posterior' },
];

export function transitionTypeLabel(type?: string, fallback?: string): string {
  if (fallback) return fallback;
  const opt = TRANSITION_TYPE_OPTIONS.find((o) => o.value === (type ?? '').toUpperCase());
  return opt?.label ?? type ?? '—';
}

export function transitionStatusLabel(active?: boolean): string {
  return active === false ? 'Inactiva' : 'Activa';
}

export function transitionStatusClass(active?: boolean): string {
  return active === false ? 'inactive' : 'active';
}
