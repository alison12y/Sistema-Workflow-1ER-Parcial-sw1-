import { DocumentRecord } from './document-repository.model';

export interface DocumentCollaborationSessionRequest {
  sessionId: string;
}

export interface DocumentCollaborationLockRequest {
  sessionId: string;
  documentFamilyId: string;
  documentId: string;
  documentName: string;
}

export interface DocumentCollaborationConnectedUser {
  userId?: string;
  username: string;
  displayName: string;
  sessionCount: number;
  viewingDocumentFamilyId?: string;
  viewingDocumentName?: string;
}

export interface DocumentCollaborationActiveLock {
  documentFamilyId: string;
  documentId: string;
  documentName: string;
  userId?: string;
  username: string;
  displayName: string;
  lockedAt?: string;
}

export interface DocumentCollaborationRecentAction {
  actionType: string;
  actionLabel: string;
  documentId?: string;
  documentName?: string;
  username?: string;
  displayName?: string;
  occurredAt?: string;
}

export interface DocumentCollaborationState {
  repositoryId: string;
  tramiteId: string;
  currentUsername: string;
  connectedUsers: DocumentCollaborationConnectedUser[];
  activeLocks: DocumentCollaborationActiveLock[];
  recentActions?: DocumentCollaborationRecentAction[];
}

export interface DocumentAccessInfo {
  documentId: string;
  documentFamilyId: string;
  permissionLevel: 'READ' | 'EDIT' | 'ADMIN' | string;
  locked: boolean;
  lockedByUsername?: string;
  lockedByDisplayName?: string;
  lockHeldByCurrentUser: boolean;
  canRead: boolean;
  canEdit: boolean;
  canAdmin: boolean;
  canUploadVersion: boolean;
}

export interface DocumentEditorSession {
  document: DocumentRecord;
  access: DocumentAccessInfo;
  collaboration: DocumentCollaborationState;
  recentActions: DocumentCollaborationRecentAction[];
  onlyOfficeEnabled: boolean;
  fallbackMode: boolean;
  readOnly: boolean;
  onlyOfficeApiScriptUrl?: string;
  onlyOfficeConfig?: Record<string, unknown>;
}

export interface DocumentPermissionEntry {
  id: string;
  documentFamilyId: string;
  granteeType: 'USER' | 'ROLE' | 'DEPARTMENT' | string;
  granteeKey: string;
  granteeLabel?: string;
  permissionLevel: 'READ' | 'EDIT' | 'ADMIN' | string;
  grantedBy?: string;
  grantedAt?: string;
}

export interface DocumentPermissionRequest {
  granteeType: string;
  granteeKey: string;
  granteeLabel?: string;
  permissionLevel: string;
}

export const DOCUMENT_COLLABORATION_POLL_MS = 12_000;

export const ONLYOFFICE_EDITABLE_EXTENSIONS = new Set(['docx', 'xlsx']);

export function isOnlyOfficeEditableDocument(extension?: string): boolean {
  if (!extension) return false;
  return ONLYOFFICE_EDITABLE_EXTENSIONS.has(extension.toLowerCase());
}
