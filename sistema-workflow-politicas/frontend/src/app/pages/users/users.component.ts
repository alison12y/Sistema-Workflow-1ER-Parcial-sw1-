import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { RoleService } from '../../services/role.service';
import { DepartmentService } from '../../services/department.service';
import { UserDto, UserRequest } from '../../models/api.models';
import { Role } from '../../models/auth.model';
import { Department } from '../../models/auth.model';

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

  form: UserRequest & { roleIdsText?: string } = this.emptyForm();

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
      roleIdsText: user.roleIds?.join(', ') ?? '',
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
    const roleIds = this.parseRoleIds(this.form.roleIdsText ?? '');
    const body: UserRequest = {
      username: this.form.username.trim(),
      fullName: this.form.fullName?.trim(),
      email: this.form.email?.trim(),
      departmentId: this.form.departmentId || undefined,
      roleIds: roleIds.length ? roleIds : undefined,
      active: this.form.active,
      password: this.form.password?.trim() || null,
    };

    if (!body.username) {
      this.error = 'El usuario es obligatorio';
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
      error: () => {
        this.saving = false;
        this.error = 'Error al guardar el usuario';
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
    return this.departments.find((d) => d.id === id)?.name ?? id;
  }

  roleNames(ids?: string[]): string {
    if (!ids?.length) return '—';
    return ids
      .map((id) => this.roles.find((r) => r.id === id)?.name ?? id)
      .join(', ');
  }

  private parseRoleIds(text: string): string[] {
    return text
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
  }

  private emptyForm(): UserRequest & { roleIdsText?: string } {
    return {
      username: '',
      fullName: '',
      email: '',
      password: '',
      departmentId: '',
      roleIds: [],
      roleIdsText: '',
      active: true,
    };
  }
}
