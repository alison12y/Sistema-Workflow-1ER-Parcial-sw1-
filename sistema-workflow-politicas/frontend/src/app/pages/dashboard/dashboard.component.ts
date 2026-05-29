import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NAV_ITEMS, VisibleNavItem, getVisibleNavItems } from '../../shared/config/nav.config';

interface DashCard {
  title: string;
  description: string;
  path: string;
  icon: string;
  accent: string;
  disabled?: boolean;
  disabledMessage?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly auth = inject(AuthService);

  cards: DashCard[] = [];
  roleLabel = '';
  accessDenied = false;

  private readonly cardMeta: Record<string, Omit<DashCard, 'path'>> = {
    '/dashboard': {
      title: 'Resumen',
      description: 'Vista general del sistema workflow',
      icon: 'dashboard',
      accent: 'primary',
    },
    '/policies': {
      title: 'Políticas de Negocio',
      description: 'Configurar políticas y reglas de negocio',
      icon: 'description',
      accent: 'primary',
    },
    '/tramites': {
      title: 'Trámites',
      description: 'Iniciar y gestionar trámites de procesos',
      icon: 'assignment',
      accent: 'blue',
    },
    '/mis-actividades': {
      title: 'Mis tareas',
      description: 'Actividades asignadas pendientes de ejecución',
      icon: 'assignment_turned_in',
      accent: 'blue',
    },
    '/monitoring': {
      title: 'Monitoreo',
      description: 'Seguimiento en tiempo real de trámites',
      icon: 'timeline',
      accent: 'blue',
    },
    '/kpis': {
      title: 'KPIs e Indicadores',
      description: 'Consultar tiempos promedio e indicadores',
      icon: 'insert_chart',
      accent: 'lavender',
    },
    '/users': {
      title: 'Usuarios',
      description: 'Administrar cuentas y asignación de roles',
      icon: 'people',
      accent: 'cream',
    },
    '/roles': {
      title: 'Roles y Permisos',
      description: 'Definir perfiles de acceso del sistema',
      icon: 'security',
      accent: 'violet',
    },
    '/departments': {
      title: 'Departamentos',
      description: 'Organizar la estructura y responsables',
      icon: 'business',
      accent: 'blue',
    },
    '/bitacora': {
      title: 'Bitácora',
      description: 'Historial de acciones y auditoría',
      icon: 'history',
      accent: 'violet',
    },
    '/settings': {
      title: 'Configuración',
      description: 'Parámetros generales del sistema',
      icon: 'settings',
      accent: 'cream',
    },
  };

  ngOnInit(): void {
    this.roleLabel = this.auth.getRoleDisplayLabel();
    this.buildCards();

    const params = new URLSearchParams(window.location.search);
    this.accessDenied = params.get('acceso') === 'denegado';
  }

  private buildCards(): void {
    const navItems: VisibleNavItem[] = getVisibleNavItems(this.auth).filter((item) => item.path !== '/dashboard');
    const result: DashCard[] = [];

    for (const item of navItems) {
      const meta = this.cardMeta[item.path];
      if (!meta) continue;
      result.push({
        path: item.path,
        ...meta,
        disabled: item.disabled,
        disabledMessage: item.pendingMessage,
      });
    }

    this.cards = result;
  }
}
