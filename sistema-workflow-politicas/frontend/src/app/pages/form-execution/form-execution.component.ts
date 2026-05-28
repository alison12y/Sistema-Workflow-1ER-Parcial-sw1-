import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { FormDesignerService } from '../../services/form-designer.service';
import { FormSubmissionService } from '../../services/form-submission.service';
import { MyActivitiesService } from '../../services/my-activities.service';
import { AuthService } from '../../services/auth.service';
import {
  AiAssistantService,
  FormFieldSuggestion,
} from '../../services/ai-assistant.service';
import {
  FormSubmissionFileMeta,
  MyActivity,
  ResponseItemPayload,
} from '../../models/my-activities.model';
import { FormDesignerFieldPayload } from '../../models/form.model';
import { httpErrorMessage, tramitePriorityLabel } from '../../utils/tramite-display.util';
import { applySavedResponses, isFileFieldType } from '../../utils/form-submission-display.util';

@Component({
  selector: 'app-form-execution',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './form-execution.component.html',
  styleUrl: './form-execution.component.scss',
})
export class FormExecutionComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly myActivitiesService = inject(MyActivitiesService);
  private readonly formDesignerService = inject(FormDesignerService);
  private readonly formSubmissionService = inject(FormSubmissionService);
  private readonly authService = inject(AuthService);
  private readonly aiAssistant = inject(AiAssistantService);

  activity: MyActivity | null = null;
  fields: FormDesignerFieldPayload[] = [];
  values: Record<string, string> = {};
  fileSelections: Record<string, File | null> = {};
  fileAttachments: Record<string, FormSubmissionFileMeta | null> = {};
  uploadingFiles: Record<string, boolean> = {};
  loading = true;
  saving = false;
  completing = false;
  missingForm = false;
  message = '';
  error = '';

  showAiPanel = false;
  aiSuggestions: FormFieldSuggestion[] = [];
  aiGenerating = false;
  aiInfoMessage = '';
  aiSkippedMessage = '';

  readonly priorityLabel = tramitePriorityLabel;

  ngOnInit(): void {
    const tramiteId = this.route.snapshot.paramMap.get('tramiteId');
    const activityName = this.route.snapshot.queryParamMap.get('activity')?.trim() ?? '';
    const taskOrder = Number(this.route.snapshot.queryParamMap.get('taskOrder') ?? '0');

    if (!tramiteId || !activityName || taskOrder <= 0) {
      this.loading = false;
      this.error = 'No se pudo identificar la actividad a completar';
      return;
    }

    this.myActivitiesService.getById(tramiteId).subscribe({
      next: (activity) => {
        this.activity = activity;
        this.loadForm(activity.policyId, activity.activityName, activity.tramiteId, activity.taskOrder);
      },
      error: (err) => {
        this.loading = false;
        this.error = httpErrorMessage(err, 'No se pudo cargar la actividad');
      },
    });
  }

  loadForm(policyId: string, activityName: string, tramiteId: string, taskOrder: number): void {
    this.missingForm = false;
    this.formDesignerService.getByPolicyAndActivity(policyId, activityName).subscribe({
      next: (form) => {
        this.fields = (form.fields ?? []).sort((a, b) => a.order - b.order);
        if (!this.fields.length) {
          this.loading = false;
          this.missingForm = true;
          this.error = `No existe formulario configurado para la actividad ${activityName}`;
          return;
        }

        this.initializeValues();
        this.formSubmissionService.getByActivity(tramiteId, activityName, taskOrder).subscribe({
          next: (saved) => {
            if (saved?.responses?.length) {
              applySavedResponses(this.fields, this.values, this.fileAttachments, saved.responses);
            }
            this.loading = false;
          },
          error: () => {
            this.loading = false;
          },
        });
      },
      error: () => {
        this.loading = false;
        this.missingForm = true;
        this.error = `No existe formulario configurado para la actividad ${activityName}`;
      },
    });
  }

  onFileSelected(fieldKey: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.error = '';

    if (!file) {
      this.fileSelections[fieldKey] = null;
      return;
    }

    this.fileSelections[fieldKey] = file;
    this.values[fieldKey] = file.name;
    this.uploadingFiles[fieldKey] = true;

    this.formSubmissionService.uploadFile(file).subscribe({
      next: (meta) => {
        this.fileAttachments[fieldKey] = meta;
        this.fileSelections[fieldKey] = null;
        this.values[fieldKey] = meta.fileName;
        this.uploadingFiles[fieldKey] = false;
      },
      error: (err) => {
        this.fileSelections[fieldKey] = null;
        this.fileAttachments[fieldKey] = null;
        this.values[fieldKey] = '';
        this.uploadingFiles[fieldKey] = false;
        input.value = '';
        this.error = httpErrorMessage(err, 'No se pudo subir el archivo adjunto');
      },
    });
  }

  getFileLabel(fieldKey: string): string {
    const attachment = this.fileAttachments[fieldKey];
    if (attachment?.fileName) {
      return attachment.fileName;
    }
    return this.values[fieldKey] || '';
  }

  hasSelectedFile(fieldKey: string): boolean {
    return !!this.fileAttachments[fieldKey]?.fileId;
  }

  isUploadingFile(fieldKey: string): boolean {
    return !!this.uploadingFiles[fieldKey];
  }

  goToFormDesigner(): void {
    if (!this.activity) {
      return;
    }
    this.router.navigate(['/form-designer', this.activity.policyId], {
      queryParams: { activity: this.activity.activityName },
    });
  }

  saveDraft(): void {
    this.persistForm(false);
  }

  completeActivity(): void {
    if (!this.validateRequiredFields()) {
      this.error = 'Complete los campos obligatorios';
      return;
    }
    this.persistForm(true);
  }

  cancel(): void {
    this.router.navigate(['/mis-actividades']);
  }

  getSelectOptions(options?: string): string[] {
    if (!options?.trim()) {
      return ['Opción 1', 'Opción 2'];
    }
    return options.split(',').map((item) => item.trim()).filter(Boolean);
  }

  openAiPanel(): void {
    this.showAiPanel = true;
    this.aiSkippedMessage = '';
    this.aiInfoMessage = '';
    this.generateAiSuggestions();
  }

  closeAiPanel(): void {
    this.showAiPanel = false;
    this.aiSuggestions = [];
    this.aiInfoMessage = '';
    this.aiSkippedMessage = '';
  }

  generateAiSuggestions(): void {
    if (!this.activity) return;

    this.aiGenerating = true;
    this.aiSuggestions = [];
    this.aiInfoMessage = '';

    const currentUser = this.authService.getCurrentUser();
    const result = this.aiAssistant.suggestFormValues(
      this.activity.activityName,
      this.fields,
      currentUser,
      this.activity.policyName
    );

    this.aiSuggestions = result.suggestions;
    this.aiGenerating = false;

    const missing = this.aiAssistant.detectMissingRequiredFields(this.fields, this.values);
    if (missing.length) {
      this.aiInfoMessage = 'La IA detectó campos obligatorios pendientes';
    }
  }

  applyAiSuggestions(): void {
    const { values, appliedCount, skippedCount } = this.aiAssistant.applyFormSuggestions(
      this.fields,
      this.aiSuggestions,
      this.values
    );

    this.values = { ...values };

    if (appliedCount > 0) {
      this.message = 'Se aplicaron sugerencias al formulario';
    }
    if (skippedCount > 0) {
      this.aiSkippedMessage = 'No se sobrescribieron campos ya completados';
    }

    const stillMissing = this.aiAssistant.detectMissingRequiredFields(this.fields, this.values);
    if (stillMissing.length) {
      this.aiInfoMessage = 'La IA detectó campos obligatorios pendientes';
    } else if (appliedCount > 0) {
      this.aiInfoMessage = '';
    }
  }

  formatSuggestionValue(suggestion: FormFieldSuggestion): string {
    if (suggestion.message && !suggestion.applicable) {
      return suggestion.message;
    }
    if (suggestion.suggestedValue === null || suggestion.suggestedValue === '') {
      return 'Sin sugerencia automática';
    }
    if (typeof suggestion.suggestedValue === 'boolean') {
      return suggestion.suggestedValue ? 'Sí' : 'No';
    }
    return String(suggestion.suggestedValue);
  }

  private persistForm(complete: boolean): void {
    if (!this.activity) {
      return;
    }

    if (Object.values(this.uploadingFiles).some(Boolean)) {
      this.error = 'Espere a que termine la subida del archivo';
      return;
    }

    if (complete) {
      this.completing = true;
    } else {
      this.saving = true;
    }
    this.error = '';
    this.message = '';

    this.uploadPendingFiles()
      .pipe(
        switchMap(() => {
          const payload = this.buildPayload();
          if (complete) {
            return this.myActivitiesService.complete(this.activity!.tramiteId, {
              activityName: this.activity!.activityName,
              taskOrder: this.activity!.taskOrder,
              responses: payload.responses,
            }).pipe(map(() => null));
          }
          return this.formSubmissionService.save(payload).pipe(map(() => null));
        })
      )
      .subscribe({
        next: () => {
          this.saving = false;
          this.completing = false;
          if (complete) {
            this.message = 'Actividad completada correctamente';
            setTimeout(() => this.router.navigate(['/mis-actividades']), 1200);
          } else {
            this.message = 'Avance guardado correctamente. Puede volver más tarde y continuar donde lo dejó.';
          }
        },
        error: (err) => {
          this.saving = false;
          this.completing = false;
          this.error = httpErrorMessage(
            err,
            complete ? 'No se pudo completar la actividad' : 'No se pudo guardar el avance'
          );
        },
      });
  }

  private uploadPendingFiles(): Observable<void> {
    const pendingUploads: Observable<FormSubmissionFileMeta | null>[] = [];

    for (const field of this.fields) {
      if (!isFileFieldType(field.type)) {
        continue;
      }

      const key = field.name || field.label;
      const pendingFile = this.fileSelections[key];
      if (!pendingFile) {
        continue;
      }

      pendingUploads.push(
        this.formSubmissionService.uploadFile(pendingFile).pipe(
          map((meta) => {
            this.fileAttachments[key] = meta;
            this.fileSelections[key] = null;
            this.values[key] = meta.fileName;
            return meta;
          })
        )
      );
    }

    if (!pendingUploads.length) {
      return of(void 0);
    }

    return forkJoin(pendingUploads).pipe(map(() => void 0));
  }

  private initializeValues(): void {
    this.values = {};
    this.fileSelections = {};
    this.fileAttachments = {};
    this.uploadingFiles = {};
    for (const field of this.fields) {
      const key = field.name || field.label;
      this.values[key] = field.type === 'checkbox' ? 'false' : '';
      this.fileSelections[key] = null;
      this.fileAttachments[key] = null;
      this.uploadingFiles[key] = false;
    }
  }

  private validateRequiredFields(): boolean {
    for (const field of this.fields) {
      if (!field.required) {
        continue;
      }
      const key = field.name || field.label;
      if (isFileFieldType(field.type)) {
        if (!this.fileAttachments[key]?.fileId) {
          return false;
        }
        continue;
      }
      const value = this.values[key];
      if (field.type === 'checkbox') {
        if (value !== 'true') {
          return false;
        }
      } else if (!value?.trim()) {
        return false;
      }
    }
    return true;
  }

  private buildResponses(): ResponseItemPayload[] {
    return this.fields.map((field) => {
      const key = field.name || field.label;
      if (isFileFieldType(field.type)) {
        const attachment = this.fileAttachments[key];
        return {
          fieldName: key,
          fieldLabel: field.label,
          fieldType: 'file',
          value: attachment?.fileName ?? '',
          fileName: attachment?.fileName ?? '',
          fileId: attachment?.fileId,
          contentType: attachment?.contentType,
          size: attachment?.size,
        };
      }

      return {
        fieldName: key,
        fieldLabel: field.label,
        fieldType: field.type,
        value: this.values[key] ?? '',
      };
    });
  }

  private buildPayload() {
    if (!this.activity) {
      throw new Error('Actividad no disponible');
    }
    return {
      tramiteId: this.activity.tramiteId,
      policyId: this.activity.policyId,
      activityName: this.activity.activityName,
      taskOrder: this.activity.taskOrder,
      responses: this.buildResponses(),
    };
  }
}
