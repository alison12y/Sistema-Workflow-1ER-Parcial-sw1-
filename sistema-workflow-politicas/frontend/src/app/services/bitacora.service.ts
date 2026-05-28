import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BitacoraEntry } from '../models/bitacora.model';

@Injectable({ providedIn: 'root' })
export class BitacoraService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/bitacora`;

  getAll(): Observable<BitacoraEntry[]> {
    return this.http.get<BitacoraEntry[]>(this.api);
  }

  getByModule(module: string): Observable<BitacoraEntry[]> {
    return this.http.get<BitacoraEntry[]>(`${this.api}/module/${encodeURIComponent(module)}`);
  }

  getByUser(userId: string): Observable<BitacoraEntry[]> {
    return this.http.get<BitacoraEntry[]>(`${this.api}/user/${encodeURIComponent(userId)}`);
  }
}
