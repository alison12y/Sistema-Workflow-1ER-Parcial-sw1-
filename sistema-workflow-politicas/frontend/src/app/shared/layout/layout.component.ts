import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent {
  private readonly auth = inject(AuthService);

  user = this.auth.getCurrentUser();

  navItems: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
    { path: '/policies', label: 'Políticas', icon: 'description' },
    { path: '/tramites', label: 'Trámites', icon: 'assignment' },
    { path: '/monitoring', label: 'Monitoreo', icon: 'timeline' },
    { path: '/kpi', label: 'KPIs', icon: 'insert_chart' },
    { path: '/users', label: 'Usuarios', icon: 'people' },
    { path: '/roles', label: 'Roles', icon: 'security' },
    { path: '/departments', label: 'Departamentos', icon: 'business' },
    { path: '/ai-assistant', label: 'Asistente IA', icon: 'auto_awesome' },
  ];

  logout(): void {
    this.auth.logout();
  }
}
