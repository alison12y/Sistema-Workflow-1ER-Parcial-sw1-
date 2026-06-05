import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { KpiService } from '../../services/kpi.service';
import { PolicyService } from '../../services/policy.service';
import { KpiBottleneck, KpiDashboard, KpiFilterParams } from '../../models/kpi.model';
import { BusinessPolicy } from '../../models/auth.model';
import { CYCLE1_POLL_INTERVAL_MS } from '../../core/polling.config';

@Component({
  selector: 'app-kpi',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './kpi.component.html',
  styleUrl: './kpi.component.scss',
})
export class KpiComponent implements OnInit, OnDestroy {
  private readonly kpiService = inject(KpiService);
  private readonly policyService = inject(PolicyService);

  dashboard: KpiDashboard | null = null;
  policies: BusinessPolicy[] = [];
  loading = true;
  message = '';
  error = '';
  lastUpdated: Date | null = null;

  filterPolicyId = '';
  filterStatus = '';
  filterFrom = '';
  filterTo = '';

  readonly statusOptions = [
    { value: '', label: 'Todos los estados' },
    { value: 'ACTIVO', label: 'Trámites activos' },
    { value: 'EN_PROCESO', label: 'En proceso' },
    { value: 'INICIADO', label: 'Iniciados' },
    { value: 'FINALIZADO', label: 'Finalizados' },
    { value: 'CANCELADO', label: 'Cancelados' },
    { value: 'ERROR', label: 'Con error workflow' },
  ];

  private pollTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.policyService.getAll().subscribe({
      next: (data) => (this.policies = data ?? []),
      error: () => (this.policies = []),
    });
    this.load(false);
    this.pollTimer = setInterval(() => this.load(false), CYCLE1_POLL_INTERVAL_MS);
  }

  ngOnDestroy(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  load(showSuccessMessage = true): void {
    if (showSuccessMessage) {
      this.loading = true;
    }
    this.error = '';
    if (showSuccessMessage) {
      this.message = '';
    }

    this.kpiService.getDashboard(this.buildFilter()).subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
        this.lastUpdated = new Date();
        if (showSuccessMessage && data.sufficientData) {
          this.message = 'Indicadores actualizados correctamente';
        }
      },
      error: (err: HttpErrorResponse) => this.handleError(err),
    });
  }

  applyFilters(): void {
    this.load(true);
  }

  clearFilters(): void {
    this.filterPolicyId = '';
    this.filterStatus = '';
    this.filterFrom = '';
    this.filterTo = '';
    this.load(true);
  }

  refresh(): void {
    this.load(true);
  }

  get summary() {
    return this.dashboard?.summary;
  }

  get hasData(): boolean {
    return this.dashboard?.sufficientData ?? false;
  }

  get hasBottlenecks(): boolean {
    return (this.dashboard?.bottlenecks?.length ?? 0) > 0;
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

  formatDate(value?: string | Date): string {
    if (!value) return '—';
    const date = value instanceof Date ? value : new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('es-BO');
  }

  private buildFilter(): KpiFilterParams {
    const filter: KpiFilterParams = {};
    if (this.filterPolicyId) filter.policyId = this.filterPolicyId;
    if (this.filterStatus) filter.status = this.filterStatus;
    if (this.filterFrom) filter.from = this.filterFrom;
    if (this.filterTo) filter.to = this.filterTo;
    return filter;
  }

  private handleError(err: HttpErrorResponse): void {
    this.dashboard = null;
    this.loading = false;
    if (err.status === 401) {
      this.error = 'Su sesión expiró. Inicie sesión nuevamente';
    } else {
      this.error = 'No se pudieron cargar los KPIs';
    }
  }
}
