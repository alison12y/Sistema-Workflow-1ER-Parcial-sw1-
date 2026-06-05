export interface TransitionTypeOption {
  value: string;
  label: string;
  enabled: boolean;
  hint?: string;
}

export const TRANSITION_TYPE_OPTIONS: TransitionTypeOption[] = [
  {
    value: 'SEQUENTIAL',
    label: 'Secuencial',
    enabled: true,
    hint: 'Pasa directamente a la siguiente actividad.',
  },
  {
    value: 'CONDITIONAL',
    label: 'Condicional',
    enabled: true,
    hint: 'Depende de una respuesta (ej. Aprobado, Observado, Rechazado).',
  },
  {
    value: 'ITERATIVE',
    label: 'Iterativa',
    enabled: true,
    hint: 'Vuelve a una actividad anterior; indique una etiqueta de ciclo (recomendado).',
  },
  {
    value: 'PARALLEL_SPLIT',
    label: 'División paralela',
    enabled: true,
    hint: 'Abre varias ramas desde la misma actividad (al menos dos salidas PARALLEL_SPLIT).',
  },
  {
    value: 'PARALLEL_JOIN',
    label: 'Unión paralela',
    enabled: true,
    hint: 'Une ramas hacia una actividad (al menos dos entradas PARALLEL_JOIN).',
  },
];

export function transitionTypeLabel(type?: string, fallback?: string): string {
  if (fallback) return fallback;
  const opt = TRANSITION_TYPE_OPTIONS.find((o) => o.value === (type ?? '').toUpperCase());
  return opt?.label ?? type ?? '—';
}

import { suggestParallelTransitionType } from './workflow-gateway.util';

export function transitionTypeHint(type?: string): string {
  const opt = TRANSITION_TYPE_OPTIONS.find((o) => o.value === (type ?? '').toUpperCase());
  return opt?.hint ?? '';
}

export function transitionShowsConditionField(type?: string): boolean {
  const normalized = (type ?? '').toUpperCase();
  return normalized === 'CONDITIONAL' || normalized === 'ITERATIVE';
}

export function transitionConditionRequired(type?: string): boolean {
  return (type ?? '').toUpperCase() === 'CONDITIONAL';
}

export function transitionWizardSkipsConditionStep(type?: string): boolean {
  return !transitionShowsConditionField(type);
}

export interface TransitionFormValidation {
  error?: string;
  warning?: string;
}

export interface TransitionFormLike {
  transitionType?: string;
  conditionLabel?: string;
  fromActivityId?: string;
  toActivityId?: string;
}

export interface TransitionLike {
  id?: string;
  fromActivityId?: string;
  toActivityId?: string;
  transitionType?: string;
  active?: boolean;
}

export function validateTransitionFormInput(
  form: TransitionFormLike,
  existingTransitions: TransitionLike[],
  editingId?: string | null,
): TransitionFormValidation {
  const type = (form.transitionType ?? 'SEQUENTIAL').toUpperCase();

  if (type === 'CONDITIONAL' && !form.conditionLabel?.trim()) {
    return { error: 'Debe indicar una condición para conexiones condicionales.' };
  }

  if (type === 'ITERATIVE' && !form.conditionLabel?.trim()) {
    return {
      warning:
        'Se recomienda indicar una etiqueta para la conexión iterativa (ej. Reintentar, Corregir).',
    };
  }

  const active = existingTransitions.filter(
    (t) => t.active !== false && (!editingId || t.id !== editingId),
  );

  if (type === 'PARALLEL_SPLIT' && form.fromActivityId) {
    const splitsFrom = active.filter(
      (t) =>
        t.fromActivityId === form.fromActivityId &&
        (t.transitionType ?? '').toUpperCase() === 'PARALLEL_SPLIT',
    );
    if (splitsFrom.length + 1 < 2) {
      return {
        warning:
          'La división paralela requiere al menos dos salidas desde la misma actividad origen.',
      };
    }
  }

  if (type === 'PARALLEL_JOIN' && form.toActivityId) {
    const joinsTo = active.filter(
      (t) =>
        t.toActivityId === form.toActivityId &&
        (t.transitionType ?? '').toUpperCase() === 'PARALLEL_JOIN',
    );
    if (joinsTo.length + 1 < 2) {
      return {
        warning:
          'La unión paralela requiere al menos dos entradas hacia la misma actividad destino.',
      };
    }
  }

  return {};
}

/** Aplica PARALLEL_SPLIT / PARALLEL_JOIN si origen/destino es pasarela Fork/Join. */
export function applyGatewayTransitionTypeHint(
  form: TransitionFormLike,
  fromActivityType?: string | null,
  toActivityType?: string | null,
): void {
  const suggested = suggestParallelTransitionType(fromActivityType, toActivityType);
  if (suggested) {
    form.transitionType = suggested;
  }
}

export function edgeCanvasLabel(
  transitionType?: string,
  conditionLabel?: string,
  transitionTypeLabel?: string,
): string {
  if (conditionLabel?.trim()) {
    return conditionLabel.trim();
  }
  const type = (transitionType ?? '').toUpperCase();
  if (type === 'PARALLEL_SPLIT') return '÷';
  if (type === 'PARALLEL_JOIN') return '⨝';
  if (type === 'CONDITIONAL') {
    return transitionTypeLabel ?? 'Condicional';
  }
  return '';
}

export function transitionStatusLabel(active?: boolean): string {
  return active === false ? 'Inactiva' : 'Activa';
}

export function transitionStatusClass(active?: boolean): string {
  return active === false ? 'inactive' : 'active';
}
