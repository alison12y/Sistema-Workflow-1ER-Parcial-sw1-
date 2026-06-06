import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineSyncService } from '../../core/offline/offline-sync.service';
import { getVisibleNavItems, VisibleNavItem } from '../config/nav.config';
import { getVisibleMobileNavItems, MobileNavItem } from '../config/mobile-nav.config';
import { ConnectivityBannerComponent } from '../components/connectivity-banner/connectivity-banner.component';
import { PwaInstallComponent } from '../components/pwa-install/pwa-install.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    ConnectivityBannerComponent,
    PwaInstallComponent,
  ],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly offlineSync = inject(OfflineSyncService);
  readonly connectivity = inject(ConnectivityService);

  user = this.auth.getCurrentUser();
  roleLabel = this.auth.getRoleDisplayLabel();
  navItems: VisibleNavItem[] = getVisibleNavItems(this.auth);
  mobileNavItems: MobileNavItem[] = getVisibleMobileNavItems(this.auth);
  sidebarOpen = false;

  ngOnInit(): void {
    void this.connectivity.refreshPendingCount();
    if (this.connectivity.isOnline) {
      void this.offlineSync.syncPending().finally(() => this.connectivity.refreshPendingCount());
    }
  }

  logout(): void {
    this.auth.logout();
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }
}
