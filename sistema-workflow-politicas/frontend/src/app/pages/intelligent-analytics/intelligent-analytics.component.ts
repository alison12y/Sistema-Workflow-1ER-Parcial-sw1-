import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IntelligentAnalyticsService } from '../../services/intelligent-analytics.service';
import { PolicyService } from '../../services/policy.service';
import { BusinessPolicy } from '../../models/auth.model';
import {
  AnalyticsFullResult,
  AnalyticsRecommendationItem,
  AnalyticsRiskItem,
  AnalyticsSummaryCard,
} from '../../models/intelligent-analytics.model';
import { httpErrorMessage } from '../../utils/tramite-display.util';
import { VoiceDictationController } from '../../utils/voice-dictation.util';

@Component({
  selector: 'app-intelligent-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './intelligent-analytics.component.html',
  styleUrl: './intelligent-analytics.component.scss',
})
export class IntelligentAnalyticsComponent implements OnInit, OnDestroy {
  private readonly analyticsService = inject(IntelligentAnalyticsService);
  private readonly policyService = inject(PolicyService);

  message = '';
  audioText = '';
  filterPolicyId = '';
  filterStatus = '';
  filterFrom = '';
  filterTo = '';

  policies: BusinessPolicy[] = [];
  loading = false;
  listening = false;
  error = '';
  result: AnalyticsFullResult | null = null;

  private voiceController: VoiceDictationController | null = null;

  readonly statusOptions = [
    { value: '', label: 'Todos los estados' },
    { value: 'ACTIVO', label: 'Trámites activos' },
    { value: 'EN_PROCESO', label: 'En proceso' },
    { value: 'INICIADO', label: 'Iniciados' },
    { value: 'FINALIZADO', label: 'Finalizados' },
    { value: 'ERROR', label: 'Con error workflow' },
  ];

  ngOnInit(): void {
    this.policyService.getAll().subscribe({
      next: (data) => (this.policies = data ?? []),
      error: () => (this.policies = []),
    });
    this.initVoice();
  }

  ngOnDestroy(): void {
    this.voiceController?.abort();
  }

  generateAnalysis(): void {
    const text = this.message.trim();
    const voice = this.audioText.trim();
    if (!text && !voice) {
      this.error = 'Indique qué reporte o análisis necesita, por texto o voz.';
      return;
    }

    this.loading = true;
    this.error = '';
    this.result = null;

    this.analyticsService.generateFullAnalysis(this.buildRequest(text, voice)).subscribe({
      next: (response) => {
        this.loading = false;
        this.result = response;
      },
      error: (err) => {
        this.loading = false;
        this.error = httpErrorMessage(err, 'No se pudo generar el análisis');
      },
    });
  }

  toggleVoice(): void {
    if (this.listening) {
      this.voiceController?.stop();
      return;
    }
    this.voiceController?.start();
  }

  allCards(): AnalyticsSummaryCard[] {
    if (!this.result) return [];
    return [
      ...(this.result.report?.cards ?? []),
      ...(this.result.risks?.cards ?? []),
      ...(this.result.recommendations?.cards ?? []),
    ];
  }

  chartMax(): number {
    const values = this.result?.report?.chart?.values ?? [];
    return values.length ? Math.max(...values, 1) : 1;
  }

  barWidth(value: number): string {
    return `${Math.max(8, (value / this.chartMax()) * 100)}%`;
  }

  sourceLabel(): string {
    const source = this.result?.report?.source ?? this.result?.risks?.source ?? '—';
    return source === 'AI_SERVICE' ? 'Servicio IA (FastAPI)' : 'Motor local (Java)';
  }

  severityClass(severity?: string): string {
    switch ((severity ?? 'info').toLowerCase()) {
      case 'danger':
        return 'severity-danger';
      case 'warning':
        return 'severity-warning';
      case 'success':
        return 'severity-success';
      default:
        return 'severity-info';
    }
  }

  riskSeverityClass(severity?: string): string {
    switch ((severity ?? 'MEDIO').toUpperCase()) {
      case 'ALTO':
        return 'severity-danger';
      case 'BAJO':
        return 'severity-success';
      default:
        return 'severity-warning';
    }
  }

  priorityClass(priority?: string): string {
    switch ((priority ?? 'MEDIA').toUpperCase()) {
      case 'ALTA':
        return 'severity-danger';
      case 'BAJA':
        return 'severity-info';
      default:
        return 'severity-warning';
    }
  }

  trackRisk(_index: number, item: AnalyticsRiskItem): string {
    return `${item.type}-${item.entityId ?? item.title}`;
  }

  trackRecommendation(_index: number, item: AnalyticsRecommendationItem): string {
    return `${item.type}-${item.tramiteCode ?? item.title}`;
  }

  private buildRequest(text: string, voice: string) {
    return {
      message: text || voice,
      audioText: voice || undefined,
      policyId: this.filterPolicyId || undefined,
      status: this.filterStatus || undefined,
      fromDate: this.filterFrom || undefined,
      toDate: this.filterTo || undefined,
    };
  }

  private initVoice(): void {
    this.voiceController = new VoiceDictationController({
      onTranscript: (text) => {
        this.audioText = text;
        if (!this.message.trim()) {
          this.message = text;
        }
      },
      onListeningChange: (value) => {
        this.listening = value;
      },
      onError: (msg) => {
        this.error = msg;
      },
    });
  }
}
