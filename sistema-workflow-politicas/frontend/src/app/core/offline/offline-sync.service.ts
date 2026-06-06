import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { OfflineDbService } from './offline-db.service';
import {
  OfflineFileBlob,
  OfflineSyncOperationType,
  PendingSyncQueueItem,
} from './offline-db.types';
import { MyActivitiesService } from '../../services/my-activities.service';
import { FormSubmissionService } from '../../services/form-submission.service';
import { DocumentRepositoryService } from '../../services/document-repository.service';
import { OfflineAuditService } from '../../services/offline-audit.service';
import { CompleteActivityPayload, FormSubmissionPayload } from '../../models/my-activities.model';
import { isStoredTokenExpired } from '../../utils/jwt-expiration.util';

@Injectable({ providedIn: 'root' })
export class OfflineSyncService {
  private readonly db = inject(OfflineDbService);
  private readonly myActivities = inject(MyActivitiesService);
  private readonly formSubmission = inject(FormSubmissionService);
  private readonly documentRepository = inject(DocumentRepositoryService);
  private readonly offlineAudit = inject(OfflineAuditService);
  private readonly router = inject(Router);

  private syncing = false;
  private storedAuditSent = false;

  async getPendingCount(): Promise<number> {
    return this.db.countPendingQueue();
  }

  async enqueueTakeTask(tramiteId: string, taskOrder: number): Promise<string> {
    return this.enqueue('TAKE_TASK', { tramiteId, taskOrder });
  }

  async enqueueFormDraft(payload: FormSubmissionPayload): Promise<string> {
    const id = await this.enqueue('FORM_DRAFT', payload as unknown as Record<string, unknown>);
    await this.db.savePendingForm({
      id,
      tramiteId: payload.tramiteId,
      taskOrder: payload.taskOrder,
      payload,
      complete: false,
      savedAt: new Date().toISOString(),
    });
    return id;
  }

  async enqueueCompleteActivity(
    tramiteId: string,
    payload: CompleteActivityPayload,
    fileBlobs: OfflineFileBlob[] = [],
  ): Promise<string> {
    const id = await this.enqueue(
      'COMPLETE_ACTIVITY',
      { tramiteId, ...payload } as unknown as Record<string, unknown>,
      fileBlobs,
    );
    await this.db.savePendingForm({
      id,
      tramiteId,
      taskOrder: payload.taskOrder,
      payload,
      complete: true,
      savedAt: new Date().toISOString(),
    });
    return id;
  }

  async enqueueDocumentUpload(
    repositoryId: string,
    tramiteId: string,
    file: File,
  ): Promise<string> {
    const id = await this.enqueue(
      'DOCUMENT_UPLOAD',
      { repositoryId, tramiteId, fileName: file.name, contentType: file.type },
    );
    await this.db.savePendingDocument({
      id,
      repositoryId,
      tramiteId,
      fileName: file.name,
      contentType: file.type,
      blob: file,
      savedAt: new Date().toISOString(),
    });
    return id;
  }

  async cacheActivities(activities: import('../../models/my-activities.model').MyActivity[]): Promise<void> {
    await this.db.cacheActivities(activities);
  }

  async getCachedActivities(): Promise<import('../../models/my-activities.model').MyActivity[]> {
    return this.db.getCachedActivities();
  }

  async syncPending(): Promise<void> {
    if (this.syncing || typeof navigator === 'undefined' || !navigator.onLine) {
      return;
    }
    if (isStoredTokenExpired()) {
      this.router.navigate(['/login']);
      return;
    }

    this.syncing = true;
    let synced = 0;
    let failed = 0;

    try {
      const pending = await this.db.listPendingQueue();
      if (!pending.length) {
        return;
      }

      if (!this.storedAuditSent) {
        const types = [...new Set(pending.map((p) => p.type))];
        await firstValueFrom(this.offlineAudit.notifyStored(pending.length, types));
        this.storedAuditSent = true;
      }

      for (const item of pending) {
        item.status = 'syncing';
        item.updatedAt = new Date().toISOString();
        await this.db.updateQueueItem(item);

        try {
          await this.processItem(item);
          await this.db.removeQueueItem(item.id);
          synced++;
        } catch (err) {
          item.status = 'failed';
          item.retryCount = (item.retryCount ?? 0) + 1;
          item.lastError = err instanceof Error ? err.message : 'Error de sincronización';
          item.updatedAt = new Date().toISOString();
          await this.db.updateQueueItem(item);
          failed++;
        }
      }

      if (synced > 0 || failed > 0) {
        await firstValueFrom(this.offlineAudit.notifySyncCompleted(synced, failed));
      }
      if (failed === 0) {
        this.storedAuditSent = false;
      }
    } finally {
      this.syncing = false;
    }
  }

  private async enqueue(
    type: OfflineSyncOperationType,
    payload: Record<string, unknown>,
    fileBlobs: OfflineFileBlob[] = [],
  ): Promise<string> {
    const id = crypto.randomUUID();
    const now = new Date().toISOString();
    const item: PendingSyncQueueItem = {
      id,
      type,
      status: 'pending',
      payload,
      fileBlobs: fileBlobs.length ? fileBlobs : undefined,
      createdAt: now,
      updatedAt: now,
      retryCount: 0,
    };
    await this.db.enqueue(item);
    return id;
  }

  private async processItem(item: PendingSyncQueueItem): Promise<void> {
    switch (item.type) {
      case 'TAKE_TASK':
        await firstValueFrom(
          this.myActivities.takeTask(
            String(item.payload['tramiteId']),
            Number(item.payload['taskOrder']),
          ),
        );
        break;

      case 'FORM_DRAFT':
        await firstValueFrom(
          this.formSubmission.save(item.payload as unknown as FormSubmissionPayload),
        );
        break;

      case 'COMPLETE_ACTIVITY': {
        const tramiteId = String(item.payload['tramiteId']);
        const responses = await this.resolveResponsesWithFiles(item);
        const payload: CompleteActivityPayload = {
          workflowActivityId: String(item.payload['workflowActivityId']),
          activityName: String(item.payload['activityName']),
          taskOrder: Number(item.payload['taskOrder']),
          responses,
        };
        await firstValueFrom(this.myActivities.complete(tramiteId, payload));
        break;
      }

      case 'DOCUMENT_UPLOAD': {
        const pendingDoc = await this.db.getPendingDocument(item.id);
        if (!pendingDoc) {
          throw new Error('Documento pendiente no encontrado en IndexedDB');
        }
        const file = new File([pendingDoc.blob], pendingDoc.fileName, {
          type: pendingDoc.contentType || 'application/octet-stream',
        });
        await firstValueFrom(
          this.documentRepository.uploadDocument(pendingDoc.repositoryId, file),
        );
        break;
      }

      default:
        throw new Error(`Tipo de operación no soportado: ${item.type}`);
    }
  }

  private async resolveResponsesWithFiles(
    item: PendingSyncQueueItem,
  ): Promise<CompleteActivityPayload['responses']> {
    const responses = (item.payload['responses'] as CompleteActivityPayload['responses']) ?? [];
    if (!item.fileBlobs?.length) {
      return responses;
    }

    const uploaded = new Map<string, string>();
    for (const blobItem of item.fileBlobs) {
      const file = new File([blobItem.blob], blobItem.fileName, {
        type: blobItem.contentType || 'application/octet-stream',
      });
      const meta = await firstValueFrom(this.formSubmission.uploadFile(file));
      uploaded.set(blobItem.fieldKey, meta.fileId);
    }

    return responses.map((r) => {
      if (r.fieldType === 'file' && r.fieldName && uploaded.has(r.fieldName)) {
        return { ...r, fileId: uploaded.get(r.fieldName) };
      }
      return r;
    });
  }

}
