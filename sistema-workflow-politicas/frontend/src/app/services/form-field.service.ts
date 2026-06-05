import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { FormField, FormFieldRequest, WorkflowDeleteResponse } from '../models/form.model';

@Injectable({ providedIn: 'root' })
export class FormFieldService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/form-fields`;

  getFieldsByForm(formId: string): Observable<FormField[]> {
    return this.http.get<FormField[]>(`${this.api}/form/${formId}`, {
      params: { _: Date.now().toString() },
    });
  }

  createField(request: FormFieldRequest): Observable<FormField> {
    return this.http.post<FormField>(this.api, request);
  }

  updateField(id: string, request: FormFieldRequest): Observable<FormField> {
    return this.http.put<FormField>(`${this.api}/${id}`, request);
  }

  deleteField(id: string): Observable<WorkflowDeleteResponse> {
    return this.http.delete<WorkflowDeleteResponse>(`${this.api}/${id}`);
  }

  activateField(id: string): Observable<FormField> {
    return this.http.patch<FormField>(`${this.api}/${id}/activate`, {});
  }

  deactivateField(id: string): Observable<FormField> {
    return this.http.patch<FormField>(`${this.api}/${id}/deactivate`, {});
  }
}
