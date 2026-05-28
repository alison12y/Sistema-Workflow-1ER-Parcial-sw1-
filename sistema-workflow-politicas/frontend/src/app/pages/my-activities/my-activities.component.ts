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

  activities: MyActivity[] = [];
  loading = true;
  message = '';
  error = '';

  readonly taskStatusLabel = tramiteTaskStatusLabel;
  readonly priorityLabel = tramitePriorityLabel;

  ngOnInit(): void {
    this.load(false);
  }

  load(showSuccessMessage = true): void {
    this.loading = true;
    this.error = '';
    this.message = '';

    this.myActivitiesService.getAll().subscribe({
      next: (data) => {
        this.activities = data;
        this.loading = false;
        if (showSuccessMessage) {
          this.message = 'Actividades actualizadas correctamente';
        }
      },
      error: (err) => {
        this.activities = [];
        this.loading = false;
        this.error = httpErrorMessage(err, 'No se pudieron cargar las actividades');
      },
    });
  }

  refresh(): void {
    this.load(true);
  }

  openForm(activity: MyActivity): void {
    this.router.navigate(['/mis-actividades', activity.tramiteId, 'form'], {
      queryParams: {
        activity: activity.activityName,
        taskOrder: activity.taskOrder,
      },
    });
  }

  formatDate(value?: string): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '—';
    }
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${minutes}`;
  }
}
