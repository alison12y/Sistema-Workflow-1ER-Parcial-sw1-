import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MonitoringService } from '../../services/monitoring.service';
import { MonitoringItem, MonitoringTrace } from '../../models/monitoring.model';
import { CYCLE1_POLL_INTERVAL_MS } from '../../core/polling.config';
import {
  httpErrorMessage,
  traceUserName,
  tramiteStatusClass,
  tramiteStatusLabel,
  tramiteTaskStatusLabel,
} from '../../utils/tramite-display.util';
import {
  taskStatusCssClass,
  traceEventCssClass,
  traceEventLabel,
  tramiteHasWorkflowError,
} from '../../utils/monitoring-display.util';

@Component({
  selector: 'app-tramite-tracking',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './tramite-tracking.component.html',
  styleUrl: '../monitoring/monitoring.component.scss',
})
export class TramiteTrackingComponent implements OnInit, OnDestroy {
  private readonly monitoringService = inject(MonitoringService);

  tramites: MonitoringItem[] = [];
  loading = true;
  error = '';
  lastUpdated: Date | null = null;

  traceModalOpen = false;
  traceLoading = false;
  traceError = '';
  traceData: MonitoringTrace | null = null;
  selectedTramiteId: string | null = null;

  readonly statusLabel = tramiteStatusLabel;
  readonly statusClass = tramiteStatusClass;
  readonly taskStatusLabel = tramiteTaskStatusLabel;
  readonly traceUser = traceUserName;
  readonly traceEventClass = traceEventCssClass;
  readonly taskStatusClass = taskStatusCssClass;
  readonly hasWorkflowError = tramiteHasWorkflowError;

  private pollTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.load(false);
    this.pollTimer = setInterval(() => this.poll(), CYCLE1_POLL_INTERVAL_MS);
  }

  ngOnDestroy(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  load(showSpinner = true): void {
    if (showSpinner) {
      this.loading = true;
    }
    this.error = '';

    this.monitoringService.getTramites().subscribe({
      next: (data) => {
        this.tramites = data;
        this.loading = false;
        this.lastUpdated = new Date();
        if (this.traceModalOpen && this.selectedTramiteId) {
          this.refreshTrace(this.selectedTramiteId, false);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.tramites = [];
        this.loading = false;
        this.error = httpErrorMessage(err, 'No se pudo cargar el seguimiento de trámites');
      },
    });
  }

  private poll(): void {
    this.monitoringService.getTramites().subscribe({
      next: (data) => {
        this.tramites = data;
        this.lastUpdated = new Date();
        if (this.traceModalOpen && this.selectedTramiteId) {
          this.refreshTrace(this.selectedTramiteId, false);
        }
      },
      error: () => {},
    });
  }

  openTrace(item: MonitoringItem): void {
    this.traceModalOpen = true;
    this.selectedTramiteId = item.id;
    this.refreshTrace(item.id, true);
  }

  private refreshTrace(tramiteId: string, showSpinner: boolean): void {
    if (showSpinner) {
      this.traceLoading = true;
    }
    this.traceError = '';
    this.monitoringService.getDetail(tramiteId).subscribe({
      next: (trace) => {
        this.traceData = trace;
        this.traceLoading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.traceLoading = false;
        this.traceError = httpErrorMessage(err, 'No se pudo cargar el seguimiento detallado');
      },
    });
  }

  closeTraceModal(): void {
    this.traceModalOpen = false;
    this.traceData = null;
    this.selectedTramiteId = null;
  }

  traceTitle(item: { eventLabel?: string; activityName?: string; eventType?: string }): string {
    return traceEventLabel(item.eventType, item.eventLabel);
  }

  formatDate(value?: string): string {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('es-BO');
  }
}
