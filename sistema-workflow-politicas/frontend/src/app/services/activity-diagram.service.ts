import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ActivityDiagram, ActivityDiagramSaveRequest } from '../models/activity-diagram.model';

@Injectable({ providedIn: 'root' })
export class ActivityDiagramService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/activity-diagrams`;

  getByPolicy(policyId: string): Observable<ActivityDiagram> {
    return this.http.get<ActivityDiagram>(`${this.api}/policy/${policyId}`);
  }

  save(payload: ActivityDiagramSaveRequest): Observable<ActivityDiagram> {
    return this.http.post<ActivityDiagram>(`${this.api}/save`, payload);
  }

  update(id: string, payload: ActivityDiagramSaveRequest): Observable<ActivityDiagram> {
    return this.http.put<ActivityDiagram>(`${this.api}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
