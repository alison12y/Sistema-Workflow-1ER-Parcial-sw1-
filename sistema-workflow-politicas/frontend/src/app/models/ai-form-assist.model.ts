export interface AiFormFieldDefinition {
  name: string;
  label: string;
  type: string;
  required?: boolean;
  options?: string;
}

export interface AiFormAssistRequest {
  report: string;
  policyId?: string;
  tramiteId?: string;
  workflowActivityId?: string;
  formId?: string;
  activityName?: string;
  userId?: string;
  fields: AiFormFieldDefinition[];
  currentValues: Record<string, string>;
}

export interface AiFormFieldSuggestionDto {
  fieldName: string;
  fieldLabel: string;
  fieldType: string;
  suggestedValue?: string | null;
  confidence?: number;
  applicable?: boolean;
  message?: string;
}

export interface AiFormAssistResponse {
  aiAvailable?: boolean;
  fallbackUsed?: boolean;
  error?: string;
  explanation?: string;
  confidence?: number;
  suggestedText?: string;
  fieldSuggestions?: AiFormFieldSuggestionDto[];
  suggestedValues?: Record<string, string>;
  warnings?: string[];
  unmatchedFields?: string[];
}

export interface AiFormAssistTraceRequest {
  workflowActivityId: string;
  taskOrder: number;
  activityName?: string;
  fieldsSuggested: number;
  fieldsApplied: number;
}
