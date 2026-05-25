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
    { path: '/dashboard', label: 'Dashboard', icon: '▣' },
    { path: '/policies', label: 'Políticas', icon: '◈' },
    { path: '/users', label: 'Usuarios', icon: '◎' },
    { path: '/roles', label: 'Roles', icon: '◇' },
    { path: '/departments', label: 'Departamentos', icon: '▤' },
    { path: '/ai-assistant', label: 'Asistente IA', icon: '✦' },
  ];

  logout(): void {
    this.auth.logout();
  }
}
