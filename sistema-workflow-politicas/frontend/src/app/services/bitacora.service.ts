import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BitacoraEntry, BitacoraFilter } from '../models/bitacora.model';

@Injectable({ providedIn: 'root' })
export class BitacoraService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/bitacora`;

  getAll(filter?: BitacoraFilter): Observable<BitacoraEntry[]> {
    const params = this.buildParams(filter);
    return this.http.get<BitacoraEntry[]>(this.api, { params });
  }

  getByModule(module: string): Observable<BitacoraEntry[]> {
    return this.http.get<BitacoraEntry[]>(`${this.api}/module/${encodeURIComponent(module)}`);
  }

  getByUser(userId: string): Observable<BitacoraEntry[]> {
    return this.http.get<BitacoraEntry[]>(`${this.api}/user/${encodeURIComponent(userId)}`);
  }

  exportCsv(filter?: BitacoraFilter): Observable<Blob> {
    const params = this.buildParams(filter);
    return this.http.get(`${this.api}/export/csv`, {
      params,
      responseType: 'blob',
    });
  }

  private buildParams(filter?: BitacoraFilter): HttpParams {
    let params = new HttpParams();
    if (!filter) {
      return params;
    }
    if (filter.userId?.trim()) {
      params = params.set('userId', filter.userId.trim());
    }
    if (filter.username?.trim()) {
      params = params.set('username', filter.username.trim());
    }
    if (filter.module?.trim()) {
      params = params.set('module', filter.module.trim());
    }
    if (filter.action?.trim()) {
      params = params.set('action', filter.action.trim());
    }
    if (filter.dateFrom?.trim()) {
      params = params.set('dateFrom', filter.dateFrom.trim());
    }
    if (filter.dateTo?.trim()) {
      params = params.set('dateTo', filter.dateTo.trim());
    }
    return params;
  }
}
