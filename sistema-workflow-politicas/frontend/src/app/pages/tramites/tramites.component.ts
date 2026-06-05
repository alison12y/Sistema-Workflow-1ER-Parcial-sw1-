import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PolicyService } from '../../services/policy.service';
import { TramiteService } from '../../services/tramite.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { BusinessPolicy } from '../../models/auth.model';
import { UserDto } from '../../models/api.models';
import { Tramite } from '../../models/tramite.model';
import {
  httpErrorMessage,
  isDemoUsername,
  tramiteDescription,
  tramitePriorityClass,
  tramitePriorityLabel,
  tramiteRequesterName,
  tramiteStatusClass,
  tramiteStatusLabel,
} from '../../utils/tramite-display.util';

@Component({
  selector: 'app-tramites',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tramites.component.html',
  styleUrl: './tramites.component.scss',
})
export class TramitesComponent implements OnInit {
  private readonly tramiteService = inject(TramiteService);
  private readonly policyService = inject(PolicyService);
  private readonly userService = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  tramites: Tramite[] = [];
  activePolicies: BusinessPolicy[] = [];
  users: UserDto[] = [];
  loading = true;
  saving = false;
  advancingId: string | null = null;
  cancellingId: string | null = null;
  deletingId: string | null = null;
  modalOpen = false;
  deleteModalOpen = false;
  tramiteToDelete: Tramite | null = null;
  readonly deleteConfirmMessage =
    '¿Está seguro de eliminar este trámite? Esta acción no se puede deshacer.';
  message = '';
  error = '';

  form = {
    policyId: '',
    description: '',
    priority: 'NORMAL',
    requestedBy: '',
  };

  readonly priorities = [
    { value: 'BAJA', label: 'Baja' },
    { value: 'NORMAL', label: 'Normal' },
    { value: 'ALTA', label: 'Alta' },
    { value: 'URGENTE', label: 'Urgente' },
  ];

  readonly statusLabel = tramiteStatusLabel;
  readonly statusClass = tramiteStatusClass;
  readonly priorityLabel = tramitePriorityLabel;
  readonly priorityClass = tramitePriorityClass;
  readonly displayDescription = tramiteDescription;
  readonly displayRequester = tramiteRequesterName;

  ngOnInit(): void {
    this.load();
    this.loadPolicies();
    this.loadUsers();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.tramiteService.getAll().subscribe({
      next: (data) => {
        this.tramites = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = httpErrorMessage(err, 'No se pudieron cargar los trámites');
        this.loading = false;
      },
    });
  }

  loadPolicies(): void {
    this.policyService.getAll().subscribe({
      next: (data) => {
        this.activePolicies = data.filter((p) => p.status === 'ACTIVE');
      },
      error: () => {
        this.activePolicies = [];
      },
    });
  }

  loadUsers(): void {
    this.userService.getAll().subscribe({
      next: (data) => {
        this.users = data.filter((u) => u.active !== false && !isDemoUsername(u.username));
        const current = this.auth.getCurrentUser();
        if (current?.username && this.users.some((u) => u.username === current.username)) {
          this.form.requestedBy = current.username;
        } else if (this.users.length) {
          this.form.requestedBy = this.users[0].username;
        }
      },
      error: () => {
        this.users = [];
      },
    });
  }

  openCreate(): void {
    this.form = {
      policyId: '',
      description: '',
      priority: 'NORMAL',
      requestedBy: this.form.requestedBy || this.auth.getCurrentUser()?.username || '',
    };
    this.error = '';
    this.modalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
    this.error = '';
  }

  userDisplay(u: UserDto): string {
    return u.fullName?.trim() ? `${u.fullName} (${u.username})` : u.username;
  }

  createTramite(): void {
    if (!this.form.policyId) {
      this.error = 'Debe seleccionar una política';
      return;
    }
    if (!this.form.description.trim()) {
      this.error = 'La descripción del trámite es obligatoria';
      return;
    }
    if (!this.form.requestedBy) {
      this.error = 'Debe seleccionar un solicitante';
      return;
    }

    this.saving = true;
    this.error = '';
    this.tramiteService
      .create({
        policyId: this.form.policyId,
        description: this.form.description.trim(),
        priority: this.form.priority,
        requestedBy: this.form.requestedBy,
      })
      .subscribe({
        next: (tramite) => {
          this.saving = false;
          this.modalOpen = false;
          this.message = 'Trámite creado correctamente';
          this.load();
          if (tramite.id) {
            this.router.navigate(['/tramites', tramite.id]);
          }
        },
        error: (err) => {
          this.saving = false;
          this.error = httpErrorMessage(err, 'No se pudo crear el trámite');
        },
      });
  }

  viewDetail(tramite: Tramite): void {
    if (tramite.id) {
      this.router.navigate(['/tramites', tramite.id]);
    }
  }

  advanceTramite(tramite: Tramite, event: Event): void {
    event.stopPropagation();
    if (!tramite.id || !this.canAdvance(tramite)) {
      return;
    }
    this.advancingId = tramite.id;
    this.tramiteService.advance(tramite.id).subscribe({
      next: () => {
        this.advancingId = null;
        this.message = `Trámite ${tramite.code} avanzado correctamente`;
        this.load();
      },
      error: (err) => {
        this.advancingId = null;
        this.error = httpErrorMessage(err, 'No se pudo avanzar el trámite');
      },
    });
  }

  cancelTramite(tramite: Tramite, event: Event): void {
    event.stopPropagation();
    if (!tramite.id || !this.canCancel(tramite)) {
      return;
    }
    if (!confirm(`¿Cancelar el trámite ${tramite.code}?`)) {
      return;
    }
    this.cancellingId = tramite.id;
    this.tramiteService.cancel(tramite.id, { comment: 'Cancelado desde el listado' }).subscribe({
      next: () => {
        this.cancellingId = null;
        this.message = `Trámite ${tramite.code} cancelado`;
        this.load();
      },
      error: (err) => {
        this.cancellingId = null;
        this.error = httpErrorMessage(err, 'No se pudo cancelar el trámite');
      },
    });
  }

  canAdvance(tramite: Tramite): boolean {
    // F1: enrutamiento automático al completar actividades; avance manual solo administrador
    return (
      this.auth.isAdmin() &&
      (tramite.status === 'INICIADO' || tramite.status === 'EN_PROCESO')
    );
  }

  canCancel(tramite: Tramite): boolean {
    return tramite.status === 'INICIADO' || tramite.status === 'EN_PROCESO';
  }

  canDelete(tramite: Tramite): boolean {
    return tramite.status === 'COMPLETADO' || tramite.status === 'CANCELADO';
  }

  openDeleteModal(tramite: Tramite, event: Event): void {
    event.stopPropagation();
    if (!tramite.id || !this.canDelete(tramite)) {
      return;
    }
    this.tramiteToDelete = tramite;
    this.deleteModalOpen = true;
    this.error = '';
  }

  closeDeleteModal(): void {
    this.deleteModalOpen = false;
    this.tramiteToDelete = null;
  }

  confirmDelete(): void {
    if (!this.tramiteToDelete?.id) {
      return;
    }
    const tramite = this.tramiteToDelete;
    this.deletingId = tramite.id!;
    this.error = '';
    this.tramiteService.delete(tramite.id!).subscribe({
      next: () => {
        this.deletingId = null;
        this.closeDeleteModal();
        this.message = `Trámite ${tramite.code} eliminado`;
        this.load();
      },
      error: (err) => {
        this.deletingId = null;
        this.error = httpErrorMessage(err, 'No se pudo eliminar el trámite');
      },
    });
  }

  formatDate(value?: string): string {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-BO');
  }

  truncate(text: string | undefined, max = 60): string {
    const value = text?.trim();
    if (!value || value === '—') return '—';
    return value.length > max ? `${value.slice(0, max)}…` : value;
  }
}
