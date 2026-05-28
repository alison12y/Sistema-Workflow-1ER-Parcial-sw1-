import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface DashCard {
  title: string;
  description: string;
  path: string;
  icon: string;
  accent: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  cards: DashCard[] = [
    {
      title: 'Políticas de Negocio',
      description: 'Configurar políticas, estados del ciclo de vida y workflows',
      path: '/policies',
      icon: 'description',
      accent: 'primary',
    },
    {
      title: 'Trámites',
      description: 'Iniciar y gestionar trámites a partir de políticas existentes',
      path: '/tramites',
      icon: 'assignment',
      accent: 'blue',
    },
    {
      title: 'Monitoreo',
      description: 'Seguimiento en tiempo real de trámites y procesos',
      path: '/monitoring',
      icon: 'timeline',
      accent: 'blue',
    },
    {
      title: 'KPIs e Indicadores',
      description: 'Consultar tiempos promedio y cuellos de botella',
      path: '/kpis',
      icon: 'insert_chart',
      accent: 'lavender',
    },
    {
      title: 'Usuarios',
      description: 'Administrar cuentas, accesos y asignación de roles',
      path: '/users',
      icon: 'people',
      accent: 'cream',
    },
    {
      title: 'Roles y Permisos',
      description: 'Definir perfiles de acceso y seguridad del sistema',
      path: '/roles',
      icon: 'security',
      accent: 'violet',
    },
    {
      title: 'Departamentos',
      description: 'Organizar la estructura jerárquica y responsables',
      path: '/departments',
      icon: 'business',
      accent: 'blue',
    },
    {
      title: 'Asistente IA',
      description: 'Sugerencias inteligentes para diseño de workflows',
      path: '/ai-assistant',
      icon: 'auto_awesome',
      accent: 'violet',
    },
  ];
}
