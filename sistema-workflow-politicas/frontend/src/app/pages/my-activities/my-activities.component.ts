import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TimeoutError } from 'rxjs';
import { finalize, timeout } from 'rxjs/operators';
import { MyActivitiesService } from '../../services/my-activities.service';
import { TaskAssistantResponse } from '../../models/task-assistant.model';
import { PolicyService } from '../../services/policy.service';
import {
  MyActivitiesFilterParams,
  MyActivity,
} from '../../models/my-activities.model';
import { BusinessPolicy } from '../../models/auth.model';
import {
  httpErrorMessage,
  tramitePriorityLabel,
  tramiteTaskStatusLabel,
} from '../../utils/tramite-display.util';

interface TaskSection {
  key: string;
  title: string;
  emptyMessage: string;
  items: MyActivity[];
}

import { CYCLE1_POLL_INTERVAL_MS } from '../../core/polling.config';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineSyncService } from '../../core/offline/offline-sync.service';
import { shouldQueueOffline } from '../../utils/network-error.util';
import { buildTaskAssistantFallback } from '../../utils/task-assistant-fallback.util';

@Component({
  selector: 'app-my-activities',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './my-activities.component.html',
  styleUrl: './my-activities.component.scss',
})
export class MyActivitiesComponent implements OnInit, OnDestroy {
  private readonly myActivitiesService = inject(MyActivitiesService);
  private readonly policyService = inject(PolicyService);
  private readonly router = inject(Router);
  private readonly connectivity = inject(ConnectivityService);
  private readonly offlineSync = inject(OfflineSyncService);

  sections: TaskSection[] = [];
  policies: BusinessPolicy[] = [];
  loading = true;
  error = '';
  message = '';
  takingTaskKey: string | null = null;
  assistantLoadingKey: string | null = null;
  assistantModalOpen = false;
  assistantActivity: MyActivity | null = null;
  assistantResult: TaskAssistantResponse | null = null;
  assistantError = '';
  lastUpdated: Date | null = null;

  private static readonly ASSISTANT_TIMEOUT_MS = 30_000;

  filterStatus = '';
  filterPolicyId = '';
  filterTramiteCode = '';
  filterPriority = '';

  readonly taskStatusLabel = tramiteTaskStatusLabel;
  readonly priorityLabel = tramitePriorityLabel;
  readonly statusFilterOptions = [
    { value: '', label: 'Todos los estados' },
    { value: 'PENDIENTE', label: 'Pendiente' },
    { value: 'EN_CURSO', label: 'En curso' },
    { value: 'COMPLETADA', label: 'Completada' },
    { value: 'OBSERVADA', label: 'Observada' },
    { value: 'ERROR', label: 'Error' },
  ];
  readonly priorityFilterOptions = [
    { value: '', label: 'Todas las prioridades' },
    { value: 'ALTA', label: 'Alta' },
    { value: 'MEDIA', label: 'Media' },
    { value: 'BAJA', label: 'Baja' },
  ];

  private pollTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.policyService.getAll().subscribe({
      next: (data) => (this.policies = data ?? []),
      error: () => (this.policies = []),
    });
    this.load();
    this.pollTimer = setInterval(() => this.load(false), CYCLE1_POLL_INTERVAL_MS);
  }

  ngOnDestroy(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  load(showSpinner = true): void {
    if (showSpinner) {
      this.loading = true;
    }
    this.error = '';
    this.myActivitiesService.getAll(this.buildFilter()).subscribe({
      next: (data) => {
        void this.offlineSync.cacheActivities(data);
        this.sections = this.buildSections(data);
        this.loading = false;
        this.lastUpdated = new Date();
        this.error = '';
      },
      error: (err) => {
        void this.loadFromCacheIfOffline(err);
      },
    });
  }

  applyFilters(): void {
    this.load();
  }

  clearFilters(): void {
    this.filterStatus = '';
    this.filterPolicyId = '';
    this.filterTramiteCode = '';
    this.filterPriority = '';
    this.load();
  }

  takeTask(activity: MyActivity): void {
    const key = this.taskKey(activity);
    this.takingTaskKey = key;
    this.error = '';

    if (!this.connectivity.isOnline) {
      void this.offlineSync.enqueueTakeTask(activity.tramiteId, activity.taskOrder).then(() => {
        this.takingTaskKey = null;
        this.error = '';
        this.message = 'Tarea guardada localmente. Se sincronizará al reconectar.';
        void this.connectivity.refreshPendingCount();
      });
      return;
    }

    this.myActivitiesService.takeTask(activity.tramiteId, activity.taskOrder).subscribe({
      next: () => {
        this.takingTaskKey = null;
        this.load(false);
      },
      error: (err) => {
        if (shouldQueueOffline(err)) {
          void this.offlineSync.enqueueTakeTask(activity.tramiteId, activity.taskOrder).then(() => {
            this.takingTaskKey = null;
            this.message = 'Sin conexión: tarea en cola de sincronización.';
            void this.connectivity.refreshPendingCount();
          });
          return;
        }
        this.takingTaskKey = null;
        this.error = httpErrorMessage(err, 'No se pudo tomar la tarea');
      },
    });
  }

  openTaskAssistant(activity: MyActivity): void {
    const key = this.taskKey(activity);
    this.assistantActivity = activity;
    this.assistantModalOpen = true;
    this.assistantResult = null;
    this.assistantError = '';
    this.assistantLoadingKey = key;

    this.myActivitiesService
      .getTaskAssistant(activity.tramiteId, activity.taskOrder)
      .pipe(
        timeout(MyActivitiesComponent.ASSISTANT_TIMEOUT_MS),
        finalize(() => {
          this.assistantLoadingKey = null;
        }),
      )
      .subscribe({
        next: (response) => {
          this.assistantResult = response;
        },
        error: (err) => {
          this.assistantResult = buildTaskAssistantFallback(activity);
          if (err instanceof TimeoutError) {
            this.assistantError =
              'No se pudo consultar la IA a tiempo, se muestra una orientación local.';
            return;
          }
          this.assistantError = httpErrorMessage(
            err,
            'No se pudo consultar la IA, se muestra una orientación local.',
          );
        },
      });
  }

  closeTaskAssistant(): void {
    this.assistantModalOpen = false;
    this.assistantActivity = null;
    this.assistantResult = null;
    this.assistantError = '';
    this.assistantLoadingKey = null;
  }

  isAssistantLoading(activity: MyActivity): boolean {
    return this.assistantLoadingKey === this.taskKey(activity);
  }

  assistantSourceLabel(): string {
    const source = this.assistantResult?.source;
    if (source === 'AI') {
      return 'Servicio IA';
    }
    if (source === 'LOCAL_FALLBACK') {
      return 'Respaldo local';
    }
    return source ?? '—';
  }

  showAssistantFallbackNotice(): boolean {
    return this.assistantResult?.source === 'LOCAL_FALLBACK';
  }

  openForm(activity: MyActivity): void {
    if (!activity.workflowActivityId) {
      this.error = 'La tarea no tiene actividad de workflow asociada';
      return;
    }
    this.router.navigate(['/mis-actividades', activity.tramiteId, 'form'], {
      queryParams: {
        workflowActivityId: activity.workflowActivityId,
        taskOrder: activity.taskOrder,
      },
    });
  }

  isTaking(activity: MyActivity): boolean {
    return this.takingTaskKey === this.taskKey(activity);
  }

  formatDate(value?: string): string {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('es-BO');
  }

  hasAnyTasks(): boolean {
    return this.sections.some((s) => s.items.length > 0);
  }

  categoryBadge(activity: MyActivity): string | null {
    if (activity.inboxCategory === 'ERROR') return 'Error workflow';
    if (activity.inboxCategory === 'OBSERVADA') return 'Observada';
    return null;
  }

  private taskKey(activity: MyActivity): string {
    return `${activity.tramiteId}-${activity.taskOrder}`;
  }

  private buildFilter(): MyActivitiesFilterParams {
    const filter: MyActivitiesFilterParams = {};
    if (this.filterStatus) filter.status = this.filterStatus;
    if (this.filterPolicyId) filter.policyId = this.filterPolicyId;
    if (this.filterTramiteCode.trim()) filter.tramiteCode = this.filterTramiteCode.trim();
    if (this.filterPriority) filter.priority = this.filterPriority;
    return filter;
  }

  private async loadFromCacheIfOffline(err: unknown): Promise<void> {
    if (shouldQueueOffline(err) || !this.connectivity.isOnline) {
      const cached = await this.offlineSync.getCachedActivities();
      if (cached.length) {
        this.sections = this.buildSections(cached);
        this.error = 'Sin conexión — mostrando última bandeja guardada.';
        this.loading = false;
        this.lastUpdated = new Date();
        return;
      }
    }
    this.sections = [];
    this.loading = false;
    this.error = httpErrorMessage(err, 'No se pudieron cargar tus tareas');
  }

  private buildSections(activities: MyActivity[]): TaskSection[] {
    const isSpecial = (a: MyActivity) =>
      a.inboxCategory === 'OBSERVADA' || a.inboxCategory === 'ERROR';

    const pending = activities.filter((a) => a.status === 'PENDIENTE' && !isSpecial(a));
    const inProgress = activities.filter((a) => a.status === 'EN_CURSO' && !isSpecial(a));
    const done = activities.filter((a) => a.status === 'COMPLETADA');
    const alerts = activities.filter((a) => isSpecial(a) && a.status !== 'COMPLETADA');

    return [
      { key: 'pending', title: 'Pendientes', emptyMessage: 'No tienes tareas pendientes.', items: pending },
      { key: 'progress', title: 'En proceso', emptyMessage: 'No tienes tareas en proceso.', items: inProgress },
      { key: 'done', title: 'Finalizadas', emptyMessage: 'No tienes tareas finalizadas.', items: done },
      {
        key: 'alerts',
        title: 'Observadas / Error',
        emptyMessage: 'No tienes tareas observadas ni con error de workflow.',
        items: alerts,
      },
    ];
  }
}
