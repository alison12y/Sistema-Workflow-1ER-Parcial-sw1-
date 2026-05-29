import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { getVisibleNavItems, VisibleNavItem } from '../config/nav.config';

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
  roleLabel = this.auth.getRoleDisplayLabel();
  navItems: VisibleNavItem[] = getVisibleNavItems(this.auth);

  logout(): void {
    this.auth.logout();
  }
}
