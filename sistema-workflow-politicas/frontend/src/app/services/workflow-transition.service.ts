import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  WorkflowFlowValidationResponse,
  WorkflowTransition,
  WorkflowTransitionCleanupResponse,
  WorkflowTransitionDedupeResponse,
  WorkflowDeleteResponse,
  WorkflowTransitionRequest,
} from '../models/workflow.model';

@Injectable({ providedIn: 'root' })
export class WorkflowTransitionService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/workflow-transitions`;

  getByPolicy(policyId: string): Observable<WorkflowTransition[]> {
    return this.http.get<WorkflowTransition[]>(`${this.api}/policy/${policyId}`, {
      params: { _: Date.now().toString() },
    });
  }

  getById(id: string): Observable<WorkflowTransition> {
    return this.http.get<WorkflowTransition>(`${this.api}/${id}`);
  }

  create(request: WorkflowTransitionRequest): Observable<WorkflowTransition> {
    return this.http.post<WorkflowTransition>(this.api, request);
  }

  update(id: string, request: WorkflowTransitionRequest): Observable<WorkflowTransition> {
    return this.http.put<WorkflowTransition>(`${this.api}/${id}`, request);
  }

  delete(id: string): Observable<WorkflowDeleteResponse> {
    return this.http.delete<WorkflowDeleteResponse>(`${this.api}/${id}`);
  }

  activate(id: string): Observable<WorkflowTransition> {
    return this.http.patch<WorkflowTransition>(`${this.api}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<WorkflowTransition> {
    return this.http.patch<WorkflowTransition>(`${this.api}/${id}/deactivate`, {});
  }

  validatePolicyFlow(policyId: string): Observable<WorkflowFlowValidationResponse> {
    return this.http.get<WorkflowFlowValidationResponse>(`${this.api}/policy/${policyId}/validate`);
  }

  deduplicateByPolicy(policyId: string): Observable<WorkflowTransitionDedupeResponse> {
    return this.http.post<WorkflowTransitionDedupeResponse>(
      `${this.api}/policy/${policyId}/deduplicate`,
      {},
    );
  }

  cleanupByPolicy(policyId: string): Observable<WorkflowTransitionCleanupResponse> {
    return this.http.post<WorkflowTransitionCleanupResponse>(
      `${this.api}/policy/${policyId}/cleanup`,
      {},
    );
  }
}
