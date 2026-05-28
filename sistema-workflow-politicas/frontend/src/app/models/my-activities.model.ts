export interface MyActivity {
  tramiteId: string;
  policyId: string;
  taskOrder: number;
  code: string;
  policyName: string;
  activityName: string;
  status: string;
  responsible: string;
  priority: string;
  assignedAt?: string;
}

export interface ResponseItemPayload {
  fieldId?: string;
  fieldName: string;
  fieldLabel: string;
  fieldType: string;
  value?: string;
  fileName?: string;
  fileId?: string;
  contentType?: string;
  size?: number;
}

export interface FormSubmissionFileMeta {
  fileId: string;
  fileName: string;
  contentType?: string;
  size?: number;
}

export interface FormSubmissionPayload {
  tramiteId: string;
  policyId: string;
  activityName: string;
  taskOrder: number;
  responses: ResponseItemPayload[];
}

export interface FormSubmission {
  id?: string;
  tramiteId: string;
  policyId: string;
  activityName: string;
  taskOrder: number;
  submittedByName?: string;
  responses: ResponseItemPayload[];
  createdAt?: string;
  updatedAt?: string;
}

export interface CompleteActivityPayload {
  activityName: string;
  taskOrder: number;
  responses: ResponseItemPayload[];
}
