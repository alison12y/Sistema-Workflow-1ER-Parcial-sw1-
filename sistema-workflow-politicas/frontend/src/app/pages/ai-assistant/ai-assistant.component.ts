import { Component } from '@angular/core';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  template: `
    <div class="page-crud">
      <div class="page-toolbar">
        <div>
          <h1>Asistente IA</h1>
          <p>Sugerencias con confirmación — disponible en fase siguiente</p>
        </div>
      </div>
      <div class="data-card">
        <div class="empty-state">Módulo de asistente IA (Fase 10) — use el dashboard para acceder cuando esté listo.</div>
      </div>
    </div>
  `,
})
export class AiAssistantComponent {}
