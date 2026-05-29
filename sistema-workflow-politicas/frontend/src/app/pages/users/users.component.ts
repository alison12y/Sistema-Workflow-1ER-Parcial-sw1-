import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { RoleService } from '../../services/role.service';
import { DepartmentService } from '../../services/department.service';
import { UserDto, UserRequest } from '../../models/api.models';
import { Role } from '../../models/auth.model';
import { Department } from '../../models/auth.model';
import { getRoleDisplayName } from '../../shared/utils/role-display.util';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
})
export class UsersComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly roleService = inject(RoleService);
  private readonly departmentService = inject(DepartmentService);

  users: UserDto[] = [];
  roles: Role[] = [];
  departments: Department[] = [];
  loading = true;
  saving = false;
  modalOpen = false;
  editingId: string | null = null;
  message = '';
  error = '';

  form: UserRequest = this.emptyForm();

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.roleService.getAll().subscribe({
      next: (r) => (this.roles = r),
    });
    this.departmentService.getAll().subscribe({
      next: (d) => (this.departments = d),
    });
    this.userService.getAll().subscribe({
      next: (u) => {
        this.users = u;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar los usuarios';
        this.loading = false;
      },
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    this.modalOpen = true;
  }

  openEdit(user: UserDto): void {
    this.editingId = user.id ?? null;
    this.form = {
      username: user.username,
      fullName: user.fullName ?? '',
      email: user.email ?? '',
      password: null,
      departmentId: user.departmentId ?? '',
      roleIds: user.roleIds ? [...user.roleIds] : [],
      active: user.active ?? true,
    };
    this.modalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
  }

  save(): void {
    this.error = '';
    this.message = '';
    const body: UserRequest = {
      username: this.form.username.trim(),
      fullName: this.form.fullName?.trim(),
      email: this.form.email?.trim(),
      departmentId: this.form.departmentId || undefined,
      roleIds: this.form.roleIds?.length ? this.form.roleIds : undefined,
      active: this.form.active,
      password: this.form.password?.trim() || null,
    };

    if (!body.username) {
      this.error = 'El usuario es obligatorio';
      return;
    }
    if (body.username.length < 3) {
      this.error = 'El usuario debe tener al menos 3 caracteres';
      return;
    }
    if (body.email && !this.isValidEmail(body.email)) {
      this.error = 'Ingrese un correo electrónico válido';
      return;
    }
    if (!this.editingId && !body.password) {
      this.error = 'La contraseña es obligatoria al crear';
      return;
    }

    this.saving = true;
    const req = this.editingId
      ? this.userService.update(this.editingId, body)
      : this.userService.create(body);

    req.subscribe({
      next: () => {
        this.saving = false;
        this.message = this.editingId ? 'Usuario actualizado' : 'Usuario creado';
        this.modalOpen = false;
        this.loadAll();
      },
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message || 'Error al guardar el usuario';
      },
    });
  }

  remove(user: UserDto): void {
    if (!user.id || !confirm(`¿Eliminar usuario ${user.username}?`)) return;
    this.userService.delete(user.id).subscribe({
      next: () => {
        this.message = 'Usuario eliminado';
        this.loadAll();
      },
      error: () => (this.error = 'No se pudo eliminar'),
    });
  }

  deptName(id?: string): string {
    if (!id) return '—';
    const dept = this.departments.find((d) => d.id === id);
    return dept?.name ?? '—';
  }

  roleNames(ids?: string[]): string {
    if (!ids?.length) return '—';
    const names = new Set<string>();
    ids.forEach((id) => {
      const role = this.roles.find((r) => r.id === id);
      if (role?.name) {
        names.add(getRoleDisplayName(role.name));
      }
    });
    return names.size ? Array.from(names).join(', ') : '—';
  }

  getPrettyRoleName(name: string): string {
    return getRoleDisplayName(name);
  }

  get activeRoles(): Role[] {
    return this.roles.filter((r) => r.active !== false);
  }

  toggleRole(roleId: string): void {
    if (!this.form.roleIds) this.form.roleIds = [];
    const index = this.form.roleIds.indexOf(roleId);
    if (index === -1) {
      this.form.roleIds.push(roleId);
    } else {
      this.form.roleIds.splice(index, 1);
    }
  }

  isRoleSelected(roleId: string): boolean {
    return this.form.roleIds?.includes(roleId) ?? false;
  }

  private emptyForm(): UserRequest {
    return {
      username: '',
      fullName: '',
      email: '',
      password: null,
      departmentId: '',
      roleIds: [],
      active: true,
    };
  }

  private isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }
}
