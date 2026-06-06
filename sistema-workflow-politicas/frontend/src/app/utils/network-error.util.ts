import { HttpErrorResponse } from '@angular/common/http';

export function isNetworkError(err: unknown): boolean {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return true;
    }
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      return true;
    }
  }
  if (err instanceof TypeError && /fetch|network|failed/i.test(err.message)) {
    return true;
  }
  return typeof navigator !== 'undefined' && !navigator.onLine;
}

export function shouldQueueOffline(err: unknown): boolean {
  return isNetworkError(err) || (typeof navigator !== 'undefined' && !navigator.onLine);
}
