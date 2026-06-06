import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AnalyticsFullResult,
  AnalyticsRecommendationResponse,
  AnalyticsReportResponse,
  AnalyticsRequest,
  AnalyticsRiskResponse,
} from '../models/intelligent-analytics.model';

@Injectable({ providedIn: 'root' })
export class IntelligentAnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/intelligent-analytics`;

  ping(): Observable<string> {
    return this.http.get(`${this.api}/ping`, { responseType: 'text' });
  }

  generateReport(request: AnalyticsRequest): Observable<AnalyticsReportResponse> {
    return this.http.post<AnalyticsReportResponse>(`${this.api}/report`, request);
  }

  analyzeRisks(request: AnalyticsRequest): Observable<AnalyticsRiskResponse> {
    return this.http.post<AnalyticsRiskResponse>(`${this.api}/risks`, request);
  }

  generateRecommendations(request: AnalyticsRequest): Observable<AnalyticsRecommendationResponse> {
    return this.http.post<AnalyticsRecommendationResponse>(`${this.api}/recommendations`, request);
  }

  generateFullAnalysis(request: AnalyticsRequest): Observable<AnalyticsFullResult> {
    return forkJoin({
      report: this.generateReport(request),
      risks: this.analyzeRisks(request),
      recommendations: this.generateRecommendations(request),
    }).pipe(
      map((result) => ({
        report: result.report,
        risks: result.risks,
        recommendations: result.recommendations,
      })),
    );
  }
}
