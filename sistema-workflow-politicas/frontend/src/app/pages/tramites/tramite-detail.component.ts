import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TramiteService } from '../../services/tramite.service';
import { FormSubmissionService } from '../../services/form-submission.service';
import { Tramite } from '../../models/tramite.model';
import {
  FormSubmissionView,
  toFormSubmissionViews,
  triggerFileDownload,
} from '../../utils/form-submission-display.util';
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

@Component({
  selector: 'app-tramite-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './tramite-detail.component.html',
  styleUrl: './tramite-detail.component.scss',
})
export class TramiteDetailComponent implements OnInit {
  private readonly tramiteService = inject(TramiteService);
  private readonly formSubmissionService = inject(FormSubmissionService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  tramite: Tramite | null = null;
  formSubmissions: FormSubmissionView[] = [];
  loading = true;
  loadingSubmissions = true;
  acting = false;
  message = '';
  error = '';

  readonly statusLabel = tramiteStatusLabel;
  readonly statusClass = tramiteStatusClass;
  readonly priorityLabel = tramitePriorityLabel;
  readonly priorityClass = tramitePriorityClass;
  readonly taskStatusLabel = tramiteTaskStatusLabel;
  readonly displayDescription = tramiteDescription;
  readonly displayRequester = tramiteRequesterName;
  readonly traceUser = traceUserName;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (!id) {
        this.router.navigate(['/tramites']);
        return;
      }
      this.load(id);
    });
  }

  load(id: string): void {
    this.loading = true;
    this.loadingSubmissions = true;
    this.error = '';
    this.formSubmissions = [];

    this.tramiteService.getById(id).subscribe({
      next: (data) => {
        this.tramite = data;
        this.loading = false;
        this.loadFormSubmissions(id);
      },
      error: (err) => {
        this.error = httpErrorMessage(err, 'No se pudo cargar el detalle del trámite');
        this.loading = false;
        this.loadingSubmissions = false;
      },
    });
  }

  loadFormSubmissions(tramiteId: string): void {
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
    return !!this.tramite && (this.tramite.status === 'INICIADO' || this.tramite.status === 'EN_PROCESO');
  }

  canCancel(): boolean {
    return this.canAdvance();
  }

  formatDate(value?: string): string {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-BO');
  }

  traceTitle(item: { eventLabel?: string; activityName?: string }): string {
    return item.eventLabel || item.activityName || 'Evento';
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
