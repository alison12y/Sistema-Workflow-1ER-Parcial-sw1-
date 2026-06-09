import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, interval, switchMap } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { DocumentRepositoryService } from '../../services/document-repository.service';
import { DocumentCollaborationService } from '../../services/document-collaboration.service';
import { DocumentEditorService } from '../../services/document-editor.service';
import {
  DOCUMENT_COLLABORATION_POLL_MS,
  DocumentCollaborationActiveLock,
  DocumentCollaborationRecentAction,
  DocumentCollaborationState,
  DocumentEditorSession,
} from '../../models/document-collaboration.model';
import { DocumentRecord } from '../../models/document-repository.model';
import { httpErrorMessage } from '../../utils/tramite-display.util';
import { validateDocumentUpload } from '../../utils/document-display.util';

declare global {
  interface Window {
    DocsAPI?: {
      DocEditor: new (elementId: string, config: Record<string, unknown>) => { destroyEditor?: () => void };
    };
  }
}

@Component({
  selector: 'app-document-edit',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './document-edit.component.html',
  styleUrl: './document-edit.component.scss',
})
export class DocumentEditComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly documentRepositoryService = inject(DocumentRepositoryService);
  private readonly collaborationService = inject(DocumentCollaborationService);
  private readonly editorService = inject(DocumentEditorService);

  tramiteId = '';
  documentId = '';
  repositoryId = '';
  sessionId = '';

  loading = true;
  error = '';
  message = '';
  session: DocumentEditorSession | null = null;
  collaborationState: DocumentCollaborationState | null = null;
  recentActions: DocumentCollaborationRecentAction[] = [];

  uploading = false;
  private pollSub?: Subscription;
  private docEditor?: { destroyEditor?: () => void };

  ngOnInit(): void {
    this.tramiteId = this.route.snapshot.paramMap.get('tramiteId') ?? '';
    this.documentId = this.route.snapshot.paramMap.get('documentId') ?? '';
    if (!this.tramiteId || !this.documentId) {
      this.error = 'Ruta de edición inválida';
      this.loading = false;
      return;
    }
    this.bootstrap();
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
    this.docEditor?.destroyEditor?.();
    if (this.repositoryId && this.sessionId) {
      if (!this.session?.readOnly) {
        this.editorService.closeEdit(this.documentId, this.repositoryId, this.sessionId).subscribe();
      }
      this.collaborationService.close(this.repositoryId, this.sessionId).subscribe();
    }
  }

  get document(): DocumentRecord | null {
    return this.session?.document ?? null;
  }

  get readOnly(): boolean {
    return !!this.session?.readOnly;
  }

  get fallbackMode(): boolean {
    return !!this.session?.fallbackMode;
  }

  get activeLock(): DocumentCollaborationActiveLock | undefined {
    const familyId = this.document?.documentFamilyId || this.document?.id;
    return this.collaborationState?.activeLocks?.find((lock) => lock.documentFamilyId === familyId);
  }

  lockLabel(): string | null {
    const lock = this.activeLock;
    if (!lock) return null;
    return `En edición por ${lock.displayName || lock.username}`;
  }

  formatDate(value?: string): string {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-BO');
  }

  onFallbackFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.repositoryId) return;
    const validationError = validateDocumentUpload(file);
    if (validationError) {
      this.error = validationError;
      input.value = '';
      return;
    }
    if (this.document && file.name.toLowerCase() !== this.document.nombreOriginal.toLowerCase()) {
      this.error = `Debe subir el mismo nombre de archivo (${this.document.nombreOriginal}) para crear una nueva versión.`;
      input.value = '';
      return;
    }
    this.uploading = true;
    this.error = '';
    this.documentRepositoryService.uploadDocument(this.repositoryId, file).subscribe({
      next: (uploaded) => {
        this.uploading = false;
        input.value = '';
        this.message = `Nueva versión v${uploaded.version} guardada correctamente.`;
        this.bootstrap();
      },
      error: (err) => {
        this.uploading = false;
        input.value = '';
        this.error = httpErrorMessage(err, 'No se pudo subir la nueva versión');
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/tramites', this.tramiteId]);
  }

  private bootstrap(): void {
    this.loading = true;
    this.error = '';
    this.documentRepositoryService.getByTramite(this.tramiteId).subscribe({
      next: (repo) => {
        this.repositoryId = repo.id;
        this.sessionId = this.ensureSessionId();
        this.collaborationService.open(this.repositoryId, this.sessionId).subscribe({
          next: () => this.loadEditorSession(),
          error: (err) => {
            this.loading = false;
            this.error = httpErrorMessage(err, 'No se pudo iniciar colaboración documental');
          },
        });
      },
      error: (err) => {
        this.loading = false;
        this.error = httpErrorMessage(err, 'No se pudo cargar el repositorio');
      },
    });
  }

  private loadEditorSession(): void {
    this.editorService.getEditorSession(this.documentId, this.repositoryId, this.sessionId).subscribe({
      next: (session) => {
        this.session = session;
        this.collaborationState = session.collaboration;
        this.recentActions = session.recentActions ?? session.collaboration?.recentActions ?? [];
        this.loading = false;
        this.startPolling();
        if (session.readOnly) {
          this.initOnlyOffice(session);
          return;
        }
        this.editorService.startEdit(this.documentId, this.repositoryId, this.sessionId).subscribe({
          next: (state) => {
            this.collaborationState = state;
            this.initOnlyOffice(session);
          },
          error: (err) => {
            this.error = httpErrorMessage(err, 'No se pudo tomar edición del documento');
            this.initOnlyOffice({ ...session, fallbackMode: true, onlyOfficeEnabled: false });
          },
        });
      },
      error: (err) => {
        this.loading = false;
        this.error = httpErrorMessage(err, 'No se pudo abrir la edición documental');
      },
    });
  }

  private initOnlyOffice(session: DocumentEditorSession): void {
    if (!session.onlyOfficeEnabled || !session.onlyOfficeApiScriptUrl || !session.onlyOfficeConfig) {
      return;
    }
    this.loadScript(session.onlyOfficeApiScriptUrl)
      .then(() => {
        if (!window.DocsAPI) {
          this.error = 'OnlyOffice no está disponible. Use el modo alternativo.';
          return;
        }
        this.docEditor = new window.DocsAPI.DocEditor('onlyoffice-editor', session.onlyOfficeConfig);
      })
      .catch(() => {
        this.error = 'No se pudo cargar OnlyOffice. Use el modo alternativo.';
      });
  }

  private startPolling(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = interval(DOCUMENT_COLLABORATION_POLL_MS)
      .pipe(switchMap(() => this.collaborationService.heartbeat(this.repositoryId, this.sessionId)))
      .subscribe({
        next: (state) => {
          this.collaborationState = state;
          if (state.recentActions?.length) {
            this.recentActions = state.recentActions;
          }
        },
      });
  }

  private loadScript(src: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (window.DocsAPI) {
        resolve();
        return;
      }
      const existing = document.querySelector(`script[src="${src}"]`);
      if (existing) {
        existing.addEventListener('load', () => resolve());
        existing.addEventListener('error', () => reject());
        return;
      }
      const script = document.createElement('script');
      script.src = src;
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject();
      document.body.appendChild(script);
    });
  }

  private ensureSessionId(): string {
    const user = this.auth.getCurrentUser()?.username ?? 'anon';
    const key = `document-collaboration-session:${this.repositoryId}:${user}`;
    let id = sessionStorage.getItem(key);
    if (!id) {
      id = crypto.randomUUID();
      sessionStorage.setItem(key, id);
    }
    return id;
  }
}
