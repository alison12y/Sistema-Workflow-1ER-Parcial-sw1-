import { Injectable, NgZone, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { OfflineSyncService } from './offline-sync.service';

@Injectable({ providedIn: 'root' })
export class ConnectivityService {
  private readonly zone = inject(NgZone);
  private readonly offlineSync = inject(OfflineSyncService);

  private readonly onlineSubject = new BehaviorSubject<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true,
  );
  private readonly pendingCountSubject = new BehaviorSubject<number>(0);

  readonly isOnline$: Observable<boolean> = this.onlineSubject.asObservable();
  readonly pendingCount$: Observable<number> = this.pendingCountSubject.asObservable();

  constructor() {
    if (typeof window === 'undefined') {
      return;
    }
    window.addEventListener('online', () => this.handleOnline());
    window.addEventListener('offline', () => this.handleOffline());
    this.refreshPendingCount();
  }

  get isOnline(): boolean {
    return this.onlineSubject.value;
  }

  get pendingCount(): number {
    return this.pendingCountSubject.value;
  }

  async refreshPendingCount(): Promise<void> {
    const count = await this.offlineSync.getPendingCount();
    this.pendingCountSubject.next(count);
  }

  private handleOnline(): void {
    this.zone.run(() => {
      this.onlineSubject.next(true);
      void this.offlineSync.syncPending().finally(() => this.refreshPendingCount());
    });
  }

  private handleOffline(): void {
    this.zone.run(() => {
      this.onlineSubject.next(false);
    });
  }
}
