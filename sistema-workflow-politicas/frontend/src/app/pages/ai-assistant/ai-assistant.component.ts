import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService } from '../../services/ai.service';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-assistant.component.html',
  styleUrl: './ai-assistant.component.scss'
})
export class AiAssistantComponent {
  private readonly aiService = inject(AiService);

  prompt: string = '';
  loading: boolean = false;
  response: string = '';
  suggestions: string[] = [
    'Crear un flujo para solicitud de permiso laboral.',
    'Sugerir actividades para una solicitud de compra.',
    'Redactar una justificación formal para permiso familiar.',
    'Revisar si el workflow tiene actividades sin responsable.'
  ];

  sendPrompt(text?: string): void {
    const message = text || this.prompt;
    if (!message.trim()) return;

    this.loading = true;
    this.response = '';
    
    // Simulate AI response for the demo if backend is not ready
    setTimeout(() => {
      this.loading = false;
      this.response = this.generateMockResponse(message);
      this.prompt = '';
    }, 1500);
  }

  private generateMockResponse(prompt: string): string {
    const p = prompt.toLowerCase();
    if (p.includes('permiso laboral')) {
      return "Sugerencia de Workflow para Permiso Laboral:\n\n1. Inicio\n2. Registrar solicitud (Funcionario)\n3. Validar información (Recursos Humanos)\n4. Revisión de saldo de vacaciones (RRHH)\n5. Decisión: ¿Aprobar?\n6. Notificar resultado (Funcionario)\n7. Fin";
    }
    if (p.includes('compra')) {
      return "Actividades sugeridas para Solicitud de Compra:\n\n1. Cotización de materiales\n2. Verificación de presupuesto\n3. Aprobación de Gerencia Administrativa\n4. Emisión de orden de compra\n5. Recepción de materiales";
    }
    if (p.includes('justificación')) {
      return "Borrador sugerido:\n\n'Por la presente, solicito permiso laboral por motivos familiares urgentes. Me comprometo a regularizar mis actividades pendientes a la brevedad posible.'";
    }
    return "Como asistente IA de Workflow, puedo ayudarte a diseñar diagramas, sugerir campos para tus formularios o redactar comunicaciones formales para tus trámites.";
  }
}
