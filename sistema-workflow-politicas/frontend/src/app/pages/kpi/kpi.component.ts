import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { KpiService } from '../../services/kpi.service';
import { KpiBottleneck, KpiSummary } from '../../models/kpi.model';

@Component({
  selector: 'app-kpi',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kpi.component.html',
  styleUrl: './kpi.component.scss',
})
export class KpiComponent implements OnInit {
  private readonly kpiService = inject(KpiService);

  summary: KpiSummary | null = null;
  bottlenecks: KpiBottleneck[] = [];
  loading = true;
  message = '';
  error = '';

  ngOnInit(): void {
    this.load(false);
  }

  load(showSuccessMessage = true): void {
    this.loading = true;
    this.error = '';
    this.message = '';

    this.kpiService.getSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.kpiService.getBottlenecks().subscribe({
          next: (bottlenecks) => {
            this.bottlenecks = bottlenecks;
            this.loading = false;
            if (showSuccessMessage) {
              this.message = 'Indicadores actualizados correctamente';
            }
          },
          error: (err) => this.handleError(err),
        });
      },
      error: (err) => this.handleError(err),
    });
  }

  refresh(): void {
    this.load(true);
  }

  get hasData(): boolean {
    return (this.summary?.totalTramites ?? 0) > 0;
  }

  get hasBottlenecks(): boolean {
    return this.bottlenecks.length > 0;
  }

  stuckCount(item: KpiBottleneck): number {
    return item.pendingCount + item.inProgressCount;
  }

  levelClass(level: string): string {
    switch (level) {
      case 'Alto':
        return 'level-high';
      case 'Medio':
        return 'level-medium';
      case 'Bajo':
        return 'level-low';
      default:
        return '';
    }
  }

  private handleError(err: HttpErrorResponse): void {
    this.summary = null;
    this.bottlenecks = [];
    this.loading = false;
    if (err.status === 401) {
      this.error = 'Su sesión expiró. Inicie sesión nuevamente';
    } else {
      this.error = 'No se pudieron cargar los KPIs';
    }
  }
}
