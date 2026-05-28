import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { KpiBottleneck, KpiSummary } from '../models/kpi.model';

@Injectable({ providedIn: 'root' })
export class KpiService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/kpis`;

  getSummary(): Observable<KpiSummary> {
    return this.http.get<KpiSummary>(`${this.api}/summary`);
  }

  getBottlenecks(): Observable<KpiBottleneck[]> {
    return this.http.get<KpiBottleneck[]>(`${this.api}/bottlenecks`);
  }
}
