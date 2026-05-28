import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import {
  ContextualAssistantService,
  ContextualHelp,
} from '../../services/contextual-assistant.service';

@Component({
  selector: 'app-contextual-assistant',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './contextual-assistant.component.html',
  styleUrl: './contextual-assistant.component.scss',
})
export class ContextualAssistantComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly assistantService = inject(ContextualAssistantService);

  isOpen = false;
  help: ContextualHelp = this.assistantService.getHelpForUrl('/dashboard');

  private navSub?: Subscription;

  ngOnInit(): void {
    this.updateHelp(this.router.url);
    this.navSub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event) => {
        const url = (event as NavigationEnd).urlAfterRedirects;
        this.updateHelp(url);
      });
  }

  ngOnDestroy(): void {
    this.navSub?.unsubscribe();
  }

  togglePanel(): void {
    this.isOpen = !this.isOpen;
  }

  closePanel(): void {
    this.isOpen = false;
  }

  private updateHelp(url: string): void {
    this.help = this.assistantService.getHelpForUrl(url);
  }
}
