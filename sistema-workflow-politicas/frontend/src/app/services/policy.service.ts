import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BusinessPolicy } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class PolicyService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/policies`;

  getAll(): Observable<BusinessPolicy[]> {
    return this.http.get<BusinessPolicy[]>(this.api);
  }

  create(policy: BusinessPolicy): Observable<BusinessPolicy> {
    return this.http.post<BusinessPolicy>(this.api, policy);
  }
}
