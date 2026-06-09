import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  DocumentAccessInfo,
  DocumentCollaborationLockRequest,
  DocumentCollaborationSessionRequest,
  DocumentCollaborationState,
  DocumentPermissionEntry,
  DocumentPermissionRequest,
} from '../models/document-collaboration.model';

@Injectable({ providedIn: 'root' })
export class DocumentCollaborationService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/document-repositories`;

  getState(repositoryId: string, sessionId: string): Observable<DocumentCollaborationState> {
    const params = new HttpParams().set('sessionId', sessionId);
    return this.http.get<DocumentCollaborationState>(
      `${this.api}/${encodeURIComponent(repositoryId)}/collaboration`,
      { params },
    );
  }

  open(repositoryId: string, sessionId: string): Observable<DocumentCollaborationState> {
    return this.http.post<DocumentCollaborationState>(
      `${this.api}/${encodeURIComponent(repositoryId)}/collaboration/open`,
      { sessionId } satisfies DocumentCollaborationSessionRequest,
    );
  }

  heartbeat(repositoryId: string, sessionId: string): Observable<DocumentCollaborationState> {
    return this.http.post<DocumentCollaborationState>(
      `${this.api}/${encodeURIComponent(repositoryId)}/collaboration/heartbeat`,
      { sessionId } satisfies DocumentCollaborationSessionRequest,
    );
  }

  close(repositoryId: string, sessionId: string): Observable<void> {
    return this.http.post<void>(
      `${this.api}/${encodeURIComponent(repositoryId)}/collaboration/close`,
      { sessionId } satisfies DocumentCollaborationSessionRequest,
    );
  }

  acquireLock(
    repositoryId: string,
    body: DocumentCollaborationLockRequest,
  ): Observable<DocumentCollaborationState> {
    return this.http.post<DocumentCollaborationState>(
      `${this.api}/${encodeURIComponent(repositoryId)}/collaboration/lock`,
      body,
    );
  }

  releaseLock(
    repositoryId: string,
    documentFamilyId: string,
    sessionId: string,
    force = false,
  ): Observable<DocumentCollaborationState> {
    let params = new HttpParams().set('sessionId', sessionId);
    if (force) params = params.set('force', 'true');
    return this.http.delete<DocumentCollaborationState>(
      `${this.api}/${encodeURIComponent(repositoryId)}/collaboration/lock/${encodeURIComponent(documentFamilyId)}`,
      { params },
    );
  }

  getDocumentAccess(documentId: string): Observable<DocumentAccessInfo> {
    return this.http.get<DocumentAccessInfo>(
      `${this.api}/documents/${encodeURIComponent(documentId)}/access`,
    );
  }

  listPermissions(documentId: string): Observable<DocumentPermissionEntry[]> {
    return this.http.get<DocumentPermissionEntry[]>(
      `${this.api}/documents/${encodeURIComponent(documentId)}/permissions`,
    );
  }

  grantPermission(
    documentId: string,
    body: DocumentPermissionRequest,
  ): Observable<DocumentPermissionEntry> {
    return this.http.post<DocumentPermissionEntry>(
      `${this.api}/documents/${encodeURIComponent(documentId)}/permissions`,
      body,
    );
  }

  removePermission(documentId: string, body: DocumentPermissionRequest): Observable<void> {
    return this.http.delete<void>(
      `${this.api}/documents/${encodeURIComponent(documentId)}/permissions`,
      { body },
    );
  }
}
