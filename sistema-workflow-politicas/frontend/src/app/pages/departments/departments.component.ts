import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DepartmentService } from '../../services/department.service';
import { Department } from '../../models/auth.model';

type DeptForm = Department & { status: string };

@Component({
  selector: 'app-departments',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './departments.component.html',
  styleUrl: './departments.component.scss',
})
export class DepartmentsComponent implements OnInit {
  private readonly departmentService = inject(DepartmentService);

  departments: Department[] = [];
  loading = true;
  saving = false;
  modalOpen = false;
  editingId: string | null = null;
  message = '';
  error = '';

  form: DeptForm = this.emptyForm();

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.departmentService.getAll().subscribe({
      next: (d) => {
        this.departments = d;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar los departamentos';
        this.loading = false;
      },
    });
  }

  displayStatus(d: Department): string {
    return d.managerId === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE';
  }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    this.modalOpen = true;
  }

  openEdit(d: Department): void {
    this.editingId = d.id ?? null;
    const inactive = d.managerId === 'INACTIVE';
    this.form = {
      name: d.name,
      description: d.description ?? '',
      status: inactive ? 'INACTIVE' : 'ACTIVE',
      managerId: inactive ? '' : (d.managerId ?? ''),
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
    const body: Department = {
      name: this.form.name.trim(),
      description: this.form.description?.trim(),
      managerId:
        this.form.status === 'INACTIVE'
          ? 'INACTIVE'
          : this.form.managerId?.trim() || undefined,
    };

    this.saving = true;
    const req = this.editingId
      ? this.departmentService.update(this.editingId, body)
      : this.departmentService.create(body);

    req.subscribe({
      next: () => {
        this.saving = false;
        this.message = this.editingId ? 'Departamento actualizado' : 'Departamento creado';
        this.modalOpen = false;
        this.load();
      },
      error: () => {
        this.saving = false;
        this.error = 'Error al guardar';
      },
    });
  }

  remove(d: Department): void {
    if (!d.id || !confirm(`¿Eliminar departamento ${d.name}?`)) return;
    this.departmentService.delete(d.id).subscribe({
      next: () => {
        this.message = 'Departamento eliminado';
        this.load();
      },
      error: () => (this.error = 'No se pudo eliminar'),
    });
  }

  statusClass(status: string): string {
    return status === 'ACTIVE' ? 'active' : 'inactive';
  }

  private emptyForm(): DeptForm {
    return { name: '', description: '', managerId: '', status: 'ACTIVE' };
  }
}
