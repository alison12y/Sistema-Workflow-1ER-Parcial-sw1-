import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { KpiDashboardResponse, KpiBottlenecksResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class KpiService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/kpi`;

  getDashboard(): Observable<KpiDashboardResponse> {
    return this.http.get<KpiDashboardResponse>(`${this.api}/dashboard`);
  }

  getBottlenecks(): Observable<KpiBottlenecksResponse> {
    return this.http.get<KpiBottlenecksResponse>(`${this.api}/bottlenecks`);
  }
}
