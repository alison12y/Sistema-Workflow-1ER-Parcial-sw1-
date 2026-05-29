import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { WorkflowActivity } from '../models/workflow.model';

@Injectable({ providedIn: 'root' })
export class WorkflowActivityService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/workflow-activities`;

  getByPolicy(policyId: string): Observable<WorkflowActivity[]> {
    return this.http.get<WorkflowActivity[]>(`${this.api}/policy/${policyId}`);
  }
}
