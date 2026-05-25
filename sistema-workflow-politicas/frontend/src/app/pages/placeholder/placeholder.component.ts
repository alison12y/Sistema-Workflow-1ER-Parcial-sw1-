import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  template: `
    <div class="placeholder-page">
      <div class="placeholder-card">
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
        <span class="badge">Próximamente</span>
      </div>
    </div>
  `,
  styles: [
    `
      .placeholder-page {
        display: flex;
        justify-content: center;
        padding: 2rem 0;
      }
      .placeholder-card {
        max-width: 480px;
        width: 100%;
        text-align: center;
        padding: 3rem 2rem;
        background: var(--color-surface);
        border-radius: var(--radius-lg);
        border: 1px solid var(--color-border);
        box-shadow: var(--shadow-sm);
      }
      h1 {
        margin: 0 0 1rem;
        color: var(--color-accent);
        font-size: 1.5rem;
      }
      p {
        color: var(--color-muted);
        margin: 0 0 1.5rem;
      }
      .badge {
        display: inline-block;
        padding: 0.35rem 1rem;
        background: var(--color-cream);
        color: var(--color-primary);
        border-radius: 999px;
        font-size: 0.8rem;
        font-weight: 500;
      }
    `,
  ],
})
export class PlaceholderComponent {
  @Input() title = 'Módulo';
  @Input() description = 'Esta sección estará disponible en una próxima versión.';
}
