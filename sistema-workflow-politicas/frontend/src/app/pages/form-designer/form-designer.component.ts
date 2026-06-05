import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PolicyService } from '../../services/policy.service';
import { FormDesignerService } from '../../services/form-designer.service';
import { ActivityDiagramService } from '../../services/activity-diagram.service';
import { BusinessPolicy } from '../../models/auth.model';
import { FormDesignerField } from '../../models/form.model';
import {
  DEFAULT_POLICY_ACTIVITIES,
  extractActivitiesFromDiagram,
} from '../../utils/policy-activities.util';
import {
  ensureUniqueTechnicalName,
  slugFieldNameFromLabel,
  validateTechnicalName,
} from '../../utils/form-field-key.util';

const SELECT_DEFAULT_OPTIONS = 'Personal, Académico, Médico, Laboral, Otro';

@Component({
  selector: 'app-form-designer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './form-designer.component.html',
  styleUrl: './form-designer.component.scss',
})
export class FormDesignerComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly policyService = inject(PolicyService);
  private readonly formDesignerService = inject(FormDesignerService);
  private readonly activityDiagramService = inject(ActivityDiagramService);

  policyId: string | null = null;
  policy: BusinessPolicy | null = null;
  activityName = DEFAULT_POLICY_ACTIVITIES[0];
  availableActivities: string[] = [...DEFAULT_POLICY_ACTIVITIES];
  formId: string | null = null;

  fields: FormDesignerField[] = [];
  loading = true;
  loadingActivities = true;
  saving = false;
  message = '';
  error = '';

  fieldTypes = [
    { type: 'text', label: 'Caja de texto', icon: 'abc' },
    { type: 'textarea', label: 'Área de texto', icon: 'notes' },
    { type: 'select', label: 'Selector', icon: 'list' },
    { type: 'checkbox', label: 'Checkbox', icon: 'check_box' },
    { type: 'date', label: 'Fecha', icon: 'calendar_today' },
    { type: 'file', label: 'Archivo', icon: 'attach_file' },
  ];

  ngOnInit(): void {
    this.policyId = this.route.snapshot.paramMap.get('id');
    const activityParam = this.route.snapshot.queryParamMap.get('activity');
    if (activityParam?.trim()) {
      this.activityName = activityParam.trim();
    }

    if (!this.policyId) {
      this.loading = false;
      this.loadingActivities = false;
      this.error = 'No se encontró la política asociada';
      return;
    }

    this.policyService.getById(this.policyId).subscribe({
      next: (p) => (this.policy = p),
      error: () => (this.error = 'No se pudo cargar la política'),
    });

    this.loadActivities();
  }

  loadActivities(): void {
    if (!this.policyId) {
      return;
    }

    this.loadingActivities = true;
    this.activityDiagramService.getByPolicy(this.policyId).subscribe({
      next: (diagram) => {
        this.availableActivities = extractActivitiesFromDiagram(diagram);
        this.ensureSelectedActivity();
        this.loadingActivities = false;
        this.loadSavedForm();
      },
      error: () => {
        this.availableActivities = [...DEFAULT_POLICY_ACTIVITIES];
        this.ensureSelectedActivity();
        this.loadingActivities = false;
        this.loadSavedForm();
      },
    });
  }

  onActivityChange(): void {
    this.loading = true;
    this.message = '';
    this.error = '';
    this.loadSavedForm();
  }

  loadSavedForm(): void {
    if (!this.policyId) {
      return;
    }

    this.formDesignerService.getByPolicyAndActivity(this.policyId, this.activityName).subscribe({
      next: (form) => {
        this.formId = form.id ?? null;
        this.fields = (form.fields ?? []).map((field) => ({
          type: field.type,
          label: field.label,
          name: field.name,
          required: field.required,
          options: field.options,
        }));
        this.loading = false;
      },
      error: () => {
        this.fields = [];
        this.loading = false;
      },
    });
  }

  addField(type: string): void {
    this.fields.push({
      type,
      label: '',
      name: '',
      required: false,
      options: type === 'select' ? SELECT_DEFAULT_OPTIONS : undefined,
    });
    this.error = '';
  }

  onFieldLabelChange(field: FormDesignerField): void {
    if (!field.label?.trim()) {
      return;
    }
    if (!field.name?.trim()) {
      field.name = slugFieldNameFromLabel(field.label);
    }
  }

  removeField(index: number): void {
    this.fields.splice(index, 1);
  }

  getFieldPlaceholder(fieldType: string): string {
    const type = fieldType.toLowerCase();
    if (type === 'textarea') return 'Ej: Motivo de la solicitud';
    if (type === 'select') return 'Ej: Tipo de permiso';
    if (type === 'checkbox') return 'Ej: Requiere aprobación del jefe';
    if (type === 'date') return 'Ej: Fecha de solicitud';
    if (type === 'file') return 'Ej: Documento de respaldo';
    return 'Ej: Nombre del solicitante';
  }

  getFieldHelpText(fieldType: string): string {
    const type = fieldType.toLowerCase();
    if (type === 'textarea') return 'Ingrese el texto descriptivo que verá el usuario.';
    if (type === 'select') return 'Defina las opciones separadas por coma.';
    if (type === 'checkbox') return 'Indique la etiqueta de la opción a marcar.';
    if (type === 'date') return 'Este campo permitirá seleccionar una fecha.';
    if (type === 'file') return 'Este campo permitirá adjuntar un documento.';
    return 'Ingrese el nombre que verá el usuario.';
  }

  getSelectOptions(options?: string): string[] {
    if (!options?.trim()) {
      return ['Seleccione una opción'];
    }
    return options
      .split(',')
      .map((opt) => opt.trim())
      .filter(Boolean);
  }

  save(): void {
    this.error = '';
    this.message = '';

    if (!this.policyId) {
      this.error = 'No se encontró la política asociada';
      return;
    }
    if (!this.activityName?.trim()) {
      this.error = 'Debe seleccionar una actividad asociada';
      return;
    }
    if (!this.fields.length) {
      this.error = 'Debe agregar al menos un campo al formulario';
      return;
    }

    const hasEmptyLabel = this.fields.some((field) => !field.label?.trim());
    if (hasEmptyLabel) {
      this.error = 'Todos los campos deben tener una etiqueta';
      return;
    }

    const usedNames = new Set<string>();
    let payloadFields;
    try {
      payloadFields = this.fields.map((field, index) => {
        const baseName = field.name?.trim()
          ? field.name.trim().toLowerCase()
          : slugFieldNameFromLabel(field.label);
        const nameError = validateTechnicalName(baseName);
        if (nameError) {
          throw new Error(nameError);
        }
        const name = ensureUniqueTechnicalName(baseName, usedNames);

        return {
          label: field.label.trim(),
          name,
          type: field.type,
          required: !!field.required,
          options: field.options?.trim() || undefined,
          order: index,
        };
      });
    } catch (err) {
      this.error = err instanceof Error ? err.message : 'Revise los nombres técnicos de los campos';
      return;
    }

    this.saving = true;
    this.formDesignerService
      .saveForm({
        policyId: this.policyId,
        activityName: this.activityName.trim(),
        name: `Formulario - ${this.activityName.trim()}`,
        fields: payloadFields,
      })
      .subscribe({
        next: (saved) => {
          this.saving = false;
          this.formId = saved.id ?? null;
          this.message = `Formulario guardado correctamente para ${this.activityName}`;
          setTimeout(() => (this.message = ''), 5000);
        },
        error: (err) => {
          this.saving = false;
          this.error =
            err instanceof Error
              ? err.message
              : (err.error?.message ?? 'No se pudo guardar el formulario');
        },
      });
  }

  cancel(): void {
    this.router.navigate(['/policies']);
  }

  private ensureSelectedActivity(): void {
    if (!this.availableActivities.includes(this.activityName)) {
      this.activityName = this.availableActivities[0];
    }
  }

}
