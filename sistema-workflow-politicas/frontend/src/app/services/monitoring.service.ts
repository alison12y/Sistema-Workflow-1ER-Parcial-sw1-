import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ProcessTraceabilityResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class MonitoringService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/monitoring`;

  getProcesses(): Observable<any[]> {
    return this.http.get<any[]>(`${this.api}/processes`);
  }

  getTraceability(processId: string): Observable<ProcessTraceabilityResponse> {
    return this.http.get<ProcessTraceabilityResponse>(`${this.api}/traceability/${processId}`);
  }
}
