import { Component, ChangeDetectorRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { WorkflowActivityService } from '../../services/workflow-activity.service';
import { PolicyService } from '../../services/policy.service';
import { AuthService } from '../../services/auth.service';
import { PolicyDetail, WorkflowActivity, WorkflowActivityRequest } from '../../models/workflow.model';
import {
  ACTIVITY_STATUS_OPTIONS,
  ACTIVITY_TYPE_OPTIONS,
  RESPONSIBLE_OPTIONS,
  RESPONSIBLE_TYPE_OPTIONS,
  activityStatusClass,
  activityStatusLabel,
  activityTypeLabel,
} from '../../utils/workflow-display.util';
import { isVisibleActivity } from '../../utils/workflow-visibility.util';

@Component({
  selector: 'app-policy-activities',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './policy-activities.component.html',
  styleUrl: './policy-activities.component.scss',
})
export class PolicyActivitiesComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly activityService = inject(WorkflowActivityService);
  private readonly policyService = inject(PolicyService);
  private readonly auth = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  policyId = '';
  policy: PolicyDetail | null = null;
  activities: WorkflowActivity[] = [];
  loading = true;
  saving = false;
  error = '';
  message = '';

  modalOpen = false;
  viewModalOpen = false;
  editingId: string | null = null;
  viewingActivity: WorkflowActivity | null = null;

  form: WorkflowActivityRequest = this.emptyForm();

  readonly canManage = this.auth.canManageWorkflowActivities();
  readonly canViewForms = this.auth.canViewDynamicForms();
  readonly activityTypeOptions = ACTIVITY_TYPE_OPTIONS;
  readonly activityStatusOptions = ACTIVITY_STATUS_OPTIONS;
  readonly responsibleOptions = RESPONSIBLE_OPTIONS;
  readonly responsibleTypeOptions = RESPONSIBLE_TYPE_OPTIONS;
  readonly activityTypeLabel = activityTypeLabel;
  readonly activityStatusLabel = activityStatusLabel;
  readonly activityStatusClass = activityStatusClass;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      this.policyId = params.get('id') ?? '';
      if (this.policyId) {
        this.loadPolicy();
        this.loadActivities();
      }
    });
  }

  private emptyForm(): WorkflowActivityRequest {
    return {
      policyId: this.policyId,
      name: '',
      description: '',
      responsibleType: 'ROLE',
      responsibleName: '',
      activityType: 'TASK',
      status: 'BORRADOR',
      orderIndex: undefined,
      estimatedTimeHours: 1,
    };
  }

  loadPolicy(): void {
    this.policyService.getDetail(this.policyId).subscribe({
      next: (p) => (this.policy = p),
      error: () => {
        this.error = 'La política seleccionada no existe.';
      },
    });
  }

  loadActivities(options?: { silent?: boolean }): void {
    if (!options?.silent) {
      this.loading = true;
    }
    this.error = '';
    this.activityService.getByPolicy(this.policyId).subscribe({
      next: (data) => {
        this.activities = [...data];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudieron cargar las actividades.');
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openCreate(): void {
    if (!this.canManage) {
      this.error = 'No tienes permiso para modificar actividades.';
      return;
    }
    this.editingId = null;
    this.form = this.emptyForm();
    this.modalOpen = true;
    this.error = '';
  }

  openEdit(activity: WorkflowActivity): void {
    if (!this.canManage) {
      this.error = 'No tienes permiso para modificar actividades.';
      return;
    }
    this.editingId = activity.id ?? null;
    this.form = {
      policyId: this.policyId,
      name: activity.name,
      description: activity.description ?? '',
      responsibleType: activity.responsibleType ?? 'ROLE',
      responsibleId: activity.responsibleId,
      responsibleName: activity.responsibleName ?? '',
      activityType: activity.activityType ?? 'TASK',
      status: activity.status ?? 'BORRADOR',
      orderIndex: activity.orderIndex,
      estimatedTimeHours: activity.estimatedTimeHours ?? 1,
    };
    this.modalOpen = true;
    this.error = '';
  }

  openView(activity: WorkflowActivity): void {
    this.viewingActivity = activity;
    this.viewModalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
    this.error = '';
  }

  closeViewModal(): void {
    this.viewModalOpen = false;
    this.viewingActivity = null;
  }

  save(): void {
    if (!this.canManage) {
      this.error = 'No tienes permiso para modificar actividades.';
      return;
    }
    if (!this.form.name?.trim()) {
      this.error = 'El nombre de la actividad es obligatorio.';
      return;
    }

    this.saving = true;
    this.error = '';
    const payload: WorkflowActivityRequest = {
      ...this.form,
      policyId: this.policyId,
      name: this.form.name.trim(),
    };

    const request$ = this.editingId
      ? this.activityService.update(this.editingId, payload)
      : this.activityService.create(payload);

    request$.subscribe({
      next: () => {
        this.message = this.editingId ? 'Actividad actualizada correctamente.' : 'Actividad creada correctamente.';
        this.modalOpen = false;
        this.saving = false;
        this.refreshActivitiesView();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudo guardar la actividad.');
        this.saving = false;
      },
    });
  }

  toggleActive(activity: WorkflowActivity): void {
    if (!this.canManage || !activity.id) return;
    const action$ = activity.status === 'ACTIVA' && activity.active !== false
      ? this.activityService.deactivate(activity.id)
      : this.activityService.activate(activity.id);

    action$.subscribe({
      next: () => {
        this.message = activity.status === 'ACTIVA'
          ? 'Actividad desactivada correctamente.'
          : 'Actividad activada correctamente.';
        this.patchActivityActiveState(activity.id!, activity.status === 'ACTIVA');
        this.refreshActivitiesView();
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudo cambiar el estado de la actividad.');
      },
    });
  }

  get visibleActivities(): WorkflowActivity[] {
    return this.activities.filter(isVisibleActivity);
  }

  remove(activity: WorkflowActivity): void {
    if (!this.canManage || !activity.id) return;
    if (
      !confirm(
        '¿Está seguro de eliminar esta actividad? También puede afectar conexiones asociadas.',
      )
    ) {
      return;
    }

    this.activityService.delete(activity.id).subscribe({
      next: (result) => {
        this.message = result.message || 'Actividad eliminada correctamente.';
        this.patchActivityRemoved(activity.id!);
        this.loadPolicy();
        this.loadActivities({ silent: true });
      },
      error: (err: HttpErrorResponse) => {
        this.error = this.resolveError(err, 'No se pudo eliminar la actividad.');
      },
    });
  }

  policyStatusText(status?: string): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'ACTIVE') return 'Activa';
    if (s === 'INACTIVE') return 'Inactiva';
    if (s === 'DRAFT' || s === 'BORRADOR') return 'Borrador';
    return s || '—';
  }

  shortDescription(text?: string): string {
    if (!text) return '—';
    return text.length > 80 ? `${text.slice(0, 80)}…` : text;
  }

  private refreshActivitiesView(): void {
    this.loadActivities({ silent: true });
    this.loadPolicy();
  }

  private patchActivityRemoved(id: string): void {
    this.activities = this.activities.filter((activity) => activity.id !== id);
    this.cdr.detectChanges();
  }

  private patchActivityActiveState(id: string, wasActive: boolean): void {
    this.activities = this.activities.map((activity) =>
      activity.id === id
        ? {
            ...activity,
            active: !wasActive,
            status: wasActive ? 'INACTIVA' : 'ACTIVA',
          }
        : activity,
    );
    this.cdr.detectChanges();
  }

  private resolveError(err: HttpErrorResponse, fallback: string): string {
    if (err.status === 403) return 'No tienes permiso para modificar actividades.';
    if (err.status === 404) return 'La política seleccionada no existe.';
    return err.error?.message ?? fallback;
  }
}
