import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { WorkflowDesignerData } from '../models/workflow.model';

@Injectable({ providedIn: 'root' })
export class WorkflowDesignerService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/workflow-designer`;

  getByPolicy(policyId: string): Observable<WorkflowDesignerData> {
    return this.http.get<WorkflowDesignerData>(`${this.api}/policy/${policyId}`, {
      params: { _: Date.now().toString() },
    });
  }
}
