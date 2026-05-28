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
  selector: 'app-monitoring',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './monitoring.component.html',
  styleUrl: './monitoring.component.scss',
})
export class MonitoringComponent implements OnInit {
  private readonly monitoringService = inject(MonitoringService);

  tramites: MonitoringItem[] = [];
  loading = true;
  message = '';
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
    this.load(false);
  }

  load(showSuccessMessage = true): void {
    this.loading = true;
    this.error = '';
    this.message = '';

    this.monitoringService.getTramites().subscribe({
      next: (data) => {
        this.tramites = data;
        this.loading = false;
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

  refresh(): void {
    this.load(true);
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
        this.traceError = httpErrorMessage(err, 'No se pudo cargar la traza del trámite');
      },
    });
  }

  closeTraceModal(): void {
    this.traceModalOpen = false;
    this.traceData = null;
    this.traceError = '';
    this.traceLoading = false;
  }

  traceTitle(item: { eventLabel?: string; activityName?: string; eventType?: string }): string {
    if (item.eventLabel?.trim()) {
      return item.eventLabel.trim();
    }
    if (item.activityName?.trim()) {
      return item.activityName.trim();
    }
    return this.eventTypeLabel(item.eventType);
  }

  formatDate(value?: string): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '—';
    }
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${minutes}`;
  }

  private eventTypeLabel(eventType?: string): string {
    const labels: Record<string, string> = {
      PROCESO_CREADO: 'Proceso creado',
      TRAMITE_INICIADO: 'Trámite iniciado',
      ACTIVIDAD_COMPLETADA: 'Actividad completada',
      PROCESO_AVANZADO: 'Proceso avanzado',
      TRAMITE_CANCELADO: 'Trámite cancelado',
      TRAMITE_FINALIZADO: 'Trámite finalizado',
    };
    return eventType ? labels[eventType] ?? eventType : 'Evento';
  }
}
