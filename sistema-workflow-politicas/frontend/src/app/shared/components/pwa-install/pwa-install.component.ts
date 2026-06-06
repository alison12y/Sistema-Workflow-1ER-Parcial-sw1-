import { Component, OnInit } from '@angular/core';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

@Component({
  selector: 'app-pwa-install',
  standalone: true,
  template: `
    @if (canInstall) {
      <button type="button" class="pwa-install-btn" (click)="install()">
        <i class="material-icons">install_mobile</i>
        Instalar app
      </button>
    }
  `,
  styles: `
    .pwa-install-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      padding: 0.4rem 0.75rem;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      background: var(--color-cream);
      color: var(--color-accent);
      font-size: 0.8rem;
      cursor: pointer;

      .material-icons {
        font-size: 1rem;
      }
    }
  `,
})
export class PwaInstallComponent implements OnInit {
  private deferredPrompt: BeforeInstallPromptEvent | null = null;
  canInstall = false;

  ngOnInit(): void {
    window.addEventListener('beforeinstallprompt', (event) => {
      event.preventDefault();
      this.deferredPrompt = event as BeforeInstallPromptEvent;
      this.canInstall = true;
    });
    window.addEventListener('appinstalled', () => {
      this.canInstall = false;
      this.deferredPrompt = null;
    });
  }

  async install(): Promise<void> {
    if (!this.deferredPrompt) {
      return;
    }
    await this.deferredPrompt.prompt();
    await this.deferredPrompt.userChoice;
    this.deferredPrompt = null;
    this.canInstall = false;
  }
}
