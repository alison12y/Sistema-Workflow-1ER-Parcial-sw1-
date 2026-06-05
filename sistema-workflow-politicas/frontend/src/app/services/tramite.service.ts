import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  Tramite,
  TramiteAdvanceRequest,
  TramiteCancelRequest,
  TramiteCreateRequest,
} from '../models/tramite.model';

@Injectable({ providedIn: 'root' })
export class TramiteService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/tramites`;

  getAll(): Observable<Tramite[]> {
    return this.http.get<Tramite[]>(this.api);
  }

  getById(id: string): Observable<Tramite> {
    return this.http.get<Tramite>(`${this.api}/${id}`);
  }

  create(request: TramiteCreateRequest): Observable<Tramite> {
    return this.http.post<Tramite>(this.api, request);
  }

  advance(id: string, request?: TramiteAdvanceRequest): Observable<Tramite> {
    return this.http.put<Tramite>(`${this.api}/${id}/advance`, request ?? {});
  }

  cancel(id: string, request?: TramiteCancelRequest): Observable<Tramite> {
    return this.http.put<Tramite>(`${this.api}/${id}/cancel`, request ?? {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete(`${this.api}/${id}`, { responseType: 'text' }).pipe(
      map(() => undefined)
    );
  }
}
