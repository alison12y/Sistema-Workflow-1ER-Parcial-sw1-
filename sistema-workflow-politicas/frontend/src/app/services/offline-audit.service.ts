import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class OfflineAuditService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/offline`;

  notifyStored(pendingCount: number, types: string[]): Observable<void> {
    return this.http
      .post<void>(`${this.api}/notify-stored`, { pendingCount, types })
      .pipe(catchError(() => of(void 0)));
  }

  notifySyncCompleted(syncedCount: number, failedCount: number): Observable<void> {
    return this.http
      .post<void>(`${this.api}/notify-sync-completed`, { syncedCount, failedCount })
      .pipe(catchError(() => of(void 0)));
  }
}
