export type FieldType =
  | 'TEXT'
  | 'TEXTAREA'
  | 'NUMBER'
  | 'DATE'
  | 'SELECT'
  | 'CHECKBOX'
  | 'FILE'
  | 'OBSERVATION';

export interface DynamicForm {
  id?: string;
  activityId?: string;
  activityName?: string;
  policyId?: string;
  name?: string;
  description?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
  fields?: FormField[];
}

export interface DynamicFormRequest {
  activityId: string;
  policyId?: string;
  name: string;
  description?: string;
  active?: boolean;
}

export interface FormField {
  id?: string;
  formId?: string;
  name?: string;
  label: string;
  fieldType: FieldType | string;
  fieldTypeLabel?: string;
  required?: boolean;
  options?: string;
  orderIndex?: number;
  placeholder?: string;
  helpText?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface FormFieldRequest {
  formId: string;
  label: string;
  /** Nombre técnico / variable para stepData y condiciones del workflow. */
  name?: string;
  fieldType: FieldType | string;
  required?: boolean;
  options?: string;
  orderIndex?: number;
  placeholder?: string;
  helpText?: string;
  active?: boolean;
}

export interface WorkflowDeleteResponse {
  message?: string;
  logicalDelete?: boolean;
  affectedConnections?: number;
}

export interface FieldTypeOption {
  value: FieldType;
  label: string;
}

export const FIELD_TYPE_OPTIONS: FieldTypeOption[] = [
  { value: 'TEXT', label: 'Texto corto' },
  { value: 'TEXTAREA', label: 'Texto largo' },
  { value: 'NUMBER', label: 'Número' },
  { value: 'DATE', label: 'Fecha' },
  { value: 'SELECT', label: 'Lista desplegable' },
  { value: 'CHECKBOX', label: 'Checkbox' },
  { value: 'FILE', label: 'Archivo' },
  { value: 'OBSERVATION', label: 'Observación' },
];

// Compatibilidad diseño anterior
export interface FormDesignerField {
  type: string;
  label: string;
  name?: string;
  required: boolean;
  options?: string;
}

export interface DynamicFormSaveRequest {
  policyId: string;
  activityName: string;
  name: string;
  fields: FormDesignerFieldPayload[];
}

export interface FormDesignerFieldPayload {
  label: string;
  name: string;
  type: string;
  required: boolean;
  options?: string;
  order: number;
}

export interface DynamicFormDetail {
  id?: string;
  policyId?: string;
  activityName?: string;
  name?: string;
  fields: FormDesignerFieldPayload[];
}

export function fieldTypeLabel(type?: string): string {
  const normalized = (type ?? 'TEXT').toUpperCase();
  return FIELD_TYPE_OPTIONS.find((opt) => opt.value === normalized)?.label ?? type ?? '—';
}

export function isVisibleFormField(field: FormField): boolean {
  return field.active !== false;
}

export function parseSelectOptions(options?: string): string[] {
  if (!options?.trim()) {
    return ['Seleccione una opción'];
  }
  return options
    .split(',')
    .map((opt) => opt.trim())
    .filter(Boolean);
}
