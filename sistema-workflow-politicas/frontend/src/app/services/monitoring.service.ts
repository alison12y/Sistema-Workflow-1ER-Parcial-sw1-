import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { MonitoringItem, MonitoringTrace } from '../models/monitoring.model';

@Injectable({ providedIn: 'root' })
export class MonitoringService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/monitoring`;

  getTramites(): Observable<MonitoringItem[]> {
    return this.http.get<MonitoringItem[]>(this.api);
  }

  getDetail(tramiteId: string): Observable<MonitoringTrace> {
    return this.http.get<MonitoringTrace>(`${this.api}/${encodeURIComponent(tramiteId)}`);
  }

  getTrace(tramiteId: string): Observable<MonitoringTrace> {
    return this.http.get<MonitoringTrace>(`${this.api}/${encodeURIComponent(tramiteId)}/trace`);
  }
}
