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
