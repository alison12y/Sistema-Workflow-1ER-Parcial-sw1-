import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiAssistantService } from '../../services/ai-assistant.service';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-assistant.component.html',
  styleUrl: './ai-assistant.component.scss'
})
export class AiAssistantComponent {
  private readonly aiAssistant = inject(AiAssistantService);

  prompt = '';
  loading = false;
  response = '';
  suggestions: string[] = [
    '¿Cómo sugiero un flujo para solicitud de permiso laboral?',
    '¿Cómo uso la Ayuda IA en el formulario de ejecución?',
    'Explícame cómo completar actividades con el asistente local.',
    '¿Cómo detectar cuellos de botella en un proceso?'
  ];

  sendPrompt(text?: string): void {
    const message = text || this.prompt;
    if (!message.trim()) return;

    this.loading = true;
    this.response = '';

    setTimeout(() => {
      this.loading = false;
      this.response = this.aiAssistant.getGeneralAnswer(message);
      this.prompt = '';
    }, 400);
  }

  copyResponse(): void {
    if (!this.response) return;
    navigator.clipboard?.writeText(this.response);
  }
}
