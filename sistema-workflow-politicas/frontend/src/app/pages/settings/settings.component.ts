import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AppSettings, AppTheme } from '../../core/constants';
import { SettingsService } from '../../services/settings.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent implements OnInit, OnDestroy {
  private readonly settingsService = inject(SettingsService);

  settings: AppSettings = this.settingsService.getSettings();
  message = '';
  private sub?: Subscription;

  ngOnInit(): void {
    this.sub = this.settingsService.settings$.subscribe((settings) => {
      this.settings = { ...settings };
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  onThemeChange(theme: AppTheme): void {
    this.settingsService.setTheme(theme);
    this.message = theme === 'dark' ? 'Tema oscuro aplicado' : 'Tema claro aplicado';
    this.clearMessageLater();
  }

  onAppNotificationsChange(enabled: boolean): void {
    this.settingsService.setAppNotifications(enabled);
    this.message = enabled
      ? 'Notificaciones en la app activadas'
      : 'Notificaciones en la app desactivadas';
    this.clearMessageLater();
  }

  onEmailNotificationsChange(enabled: boolean): void {
    this.settingsService.setEmailNotifications(enabled);
    this.message = enabled
      ? 'Preferencia de email guardada (envío real pendiente de implementación)'
      : 'Notificaciones por email desactivadas';
    this.clearMessageLater();
  }

  private clearMessageLater(): void {
    setTimeout(() => (this.message = ''), 3500);
  }
}
