import { Injectable } from '@angular/core';
import {
  CachedActivitiesRecord,
  OFFLINE_DB_NAME,
  OFFLINE_DB_VERSION,
  PendingDocumentRecord,
  PendingFormRecord,
  PendingSyncQueueItem,
  STORE_CACHED_ACTIVITIES,
  STORE_PENDING_DOCUMENTS,
  STORE_PENDING_FORMS,
  STORE_PENDING_SYNC_QUEUE,
  STORE_PENDING_TASKS,
} from './offline-db.types';
import { MyActivity } from '../../models/my-activities.model';

@Injectable({ providedIn: 'root' })
export class OfflineDbService {
  private dbPromise: Promise<IDBDatabase> | null = null;

  open(): Promise<IDBDatabase> {
    if (!this.dbPromise) {
      this.dbPromise = new Promise((resolve, reject) => {
        const request = indexedDB.open(OFFLINE_DB_NAME, OFFLINE_DB_VERSION);
        request.onupgradeneeded = () => {
          const db = request.result;
          if (!db.objectStoreNames.contains(STORE_PENDING_SYNC_QUEUE)) {
            db.createObjectStore(STORE_PENDING_SYNC_QUEUE, { keyPath: 'id' });
          }
          if (!db.objectStoreNames.contains(STORE_PENDING_FORMS)) {
            db.createObjectStore(STORE_PENDING_FORMS, { keyPath: 'id' });
          }
          if (!db.objectStoreNames.contains(STORE_PENDING_TASKS)) {
            db.createObjectStore(STORE_PENDING_TASKS, { keyPath: 'id' });
          }
          if (!db.objectStoreNames.contains(STORE_PENDING_DOCUMENTS)) {
            db.createObjectStore(STORE_PENDING_DOCUMENTS, { keyPath: 'id' });
          }
          if (!db.objectStoreNames.contains(STORE_CACHED_ACTIVITIES)) {
            db.createObjectStore(STORE_CACHED_ACTIVITIES, { keyPath: 'id' });
          }
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error ?? new Error('IndexedDB open failed'));
      });
    }
    return this.dbPromise;
  }

  async enqueue(item: PendingSyncQueueItem): Promise<void> {
    await this.put(STORE_PENDING_SYNC_QUEUE, item);
  }

  async updateQueueItem(item: PendingSyncQueueItem): Promise<void> {
    await this.put(STORE_PENDING_SYNC_QUEUE, item);
  }

  async getQueueItem(id: string): Promise<PendingSyncQueueItem | undefined> {
    return this.get<PendingSyncQueueItem>(STORE_PENDING_SYNC_QUEUE, id);
  }

  async listPendingQueue(): Promise<PendingSyncQueueItem[]> {
    const items = await this.getAll<PendingSyncQueueItem>(STORE_PENDING_SYNC_QUEUE);
    return items
      .filter((i) => i.status === 'pending' || i.status === 'failed')
      .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  }

  async countPendingQueue(): Promise<number> {
    const items = await this.listPendingQueue();
    return items.length;
  }

  async removeQueueItem(id: string): Promise<void> {
    await this.delete(STORE_PENDING_SYNC_QUEUE, id);
  }

  async savePendingForm(record: PendingFormRecord): Promise<void> {
    await this.put(STORE_PENDING_FORMS, record);
  }

  async savePendingDocument(record: PendingDocumentRecord): Promise<void> {
    await this.put(STORE_PENDING_DOCUMENTS, record);
  }

  async getPendingDocument(id: string): Promise<PendingDocumentRecord | undefined> {
    return this.get<PendingDocumentRecord>(STORE_PENDING_DOCUMENTS, id);
  }

  async cacheActivities(activities: MyActivity[]): Promise<void> {
    const record: CachedActivitiesRecord = {
      id: 'latest',
      activities,
      cachedAt: new Date().toISOString(),
    };
    await this.put(STORE_CACHED_ACTIVITIES, record);
  }

  async getCachedActivities(): Promise<MyActivity[]> {
    const record = await this.get<CachedActivitiesRecord>(STORE_CACHED_ACTIVITIES, 'latest');
    return record?.activities ?? [];
  }

  private async put<T>(storeName: string, value: T): Promise<void> {
    const db = await this.open();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readwrite');
      tx.objectStore(storeName).put(value);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error ?? new Error(`IndexedDB put failed: ${storeName}`));
    });
  }

  private async get<T>(storeName: string, key: IDBValidKey): Promise<T | undefined> {
    const db = await this.open();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readonly');
      const request = tx.objectStore(storeName).get(key);
      request.onsuccess = () => resolve(request.result as T | undefined);
      request.onerror = () => reject(request.error ?? new Error(`IndexedDB get failed: ${storeName}`));
    });
  }

  private async getAll<T>(storeName: string): Promise<T[]> {
    const db = await this.open();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readonly');
      const request = tx.objectStore(storeName).getAll();
      request.onsuccess = () => resolve((request.result as T[]) ?? []);
      request.onerror = () => reject(request.error ?? new Error(`IndexedDB getAll failed: ${storeName}`));
    });
  }

  private async delete(storeName: string, key: IDBValidKey): Promise<void> {
    const db = await this.open();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readwrite');
      tx.objectStore(storeName).delete(key);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error ?? new Error(`IndexedDB delete failed: ${storeName}`));
    });
  }
}
