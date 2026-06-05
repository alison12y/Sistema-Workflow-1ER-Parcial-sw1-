export interface WorkflowCollaborationConnectedUser {

  userId: string;

  username: string;

  fullName: string;

  openedAt?: string;

  lastSeenAt?: string;

  currentUser: boolean;

  sessionCount?: number;

}



export interface WorkflowCollaborationRecentAction {
  actionType?: string;
  actionLabel?: string;
  elementType?: 'ACTIVITY' | 'TRANSITION' | string;
  elementName?: string;
  modifiedByUserId?: string;
  modifiedByUsername?: string;
  modifiedByDisplayName?: string;
  modifiedByName?: string;
  modifiedAt?: string;
  summary?: string;
}

export interface WorkflowCollaborationActiveEdit {

  userId?: string;

  username: string;

  fullName: string;

  elementType: 'ACTIVITY' | 'TRANSITION' | string;

  elementId: string;

  elementName: string;

  action: 'SELECTING' | 'EDITING' | 'MOVING' | string;

  lastSeenAt?: string;

  currentUser: boolean;

}



/** @deprecated Lista agrupada; usar connectedUsers. */

export interface WorkflowCollaborationEditor {

  sessionId: string;

  username: string;

  displayName: string;

  currentUser: boolean;

  canEdit: boolean;

}



export interface WorkflowCollaborationState {

  policyId: string;

  revision: number;

  currentRevision?: number;

  lastModifiedAt?: string;

  lastModifiedByUserId?: string;

  lastModifiedByUsername?: string;

  lastModifiedByDisplayName?: string;

  lastModifiedByName?: string;

  lastModifiedActionType?: string;

  lastModifiedActionLabel?: string;

  lastModifiedElementType?: string;

  lastModifiedElementName?: string;

  lastModifiedSummary?: string;

  currentUsername?: string;

  staleForClient: boolean;

  activeSessionsCount?: number;

  connectedUsers: WorkflowCollaborationConnectedUser[];

  activeEdits?: WorkflowCollaborationActiveEdit[];

  recentActions?: WorkflowCollaborationRecentAction[];

  connectedEditors?: WorkflowCollaborationEditor[];

}



export interface WorkflowCollaborationSessionRequest {

  sessionId: string;

  baseRevision?: number;

}



export interface WorkflowCollaborationEditingRequest {

  sessionId: string;

  elementType: 'ACTIVITY' | 'TRANSITION';

  elementId: string;

  elementName: string;

  action: 'SELECTING' | 'EDITING' | 'MOVING';

}


