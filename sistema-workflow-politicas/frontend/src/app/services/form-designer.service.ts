import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DynamicFormDetail, DynamicFormSaveRequest } from '../models/form.model';

@Injectable({ providedIn: 'root' })
export class FormDesignerService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/forms`;

  getByPolicyAndActivity(policyId: string, activityName: string): Observable<DynamicFormDetail> {
    return this.http.get<DynamicFormDetail>(`${this.api}/policy/${policyId}`, {
      params: { activity: activityName },
    });
  }

  saveForm(payload: DynamicFormSaveRequest): Observable<DynamicFormDetail> {
    return this.http.post<DynamicFormDetail>(`${this.api}/save`, payload);
  }
}
