import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription, filter } from 'rxjs';
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
export class LayoutComponent implements OnInit, OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly offlineSync = inject(OfflineSyncService);
  private readonly router = inject(Router);
  readonly connectivity = inject(ConnectivityService);

  user = this.auth.getCurrentUser();
  roleLabel = this.auth.getRoleDisplayLabel();
  navItems: VisibleNavItem[] = getVisibleNavItems(this.auth);
  mobileNavItems: MobileNavItem[] = getVisibleMobileNavItems(this.auth);
  sidebarOpen = false;
  documentEditMode = false;

  private routerSub?: Subscription;

  ngOnInit(): void {
    void this.connectivity.refreshPendingCount();
    if (this.connectivity.isOnline) {
      void this.offlineSync.syncPending().finally(() => this.connectivity.refreshPendingCount());
    }
    this.updateDocumentEditMode(this.router.url);
    this.routerSub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event) => this.updateDocumentEditMode((event as NavigationEnd).urlAfterRedirects));
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
  }

  private updateDocumentEditMode(url: string): void {
    this.documentEditMode = url.includes('/tramites/') && url.includes('/documentos/');
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
