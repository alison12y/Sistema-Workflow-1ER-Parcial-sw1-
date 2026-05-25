import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RoleService } from '../../services/role.service';
import { Role } from '../../models/auth.model';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './roles.component.html',
  styleUrl: './roles.component.scss',
})
export class RolesComponent implements OnInit {
  private readonly roleService = inject(RoleService);

  roles: Role[] = [];
  loading = true;
  saving = false;
  modalOpen = false;
  editingId: string | null = null;
  message = '';
  error = '';

  form: Role & { permissionsText?: string } = this.emptyForm();

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.roleService.getAll().subscribe({
      next: (r) => {
        this.roles = r;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar los roles';
        this.loading = false;
      },
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    this.modalOpen = true;
  }

  openEdit(role: Role): void {
    this.editingId = role.id ?? null;
    this.form = {
      name: role.name,
      description: role.description ?? '',
      permissionIds: role.permissionIds ? [...role.permissionIds] : [],
      permissionsText: role.permissionIds?.join(', ') ?? '',
    };
    this.modalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
  }

  save(): void {
    this.error = '';
    if (!this.form.name?.trim()) {
      this.error = 'El nombre es obligatorio';
      return;
    }
    const body: Role = {
      name: this.form.name.trim(),
      description: this.form.description?.trim(),
      permissionIds: this.parsePermissions(this.form.permissionsText ?? ''),
    };

    this.saving = true;
    const req = this.editingId
      ? this.roleService.update(this.editingId, body)
      : this.roleService.create(body);

    req.subscribe({
      next: () => {
        this.saving = false;
        this.message = this.editingId ? 'Rol actualizado' : 'Rol creado';
        this.modalOpen = false;
        this.load();
      },
      error: () => {
        this.saving = false;
        this.error = 'Error al guardar el rol';
      },
    });
  }

  remove(role: Role): void {
    if (!role.id || !confirm(`¿Eliminar rol ${role.name}?`)) return;
    this.roleService.delete(role.id).subscribe({
      next: () => {
        this.message = 'Rol eliminado';
        this.load();
      },
      error: () => (this.error = 'No se pudo eliminar'),
    });
  }

  private parsePermissions(text: string): string[] {
    return text
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
  }

  private emptyForm(): Role & { permissionsText?: string } {
    return { name: '', description: '', permissionIds: [], permissionsText: '' };
  }
}
