import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DynamicForm, DynamicFormRequest, WorkflowDeleteResponse } from '../models/form.model';

@Injectable({ providedIn: 'root' })
export class FormService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/forms`;

  getFormByActivity(activityId: string): Observable<DynamicForm> {
    return this.http.get<DynamicForm>(`${this.api}/activity/${activityId}`, {
      params: { _: Date.now().toString() },
    });
  }

  getDetailByActivity(workflowActivityId: string): Observable<DynamicForm> {
    return this.http.get<DynamicForm>(`${this.api}/activity/${workflowActivityId}/detail`, {
      params: { _: Date.now().toString() },
    });
  }

  createForm(request: DynamicFormRequest): Observable<DynamicForm> {
    return this.http.post<DynamicForm>(this.api, request);
  }

  updateForm(id: string, request: DynamicFormRequest): Observable<DynamicForm> {
    return this.http.put<DynamicForm>(`${this.api}/${id}`, request);
  }

  deleteForm(id: string): Observable<WorkflowDeleteResponse> {
    return this.http.delete<WorkflowDeleteResponse>(`${this.api}/${id}`);
  }

  activateForm(id: string): Observable<DynamicForm> {
    return this.http.patch<DynamicForm>(`${this.api}/${id}/activate`, {});
  }

  deactivateForm(id: string): Observable<DynamicForm> {
    return this.http.patch<DynamicForm>(`${this.api}/${id}/deactivate`, {});
  }
}
