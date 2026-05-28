import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import {
  AppSettings,
  AppTheme,
  DEFAULT_SETTINGS,
  SETTINGS_KEY,
} from '../core/constants';

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly settingsSubject = new BehaviorSubject<AppSettings>(DEFAULT_SETTINGS);
  readonly settings$ = this.settingsSubject.asObservable();

  constructor() {
    this.loadAndApply();
  }

  getSettings(): AppSettings {
    return { ...this.settingsSubject.value };
  }

  setTheme(theme: AppTheme): void {
    this.updateSettings({ theme });
    this.applyTheme(theme);
  }

  setAppNotifications(enabled: boolean): void {
    this.updateSettings({ appNotifications: enabled });
  }

  setEmailNotifications(enabled: boolean): void {
    this.updateSettings({ emailNotifications: enabled });
  }

  private loadAndApply(): void {
    const stored = this.readStored();
    this.settingsSubject.next(stored);
    this.applyTheme(stored.theme);
  }

  private updateSettings(partial: Partial<AppSettings>): void {
    const next = { ...this.settingsSubject.value, ...partial };
    this.settingsSubject.next(next);
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(next));
  }

  private readStored(): AppSettings {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (!raw) {
      return { ...DEFAULT_SETTINGS };
    }
    try {
      const parsed = JSON.parse(raw) as Partial<AppSettings>;
      return {
        theme: parsed.theme === 'dark' ? 'dark' : 'light',
        appNotifications: parsed.appNotifications !== false,
        emailNotifications: parsed.emailNotifications === true,
      };
    } catch {
      return { ...DEFAULT_SETTINGS };
    }
  }

  private applyTheme(theme: AppTheme): void {
    document.documentElement.setAttribute('data-theme', theme);
  }
}
