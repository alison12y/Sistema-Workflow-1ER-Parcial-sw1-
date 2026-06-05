import { Component, ChangeDetectorRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { WorkflowTransitionService } from '../../services/workflow-transition.service';
import { WorkflowActivityService } from '../../services/workflow-activity.service';
import { PolicyService } from '../../services/policy.service';
import { AuthService } from '../../services/auth.service';
import {
  PolicyDetail,
  WorkflowActivity,
  WorkflowFlowValidationResponse,
  WorkflowTransition,
  WorkflowTransitionRequest,
} from '../../models/workflow.model';
import {
  TRANSITION_TYPE_OPTIONS,
  transitionConditionRequired,
  transitionShowsConditionField,
  transitionStatusClass,
  transitionStatusLabel,
  transitionTypeHint,
  transitionTypeLabel,
  validateTransitionFormInput,
} from '../../utils/transition-display.util';
import { isVisibleActivity, isVisibleTransition } from '../../utils/workflow-visibility.util';

@Component({
  selector: 'app-policy-transitions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './policy-transitions.component.html',
  styleUrl: './policy-transitions.component.scss',
})
export class PolicyTransitionsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly transitionService = inject(WorkflowTransitionService);
  private readonly activityService = inject(WorkflowActivityService);
  private readonly policyService = inject(PolicyService);
  private readonly auth = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  policyId = '';
  policy: PolicyDetail | null = null;
  activities: WorkflowActivity[] = [];
  transitions: WorkflowTransition[] = [];
  flowPreview: string[] = [];
  validation: WorkflowFlowValidationResponse | null = null;

  loading = true;
  saving = false;
  validating = false;
  error = '';
  message = '';

  modalOpen = false;
  viewModalOpen = false;
  editingId: string | null = null;
  viewingTransition: WorkflowTransition | null = null;

  form: WorkflowTransitionRequest = this.emptyForm();
  formValidationWarning = '';

  readonly canManage = this.auth.canManageWorkflowActivities();
  readonly transitionTypeOptions = TRANSITION_TYPE_OPTIONS;
  readonly transitionTypeLabel = transitionTypeLabel;
  readonly transitionStatusLabel = transitionStatusLabel;
  readonly transitionStatusClass = transitionStatusClass;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      this.policyId = params.get('id') ?? '';
      if (this.policyId) {
        this.loadPolicy();
        this.loadActivities();
        this.loadTransitions();
      }
    });
  }

  private emptyForm(): WorkflowTransitionRequest {
    return {
      policyId: this.policyId,
      fromActivityId: '',
      toActivityId: '',
      transitionType: 'SEQUENTIAL',
      conditionLabel: '',
      conditionExpression: '',
      orderIndex: undefined,
      active: true,
    };
  }

  loadPolicy(): void {
    this.policyService.getDetail(this.policyId).subscribe({
      next: (p) => {
        this.policy = p;
        this.flowPreview = p.flowPreview ?? [];
      },
      error: () => {
        this.error = 'La política seleccionada no existe.';
      },
    });
  }

  loadActivities(): void {
    this.activityService.getByPolicy(this.policyId).subscribe({
      next: (data) => {
        this.activities = [...data];
        this.cdr.detectChanges();
      },
    });
  }

  loadTransitions(options?: { silent?: boolean }): void {
    if (!options?.silent) {
      this.loading = true;
    }
    this.error = '';
    this.transitionService.getByPolicy(this.policyId).subscribe({
      next: (data) => {
        this.transitions = [...data];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudieron cargar las conexiones.');
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openCreate(): void {
    if (!this.canManage) {
      this.error = 'No tienes permiso para modificar conexiones.';
      return;
    }
    this.editingId = null;
    this.form = this.emptyForm();
    this.modalOpen = true;
    this.error = '';
  }

  openEdit(transition: WorkflowTransition): void {
    if (!this.canManage) {
      this.error = 'No tienes permiso para modificar conexiones.';
      return;
    }
    this.editingId = transition.id ?? null;
    this.form = {
      policyId: this.policyId,
      fromActivityId: transition.fromActivityId,
      toActivityId: transition.toActivityId,
      transitionType: (transition.transitionType ?? 'SEQUENTIAL').toUpperCase(),
      conditionLabel: transition.conditionLabel ?? '',
      conditionExpression: transition.conditionExpression ?? '',
      orderIndex: transition.orderIndex,
      active: transition.active !== false,
    };
    this.modalOpen = true;
    this.error = '';
  }

  openView(transition: WorkflowTransition): void {
    this.viewingTransition = transition;
    this.viewModalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
    this.error = '';
  }

  closeViewModal(): void {
    this.viewModalOpen = false;
    this.viewingTransition = null;
  }

  save(): void {
    if (!this.canManage) {
      this.error = 'No tienes permiso para modificar conexiones.';
      return;
    }
    if (!this.form.fromActivityId || !this.form.toActivityId) {
      this.error = 'Debe seleccionar actividad origen y destino.';
      return;
    }
    if (this.form.fromActivityId === this.form.toActivityId) {
      this.error = 'La actividad origen y destino no pueden ser iguales.';
      return;
    }
    const clientValidation = validateTransitionFormInput(
      this.form,
      this.transitions,
      this.editingId,
    );
    if (clientValidation.error) {
      this.error = clientValidation.error;
      this.formValidationWarning = '';
      return;
    }
    this.formValidationWarning = clientValidation.warning ?? '';

    this.saving = true;
    this.error = '';
    const payload: WorkflowTransitionRequest = {
      ...this.form,
      policyId: this.policyId,
      transitionType: (this.form.transitionType ?? 'SEQUENTIAL').toUpperCase(),
      conditionLabel: this.form.conditionLabel?.trim() || undefined,
      conditionExpression: this.form.conditionExpression?.trim() || undefined,
    };

    const request$ = this.editingId
      ? this.transitionService.update(this.editingId, payload)
      : this.transitionService.create(payload);

    request$.subscribe({
      next: () => {
        const base = this.editingId ? 'Conexión actualizada correctamente.' : 'Conexión creada correctamente.';
        this.message = this.formValidationWarning ? `${base} ${this.formValidationWarning}` : base;
        this.formValidationWarning = '';
        this.modalOpen = false;
        this.saving = false;
        this.validation = null;
        this.refreshTransitionsView();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudo guardar la conexión.');
        this.saving = false;
      },
    });
  }

  toggleActive(transition: WorkflowTransition): void {
    if (!this.canManage || !transition.id) return;
    const action$ = transition.active !== false
      ? this.transitionService.deactivate(transition.id)
      : this.transitionService.activate(transition.id);

    action$.subscribe({
      next: () => {
        this.message = transition.active !== false
          ? 'Conexión desactivada correctamente.'
          : 'Conexión activada correctamente.';
        this.patchTransitionActiveState(transition.id!, transition.active !== false);
        this.refreshTransitionsView();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudo cambiar el estado de la conexión.');
      },
    });
  }

  get visibleActivities(): WorkflowActivity[] {
    return this.activities.filter(isVisibleActivity);
  }

  get visibleTransitions(): WorkflowTransition[] {
    return this.transitions.filter(isVisibleTransition);
  }

  remove(transition: WorkflowTransition): void {
    if (!this.canManage || !transition.id) return;
    if (
      !confirm(
        `¿Eliminar la conexión de "${transition.fromActivityName}" hacia "${transition.toActivityName}"?`,
      )
    ) {
      return;
    }

    this.transitionService.delete(transition.id).subscribe({
      next: (result) => {
        this.message = result.message || 'Conexión eliminada correctamente.';
        this.validation = null;
        this.patchTransitionRemoved(transition.id!);
        this.loadPolicy();
        this.loadActivities();
        this.loadTransitions({ silent: true });
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudo eliminar la conexión.');
      },
    });
  }

  validateFlow(): void {
    this.validating = true;
    this.validation = null;
    this.transitionService.validatePolicyFlow(this.policyId).subscribe({
      next: (result) => {
        this.validation = result;
        this.validating = false;
      },
      error: () => {
        this.error = 'No se pudo validar el flujo.';
        this.validating = false;
      },
    });
  }

  activityName(id?: string): string {
    return this.activities.find((a) => a.id === id)?.name ?? '—';
  }

  policyStatusText(status?: string): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'ACTIVE') return 'Activa';
    if (s === 'INACTIVE') return 'Inactiva';
    if (s === 'DRAFT' || s === 'BORRADOR') return 'Borrador';
    return s || '—';
  }

  showsConditionField(): boolean {
    return transitionShowsConditionField(this.form.transitionType);
  }

  conditionFieldRequired(): boolean {
    return transitionConditionRequired(this.form.transitionType);
  }

  currentTransitionTypeHint(): string {
    return transitionTypeHint(this.form.transitionType);
  }

  private refreshTransitionsView(): void {
    this.loadTransitions({ silent: true });
    this.loadActivities();
    this.loadPolicy();
  }

  private patchTransitionRemoved(id: string): void {
    this.transitions = this.transitions.filter((transition) => transition.id !== id);
    this.cdr.detectChanges();
  }

  private patchTransitionActiveState(id: string, wasActive: boolean): void {
    this.transitions = this.transitions.map((transition) =>
      transition.id === id ? { ...transition, active: !wasActive } : transition,
    );
    this.cdr.detectChanges();
  }

  private resolveError(err: HttpErrorResponse, fallback: string): string {
    if (err.status === 403) return 'No tienes permiso para modificar conexiones.';
    if (err.status === 404) return 'La política seleccionada no existe.';
    return err.error?.message ?? fallback;
  }
}
