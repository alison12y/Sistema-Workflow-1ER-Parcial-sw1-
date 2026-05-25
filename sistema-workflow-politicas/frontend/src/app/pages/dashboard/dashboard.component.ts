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
      title: 'Usuarios',
      description: 'Administrar cuentas y accesos del sistema',
      path: '/users',
      icon: '◎',
      accent: 'lavender',
    },
    {
      title: 'Roles',
      description: 'Definir permisos y perfiles de acceso',
      path: '/roles',
      icon: '◇',
      accent: 'blue',
    },
    {
      title: 'Departamentos',
      description: 'Organizar áreas y responsables',
      path: '/departments',
      icon: '▤',
      accent: 'cream',
    },
    {
      title: 'Políticas',
      description: 'Configurar políticas de negocio y workflows',
      path: '/policies',
      icon: '◈',
      accent: 'primary',
    },
    {
      title: 'Asistente IA',
      description: 'Sugerencias inteligentes con confirmación',
      path: '/ai-assistant',
      icon: '✦',
      accent: 'violet',
    },
  ];
}
