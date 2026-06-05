/** Nombre técnico de campo: letras minúsculas, números y guion bajo. */
export const TECHNICAL_NAME_PATTERN = /^[a-z][a-z0-9_]*$/;

export function slugFieldNameFromLabel(label: string): string {
  const base = label
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '');
  return base || 'campo';
}

export function normalizeTechnicalName(raw: string): string {
  return raw.trim().toLowerCase();
}

export function validateTechnicalName(name: string): string | null {
  const normalized = normalizeTechnicalName(name);
  if (!normalized) {
    return 'El nombre técnico es obligatorio';
  }
  if (normalized.includes(' ')) {
    return 'El nombre técnico no puede contener espacios';
  }
  if (!TECHNICAL_NAME_PATTERN.test(normalized)) {
    return 'Use solo letras minúsculas, números y guion bajo (ej.: valido)';
  }
  return null;
}

export function ensureUniqueTechnicalName(base: string, used: Set<string>): string {
  let candidate = base;
  let suffix = 1;
  while (used.has(candidate)) {
    suffix += 1;
    candidate = `${base}_${suffix}`;
  }
  used.add(candidate);
  return candidate;
}
