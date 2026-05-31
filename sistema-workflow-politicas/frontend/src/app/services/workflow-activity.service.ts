import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { WorkflowActivity, WorkflowActivityRequest, WorkflowDeleteResponse } from '../models/workflow.model';

@Injectable({ providedIn: 'root' })
export class WorkflowActivityService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/workflow-activities`;

  getByPolicy(policyId: string): Observable<WorkflowActivity[]> {
    return this.http.get<WorkflowActivity[]>(`${this.api}/policy/${policyId}`, {
      params: { _: Date.now().toString() },
    });
  }

  getById(id: string): Observable<WorkflowActivity> {
    return this.http.get<WorkflowActivity>(`${this.api}/${id}`);
  }

  create(request: WorkflowActivityRequest): Observable<WorkflowActivity> {
    return this.http.post<WorkflowActivity>(this.api, request);
  }

  update(id: string, request: WorkflowActivityRequest): Observable<WorkflowActivity> {
    return this.http.put<WorkflowActivity>(`${this.api}/${id}`, request);
  }

  delete(id: string): Observable<WorkflowDeleteResponse> {
    return this.http.delete<WorkflowDeleteResponse>(`${this.api}/${id}`);
  }

  activate(id: string): Observable<WorkflowActivity> {
    return this.http.patch<WorkflowActivity>(`${this.api}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<WorkflowActivity> {
    return this.http.patch<WorkflowActivity>(`${this.api}/${id}/deactivate`, {});
  }

  updatePosition(id: string, positionX: number, positionY: number): Observable<WorkflowActivity> {
    return this.http.patch<WorkflowActivity>(`${this.api}/${id}/position`, { positionX, positionY });
  }

  clearPosition(id: string): Observable<WorkflowActivity> {
    return this.http.delete<WorkflowActivity>(`${this.api}/${id}/position`);
  }
}
