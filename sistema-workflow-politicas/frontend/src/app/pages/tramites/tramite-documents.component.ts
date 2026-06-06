import { Component, ElementRef, Input, OnChanges, SimpleChanges, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';

import { AuthService } from '../../services/auth.service';
import { DocumentRepositoryService } from '../../services/document-repository.service';
import { DocumentRecord, DocumentRepository } from '../../models/document-repository.model';
import {
  ACCEPTED_DOCUMENT_EXTENSIONS,
  documentStatusClass,
  documentStatusLabel,
  documentTypeLabel,
  formatFileSize,
  isKnownDocumentExtension,
  isPreviewableDocument,
  validateDocumentUpload,
} from '../../utils/document-display.util';
import { httpErrorMessage } from '../../utils/tramite-display.util';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineSyncService } from '../../core/offline/offline-sync.service';
import { shouldQueueOffline } from '../../utils/network-error.util';
import { DocumentDetailDialogComponent } from './document-detail-dialog.component';



type LoadPhase = 'idle' | 'loading' | 'ready' | 'no-repository' | 'error';



@Component({

  selector: 'app-tramite-documents',

  standalone: true,

  imports: [CommonModule],

  templateUrl: './tramite-documents.component.html',

  styleUrl: './tramite-documents.component.scss',

})

export class TramiteDocumentsComponent implements OnChanges {

  private readonly documentService = inject(DocumentRepositoryService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly connectivity = inject(ConnectivityService);
  private readonly offlineSync = inject(OfflineSyncService);



  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;



  @Input({ required: true }) tramiteId!: string;

  @Input() syncKey = 0;



  repository: DocumentRepository | null = null;

  documents: DocumentRecord[] = [];

  phase: LoadPhase = 'idle';

  loadingDocuments = false;

  uploading = false;

  actingOnDocumentId: string | null = null;



  expandedFamilyId: string | null = null;

  versionHistory: DocumentRecord[] = [];

  loadingVersions = false;

  versionsError = '';



  message = '';

  error = '';



  deleteModalOpen = false;

  selectedDocument: DocumentRecord | null = null;



  readonly canViewDocuments = this.auth.canViewDocuments();

  readonly canUploadDocuments = this.auth.canUploadDocuments();

  readonly canDeleteDocuments = this.auth.canDeleteDocuments();



  readonly statusLabel = documentStatusLabel;

  readonly statusClass = documentStatusClass;

  readonly typeLabel = documentTypeLabel;

  readonly formatSize = formatFileSize;

  readonly acceptedExtensions = ACCEPTED_DOCUMENT_EXTENSIONS;



  ngOnChanges(changes: SimpleChanges): void {

    if (changes['tramiteId'] || changes['syncKey']) {

      this.loadRepository();

    }

  }



  loadRepository(): void {

    if (!this.tramiteId || !this.canViewDocuments) {

      this.phase = 'idle';

      return;

    }



    this.phase = 'loading';

    this.error = '';

    this.repository = null;

    this.documents = [];

    this.expandedFamilyId = null;

    this.versionHistory = [];



    this.documentService.getByTramite(this.tramiteId).subscribe({

      next: (repo) => {

        this.repository = repo;

        this.phase = 'ready';

        this.loadDocuments();

      },

      error: (err) => {

        const status = (err as { status?: number })?.status;

        const msg = httpErrorMessage(err, 'No se pudo cargar el repositorio documental');

        const normalized = msg.toLowerCase();

        if (

          status === 404 ||

          normalized.includes('repositorio documental no encontrado')

        ) {

          this.phase = 'no-repository';

          this.error = '';

        } else {

          this.phase = 'error';

          this.error = msg;

        }

      },

    });

  }



  loadDocuments(): void {

    if (!this.repository?.id) return;



    this.loadingDocuments = true;

    this.documentService.listDocuments(this.repository.id).subscribe({

      next: (docs) => {

        this.documents = docs ?? [];

        this.loadingDocuments = false;

        if (this.expandedFamilyId) {

          const current = this.documents.find(

            (doc) => (doc.documentFamilyId || doc.id) === this.expandedFamilyId

          );

          if (current) {

            this.loadVersions(current);

          } else {

            this.expandedFamilyId = null;

            this.versionHistory = [];

          }

        }

      },

      error: (err) => {

        this.loadingDocuments = false;

        this.error = httpErrorMessage(err, 'No se pudo listar los documentos');

      },

    });

  }



  refreshDocuments(): void {

    this.message = '';

    if (this.repository?.id) {

      this.loadDocuments();

      return;

    }

    this.loadRepository();

  }



  onFileSelected(event: Event): void {

    const input = event.target as HTMLInputElement;

    const file = input.files?.[0];

    if (!file || !this.repository?.id) {

      this.resetFileInput();

      return;

    }



    const validationError = validateDocumentUpload(file);

    if (validationError) {

      this.error = validationError;

      this.resetFileInput();

      return;

    }



    if (!isKnownDocumentExtension(file.name)) {

      this.message =

        'Formato no habitual; se almacenará como archivo genérico si el servidor lo admite.';

    } else {

      this.message = '';

    }



    this.uploading = true;

    this.error = '';

    if (!this.connectivity.isOnline) {
      void this.offlineSync.enqueueDocumentUpload(this.repository.id, this.tramiteId, file).then(async () => {
        this.uploading = false;
        this.resetFileInput();
        this.message = `Documento "${file.name}" guardado localmente. Se subirá al reconectar.`;
        await this.connectivity.refreshPendingCount();
      });
      return;
    }

    this.documentService.uploadDocument(this.repository.id, file).subscribe({

      next: (uploaded) => {

        this.uploading = false;

        this.resetFileInput();

        const versionLabel = uploaded.version > 1 ? ` (v${uploaded.version})` : '';

        this.message = `Documento "${file.name}"${versionLabel} subido correctamente`;

        this.loadDocuments();

      },

      error: (err) => {

        if (shouldQueueOffline(err) && this.repository?.id) {
          void this.offlineSync.enqueueDocumentUpload(this.repository.id, this.tramiteId, file).then(async () => {
            this.uploading = false;
            this.resetFileInput();
            this.message = `Documento "${file.name}" en cola de sincronización.`;
            await this.connectivity.refreshPendingCount();
          });
          return;
        }

        this.uploading = false;

        this.resetFileInput();

        this.error = httpErrorMessage(err, 'No se pudo subir el documento');

      },

    });

  }



  toggleVersions(document: DocumentRecord): void {

    const familyId = document.documentFamilyId || document.id;

    if (this.expandedFamilyId === familyId) {

      this.expandedFamilyId = null;

      this.versionHistory = [];

      this.versionsError = '';

      return;

    }

    this.expandedFamilyId = familyId;

    this.loadVersions(document);

  }



  loadVersions(document: DocumentRecord): void {

    this.loadingVersions = true;

    this.versionsError = '';

    this.documentService.listDocumentVersions(document.id).subscribe({

      next: (versions) => {

        this.versionHistory = versions ?? [];

        this.loadingVersions = false;

      },

      error: (err) => {

        this.loadingVersions = false;

        this.versionsError = httpErrorMessage(err, 'No se pudo cargar el historial de versiones');

      },

    });

  }



  isVersionsExpanded(document: DocumentRecord): boolean {

    const familyId = document.documentFamilyId || document.id;

    return this.expandedFamilyId === familyId;

  }



  openDocument(document: DocumentRecord): void {

    this.actingOnDocumentId = document.id;

    this.error = '';

    this.documentService.getDownloadInfo(document.id).subscribe({

      next: (info) => {

        this.actingOnDocumentId = null;

        if (info.presignedDownloadUrl) {

          window.open(info.presignedDownloadUrl, '_blank', 'noopener,noreferrer');

        } else {

          this.error = 'No se recibió URL de descarga';

        }

      },

      error: (err) => {

        this.actingOnDocumentId = null;

        this.error = httpErrorMessage(err, 'No se pudo abrir el documento');

      },

    });

  }



  openDetail(document: DocumentRecord): void {
    this.actingOnDocumentId = document.id;

    this.documentService.getDownloadInfo(document.id).subscribe({
      next: (info) => {
        this.actingOnDocumentId = null;
        this.dialog.open(DocumentDetailDialogComponent, {
          width: '720px',
          maxWidth: '95vw',
          panelClass: 'document-detail-dialog-panel',
          autoFocus: false,
          data: {
            document,
            downloadUrl: info.presignedDownloadUrl,
          },
        });
      },
      error: (err) => {
        this.actingOnDocumentId = null;
        this.error = httpErrorMessage(err, 'No se pudo cargar el detalle del documento');
      },
    });
  }



  openDeleteModal(document: DocumentRecord): void {

    this.selectedDocument = document;

    this.deleteModalOpen = true;

    this.error = '';

  }



  closeDeleteModal(): void {

    this.deleteModalOpen = false;

    this.selectedDocument = null;

  }



  confirmDelete(): void {

    if (!this.selectedDocument?.id) return;



    const name = this.selectedDocument.nombreOriginal;

    this.actingOnDocumentId = this.selectedDocument.id;

    this.error = '';



    this.documentService.deleteDocument(this.selectedDocument.id).subscribe({

      next: () => {

        this.actingOnDocumentId = null;

        this.deleteModalOpen = false;

        this.selectedDocument = null;

        this.message = `Documento "${name}" eliminado`;

        this.expandedFamilyId = null;

        this.versionHistory = [];

        this.loadDocuments();

      },

      error: (err) => {

        this.actingOnDocumentId = null;

        this.error = httpErrorMessage(err, 'No se pudo eliminar el documento');

      },

    });

  }



  formatDate(value?: string): string {

    if (!value) return '—';

    const date = new Date(value);

    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-BO');

  }



  canOpenPreview(document: DocumentRecord): boolean {

    return isPreviewableDocument(document.extension, document.contentType);

  }



  isActingOn(documentId: string): boolean {

    return this.actingOnDocumentId === documentId;

  }



  private resetFileInput(): void {

    if (this.fileInput?.nativeElement) {

      this.fileInput.nativeElement.value = '';

    }

  }

}


