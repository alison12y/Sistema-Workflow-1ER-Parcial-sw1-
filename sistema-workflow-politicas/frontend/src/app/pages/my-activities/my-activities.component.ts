import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MyActivitiesService } from '../../services/my-activities.service';
import { MyActivity } from '../../models/my-activities.model';
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

@Component({
  selector: 'app-my-activities',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-activities.component.html',
  styleUrl: './my-activities.component.scss',
})
export class MyActivitiesComponent implements OnInit {
  private readonly myActivitiesService = inject(MyActivitiesService);
  private readonly router = inject(Router);

  sections: TaskSection[] = [];
  loading = true;
  error = '';

  readonly taskStatusLabel = tramiteTaskStatusLabel;
  readonly priorityLabel = tramitePriorityLabel;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.myActivitiesService.getAll().subscribe({
      next: (data) => {
        this.sections = this.buildSections(data);
        this.loading = false;
      },
      error: (err) => {
        this.sections = [];
        this.loading = false;
        this.error = httpErrorMessage(err, 'No se pudieron cargar tus tareas');
      },
    });
  }

  openForm(activity: MyActivity): void {
    this.router.navigate(['/mis-actividades', activity.tramiteId, 'form'], {
      queryParams: { activity: activity.activityName, taskOrder: activity.taskOrder },
    });
  }

  formatDate(value?: string): string {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('es-BO');
  }

  hasAnyTasks(): boolean {
    return this.sections.some((s) => s.items.length > 0);
  }

  private buildSections(activities: MyActivity[]): TaskSection[] {
    const pending = activities.filter((a) => a.status === 'PENDIENTE');
    const inProgress = activities.filter((a) => a.status === 'EN_CURSO');
    const done = activities.filter((a) => a.status === 'COMPLETADA');
    const observed = activities.filter((a) => !!a.activityName?.toLowerCase().includes('observ'));

    return [
      { key: 'pending', title: 'Pendientes', emptyMessage: 'No tienes tareas pendientes.', items: pending },
      { key: 'progress', title: 'En proceso', emptyMessage: 'No tienes tareas en proceso.', items: inProgress },
      { key: 'done', title: 'Finalizadas', emptyMessage: 'No tienes tareas finalizadas.', items: done },
      { key: 'observed', title: 'Observadas', emptyMessage: 'No tienes tareas observadas.', items: observed },
    ];
  }
}
