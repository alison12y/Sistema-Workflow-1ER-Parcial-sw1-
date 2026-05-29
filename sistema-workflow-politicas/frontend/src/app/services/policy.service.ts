import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BusinessPolicy } from '../models/auth.model';
import { PolicyDetail, PolicySummary } from '../models/workflow.model';

@Injectable({ providedIn: 'root' })
export class PolicyService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/policies`;

  getAll(search?: string): Observable<BusinessPolicy[]> {
    const params = search?.trim() ? { search: search.trim() } : undefined;
    return this.http.get<BusinessPolicy[]>(this.api, { params });
  }

  search(query: string): Observable<BusinessPolicy[]> {
    return this.http.get<BusinessPolicy[]>(`${this.api}/search`, {
      params: { q: query.trim() },
    });
  }

  getSummaries(search?: string): Observable<PolicySummary[]> {
    const params = search?.trim() ? { search: search.trim() } : undefined;
    return this.http.get<PolicySummary[]>(`${this.api}/summaries`, { params });
  }

  getDetail(id: string): Observable<PolicyDetail> {
    return this.http.get<PolicyDetail>(`${this.api}/${id}/detail`);
  }

  getById(id: string): Observable<BusinessPolicy> {
    return this.http.get<BusinessPolicy>(`${this.api}/${id}`);
  }

  create(policy: BusinessPolicy): Observable<BusinessPolicy> {
    return this.http.post<BusinessPolicy>(this.api, policy);
  }

  update(id: string, policy: BusinessPolicy): Observable<BusinessPolicy> {
    return this.http.put<BusinessPolicy>(`${this.api}/${id}`, policy);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  activate(id: string): Observable<BusinessPolicy> {
    return this.http.patch<BusinessPolicy>(`${this.api}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<BusinessPolicy> {
    return this.http.patch<BusinessPolicy>(`${this.api}/${id}/deactivate`, {});
  }
}
