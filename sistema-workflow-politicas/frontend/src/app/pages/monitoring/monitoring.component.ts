import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MonitoringService } from '../../services/monitoring.service';

@Component({
  selector: 'app-monitoring',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './monitoring.component.html',
  styleUrl: './monitoring.component.scss'
})
export class MonitoringComponent implements OnInit {
  private readonly monitoringService = inject(MonitoringService);

  processes: any[] = [];
  loading = true;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.monitoringService.getProcesses().subscribe({
      next: (data) => {
        this.processes = data.length ? data : this.getMockProcesses();
        this.loading = false;
      },
      error: () => {
        this.processes = this.getMockProcesses();
        this.loading = false;
      }
    });
  }

  getMockProcesses() {
    return [
      { id: 'TRM-001', policyName: 'Solicitud de Permiso Laboral', status: 'En proceso', currentActivity: 'Validar información', responsible: 'Recursos Humanos', timeElapsed: '2 días', progress: 60 },
      { id: 'TRM-002', policyName: 'Solicitud de Compra de Materiales', status: 'En revisión', currentActivity: 'Aprobar presupuesto', responsible: 'Administración', timeElapsed: '5 horas', progress: 30 },
      { id: 'TRM-003', policyName: 'Aprobación de Documento Interno', status: 'Finalizado', currentActivity: 'Notificar resultado', responsible: 'Sistemas', timeElapsed: '3 días', progress: 100 },
      { id: 'TRM-004', policyName: 'Atención de Reclamo', status: 'Iniciado', currentActivity: 'Registrar solicitud', responsible: 'Atención al Cliente', timeElapsed: '15 min', progress: 10 }
    ];
  }

  statusBadgeClass(status: string): string {
    const s = status.toLowerCase();
    if (s.includes('finalizado')) return 'active';
    if (s.includes('proceso') || s.includes('revisión')) return 'versioned';
    if (s.includes('iniciado')) return 'draft';
    return 'inactive';
  }
}
