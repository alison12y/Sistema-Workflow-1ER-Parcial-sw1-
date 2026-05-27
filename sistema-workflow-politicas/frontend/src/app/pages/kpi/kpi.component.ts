import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KpiService } from '../../services/kpi.service';

@Component({
  selector: 'app-kpi',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kpi.component.html',
  styleUrl: './kpi.component.scss'
})
export class KpiComponent implements OnInit {
  private readonly kpiService = inject(KpiService);

  dashboardData: any = null;
  bottlenecks: any[] = [];
  loading = true;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.kpiService.getDashboard().subscribe({
      next: (data) => {
        this.dashboardData = data || this.getMockDashboard();
        this.loadBottlenecks();
      },
      error: () => {
        this.dashboardData = this.getMockDashboard();
        this.loadBottlenecks();
      }
    });
  }

  loadBottlenecks(): void {
    this.kpiService.getBottlenecks().subscribe({
      next: (data) => {
        this.bottlenecks = data?.activitiesWithDelays || this.getMockBottlenecks();
        this.loading = false;
      },
      error: () => {
        this.bottlenecks = this.getMockBottlenecks();
        this.loading = false;
      }
    });
  }

  getMockDashboard() {
    return {
      averageProcessTime: '3.5 días',
      averageActivityTime: '8 horas',
      totalPendingProcesses: 24,
      totalCompletedProcesses: 156,
      delayedTasksCount: 5,
      complianceRate: '92%'
    };
  }

  getMockBottlenecks() {
    return [
      { activityName: 'Revisión de solicitud', averageDelay: '3 días', instanceCount: 12, responsible: 'Supervisor', motive: 'Alta carga de tareas pendientes' },
      { activityName: 'Validar presupuesto', averageDelay: '1.5 días', instanceCount: 8, responsible: 'Administración', motive: 'Falta de documentación de respaldo' },
      { activityName: 'Firma de gerencia', averageDelay: '2 días', instanceCount: 5, responsible: 'Gerente General', motive: 'Disponibilidad limitada' }
    ];
  }
}
