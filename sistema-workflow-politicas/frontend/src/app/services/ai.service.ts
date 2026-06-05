import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AiWorkflowSuggestRequest,
  AiWorkflowSuggestResponse,
} from '../models/ai-workflow-suggest.model';
import { AiFormAssistRequest, AiFormAssistResponse } from '../models/ai-form-assist.model';

export interface AiAssistantRequest {
  prompt: string;
  module: string;
  context?: Record<string, unknown>;
  userId?: string;
}

export interface AiAssistantResponse {
  aiAvailable?: boolean;
  fallbackUsed?: boolean;
  error?: string;
  module: string;
  intent: string;
  answer: string;
  suggestedData: Record<string, unknown>;
  suggestedEndpoint?: { method: string; url: string };
  requiresConfirmation: boolean;
  suggestions: string[];
  warnings: string[];
}

@Injectable({ providedIn: 'root' })
export class AiService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/ai`;

  assistant(request: AiAssistantRequest): Observable<AiAssistantResponse> {
    return this.http.post<AiAssistantResponse>(`${this.api}/assistant`, request);
  }

  generateWorkflow(prompt: string): Observable<unknown> {
    return this.http.post(`${this.api}/generate-workflow`, { prompt });
  }

  suggestWorkflow(request: AiWorkflowSuggestRequest): Observable<AiWorkflowSuggestResponse> {
    return this.http.post<AiWorkflowSuggestResponse>(`${this.api}/workflow/suggest`, request);
  }

  assistForm(request: AiFormAssistRequest): Observable<AiFormAssistResponse> {
    return this.http.post<AiFormAssistResponse>(`${this.api}/assist-form`, request);
  }
}
