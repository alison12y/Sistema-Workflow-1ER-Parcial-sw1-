import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MonitoringService } from '../../services/monitoring.service';
import { MonitoringItem, MonitoringTrace } from '../../models/monitoring.model';
import {
  httpErrorMessage,
  traceUserName,
  tramiteStatusClass,
  tramiteStatusLabel,
  tramiteTaskStatusLabel,
} from '../../utils/tramite-display.util';

@Component({
  selector: 'app-tramite-tracking',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './tramite-tracking.component.html',
  styleUrl: '../monitoring/monitoring.component.scss',
})
export class TramiteTrackingComponent implements OnInit {
  private readonly monitoringService = inject(MonitoringService);

  tramites: MonitoringItem[] = [];
  loading = true;
  error = '';

  traceModalOpen = false;
  traceLoading = false;
  traceError = '';
  traceData: MonitoringTrace | null = null;

  readonly statusLabel = tramiteStatusLabel;
  readonly statusClass = tramiteStatusClass;
  readonly taskStatusLabel = tramiteTaskStatusLabel;
  readonly traceUser = traceUserName;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.monitoringService.getTramites().subscribe({
      next: (data) => {
        this.tramites = data;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.tramites = [];
        this.loading = false;
        this.error = httpErrorMessage(err, 'No se pudo cargar el seguimiento de trámites');
      },
    });
  }

  openTrace(item: MonitoringItem): void {
    this.traceModalOpen = true;
    this.traceLoading = true;
    this.traceError = '';
    this.traceData = null;
    this.monitoringService.getTrace(item.id).subscribe({
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
  }

  completedTasks(trace: MonitoringTrace): number {
    return trace.tasks?.filter((t) => t.status === 'COMPLETADA').length ?? 0;
  }

  pendingTasks(trace: MonitoringTrace): number {
    return trace.tasks?.filter((t) => t.status === 'PENDIENTE').length ?? 0;
  }

  formatDate(value?: string): string {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return date.toLocaleString('es-BO');
  }
}
