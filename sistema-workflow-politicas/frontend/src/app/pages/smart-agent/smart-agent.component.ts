import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { TimeoutError } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { SmartAgentService } from '../../services/smart-agent.service';
import { PolicyService } from '../../services/policy.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { BusinessPolicy } from '../../models/auth.model';
import { UserDto } from '../../models/api.models';
import { SmartAgentAnalyzeResponse } from '../../models/smart-agent.model';
import { httpErrorMessage } from '../../utils/tramite-display.util';
import { isNetworkError } from '../../utils/network-error.util';
import { matchSmartAgentFallback } from '../../utils/smart-agent-fallback.util';

@Component({
  selector: 'app-smart-agent',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './smart-agent.component.html',
  styleUrl: './smart-agent.component.scss',
})
export class SmartAgentComponent implements OnInit, OnDestroy {
  private readonly smartAgentService = inject(SmartAgentService);
  private readonly policyService = inject(PolicyService);
  private readonly userService = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  message = '';
  audioText = '';
  requesterName = '';
  requestedBy = '';
  priority = 'NORMAL';
  selectedFile: File | null = null;

  users: UserDto[] = [];
  policies: BusinessPolicy[] = [];
  analyzing = false;
  starting = false;
  listening = false;
  error = '';
  success = '';
  result: SmartAgentAnalyzeResponse | null = null;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private speechRecognition: any = null;

  readonly priorities = [
    { value: 'BAJA', label: 'Baja' },
    { value: 'NORMAL', label: 'Normal' },
    { value: 'ALTA', label: 'Alta' },
    { value: 'URGENTE', label: 'Urgente' },
  ];

  ngOnInit(): void {
    const current = this.auth.getCurrentUser();
    this.requesterName = current?.fullName ?? current?.username ?? '';
    this.requestedBy = current?.username ?? '';
    this.loadUsers();
    this.loadPolicies();
    this.initSpeech();
  }

  ngOnDestroy(): void {
    this.stopListening();
  }

  loadUsers(): void {
    this.userService.getAll().subscribe({
      next: (users) => {
        this.users = users.filter((u) => u.active !== false);
      },
      error: () => {
        this.users = [];
      },
    });
  }

  loadPolicies(): void {
    this.policyService.getAll().subscribe({
      next: (policies) => {
        this.policies = (policies ?? []).filter((p) => p.status?.toUpperCase() === 'ACTIVE');
      },
      error: () => {
        this.policies = [];
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  clearAttachment(): void {
    this.selectedFile = null;
  }

  analyze(): void {
    const text = this.message.trim();
    const voice = this.audioText.trim();
    if (!text && !voice) {
      this.error = 'Describa su solicitud por texto o dictado por voz.';
      return;
    }

    this.analyzing = true;
    this.error = '';
    this.success = '';
    this.result = null;

    const combinedText = this.buildCombinedText(text, voice);

    this.smartAgentService
      .analyze(
        {
          message: text || voice,
          audioText: voice || undefined,
          requesterName: this.requesterName || undefined,
        },
        this.selectedFile,
      )
      .pipe(finalize(() => {
        this.analyzing = false;
      }))
      .subscribe({
        next: (response) => {
          this.result = response;
          this.applySuggestedDescription(response);
        },
        error: (err) => {
          if (this.shouldUseLocalFallback(err)) {
            this.applyLocalFallback(combinedText);
            return;
          }
          this.error = httpErrorMessage(err, 'No se pudo analizar la solicitud');
        },
      });
  }

  startTramite(): void {
    if (!this.result?.recommendedPolicyId) {
      this.error = 'No hay política recomendada para iniciar el trámite.';
      return;
    }
    if (!this.requestedBy) {
      this.error = 'Seleccione el solicitante del trámite.';
      return;
    }

    const description = this.message.trim() || this.audioText.trim() || this.result.explanation || 'Solicitud vía agente inteligente';

    this.starting = true;
    this.error = '';
    this.success = '';

    this.smartAgentService
      .startTramite(
        {
          policyId: this.result.recommendedPolicyId,
          description,
          requestedBy: this.requestedBy,
          priority: this.priority,
          detectedIntent: this.result.detectedIntent,
          agentExplanation: this.result.explanation,
        },
        this.selectedFile,
      )
      .pipe(finalize(() => {
        this.starting = false;
      }))
      .subscribe({
        next: (response) => {
          this.success = response.message ?? `Trámite ${response.tramite.code} iniciado correctamente.`;
          if (response.tramite?.id) {
            setTimeout(() => this.router.navigate(['/tramites', response.tramite.id]), 900);
          }
        },
        error: (err) => {
          this.error = httpErrorMessage(err, 'No se pudo iniciar el trámite');
        },
      });
  }

  toggleVoice(): void {
    if (this.listening) {
      this.stopListening();
      return;
    }
    if (!this.speechRecognition) {
      this.error = 'El dictado por voz no está disponible en este navegador.';
      return;
    }
    this.error = '';
    this.listening = true;
    this.speechRecognition.start();
  }

  confidencePercent(): string {
    const score = this.result?.confidenceScore ?? 0;
    return `${Math.round(score * 100)}%`;
  }

  sourceLabel(): string {
    return this.result?.source === 'AI_SERVICE' ? 'Servicio IA' : 'Motor local';
  }

  private initSpeech(): void {
    const win = window as Window & { SpeechRecognition?: new () => unknown; webkitSpeechRecognition?: new () => unknown };
    const ctor = win.SpeechRecognition ?? win.webkitSpeechRecognition;
    if (!ctor) return;

    this.speechRecognition = new ctor();
    this.speechRecognition.lang = 'es-BO';
    this.speechRecognition.interimResults = false;
    this.speechRecognition.maxAlternatives = 1;

    this.speechRecognition.onresult = (event: { results: { [index: number]: { [index: number]: { transcript?: string } } } }) => {
      const transcript = event.results[0]?.[0]?.transcript?.trim();
      if (transcript) {
        this.audioText = this.audioText ? `${this.audioText} ${transcript}` : transcript;
        if (!this.message.trim()) {
          this.message = transcript;
        }
      }
    };

    this.speechRecognition.onerror = () => {
      this.listening = false;
      this.error = 'No se pudo capturar el dictado por voz.';
    };

    this.speechRecognition.onend = () => {
      this.listening = false;
    };
  }

  private stopListening(): void {
    if (this.speechRecognition && this.listening) {
      this.speechRecognition.stop();
    }
    this.listening = false;
  }

  private buildCombinedText(text: string, voice: string): string {
    const parts = [text, voice !== text ? voice : ''].filter((part) => !!part?.trim());
    let combined = parts.join(' ').trim();
    if (this.selectedFile?.name) {
      combined = combined
        ? `${combined} Documento adjunto: ${this.selectedFile.name}`
        : `Documento adjunto: ${this.selectedFile.name}`;
    }
    return combined;
  }

  private applySuggestedDescription(response: SmartAgentAnalyzeResponse): void {
    if (!response.suggestedFields?.length) return;
    const descriptionField = response.suggestedFields.find((f) => f.name === 'description');
    if (descriptionField?.suggestedValue && !this.message.trim()) {
      this.message = descriptionField.suggestedValue;
    }
  }

  private applyLocalFallback(combinedText: string): void {
    this.error = '';
    this.result = matchSmartAgentFallback(
      combinedText,
      this.policies,
      this.requesterName || undefined,
      this.selectedFile?.name,
    );
    this.applySuggestedDescription(this.result);
  }

  private shouldUseLocalFallback(err: unknown): boolean {
    if (err instanceof TimeoutError) {
      return true;
    }
    if (isNetworkError(err)) {
      return true;
    }
    if (err instanceof HttpErrorResponse) {
      if (err.status === 403 || err.status === 401 || err.status === 400) {
        return false;
      }
      return err.status === 0 || err.status >= 500 || err.status === 408 || err.status === 504;
    }
    return false;
  }
}
