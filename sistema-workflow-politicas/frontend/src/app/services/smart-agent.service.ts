import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  SmartAgentAnalyzeRequest,
  SmartAgentAnalyzeResponse,
  SmartAgentStartTramiteRequest,
  SmartAgentStartTramiteResponse,
} from '../models/smart-agent.model';

/** Tiempo máximo de espera antes de activar fallback local en el cliente. */
export const SMART_AGENT_ANALYZE_TIMEOUT_MS = 45_000;

@Injectable({ providedIn: 'root' })
export class SmartAgentService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/smart-agent`;

  analyze(request: SmartAgentAnalyzeRequest, attachment?: File | null): Observable<SmartAgentAnalyzeResponse> {
    const call = attachment
      ? this.http.post<SmartAgentAnalyzeResponse>(`${this.api}/analyze`, this.buildAnalyzeFormData(request, attachment))
      : this.http.post<SmartAgentAnalyzeResponse>(`${this.api}/analyze`, request);
    return call.pipe(timeout(SMART_AGENT_ANALYZE_TIMEOUT_MS));
  }

  private buildAnalyzeFormData(request: SmartAgentAnalyzeRequest, attachment: File): FormData {
    const formData = new FormData();
    formData.append('message', request.message ?? '');
    if (request.audioText) formData.append('audioText', request.audioText);
    if (request.documentId) formData.append('documentId', request.documentId);
    if (request.requesterName) formData.append('requesterName', request.requesterName);
    formData.append('attachment', attachment, attachment.name);
    return formData;
  }

  startTramite(
    request: SmartAgentStartTramiteRequest,
    attachment?: File | null,
  ): Observable<SmartAgentStartTramiteResponse> {
    if (attachment) {
      const formData = new FormData();
      formData.append('request', JSON.stringify(request));
      formData.append('attachment', attachment, attachment.name);
      return this.http.post<SmartAgentStartTramiteResponse>(`${this.api}/start-tramite`, formData);
    }
    return this.http.post<SmartAgentStartTramiteResponse>(`${this.api}/start-tramite`, request);
  }
}
