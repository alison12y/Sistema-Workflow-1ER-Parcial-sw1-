import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Role } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/roles`;

  getAll(): Observable<Role[]> {
    return this.http.get<Role[]>(this.api);
  }

  create(role: Role): Observable<Role> {
    return this.http.post<Role>(this.api, role);
  }

  update(id: string, role: Role): Observable<Role> {
    return this.http.put<Role>(`${this.api}/${id}`, role);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
