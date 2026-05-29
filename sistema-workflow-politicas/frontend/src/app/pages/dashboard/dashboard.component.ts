import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { DashboardService } from '../../services/dashboard.service';
import { DashboardStats } from '../../models/workflow.model';
import { getWelcomeMessage } from '../../shared/config/nav.config';

interface StatCard {
  label: string;
  value: number;
  icon: string;
  hint: string;
  accent: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);

  roleLabel = '';
  welcomeMessage = '';
  accessDenied = false;
  loading = true;
  error = '';
  stats: DashboardStats | null = null;
  statCards: StatCard[] = [];

  quickLinks = [
    { path: '/policies', label: 'Políticas de negocio', icon: 'description', permissions: ['POLICIES_MANAGE'] },
    { path: '/tramites', label: 'Trámites', icon: 'assignment', permissions: ['TASKS_EXECUTE', 'POLICIES_MANAGE'] },
    { path: '/mis-actividades', label: 'Mis tareas', icon: 'assignment_turned_in', permissions: ['TASKS_EXECUTE'] },
    { path: '/seguimiento', label: 'Seguimiento', icon: 'track_changes', permissions: ['MONITORING_VIEW', 'REPORTS_VIEW'] },
    { path: '/monitoring', label: 'Monitoreo', icon: 'timeline', permissions: ['MONITORING_VIEW'] },
    { path: '/kpis', label: 'KPIs', icon: 'insert_chart', permissions: ['KPI_VIEW'] },
  ];

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    this.roleLabel = user?.roleName ?? this.auth.getRoleDisplayLabel();
    this.welcomeMessage = getWelcomeMessage(this.roleLabel);

    const params = new URLSearchParams(window.location.search);
    this.accessDenied = params.get('acceso') === 'denegado';

    this.loadStats();
  }

  visibleQuickLinks() {
    return this.quickLinks.filter((l) => this.auth.hasAnyPermission(l.permissions));
  }

  loadStats(): void {
    this.loading = true;
    this.error = '';
    this.dashboardService.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.buildStatCards(stats);
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar las métricas del panel workflow.';
        this.stats = {
          politicasActivas: 0,
          politicasBorrador: 0,
          tramitesEnProceso: 0,
          tareasPendientes: 0,
          tareasFinalizadas: 0,
          tramitesObservados: 0,
          posiblesCuellosDeBotella: 0,
        };
        this.buildStatCards(this.stats);
        this.loading = false;
      },
    });
  }

  hasAnyData(): boolean {
    if (!this.stats) return false;
    return Object.values(this.stats).some((v) => v > 0);
  }

  private buildStatCards(stats: DashboardStats): void {
    this.statCards = [
      { label: 'Políticas activas', value: stats.politicasActivas, icon: 'description', hint: 'Políticas en producción', accent: 'primary' },
      { label: 'Políticas en borrador', value: stats.politicasBorrador, icon: 'edit_note', hint: 'Pendientes de activación', accent: 'draft' },
      { label: 'Trámites en proceso', value: stats.tramitesEnProceso, icon: 'assignment', hint: 'Ejecuciones activas', accent: 'blue' },
      { label: 'Tareas pendientes', value: stats.tareasPendientes, icon: 'pending_actions', hint: 'Por atender', accent: 'warning' },
      { label: 'Tareas finalizadas', value: stats.tareasFinalizadas, icon: 'task_alt', hint: 'Completadas en trámites', accent: 'success' },
      { label: 'Trámites observados', value: stats.tramitesObservados, icon: 'report_problem', hint: 'Con observaciones registradas', accent: 'orange' },
      { label: 'Posibles cuellos de botella', value: stats.posiblesCuellosDeBotella, icon: 'speed', hint: 'Indicador preliminar', accent: 'violet' },
    ];
  }
}
