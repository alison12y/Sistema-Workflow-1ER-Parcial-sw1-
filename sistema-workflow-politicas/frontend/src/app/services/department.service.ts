import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Department } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/departments`;

  getAll(): Observable<Department[]> {
    return this.http.get<Department[]>(this.api);
  }

  create(department: Department): Observable<Department> {
    return this.http.post<Department>(this.api, department);
  }

  update(id: string, department: Department): Observable<Department> {
    return this.http.put<Department>(`${this.api}/${id}`, department);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
