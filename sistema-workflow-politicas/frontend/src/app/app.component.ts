import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ContextualAssistantComponent } from './shared/contextual-assistant/contextual-assistant.component';
import { SettingsService } from './services/settings.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ContextualAssistantComponent],
  template: `
    <router-outlet />
    <app-contextual-assistant />
  `,
  styles: [':host { display: block; min-height: 100vh; }'],
})
export class AppComponent {
  constructor(_settings: SettingsService) {}
}
