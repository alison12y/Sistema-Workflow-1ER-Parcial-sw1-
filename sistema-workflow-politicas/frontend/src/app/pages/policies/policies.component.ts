import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PolicyService } from '../../services/policy.service';
import { AuthService } from '../../services/auth.service';
import { BusinessPolicy } from '../../models/auth.model';

@Component({
  selector: 'app-policies',
  standalone: true,
  imports: [FormsModule],
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
  editingId: string | null = null;
  message = '';
  error = '';

  form: BusinessPolicy = this.emptyForm();

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.policyService.getAll().subscribe({
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

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    const user = this.auth.getCurrentUser();
    this.form.createdBy = user?.username ?? '';
    this.modalOpen = true;
  }

  openEdit(p: BusinessPolicy): void {
    this.editingId = p.id ?? null;
    this.form = {
      name: p.name,
      description: p.description ?? '',
      type: p.type ?? '',
      createdBy: p.createdBy ?? '',
      status: p.status,
    };
    this.modalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
  }

  save(): void {
    this.error = '';
    if (!this.form.name?.trim() || !this.form.description?.trim() || !this.form.createdBy?.trim()) {
      this.error = 'Nombre, descripción y creador son obligatorios';
      return;
    }

    this.saving = true;
    if (this.editingId) {
      this.policyService.update(this.editingId, this.form).subscribe({
        next: () => this.afterSave('Política actualizada'),
        error: () => this.onSaveError(),
      });
    } else {
      this.policyService.create(this.form).subscribe({
        next: () => this.afterSave('Política creada'),
        error: () => this.onSaveError(),
      });
    }
  }

  activate(p: BusinessPolicy): void {
    if (!p.id) return;
    this.policyService.activate(p.id).subscribe({
      next: () => {
        this.message = 'Política activada';
        this.load();
      },
      error: () => (this.error = 'No se pudo activar'),
    });
  }

  deactivate(p: BusinessPolicy): void {
    if (!p.id) return;
    this.policyService.deactivate(p.id).subscribe({
      next: () => {
        this.message = 'Política desactivada';
        this.load();
      },
      error: () => (this.error = 'No se pudo desactivar'),
    });
  }

  remove(p: BusinessPolicy): void {
    if (!p.id || !confirm(`¿Eliminar política ${p.name}?`)) return;
    this.policyService.delete(p.id).subscribe({
      next: () => {
        this.message = 'Política eliminada';
        this.load();
      },
      error: () => (this.error = 'No se pudo eliminar'),
    });
  }

  statusBadgeClass(status?: string): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'ACTIVE') return 'active';
    if (s === 'INACTIVE') return 'inactive';
    return 'draft';
  }

  private afterSave(msg: string): void {
    this.saving = false;
    this.message = msg;
    this.modalOpen = false;
    this.load();
  }

  private onSaveError(): void {
    this.saving = false;
    this.error = 'Error al guardar la política';
  }

  private emptyForm(): BusinessPolicy {
    return {
      name: '',
      description: '',
      type: 'GENERAL_REQUEST',
      createdBy: '',
      status: 'DRAFT',
    };
  }
}
