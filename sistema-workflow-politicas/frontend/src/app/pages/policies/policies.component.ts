import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PolicyService } from '../../services/policy.service';
import { AuthService } from '../../services/auth.service';
import { PolicySummary } from '../../models/workflow.model';

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

  policies: PolicySummary[] = [];
  loading = true;
  saving = false;
  modalOpen = false;
  editingId: string | null = null;
  searchTerm = '';
  message = '';
  error = '';

  form = this.emptyForm();
  readonly canViewDesigner = this.auth.canViewWorkflowDesigner();

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    const term = this.searchTerm.trim();
    this.policyService.getSummaries(term || undefined).subscribe({
      next: (p) => {
        this.policies = p;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar las políticas de negocio';
        this.loading = false;
      },
    });
  }

  onSearch(): void { this.load(); }
  clearSearch(): void { this.searchTerm = ''; this.load(); }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    const user = this.auth.getCurrentUser();
    this.form.responsible = user?.fullName ?? user?.username ?? '';
    this.error = '';
    this.modalOpen = true;
  }

  openEdit(p: PolicySummary): void {
    this.editingId = p.id ?? null;
    this.form = {
      name: p.name,
      description: p.description ?? '',
      type: p.type ?? 'GENERAL_REQUEST',
      responsible: p.responsible ?? '',
      version: p.version ?? '1.0',
      status: p.status ?? 'DRAFT',
    };
    this.error = '';
    this.modalOpen = true;
  }

  closeModal(): void { this.modalOpen = false; this.error = ''; }

  save(): void {
    this.error = '';
    if (!this.form.name?.trim() || this.form.name.trim().length < 3) {
      this.error = 'El nombre es obligatorio (mínimo 3 caracteres)';
      return;
    }
    if (!this.form.description?.trim()) {
      this.error = 'La descripción es obligatoria';
      return;
    }

    this.saving = true;
    const user = this.auth.getCurrentUser();
    const body = {
      ...this.form,
      name: this.form.name.trim(),
      description: this.form.description!.trim(),
      responsible: this.form.responsible?.trim(),
      createdBy: user?.username || 'system',
    };

    const req = this.editingId
      ? this.policyService.update(this.editingId, body)
      : this.policyService.create(body);

    req.subscribe({
      next: () => {
        this.saving = false;
        this.message = this.editingId ? 'Política actualizada' : 'Política creada';
        this.modalOpen = false;
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message || 'Error al guardar la política';
      },
    });
  }

  activate(p: PolicySummary): void {
    if (!p.id) return;
    this.policyService.activate(p.id).subscribe({
      next: () => { this.message = 'Política activada'; this.load(); },
      error: () => (this.error = 'No se pudo activar'),
    });
  }

  deactivate(p: PolicySummary): void {
    if (!p.id) return;
    this.policyService.deactivate(p.id).subscribe({
      next: () => { this.message = 'Política desactivada'; this.load(); },
      error: () => (this.error = 'No se pudo desactivar'),
    });
  }

  remove(p: PolicySummary): void {
    if (!p.id || !confirm(`¿Eliminar la política "${p.name}"?`)) return;
    this.policyService.delete(p.id).subscribe({
      next: () => { this.message = 'Política eliminada'; this.load(); },
      error: () => (this.error = 'No se pudo eliminar'),
    });
  }

  statusLabel(status?: string): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'ACTIVE') return 'Activa';
    if (s === 'INACTIVE') return 'Inactiva';
    if (s === 'DRAFT' || s === 'BORRADOR') return 'Borrador';
    return s || 'Borrador';
  }

  statusBadgeClass(status?: string): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'ACTIVE') return 'active';
    if (s === 'INACTIVE') return 'inactive';
    return 'draft';
  }

  private emptyForm() {
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
