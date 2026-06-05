import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  WorkflowCollaborationEditingRequest,
  WorkflowCollaborationSessionRequest,
  WorkflowCollaborationState,
} from '../models/workflow-collaboration.model';

@Injectable({ providedIn: 'root' })
export class WorkflowCollaborationService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/workflow-designer`;

  getState(
    policyId: string,
    sessionId: string,
    baseRevision?: number,
  ): Observable<WorkflowCollaborationState> {
    let params = new HttpParams().set('sessionId', sessionId);
    if (baseRevision != null) {
      params = params.set('baseRevision', String(baseRevision));
    }
    return this.http.get<WorkflowCollaborationState>(
      `${this.api}/policy/${policyId}/collaboration`,
      { params },
    );
  }

  open(policyId: string, sessionId: string): Observable<WorkflowCollaborationState> {
    return this.http.post<WorkflowCollaborationState>(
      `${this.api}/policy/${policyId}/collaboration/open`,
      { sessionId } satisfies WorkflowCollaborationSessionRequest,
    );
  }

  heartbeat(
    policyId: string,
    sessionId: string,
    baseRevision?: number,
  ): Observable<WorkflowCollaborationState> {
    const body: WorkflowCollaborationSessionRequest = { sessionId };
    if (baseRevision != null) {
      body.baseRevision = baseRevision;
    }
    return this.http.post<WorkflowCollaborationState>(
      `${this.api}/policy/${policyId}/collaboration/heartbeat`,
      body,
    );
  }

  close(policyId: string, sessionId: string): Observable<void> {
    return this.http.post<void>(
      `${this.api}/policy/${policyId}/collaboration/close`,
      { sessionId } satisfies WorkflowCollaborationSessionRequest,
    );
  }

  reportConflict(
    policyId: string,
    sessionId: string,
    baseRevision: number,
  ): Observable<void> {
    return this.http.post<void>(
      `${this.api}/policy/${policyId}/collaboration/conflict`,
      { sessionId, baseRevision } satisfies WorkflowCollaborationSessionRequest,
    );
  }

  reportEditing(
    policyId: string,
    body: WorkflowCollaborationEditingRequest,
  ): Observable<WorkflowCollaborationState> {
    return this.http.post<WorkflowCollaborationState>(
      `${this.api}/policy/${policyId}/collaboration/editing`,
      body,
    );
  }

  clearEditing(
    policyId: string,
    sessionId: string,
    elementId: string,
  ): Observable<WorkflowCollaborationState> {
    const params = new HttpParams().set('sessionId', sessionId);
    return this.http.delete<WorkflowCollaborationState>(
      `${this.api}/policy/${policyId}/collaboration/editing/${encodeURIComponent(elementId)}`,
      { params },
    );
  }
}
