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
    '¿Cómo diseñar un workflow UML 2.5 con carriles y decisiones?',
    '¿Cómo usar dictado por voz en el diseñador workflow?',
    '¿Para qué sirve el nombre técnico en formularios?',
    '¿Cómo funciona la asistencia IA en formularios?',
    '¿Cómo se ejecuta un trámite y avanza el motor?',
    '¿Qué muestra el monitoreo, KPIs y la colaboración?',
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
