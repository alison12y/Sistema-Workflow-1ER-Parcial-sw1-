import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  DocumentCollaborationSessionRequest,
  DocumentCollaborationState,
  DocumentEditorSession,
} from '../models/document-collaboration.model';

@Injectable({ providedIn: 'root' })
export class DocumentEditorService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/document-repositories`;

  getEditorSession(
    documentId: string,
    repositoryId: string,
    sessionId: string,
  ): Observable<DocumentEditorSession> {
    const params = new HttpParams()
      .set('repositoryId', repositoryId)
      .set('sessionId', sessionId);
    return this.http.get<DocumentEditorSession>(
      `${this.api}/documents/${encodeURIComponent(documentId)}/editor-session`,
      { params },
    );
  }

  startEdit(
    documentId: string,
    repositoryId: string,
    sessionId: string,
  ): Observable<DocumentCollaborationState> {
    const params = new HttpParams().set('repositoryId', repositoryId);
    return this.http.post<DocumentCollaborationState>(
      `${this.api}/documents/${encodeURIComponent(documentId)}/edit/start`,
      { sessionId } satisfies DocumentCollaborationSessionRequest,
      { params },
    );
  }

  closeEdit(
    documentId: string,
    repositoryId: string,
    sessionId: string,
  ): Observable<DocumentCollaborationState> {
    const params = new HttpParams().set('repositoryId', repositoryId);
    return this.http.post<DocumentCollaborationState>(
      `${this.api}/documents/${encodeURIComponent(documentId)}/edit/close`,
      { sessionId } satisfies DocumentCollaborationSessionRequest,
      { params },
    );
  }
}
