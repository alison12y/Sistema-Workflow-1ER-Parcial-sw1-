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
  selector: 'app-monitoring',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './monitoring.component.html',
  styleUrl: './monitoring.component.scss',
})
export class MonitoringComponent implements OnInit, OnDestroy {
  private readonly monitoringService = inject(MonitoringService);

  tramites: MonitoringItem[] = [];
  loading = true;
  message = '';
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
  readonly traceEventLabel = traceEventLabel;
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

  load(showSuccessMessage = true): void {
    this.loading = true;
    this.error = '';
    if (showSuccessMessage) {
      this.message = '';
    }

    this.monitoringService.getTramites().subscribe({
      next: (data) => {
        this.tramites = data;
        this.loading = false;
        this.lastUpdated = new Date();
        if (showSuccessMessage) {
          this.message = 'Monitoreo actualizado correctamente';
        }
      },
      error: (err: HttpErrorResponse) => {
        this.tramites = [];
        this.loading = false;
        this.error = httpErrorMessage(err, 'No se pudo cargar el monitoreo');
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
      error: () => {
        /* mantener última vista en polling silencioso */
      },
    });
  }

  refresh(): void {
    this.load(true);
    if (this.traceModalOpen && this.selectedTramiteId) {
      this.refreshTrace(this.selectedTramiteId, true);
    }
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
        this.traceError = httpErrorMessage(err, 'No se pudo cargar la traza del trámite');
      },
    });
  }

  closeTraceModal(): void {
    this.traceModalOpen = false;
    this.traceData = null;
    this.selectedTramiteId = null;
    this.traceError = '';
    this.traceLoading = false;
  }

  traceTitle(item: { eventLabel?: string; activityName?: string; eventType?: string }): string {
    return traceEventLabel(item.eventType, item.eventLabel)
      + (item.activityName?.trim() ? ` — ${item.activityName.trim()}` : '');
  }

  formatDate(value?: string): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '—';
    }
    return date.toLocaleString('es-BO');
  }
}
