import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  SmartAgentAnalyzeRequest,
  SmartAgentAnalyzeResponse,
  SmartAgentStartTramiteRequest,
  SmartAgentStartTramiteResponse,
} from '../models/smart-agent.model';

@Injectable({ providedIn: 'root' })
export class SmartAgentService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/smart-agent`;

  analyze(request: SmartAgentAnalyzeRequest, attachment?: File | null): Observable<SmartAgentAnalyzeResponse> {
    if (attachment) {
      const formData = new FormData();
      formData.append('message', request.message ?? '');
      if (request.audioText) formData.append('audioText', request.audioText);
      if (request.documentId) formData.append('documentId', request.documentId);
      if (request.requesterName) formData.append('requesterName', request.requesterName);
      formData.append('attachment', attachment, attachment.name);
      return this.http.post<SmartAgentAnalyzeResponse>(`${this.api}/analyze`, formData);
    }
    return this.http.post<SmartAgentAnalyzeResponse>(`${this.api}/analyze`, request);
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
