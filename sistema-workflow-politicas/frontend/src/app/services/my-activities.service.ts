import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AiFormAssistTraceRequest } from '../models/ai-form-assist.model';
import {
  CompleteActivityPayload,
  MyActivitiesFilterParams,
  MyActivity,
} from '../models/my-activities.model';
import { Tramite } from '../models/tramite.model';
import { TaskAssistantResponse } from '../models/task-assistant.model';

@Injectable({ providedIn: 'root' })
export class MyActivitiesService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/my-activities`;

  getAll(filter?: MyActivitiesFilterParams): Observable<MyActivity[]> {
    let params = new HttpParams();
    if (filter?.status) params = params.set('status', filter.status);
    if (filter?.policyId) params = params.set('policyId', filter.policyId);
    if (filter?.tramiteId) params = params.set('tramiteId', filter.tramiteId);
    if (filter?.tramiteCode) params = params.set('tramiteCode', filter.tramiteCode);
    if (filter?.priority) params = params.set('priority', filter.priority);
    return this.http.get<MyActivity[]>(this.api, { params });
  }

  getById(tramiteId: string, taskOrder?: number): Observable<MyActivity> {
    let params = new HttpParams();
    if (taskOrder != null && taskOrder > 0) {
      params = params.set('taskOrder', String(taskOrder));
    }
    return this.http.get<MyActivity>(`${this.api}/${encodeURIComponent(tramiteId)}`, { params });
  }

  takeTask(tramiteId: string, taskOrder: number): Observable<Tramite> {
    return this.http.put<Tramite>(
      `${this.api}/${encodeURIComponent(tramiteId)}/tasks/${taskOrder}/take`,
      {}
    );
  }

  complete(tramiteId: string, payload: CompleteActivityPayload): Observable<Tramite> {
    return this.http.put<Tramite>(`${this.api}/${encodeURIComponent(tramiteId)}/complete`, payload);
  }

  recordAiFormAssisted(tramiteId: string, payload: AiFormAssistTraceRequest): Observable<void> {
    return this.http.post<void>(
      `${this.api}/${encodeURIComponent(tramiteId)}/ai-form-assisted`,
      payload
    );
  }

  /** activityId = tramiteId, taskId = taskOrder de la bandeja. */
  getTaskAssistant(tramiteId: string, taskOrder: number): Observable<TaskAssistantResponse> {
    return this.http.post<TaskAssistantResponse>(
      `${this.api}/${encodeURIComponent(tramiteId)}/tasks/${taskOrder}/ai-assistant`,
      {}
    );
  }
}
