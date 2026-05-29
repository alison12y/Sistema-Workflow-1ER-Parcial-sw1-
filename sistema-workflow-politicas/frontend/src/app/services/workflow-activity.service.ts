import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { WorkflowActivity, WorkflowActivityRequest } from '../models/workflow.model';

@Injectable({ providedIn: 'root' })
export class WorkflowActivityService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/workflow-activities`;

  getByPolicy(policyId: string): Observable<WorkflowActivity[]> {
    return this.http.get<WorkflowActivity[]>(`${this.api}/policy/${policyId}`);
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

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  activate(id: string): Observable<WorkflowActivity> {
    return this.http.patch<WorkflowActivity>(`${this.api}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<WorkflowActivity> {
    return this.http.patch<WorkflowActivity>(`${this.api}/${id}/deactivate`, {});
  }
}
