import { CompleteActivityPayload, FormSubmissionPayload, MyActivity } from '../../models/my-activities.model';

export type OfflineSyncOperationType =
  | 'TAKE_TASK'
  | 'FORM_DRAFT'
  | 'COMPLETE_ACTIVITY'
  | 'DOCUMENT_UPLOAD'
  | 'SMART_AGENT_DRAFT';

export type OfflineSyncStatus = 'pending' | 'syncing' | 'failed' | 'completed';

export interface PendingSyncQueueItem {
  id: string;
  type: OfflineSyncOperationType;
  status: OfflineSyncStatus;
  payload: Record<string, unknown>;
  fileBlobs?: OfflineFileBlob[];
  createdAt: string;
  updatedAt: string;
  lastError?: string;
  retryCount: number;
}

export interface OfflineFileBlob {
  fieldKey: string;
  fileName: string;
  contentType: string;
  blob: Blob;
}

export interface CachedActivitiesRecord {
  id: string;
  activities: MyActivity[];
  cachedAt: string;
}

export interface PendingFormRecord {
  id: string;
  tramiteId: string;
  taskOrder: number;
  payload: FormSubmissionPayload | CompleteActivityPayload;
  complete: boolean;
  savedAt: string;
}

export interface PendingDocumentRecord {
  id: string;
  repositoryId: string;
  tramiteId: string;
  fileName: string;
  contentType: string;
  blob: Blob;
  savedAt: string;
}

export const OFFLINE_DB_NAME = 'workflow_offline_db';
export const OFFLINE_DB_VERSION = 1;
export const STORE_PENDING_SYNC_QUEUE = 'pending_sync_queue';
export const STORE_PENDING_FORMS = 'pending_forms';
export const STORE_PENDING_TASKS = 'pending_tasks';
export const STORE_PENDING_DOCUMENTS = 'pending_documents';
export const STORE_CACHED_ACTIVITIES = 'cached_activities';
