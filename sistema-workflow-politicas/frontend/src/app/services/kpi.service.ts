import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { KpiBottleneck, KpiDashboard, KpiFilterParams, KpiSummary } from '../models/kpi.model';

@Injectable({ providedIn: 'root' })
export class KpiService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/kpis`;

  getDashboard(filter?: KpiFilterParams): Observable<KpiDashboard> {
    return this.http.get<KpiDashboard>(`${this.api}/dashboard`, { params: this.toParams(filter) });
  }

  getSummary(filter?: KpiFilterParams): Observable<KpiSummary> {
    return this.http.get<KpiSummary>(`${this.api}/summary`, { params: this.toParams(filter) });
  }

  getBottlenecks(filter?: KpiFilterParams): Observable<KpiBottleneck[]> {
    return this.http.get<KpiBottleneck[]>(`${this.api}/bottlenecks`, { params: this.toParams(filter) });
  }

  private toParams(filter?: KpiFilterParams): HttpParams {
    let params = new HttpParams();
    if (filter?.policyId) params = params.set('policyId', filter.policyId);
    if (filter?.status) params = params.set('status', filter.status);
    if (filter?.from) params = params.set('from', filter.from);
    if (filter?.to) params = params.set('to', filter.to);
    return params;
  }
}
