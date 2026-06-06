import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { FormService } from '../../services/form.service';
import { FormSubmissionService } from '../../services/form-submission.service';
import { MyActivitiesService } from '../../services/my-activities.service';
import { AuthService } from '../../services/auth.service';
import { AiService } from '../../services/ai.service';
import {
  AiAssistantService,
  FormFieldSuggestion,
} from '../../services/ai-assistant.service';
import { AiFormAssistResponse } from '../../models/ai-form-assist.model';
import { HttpErrorResponse } from '@angular/common/http';
import {
  FormSubmissionFileMeta,
  MyActivity,
  ResponseItemPayload,
} from '../../models/my-activities.model';
import { FormDesignerFieldPayload } from '../../models/form.model';
import { httpErrorMessage, tramitePriorityLabel } from '../../utils/tramite-display.util';
import { applySavedResponses, isFileFieldType } from '../../utils/form-submission-display.util';
import {
  mapDynamicFieldsToExecution,
  NO_FORM_OBSERVATION_FIELD,
} from '../../utils/form-field-mapper.util';
import {
  appendDictationText,
  isVoiceDictationSupported,
  VoiceDictationController,
} from '../../utils/voice-dictation.util';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineSyncService } from '../../core/offline/offline-sync.service';
import { OfflineFileBlob } from '../../core/offline/offline-db.types';
import { shouldQueueOffline } from '../../utils/network-error.util';

@Component({
  selector: 'app-form-execution',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './form-execution.component.html',
  styleUrl: './form-execution.component.scss',
})
export class FormExecutionComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly myActivitiesService = inject(MyActivitiesService);
  private readonly formService = inject(FormService);
  private readonly formSubmissionService = inject(FormSubmissionService);
  private readonly authService = inject(AuthService);
  private readonly aiService = inject(AiService);
  private readonly aiAssistant = inject(AiAssistantService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly connectivity = inject(ConnectivityService);
  private readonly offlineSync = inject(OfflineSyncService);

  activity: MyActivity | null = null;
  formId: string | null = null;
  fields: FormDesignerFieldPayload[] = [];
  values: Record<string, string> = {};
  fileSelections: Record<string, File | null> = {};
  fileAttachments: Record<string, FormSubmissionFileMeta | null> = {};
  uploadingFiles: Record<string, boolean> = {};
  loading = true;
  saving = false;
  completing = false;
  missingForm = false;
  noFormMode = false;
  readonly maxFileSizeBytes = 10 * 1024 * 1024;
  message = '';
  error = '';

  aiReport = '';
  aiAssistResponse: AiFormAssistResponse | null = null;
  aiSuggestions: FormFieldSuggestion[] = [];
  aiGenerating = false;
  aiInfoMessage = '';
  aiSkippedMessage = '';
  aiError = '';
  voiceListening = false;
  voiceStatus = '';
  readonly voiceSupported = isVoiceDictationSupported();

  private voiceDictation: VoiceDictationController | null = null;

  readonly priorityLabel = tramitePriorityLabel;
  readonly canUseFormAi = this.authService.canExecuteTasks();

  ngOnInit(): void {
    const tramiteId = this.route.snapshot.paramMap.get('tramiteId');
    const workflowActivityId = this.route.snapshot.queryParamMap.get('workflowActivityId')?.trim() ?? '';
    const taskOrder = Number(this.route.snapshot.queryParamMap.get('taskOrder') ?? '0');

    if (!tramiteId || !workflowActivityId || taskOrder <= 0) {
      this.loading = false;
      this.error = 'No se pudo identificar la actividad de workflow a completar';
      return;
    }

    this.myActivitiesService.getById(tramiteId, taskOrder).subscribe({
      next: (activity) => {
        if (activity.status !== 'EN_CURSO' && !activity.canComplete) {
          this.loading = false;
          this.error =
            activity.status === 'PENDIENTE'
              ? 'Debe tomar la tarea en la bandeja antes de completarla'
              : 'Esta tarea ya no está en curso';
          return;
        }
        this.activity = activity;
        this.loadForm(
          workflowActivityId,
          activity.tramiteId,
          activity.taskOrder,
          activity.activityName
        );
      },
      error: (err) => {
        this.loading = false;
        this.error = httpErrorMessage(err, 'No se pudo cargar la actividad');
      },
    });
  }

  ngOnDestroy(): void {
    this.voiceDictation?.abort();
    this.voiceDictation = null;
  }

  loadForm(
    workflowActivityId: string,
    tramiteId: string,
    taskOrder: number,
    activityName: string
  ): void {
    this.missingForm = false;
    this.noFormMode = false;
    this.formService.getFormByActivity(workflowActivityId).subscribe({
      next: (form) => {
        const mapped = mapDynamicFieldsToExecution(form.fields ?? []);
        if (!form.id || !mapped.length) {
          this.noFormMode = true;
          this.formId = null;
          this.fields = [NO_FORM_OBSERVATION_FIELD];
          this.message =
            'No hay formulario configurado para esta actividad. Indique una observación para completar.';
        } else {
          this.formId = form.id ?? null;
          this.fields = mapped;
        }

        this.initializeValues();
        this.formSubmissionService
          .getForTask(tramiteId, taskOrder, workflowActivityId, activityName)
          .subscribe({
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
        this.noFormMode = true;
        this.fields = [NO_FORM_OBSERVATION_FIELD];
        this.initializeValues();
        this.message =
          'No se pudo cargar el formulario. Puede completar con una observación o configurarlo en el diseñador.';
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

    if (file.size > this.maxFileSizeBytes) {
      input.value = '';
      this.error = 'El archivo no puede superar 10 MB';
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
    if (!this.activity?.workflowActivityId) {
      return;
    }
    this.router.navigate(['/activities', this.activity.workflowActivityId, 'form']);
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

  assistFormFromReport(): void {
    if (!this.canUseFormAi) {
      this.aiError = 'No tiene permiso para usar asistencia IA en formularios.';
      return;
    }
    if (!this.activity || !this.aiReport.trim()) {
      this.aiError = 'Escriba o dicte un informe antes de solicitar asistencia.';
      return;
    }

    this.aiGenerating = true;
    this.aiError = '';
    this.aiAssistResponse = null;
    this.aiSuggestions = [];
    this.aiSkippedMessage = '';
    this.aiInfoMessage = '';

    const user = this.authService.getCurrentUser();
    this.aiService
      .assistForm({
        report: this.aiReport.trim(),
        policyId: this.activity.policyId,
        tramiteId: this.activity.tramiteId,
        workflowActivityId: this.activity.workflowActivityId,
        formId: this.formId ?? undefined,
        activityName: this.activity.activityName,
        userId: user?.username,
        fields: this.fields.map((f) => ({
          name: f.name || f.label,
          label: f.label,
          type: (f.type || 'TEXT').toUpperCase(),
          required: f.required,
          options: f.options,
        })),
        currentValues: { ...this.values },
      })
      .subscribe({
        next: (response) => {
          this.aiGenerating = false;
          this.aiAssistResponse = response;
          this.aiSuggestions = this.mapApiSuggestions(response);
          if (response.fallbackUsed) {
            this.aiInfoMessage =
              'Asistencia con parser local (IA externa no disponible). Revise antes de aplicar.';
          } else if (response.explanation) {
            this.aiInfoMessage = response.explanation;
          }
          if (response.unmatchedFields?.length) {
            this.aiSkippedMessage =
              'Campos no mapeados: ' + response.unmatchedFields.join(', ');
          }
        },
        error: (err: HttpErrorResponse) => {
          this.aiGenerating = false;
          if (err.status === 403) {
            this.aiError = 'No tiene permiso para asistencia IA.';
            return;
          }
          this.fallbackLocalAssist();
        },
      });
  }

  private fallbackLocalAssist(): void {
    if (!this.activity) return;
    const result = this.aiAssistant.suggestFormValues(
      this.aiReport.trim() || this.activity.activityName,
      this.fields,
      this.authService.getCurrentUser(),
      this.activity.policyName
    );
    this.aiSuggestions = result.suggestions;
    this.aiAssistResponse = {
      fallbackUsed: true,
      explanation: 'Sugerencia local en el navegador (servicio IA no disponible).',
      fieldSuggestions: [],
    };
    this.aiInfoMessage = this.aiAssistResponse.explanation ?? '';
  }

  private mapApiSuggestions(response: AiFormAssistResponse): FormFieldSuggestion[] {
    const list = response.fieldSuggestions ?? [];
    return list.map((s) => ({
      fieldId: s.fieldName,
      fieldLabel: s.fieldLabel,
      fieldType: (s.fieldType || 'text').toLowerCase(),
      suggestedValue: s.suggestedValue ?? null,
      message: s.message,
      applicable: s.applicable !== false && s.suggestedValue != null && s.suggestedValue !== '',
    }));
  }

  applyAiSuggestions(): void {
    if (!this.aiSuggestions.length) return;

    const filledKeys = this.fields
      .filter((f) => {
        const key = f.name || f.label;
        if (isFileFieldType(f.type)) return false;
        const v = this.values[key];
        return f.type === 'checkbox' ? v === 'true' : !!v?.trim();
      })
      .map((f) => f.name || f.label);

    const wouldOverwrite = this.aiSuggestions.some((s) => {
      if (!s.applicable) return false;
      return filledKeys.includes(s.fieldId);
    });

    if (wouldOverwrite) {
      const ok = confirm(
        'Algunos campos ya tienen valor. ¿Desea aplicar la sugerencia de IA solo en campos vacíos? ' +
          'Para sobrescribir, vacíe el campo manualmente primero.'
      );
      if (!ok) return;
    }

    const { values, appliedCount, skippedCount } = this.aiAssistant.applyFormSuggestions(
      this.fields,
      this.aiSuggestions,
      this.values
    );

    this.values = { ...values };

    if (appliedCount > 0) {
      this.message = `Se aplicaron ${appliedCount} sugerencia(s) al formulario. Puede editar antes de enviar.`;
      this.recordAiAssistTrace(appliedCount);
    }
    if (skippedCount > 0) {
      this.aiSkippedMessage = 'No se sobrescribieron campos ya completados.';
    }

    const stillMissing = this.aiAssistant.detectMissingRequiredFields(this.fields, this.values);
    if (stillMissing.length) {
      this.aiInfoMessage = 'Quedan campos obligatorios pendientes tras aplicar sugerencias.';
    }
  }

  private recordAiAssistTrace(appliedCount: number): void {
    if (!this.activity) return;
    this.myActivitiesService
      .recordAiFormAssisted(this.activity.tramiteId, {
        workflowActivityId: this.activity.workflowActivityId,
        taskOrder: this.activity.taskOrder,
        activityName: this.activity.activityName,
        fieldsSuggested: this.aiSuggestions.filter((s) => s.applicable).length,
        fieldsApplied: appliedCount,
      })
      .subscribe({ error: () => {} });
  }

  toggleVoiceDictation(): void {
    if (this.voiceListening) {
      this.stopVoiceDictation();
      return;
    }
    this.startVoiceDictation();
  }

  startVoiceDictation(): void {
    if (!this.voiceSupported) {
      this.aiError = 'El navegador no soporta dictado por voz.';
      this.voiceStatus = '';
      this.cdr.detectChanges();
      return;
    }

    if (!this.voiceDictation) {
      this.voiceDictation = new VoiceDictationController({
        onTranscript: (text) => {
          this.aiReport = appendDictationText(this.aiReport, text);
          this.cdr.detectChanges();
        },
        onListeningChange: (listening) => {
          this.voiceListening = listening;
          this.cdr.detectChanges();
        },
        onStatus: (message) => {
          this.voiceStatus = message;
          if (message !== 'Escuchando...') {
            this.aiError = '';
          }
          this.cdr.detectChanges();
        },
        onError: (message) => {
          this.aiError = message;
          this.voiceStatus = '';
          this.cdr.detectChanges();
        },
      });
    }

    this.aiError = '';
    this.voiceStatus = '';
    this.voiceDictation.start();
  }

  stopVoiceDictation(): void {
    this.voiceDictation?.stop();
    this.voiceListening = false;
    this.cdr.detectChanges();
  }

  get voiceButtonLabel(): string {
    if (this.voiceListening) {
      return 'Detener dictado';
    }
    return 'Dictar por voz';
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

    if (!this.connectivity.isOnline) {
      void this.queueOfflinePersist(complete);
      return;
    }

    this.uploadPendingFiles()
      .pipe(
        switchMap(() => {
          const payload = this.buildPayload();
          if (complete) {
            return this.myActivitiesService.complete(this.activity!.tramiteId, {
              workflowActivityId: this.activity!.workflowActivityId,
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
          if (shouldQueueOffline(err)) {
            void this.queueOfflinePersist(complete);
            return;
          }
          this.saving = false;
          this.completing = false;
          this.error = httpErrorMessage(
            err,
            complete ? 'No se pudo completar la actividad' : 'No se pudo guardar el avance'
          );
        },
      });
  }

  private async queueOfflinePersist(complete: boolean): Promise<void> {
    if (!this.activity) {
      return;
    }
    const payload = this.buildPayload();
    const fileBlobs = this.collectPendingFileBlobs();

    if (complete) {
      await this.offlineSync.enqueueCompleteActivity(
        this.activity.tramiteId,
        {
          workflowActivityId: this.activity.workflowActivityId,
          activityName: this.activity.activityName,
          taskOrder: this.activity.taskOrder,
          responses: payload.responses,
        },
        fileBlobs,
      );
      this.message = 'Completado guardado localmente. Se sincronizará al reconectar.';
    } else {
      await this.offlineSync.enqueueFormDraft(payload);
      this.message = 'Borrador guardado sin conexión. Se sincronizará automáticamente.';
    }

    this.saving = false;
    this.completing = false;
    this.error = '';
    await this.connectivity.refreshPendingCount();
    if (complete) {
      setTimeout(() => this.router.navigate(['/mis-actividades']), 1200);
    }
  }

  private collectPendingFileBlobs(): OfflineFileBlob[] {
    const blobs: OfflineFileBlob[] = [];
    for (const field of this.fields) {
      if (!isFileFieldType(field.type)) {
        continue;
      }
      const key = field.name || field.label;
      const file = this.fileSelections[key];
      if (!file) {
        continue;
      }
      blobs.push({
        fieldKey: key,
        fileName: file.name,
        contentType: file.type || 'application/octet-stream',
        blob: file,
      });
    }
    return blobs;
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
      const key = field.name?.trim() || field.label;
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
      const key = field.name?.trim();
      if (!key) {
        console.warn('[CU7] Campo sin nombre técnico, se omite en stepData:', field.label);
      }
      const fieldKey = key || field.label;
      if (isFileFieldType(field.type)) {
        const attachment = this.fileAttachments[key];
        return {
          fieldName: fieldKey,
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
        fieldName: fieldKey,
        fieldLabel: field.label,
        fieldType: field.type,
        value: this.values[fieldKey] ?? (field.type === 'checkbox' ? 'false' : ''),
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
      workflowActivityId: this.activity.workflowActivityId,
      activityName: this.activity.activityName,
      taskOrder: this.activity.taskOrder,
      responses: this.buildResponses(),
    };
  }
}
