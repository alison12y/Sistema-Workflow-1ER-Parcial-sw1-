import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WorkflowService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/workflows`;

  getDiagram(policyId: string): Observable<any> {
    return this.http.get<any>(`${this.api}/policy/${policyId}`);
  }

  saveDiagram(policyId: string, diagram: any): Observable<any> {
    return this.http.post<any>(`${this.api}/policy/${policyId}`, diagram);
  }
}
