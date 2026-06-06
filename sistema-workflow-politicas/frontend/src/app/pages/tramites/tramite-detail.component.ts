import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { TramiteService } from '../../services/tramite.service';
import { FormSubmissionService } from '../../services/form-submission.service';
import { Tramite } from '../../models/tramite.model';
import {
  FormSubmissionView,
  toFormSubmissionViews,
  triggerFileDownload,
} from '../../utils/form-submission-display.util';
import { CYCLE1_POLL_INTERVAL_MS } from '../../core/polling.config';
import {
  traceEventLabel,
  traceEventCssClass,
  tramiteHasWorkflowError,
} from '../../utils/monitoring-display.util';
import {
  httpErrorMessage,
  traceUserName,
  tramiteDescription,
  tramitePriorityClass,
  tramitePriorityLabel,
  tramiteRequesterName,
  tramiteStatusClass,
  tramiteStatusLabel,
  tramiteTaskStatusLabel,
} from '../../utils/tramite-display.util';
import { TramiteDocumentsComponent } from './tramite-documents.component';

@Component({
  selector: 'app-tramite-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, TramiteDocumentsComponent],
  templateUrl: './tramite-detail.component.html',
  styleUrl: './tramite-detail.component.scss',
})
export class TramiteDetailComponent implements OnInit, OnDestroy {
  private readonly tramiteService = inject(TramiteService);
  private readonly auth = inject(AuthService);
  private readonly formSubmissionService = inject(FormSubmissionService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  tramite: Tramite | null = null;
  formSubmissions: FormSubmissionView[] = [];
  loading = true;
  loadingSubmissions = true;
  acting = false;
  deleting = false;
  deleteModalOpen = false;
  message = '';
  error = '';
  readonly deleteConfirmMessage =
    '¿Está seguro de eliminar este trámite? Esta acción no se puede deshacer.';

  readonly statusLabel = tramiteStatusLabel;
  readonly statusClass = tramiteStatusClass;
  readonly priorityLabel = tramitePriorityLabel;
  readonly priorityClass = tramitePriorityClass;
  readonly taskStatusLabel = tramiteTaskStatusLabel;
  readonly displayDescription = tramiteDescription;
  readonly displayRequester = tramiteRequesterName;
  readonly traceUser = traceUserName;
  readonly traceEventLabel = traceEventLabel;
  readonly traceEventClass = traceEventCssClass;
  readonly hasWorkflowError = tramiteHasWorkflowError;
  lastUpdated: Date | null = null;

  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private currentTramiteId: string | null = null;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (!id) {
        this.router.navigate(['/tramites']);
        return;
      }
      this.currentTramiteId = id;
      this.load(id, true);
      if (this.pollTimer) {
        clearInterval(this.pollTimer);
      }
      this.pollTimer = setInterval(() => {
        if (this.currentTramiteId) {
          this.load(this.currentTramiteId, false);
        }
      }, CYCLE1_POLL_INTERVAL_MS);
    });
  }

  ngOnDestroy(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  load(id: string, showSpinner = true): void {
    if (showSpinner) {
      this.loading = true;
      this.loadingSubmissions = true;
    }
    this.error = '';

    this.tramiteService.getById(id).subscribe({
      next: (data) => {
        this.tramite = data;
        this.loading = false;
        this.lastUpdated = new Date();
        this.loadFormSubmissions(id, showSpinner);
      },
      error: (err) => {
        this.error = httpErrorMessage(err, 'No se pudo cargar el detalle del trámite');
        this.loading = false;
        this.loadingSubmissions = false;
      },
    });
  }

  loadFormSubmissions(tramiteId: string, showSpinner = true): void {
    if (showSpinner) {
      this.loadingSubmissions = true;
    }
    this.formSubmissionService.getByTramite(tramiteId).subscribe({
      next: (submissions) => {
        this.formSubmissions = toFormSubmissionViews(submissions);
        this.loadingSubmissions = false;
      },
      error: () => {
        this.formSubmissions = [];
        this.loadingSubmissions = false;
      },
    });
  }

  refresh(): void {
    if (this.tramite?.id) {
      this.message = '';
      this.load(this.tramite.id);
    }
  }

  advance(): void {
    if (!this.tramite?.id || !this.canAdvance()) return;
    this.acting = true;
    this.tramiteService.advance(this.tramite.id).subscribe({
      next: (updated) => {
        this.tramite = updated;
        this.acting = false;
        this.message = 'Trámite actualizado correctamente';
      },
      error: (err) => {
        this.acting = false;
        this.error = httpErrorMessage(err, 'No se pudo avanzar el trámite');
      },
    });
  }

  cancel(): void {
    if (!this.tramite?.id || !this.canCancel()) return;
    if (!confirm(`¿Cancelar el trámite ${this.tramite.code}?`)) return;
    this.acting = true;
    this.tramiteService.cancel(this.tramite.id, { comment: 'Cancelado desde el detalle' }).subscribe({
      next: (updated) => {
        this.tramite = updated;
        this.acting = false;
        this.message = 'Trámite cancelado';
      },
      error: (err) => {
        this.acting = false;
        this.error = httpErrorMessage(err, 'No se pudo cancelar el trámite');
      },
    });
  }

  canAdvance(): boolean {
    return (
      !!this.tramite &&
      this.auth.isAdmin() &&
      (this.tramite.status === 'INICIADO' || this.tramite.status === 'EN_PROCESO')
    );
  }

  canCancel(): boolean {
    return (
      !!this.tramite &&
      (this.tramite.status === 'INICIADO' || this.tramite.status === 'EN_PROCESO')
    );
  }

  canDelete(): boolean {
    return (
      !!this.tramite &&
      (this.tramite.status === 'COMPLETADO' || this.tramite.status === 'CANCELADO')
    );
  }

  openDeleteModal(): void {
    if (!this.canDelete()) {
      return;
    }
    this.deleteModalOpen = true;
    this.error = '';
  }

  closeDeleteModal(): void {
    this.deleteModalOpen = false;
  }

  confirmDelete(): void {
    if (!this.tramite?.id || !this.canDelete()) {
      return;
    }
    const tramiteId = this.tramite.id;
    const code = this.tramite.code;
    this.deleting = true;
    this.error = '';
    this.tramiteService.delete(tramiteId).subscribe({
      next: () => {
        this.deleting = false;
        this.deleteModalOpen = false;
        this.router.navigate(['/tramites'], {
          state: { message: `Trámite ${code} eliminado` },
        });
      },
      error: (err) => {
        this.deleting = false;
        this.error = httpErrorMessage(err, 'No se pudo eliminar el trámite');
      },
    });
  }

  formatDate(value?: string): string {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-BO');
  }

  traceTitle(item: { eventLabel?: string; activityName?: string; eventType?: string }): string {
    const label = traceEventLabel(item.eventType, item.eventLabel);
    if (item.activityName?.trim()) {
      return `${label} — ${item.activityName.trim()}`;
    }
    return label;
  }

  downloadFile(fileId: string | undefined, fileName: string | undefined): void {
    if (!fileId || !fileName) {
      this.error = 'No se pudo descargar el archivo adjunto';
      return;
    }

    this.formSubmissionService.downloadFile(fileId).subscribe({
      next: (blob) => triggerFileDownload(blob, fileName),
      error: () => {
        this.error = 'No se pudo descargar el archivo adjunto';
      },
    });
  }
}
