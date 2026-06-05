import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  FormSubmission,
  FormSubmissionFileMeta,
  FormSubmissionPayload,
} from '../models/my-activities.model';

@Injectable({ providedIn: 'root' })
export class FormSubmissionService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/form-submissions`;

  save(payload: FormSubmissionPayload): Observable<FormSubmission> {
    return this.http.post<FormSubmission>(this.api, payload);
  }

  uploadFile(file: File): Observable<FormSubmissionFileMeta> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<Record<string, unknown>>(`${this.api}/files`, formData).pipe(
      map((response) => this.normalizeFileMeta(response, file.name)),
      catchError((error) => throwError(() => error))
    );
  }

  downloadFile(fileId: string): Observable<Blob> {
    return this.http.get(`${this.api}/files/${encodeURIComponent(fileId)}/download`, {
      responseType: 'blob',
    });
  }

  getByTramite(tramiteId: string): Observable<FormSubmission[]> {
    return this.http.get<FormSubmission[]>(`${this.api}/tramite/${encodeURIComponent(tramiteId)}`);
  }

  getForTask(
    tramiteId: string,
    taskOrder: number,
    workflowActivityId?: string,
    activityName?: string
  ): Observable<FormSubmission | null> {
    let params = new HttpParams().set('taskOrder', String(taskOrder));
    if (workflowActivityId) {
      params = params.set('workflowActivityId', workflowActivityId);
    }
    if (activityName) {
      params = params.set('activity', activityName);
    }
    return this.http
      .get<FormSubmission>(`${this.api}/tramite/${encodeURIComponent(tramiteId)}/activity`, { params })
      .pipe(catchError(() => of(null)));
  }

  /** @deprecated Usar getForTask */
  getByActivity(
    tramiteId: string,
    activityName: string,
    taskOrder: number
  ): Observable<FormSubmission | null> {
    return this.getForTask(tramiteId, taskOrder, undefined, activityName);
  }

  private normalizeFileMeta(response: Record<string, unknown>, fallbackName: string): FormSubmissionFileMeta {
    const fileId = String(response['fileId'] ?? response['id'] ?? '').trim();
    const fileName = String(response['fileName'] ?? response['originalFileName'] ?? fallbackName).trim();

    if (!fileId) {
      throw new Error('El servidor no devolvió el identificador del archivo');
    }

    return {
      fileId,
      fileName: fileName || fallbackName,
      contentType: response['contentType'] ? String(response['contentType']) : undefined,
      size: typeof response['size'] === 'number' ? response['size'] : undefined,
    };
  }
}
