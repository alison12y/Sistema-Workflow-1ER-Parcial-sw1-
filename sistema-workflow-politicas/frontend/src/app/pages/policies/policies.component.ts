import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PolicyService } from '../../services/policy.service';
import { AuthService } from '../../services/auth.service';
import { BusinessPolicy } from '../../models/auth.model';

@Component({
  selector: 'app-policies',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './policies.component.html',
  styleUrl: './policies.component.scss',
})
export class PoliciesComponent implements OnInit {
  private readonly policyService = inject(PolicyService);
  private readonly auth = inject(AuthService);

  policies: BusinessPolicy[] = [];
  loading = true;
  saving = false;
  modalOpen = false;
  viewModalOpen = false;
  viewingPolicy: BusinessPolicy | null = null;
  editingId: string | null = null;
  searchTerm = '';
  message = '';
  error = '';

  form: BusinessPolicy = this.emptyForm();

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    const term = this.searchTerm.trim();
    const request = term ? this.policyService.search(term) : this.policyService.getAll();

    request.subscribe({
      next: (p) => {
        this.policies = p;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar las políticas';
        this.loading = false;
      },
    });
  }

  onSearch(): void {
    this.load();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.load();
  }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    const user = this.auth.getCurrentUser();
    this.form.responsible = user?.fullName ?? user?.username ?? '';
    this.form.version = '1.0';
    this.error = '';
    this.modalOpen = true;
  }

  openView(p: BusinessPolicy): void {
    this.viewingPolicy = p;
    this.viewModalOpen = true;
  }

  closeViewModal(): void {
    this.viewModalOpen = false;
    this.viewingPolicy = null;
  }

  openEdit(p: BusinessPolicy): void {
    this.editingId = p.id ?? null;
    this.form = {
      ...p,
      responsible: p.responsible ?? p.createdBy ?? '',
      version: p.version ?? '1.0',
    };
    this.error = '';
    this.modalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
    this.error = '';
  }

  save(): void {
    this.error = '';

    if (!this.form.name?.trim()) {
      this.error = 'El nombre es obligatorio';
      return;
    }
    if (this.form.name.trim().length < 3) {
      this.error = 'El nombre debe tener al menos 3 caracteres';
      return;
    }
    if (!this.form.description?.trim()) {
      this.error = 'La descripción es obligatoria';
      return;
    }
    if (!this.form.responsible?.trim()) {
      this.error = 'El responsable es obligatorio';
      return;
    }
    if (!this.form.version?.trim()) {
      this.error = 'La versión es obligatoria';
      return;
    }

    this.saving = true;
    const user = this.auth.getCurrentUser();
    const body: BusinessPolicy = {
      ...this.form,
      name: this.form.name.trim(),
      description: this.form.description.trim(),
      responsible: this.form.responsible.trim(),
      version: this.form.version.trim(),
      createdBy: user?.username || 'system',
    };

    if (this.editingId) {
      this.policyService.update(this.editingId, body).subscribe({
        next: () => this.afterSave('Política actualizada correctamente'),
        error: (err) => this.onSaveError(err),
      });
    } else {
      this.policyService.create(body).subscribe({
        next: () => this.afterSave('Política guardada correctamente'),
        error: (err) => this.onSaveError(err),
      });
    }
  }

  remove(p: BusinessPolicy): void {
    if (!p.id || !confirm(`¿Eliminar la política "${p.name}"?`)) return;
    this.policyService.delete(p.id).subscribe({
      next: () => {
        this.message = 'Política eliminada correctamente';
        this.load();
        setTimeout(() => (this.message = ''), 5000);
      },
      error: (err) => {
        this.error =
          err.status === 403
            ? 'No tienes permisos para eliminar políticas'
            : 'No se pudo eliminar la política';
      },
    });
  }

  typeLabel(type?: string): string {
    const map: Record<string, string> = {
      GENERAL_REQUEST: 'Solicitud General',
      PURCHASE_REQUEST: 'Solicitud de Compra',
      LEAVE_REQUEST: 'Permiso de Ausencia',
      DOCUMENT_APPROVAL: 'Aprobación de Documento',
      CLAIM_ATTENTION: 'Atención de Reclamo',
    };
    return map[type ?? ''] ?? type ?? '—';
  }

  statusLabel(status?: string): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'ACTIVE') return 'Activa';
    if (s === 'INACTIVE') return 'Inactiva';
    if (s === 'VERSIONED') return 'Versionada';
    if (s === 'DRAFT' || s === 'BORRADOR') return 'Borrador';
    return s || 'Borrador';
  }

  statusBadgeClass(status?: string): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'ACTIVE') return 'active';
    if (s === 'INACTIVE') return 'inactive';
    if (s === 'VERSIONED') return 'versioned';
    return 'draft';
  }

  private afterSave(msg: string): void {
    this.saving = false;
    this.message = msg;
    this.modalOpen = false;
    this.load();
    setTimeout(() => (this.message = ''), 5000);
  }

  private onSaveError(err: { status?: number; error?: { message?: string } }): void {
    this.saving = false;
    if (err.status === 403) {
      this.error =
        'No tienes permisos suficientes para guardar políticas. Inicia sesión con un rol Administrador o Diseñador de Políticas.';
      return;
    }
    if (err.error?.message) {
      this.error = err.error.message;
      return;
    }
    this.error = 'Error al guardar la política. Inténtalo de nuevo.';
  }

  private emptyForm(): BusinessPolicy {
    return {
      name: '',
      description: '',
      type: 'GENERAL_REQUEST',
      responsible: '',
      version: '1.0',
      status: 'DRAFT',
    };
  }
}
