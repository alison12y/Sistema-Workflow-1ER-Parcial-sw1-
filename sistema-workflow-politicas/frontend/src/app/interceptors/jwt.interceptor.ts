import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TOKEN_KEY, USER_KEY } from '../core/constants';

const AUTH_PATHS = ['/api/auth/login', '/api/auth/register'];

function resolveToken(): string | null {
  const raw = localStorage.getItem(TOKEN_KEY);
  if (!raw) {
    return null;
  }
  const token = raw.trim();
  if (!token || token === 'undefined' || token === 'null') {
    return null;
  }
  return token;
}

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const isAuthRequest = AUTH_PATHS.some((path) => req.url.includes(path));
  const token = isAuthRequest ? null : resolveToken();

  if (!isAuthRequest && req.url.includes('/api/workflow-activities')) {
    console.debug(
      '[jwtInterceptor]',
      req.method,
      req.url,
      token ? 'token present' : 'token missing',
      `(key: ${TOKEN_KEY})`
    );
  }

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 403) {
        return throwError(() => err);
      }

      if (err.status === 401 && !isAuthRequest && token) {
        console.warn('[jwtInterceptor] 401 with token attached, clearing session');
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        inject(Router).navigate(['/login']);
      }

      return throwError(() => err);
    })
  );
};
