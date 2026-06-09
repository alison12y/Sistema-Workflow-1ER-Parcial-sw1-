import { Component, Inject, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import { DocumentRecord } from '../../models/document-repository.model';
import { DocumentAccessInfo, DocumentPermissionEntry } from '../../models/document-collaboration.model';
import { DocumentCollaborationService } from '../../services/document-collaboration.service';
import { AuthService } from '../../services/auth.service';
import {
  documentIconType,
  documentMaterialIcon,
  documentStatusClass,
  documentStatusLabel,
  documentTypeLabel,
  formatFileSize,
} from '../../utils/document-display.util';
import { httpErrorMessage } from '../../utils/tramite-display.util';

export interface DocumentDetailDialogData {
  document: DocumentRecord;
  downloadUrl?: string;
  repositoryId?: string;
  sessionId?: string;
}

@Component({
  selector: 'app-document-detail-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatIconModule],
  templateUrl: './document-detail-dialog.component.html',
  styleUrl: './document-detail-dialog.component.scss',
})
export class DocumentDetailDialogComponent implements OnInit {
  private readonly collaborationService = inject(DocumentCollaborationService);
  private readonly auth = inject(AuthService);

  readonly typeLabel = documentTypeLabel;
  readonly formatSize = formatFileSize;
  readonly statusLabel = documentStatusLabel;
  readonly statusClass = documentStatusClass;
  readonly iconType = documentIconType;

  access: DocumentAccessInfo | null = null;
  permissions: DocumentPermissionEntry[] = [];
  permissionsError = '';
  permissionForm = {
    granteeType: 'USER',
    granteeKey: '',
    granteeLabel: '',
    permissionLevel: 'READ',
  };
  savingPermission = false;

  constructor(
    @Inject(MAT_DIALOG_DATA) public readonly data: DocumentDetailDialogData,
    private readonly dialogRef: MatDialogRef<DocumentDetailDialogComponent>,
  ) {}

  ngOnInit(): void {
    this.collaborationService.getDocumentAccess(this.document.id).subscribe({
      next: (access) => {
        this.access = access;
        if (access.canAdmin) {
          this.loadPermissions();
        }
      },
    });
  }

  get document(): DocumentRecord {
    return this.data.document;
  }

  get downloadUrl(): string | undefined {
    return this.data.downloadUrl;
  }

  get canManagePermissions(): boolean {
    return !!this.access?.canAdmin;
  }

  materialIcon(): string {
    return documentMaterialIcon(this.document.extension, this.document.contentType);
  }

  formatDate(value?: string): string {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-BO');
  }

  loadPermissions(): void {
    this.permissionsError = '';
    this.collaborationService.listPermissions(this.document.id).subscribe({
      next: (items) => {
        this.permissions = items ?? [];
      },
      error: (err) => {
        this.permissionsError = httpErrorMessage(err, 'No se pudieron cargar permisos');
      },
    });
  }

  grantPermission(): void {
    if (!this.permissionForm.granteeKey.trim()) return;
    this.savingPermission = true;
    this.collaborationService.grantPermission(this.document.id, {
      granteeType: this.permissionForm.granteeType,
      granteeKey: this.permissionForm.granteeKey.trim(),
      granteeLabel: this.permissionForm.granteeLabel.trim() || this.permissionForm.granteeKey.trim(),
      permissionLevel: this.permissionForm.permissionLevel,
    }).subscribe({
      next: () => {
        this.savingPermission = false;
        this.permissionForm.granteeKey = '';
        this.permissionForm.granteeLabel = '';
        this.loadPermissions();
        this.collaborationService.getDocumentAccess(this.document.id).subscribe({
          next: (access) => (this.access = access),
        });
      },
      error: (err) => {
        this.savingPermission = false;
        this.permissionsError = httpErrorMessage(err, 'No se pudo asignar permiso');
      },
    });
  }

  removePermission(entry: DocumentPermissionEntry): void {
    this.collaborationService.removePermission(this.document.id, {
      granteeType: entry.granteeType,
      granteeKey: entry.granteeKey,
      permissionLevel: entry.permissionLevel,
    }).subscribe({
      next: () => this.loadPermissions(),
      error: (err) => {
        this.permissionsError = httpErrorMessage(err, 'No se pudo quitar permiso');
      },
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
