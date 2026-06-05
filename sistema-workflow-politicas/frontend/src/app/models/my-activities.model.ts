export interface MyActivity {
  tramiteId: string;
  policyId: string;
  taskOrder: number;
  code: string;
  policyName: string;
  activityName: string;
  workflowActivityId?: string;
  status: string;
  responsible: string;
  priority: string;
  tramiteStatus?: string;
  assignedAt?: string;
  takenAt?: string;
  completedAt?: string;
  takenBy?: string;
  inboxCategory?: 'NORMAL' | 'OBSERVADA' | 'ERROR';
  canTake?: boolean;
  canComplete?: boolean;
  workflowError?: string;
}

export interface MyActivitiesFilterParams {
  status?: string;
  policyId?: string;
  tramiteId?: string;
  tramiteCode?: string;
  priority?: string;
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
  workflowActivityId?: string;
  activityName: string;
  taskOrder: number;
  responses: ResponseItemPayload[];
}

export interface FormSubmission {
  id?: string;
  tramiteId: string;
  policyId: string;
  workflowActivityId?: string;
  activityName: string;
  taskOrder: number;
  submittedBy?: string;
  submittedByName?: string;
  submittedAt?: string;
  responses: ResponseItemPayload[];
  createdAt?: string;
  updatedAt?: string;
}

export interface CompleteActivityPayload {
  workflowActivityId?: string;
  activityName: string;
  taskOrder: number;
  responses: ResponseItemPayload[];
}
