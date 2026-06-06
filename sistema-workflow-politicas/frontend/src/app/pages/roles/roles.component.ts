import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RoleService } from '../../services/role.service';
import { Role } from '../../models/auth.model';
import { getRoleDisplayName as formatRoleDisplayName } from '../../shared/utils/role-display.util';

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

  availablePermissions = [
    { code: 'USERS_MANAGE', name: 'Gestionar usuarios' },
    { code: 'ROLES_MANAGE', name: 'Gestionar roles' },
    { code: 'DEPARTMENTS_MANAGE', name: 'Gestionar departamentos' },
    { code: 'SETTINGS_MANAGE', name: 'Gestionar configuración' },
    { code: 'POLICIES_MANAGE', name: 'Gestionar políticas de negocio' },
    { code: 'WORKFLOW_MANAGE', name: 'Gestionar workflow' },
    { code: 'WORKFLOW_DESIGN', name: 'Diseñar conexiones del workflow' },
    { code: 'WORKFLOW_VIEW', name: 'Ver actividades del workflow' },
    { code: 'REPORTS_VIEW', name: 'Ver reportes' },
    { code: 'AUDIT_VIEW', name: 'Ver auditoría' },
    { code: 'KPI_VIEW', name: 'Ver KPIs' },
    { code: 'MONITORING_VIEW', name: 'Ver monitoreo' },
    { code: 'FORMS_MANAGE', name: 'Gestionar formularios dinámicos' },
    { code: 'TASKS_EXECUTE', name: 'Ejecutar tareas asignadas' },
    { code: 'AI_ASSIST', name: 'Usar asistente IA' },
    { code: 'AI_AGENT_USE', name: 'Usar agente inteligente' },
    { code: 'INTELLIGENT_ANALYTICS_VIEW', name: 'Ver analítica inteligente' },
    { code: 'DOCUMENTS_VIEW', name: 'Ver documentos' },
    { code: 'DOCUMENTS_UPLOAD', name: 'Subir documentos' },
    { code: 'DOCUMENTS_DELETE', name: 'Eliminar documentos' },
  ];

  getPermissionName(code: string): string {
    return this.availablePermissions.find(p => p.code === code)?.name || code;
  }

  getRoleDisplayName(name: string): string {
    return formatRoleDisplayName(name);
  }

  togglePermission(code: string): void {
    if (!this.form.permissionIds) this.form.permissionIds = [];
    const index = this.form.permissionIds.indexOf(code);
    if (index === -1) {
      this.form.permissionIds.push(code);
    } else {
      this.form.permissionIds.splice(index, 1);
    }
  }

  isPermissionSelected(code: string): boolean {
    return this.form.permissionIds?.includes(code) ?? false;
  }

  openEdit(role: Role): void {
    this.editingId = role.id ?? null;
    this.form = {
      name: role.name,
      description: role.description ?? '',
      permissionIds: role.permissionIds ? [...role.permissionIds] : [],
      active: role.active ?? true,
    };
    this.modalOpen = true;
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
    const body: Role = {
      name: this.form.name.trim(),
      description: this.form.description?.trim(),
      permissionIds: this.form.permissionIds,
      active: this.form.active ?? true,
    };

    this.saving = true;
    const req = this.editingId
      ? this.roleService.update(this.editingId, body)
      : this.roleService.create(body);

    req.subscribe({
      next: () => {
        this.saving = false;
        this.message = this.editingId ? 'Rol actualizado correctamente' : 'Rol creado correctamente';
        this.modalOpen = false;
        this.load();
        setTimeout(() => (this.message = ''), 5000);
      },
      error: () => {
        this.saving = false;
        this.error = 'Error al guardar el rol';
      },
    });
  }

  closeModal(): void {
    this.modalOpen = false;
  }

  remove(role: Role): void {
    if (!role.id || !confirm(`¿Eliminar rol ${role.name}?`)) return;
    this.roleService.delete(role.id).subscribe({
      next: () => {
        this.message = 'Rol eliminado correctamente';
        this.load();
        setTimeout(() => (this.message = ''), 5000);
      },
      error: () => (this.error = 'No se pudo eliminar'),
    });
  }

  private emptyForm(): Role {
    return { name: '', description: '', permissionIds: [], active: true };
  }
}
