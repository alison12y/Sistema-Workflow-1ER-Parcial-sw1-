import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import { DocumentRecord } from '../../models/document-repository.model';
import {
  documentIconType,
  documentMaterialIcon,
  documentStatusClass,
  documentStatusLabel,
  documentTypeLabel,
  formatFileSize,
} from '../../utils/document-display.util';

export interface DocumentDetailDialogData {
  document: DocumentRecord;
  downloadUrl?: string;
}

@Component({
  selector: 'app-document-detail-dialog',
  standalone: true,
  imports: [MatDialogModule, MatIconModule],
  templateUrl: './document-detail-dialog.component.html',
  styleUrl: './document-detail-dialog.component.scss',
})
export class DocumentDetailDialogComponent {
  readonly typeLabel = documentTypeLabel;
  readonly formatSize = formatFileSize;
  readonly statusLabel = documentStatusLabel;
  readonly statusClass = documentStatusClass;
  readonly iconType = documentIconType;

  constructor(
    @Inject(MAT_DIALOG_DATA) public readonly data: DocumentDetailDialogData,
    private readonly dialogRef: MatDialogRef<DocumentDetailDialogComponent>,
  ) {}

  get document(): DocumentRecord {
    return this.data.document;
  }

  get downloadUrl(): string | undefined {
    return this.data.downloadUrl;
  }

  materialIcon(): string {
    return documentMaterialIcon(this.document.extension, this.document.contentType);
  }

  formatDate(value?: string): string {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-BO');
  }

  close(): void {
    this.dialogRef.close();
  }
}
