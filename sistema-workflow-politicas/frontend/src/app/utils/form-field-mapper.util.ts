import { FormField, FormDesignerFieldPayload } from '../models/form.model';
import { slugFieldNameFromLabel } from './form-field-key.util';

export function mapDynamicFieldsToExecution(fields: FormField[]): FormDesignerFieldPayload[] {
  return (fields ?? [])
    .filter((f) => f.active !== false)
    .sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0))
    .map((field, index) => ({
      label: field.label,
      name: field.name?.trim() || slugFieldNameFromLabel(field.label),
      type: normalizeExecutionType(field.fieldType),
      required: !!field.required,
      options: field.options,
      order: field.orderIndex ?? index + 1,
    }));
}

function normalizeExecutionType(fieldType?: string): string {
  const normalized = (fieldType ?? 'TEXT').toUpperCase();
  if (normalized === 'FILE') return 'file';
  if (normalized === 'CHECKBOX') return 'checkbox';
  if (normalized === 'SELECT') return 'select';
  if (normalized === 'RADIO') return 'radio';
  if (normalized === 'TEXTAREA') return 'textarea';
  if (normalized === 'NUMBER') return 'number';
  if (normalized === 'DATE') return 'date';
  if (normalized === 'OBSERVATION') return 'textarea';
  return normalized.toLowerCase();
}

export const NO_FORM_OBSERVATION_FIELD: FormDesignerFieldPayload = {
  label: 'Observación de cierre',
  name: 'observacion_cierre',
  type: 'textarea',
  required: true,
  order: 1,
};
