import { Component, ChangeDetectorRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { FormService } from '../../services/form.service';
import { FormFieldService } from '../../services/form-field.service';
import { WorkflowActivityService } from '../../services/workflow-activity.service';
import { AuthService } from '../../services/auth.service';
import { WorkflowActivity } from '../../models/workflow.model';
import {
  DynamicForm,
  DynamicFormRequest,
  FIELD_TYPE_OPTIONS,
  FormField,
  FormFieldRequest,
  fieldTypeLabel,
  isVisibleFormField,
  parseSelectOptions,
} from '../../models/form.model';
import { slugFieldNameFromLabel, validateTechnicalName } from '../../utils/form-field-key.util';

@Component({
  selector: 'app-activity-form-designer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './activity-form-designer.component.html',
  styleUrl: './activity-form-designer.component.scss',
})
export class ActivityFormDesignerComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly formService = inject(FormService);
  private readonly fieldService = inject(FormFieldService);
  private readonly activityService = inject(WorkflowActivityService);
  private readonly auth = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  activityId = '';
  activity: WorkflowActivity | null = null;
  form: DynamicForm | null = null;
  fields: FormField[] = [];

  loading = true;
  saving = false;
  error = '';
  message = '';

  formModalOpen = false;
  fieldModalOpen = false;
  editingFieldId: string | null = null;

  formData: DynamicFormRequest = this.emptyFormData();
  fieldForm: FormFieldRequest = this.emptyFieldForm();

  readonly canManage = this.auth.canManageDynamicForms();
  readonly fieldTypeOptions = FIELD_TYPE_OPTIONS;
  readonly fieldTypeLabel = fieldTypeLabel;
  readonly parseSelectOptions = parseSelectOptions;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      this.activityId = params.get('activityId') ?? '';
      if (this.activityId) {
        this.loadActivity();
        this.loadFormData();
      }
    });
  }

  get hasForm(): boolean {
    return !!this.form?.id;
  }

  get visibleFields(): FormField[] {
    return this.fields.filter(isVisibleFormField);
  }

  get previewFields(): FormField[] {
    return this.visibleFields.slice().sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
  }

  get isSelectFieldType(): boolean {
    return (this.fieldForm.fieldType ?? '').toUpperCase() === 'SELECT';
  }

  loadActivity(): void {
    this.activityService.getById(this.activityId).subscribe({
      next: (activity) => {
        this.activity = activity;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo cargar la actividad.';
        this.cdr.detectChanges();
      },
    });
  }

  loadFormData(): void {
    this.loading = true;
    this.error = '';
    this.formService.getFormByActivity(this.activityId).subscribe({
      next: (form) => {
        this.form = form.id ? form : null;
        this.fields = [...(form.fields ?? [])];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudo cargar el formulario.');
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openCreateForm(): void {
    if (!this.canManage) {
      this.error = 'No tienes permiso para diseñar formularios.';
      return;
    }
    this.formData = {
      activityId: this.activityId,
      policyId: this.activity?.policyId,
      name: this.activity ? `Formulario de ${this.activity.name}` : '',
      description: this.activity
        ? `Permite registrar la información requerida para la actividad ${this.activity.name}.`
        : '',
      active: true,
    };
    this.formModalOpen = true;
    this.error = '';
  }

  openEditForm(): void {
    if (!this.canManage || !this.form) return;
    this.formData = {
      activityId: this.activityId,
      policyId: this.form.policyId,
      name: this.form.name ?? '',
      description: this.form.description ?? '',
      active: this.form.active !== false,
    };
    this.formModalOpen = true;
    this.error = '';
  }

  closeFormModal(): void {
    this.formModalOpen = false;
  }

  saveForm(): void {
    if (!this.canManage) return;
    if (!this.formData.name?.trim()) {
      this.error = 'El nombre del formulario es obligatorio.';
      return;
    }

    this.saving = true;
    this.error = '';
    const payload: DynamicFormRequest = {
      ...this.formData,
      activityId: this.activityId,
      policyId: this.activity?.policyId ?? this.formData.policyId,
      name: this.formData.name.trim(),
      description: this.formData.description?.trim() || undefined,
    };

    const request$ = this.form?.id
      ? this.formService.updateForm(this.form.id, payload)
      : this.formService.createForm(payload);

    request$.subscribe({
      next: (saved) => {
        this.message = this.form?.id
          ? 'Formulario actualizado correctamente.'
          : 'Formulario creado correctamente.';
        this.formModalOpen = false;
        this.saving = false;
        this.form = saved;
        this.fields = [...(saved.fields ?? [])];
        this.loadActivity();
        this.loadFormData();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudo guardar el formulario.');
        this.saving = false;
        this.cdr.detectChanges();
      },
    });
  }

  openCreateField(): void {
    if (!this.canManage || !this.form?.id) return;
    this.editingFieldId = null;
    this.fieldForm = this.emptyFieldForm(this.form.id);
    this.fieldModalOpen = true;
    this.error = '';
  }

  openEditField(field: FormField): void {
    if (!this.canManage || !field.id || !this.form?.id) return;
    this.editingFieldId = field.id;
    this.fieldForm = {
      formId: this.form.id,
      label: field.label,
      name: field.name ?? '',
      fieldType: (field.fieldType ?? 'TEXT').toUpperCase(),
      required: field.required ?? false,
      options: field.options ?? '',
      orderIndex: field.orderIndex,
      placeholder: field.placeholder ?? '',
      helpText: field.helpText ?? '',
      active: field.active !== false,
    };
    this.fieldModalOpen = true;
    this.error = '';
  }

  closeFieldModal(): void {
    this.fieldModalOpen = false;
    this.editingFieldId = null;
  }

  onFieldLabelChange(): void {
    if (!this.fieldForm.label?.trim()) {
      return;
    }
    if (!this.fieldForm.name?.trim() || !this.editingFieldId) {
      this.fieldForm.name = slugFieldNameFromLabel(this.fieldForm.label);
    }
  }

  saveField(): void {
    if (!this.canManage || !this.form?.id) return;
    if (!this.fieldForm.label?.trim()) {
      this.error = 'La etiqueta del campo es obligatoria.';
      return;
    }
    const technicalName = this.fieldForm.name?.trim()
      ? this.fieldForm.name
      : slugFieldNameFromLabel(this.fieldForm.label);
    const nameError = validateTechnicalName(technicalName);
    if (nameError) {
      this.error = nameError;
      return;
    }
    if (this.isSelectFieldType && !this.fieldForm.options?.trim()) {
      this.error = 'Debe indicar opciones para la lista desplegable.';
      return;
    }

    this.saving = true;
    this.error = '';
    const payload: FormFieldRequest = {
      ...this.fieldForm,
      formId: this.form.id,
      label: this.fieldForm.label.trim(),
      name: technicalName.trim().toLowerCase(),
      fieldType: (this.fieldForm.fieldType ?? 'TEXT').toUpperCase(),
      options: this.fieldForm.options?.trim() || undefined,
      placeholder: this.fieldForm.placeholder?.trim() || undefined,
      helpText: this.fieldForm.helpText?.trim() || undefined,
    };

    const request$ = this.editingFieldId
      ? this.fieldService.updateField(this.editingFieldId, payload)
      : this.fieldService.createField(payload);

    request$.subscribe({
      next: () => {
        this.message = this.editingFieldId
          ? 'Campo actualizado correctamente.'
          : 'Campo agregado correctamente.';
        this.fieldModalOpen = false;
        this.saving = false;
        this.editingFieldId = null;
        this.refreshFields();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudo guardar el campo.');
        this.saving = false;
        this.cdr.detectChanges();
      },
    });
  }

  toggleFieldActive(field: FormField): void {
    if (!this.canManage || !field.id) return;
    const action$ =
      field.active !== false
        ? this.fieldService.deactivateField(field.id)
        : this.fieldService.activateField(field.id);

    action$.subscribe({
      next: () => {
        this.message =
          field.active !== false
            ? 'Campo desactivado correctamente.'
            : 'Campo activado correctamente.';
        this.patchFieldActive(field.id!, field.active !== false);
        this.refreshFields();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudo cambiar el estado del campo.');
      },
    });
  }

  removeField(field: FormField): void {
    if (!this.canManage || !field.id) return;
    if (!confirm(`¿Eliminar el campo "${field.label}"?`)) return;

    this.fieldService.deleteField(field.id).subscribe({
      next: (result) => {
        this.message = result.message || 'Campo eliminado correctamente.';
        this.fields = this.fields.filter((item) => item.id !== field.id);
        this.cdr.detectChanges();
        this.refreshFields();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudo eliminar el campo.');
      },
    });
  }

  formStatusText(): string {
    if (!this.form?.id) return 'Sin formulario';
    return this.form.active !== false ? 'Formulario configurado' : 'Formulario inactivo';
  }

  requiredText(required?: boolean): string {
    return required ? 'Sí' : 'No';
  }

  fieldStatusText(active?: boolean): string {
    return active === false ? 'Inactivo' : 'Activo';
  }

  private emptyFormData(): DynamicFormRequest {
    return {
      activityId: '',
      name: '',
      description: '',
      active: true,
    };
  }

  private emptyFieldForm(formId = ''): FormFieldRequest {
    return {
      formId,
      label: '',
      name: '',
      fieldType: 'TEXT',
      required: false,
      options: '',
      orderIndex: undefined,
      placeholder: '',
      helpText: '',
      active: true,
    };
  }

  private refreshFields(): void {
    if (!this.form?.id) {
      this.loadFormData();
      return;
    }
    this.fieldService.getFieldsByForm(this.form.id).subscribe({
      next: (list) => {
        this.fields = [...list];
        this.cdr.detectChanges();
      },
      error: () => this.loadFormData(),
    });
  }

  private patchFieldActive(id: string, wasActive: boolean): void {
    this.fields = this.fields.map((field) =>
      field.id === id ? { ...field, active: !wasActive } : field,
    );
    this.cdr.detectChanges();
  }

  private resolveError(err: HttpErrorResponse, fallback: string): string {
    if (err.status === 403) return 'No tienes permiso para diseñar formularios.';
    if (err.status === 404) return 'Recurso no encontrado.';
    return err.error?.message ?? fallback;
  }
}
