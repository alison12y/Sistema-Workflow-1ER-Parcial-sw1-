import { Component } from '@angular/core';
import { PlaceholderComponent } from '../placeholder/placeholder.component';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [PlaceholderComponent],
  template: `<app-placeholder title="Asistente IA" description="Sugerencias inteligentes con confirmación antes de aplicar cambios." />`,
})
export class AiAssistantComponent {}
