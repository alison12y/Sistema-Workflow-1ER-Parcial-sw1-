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

function isWorkflowApi(url: string): boolean {
  return (
    url.includes('/api/workflow-activities') ||
    url.includes('/api/workflow-transitions') ||
    url.includes('/api/workflow-designer')
  );
}

function isDeduplicateRequest(url: string): boolean {
  return url.includes('/api/workflow-transitions') && url.includes('/deduplicate');
}

function isCleanupRequest(url: string): boolean {
  return url.includes('/api/workflow-transitions') && url.includes('/cleanup');
}

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const isAuthRequest = AUTH_PATHS.some((path) => req.url.includes(path));
  const token = isAuthRequest ? null : resolveToken();

  if (!isAuthRequest && isWorkflowApi(req.url)) {
    console.debug(
      `[jwtInterceptor] ${req.method} ${req.url}`,
      token ? 'token present' : 'token missing',
      `(key: ${TOKEN_KEY})`
    );
  }

  if (!isAuthRequest && isDeduplicateRequest(req.url)) {
    console.debug(
      `[jwtInterceptor] deduplicate ${req.method} ${req.url}`,
      token ? 'Authorization attached' : 'Authorization missing'
    );
  }

  if (!isAuthRequest && isCleanupRequest(req.url)) {
    console.debug(
      `[jwtInterceptor] cleanup ${req.method} ${req.url}`,
      token ? 'Authorization attached' : 'Authorization missing'
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
        const message = (err.error?.message ?? '').toLowerCase();
        if (
          message.includes('acceso denegado') ||
          message.includes('denied') ||
          message.includes('permiso')
        ) {
          return throwError(() => err);
        }
        const isAuthFailure =
          !message ||
          message.includes('no autenticado') ||
          message.includes('inicie sesión') ||
          message.includes('iniciar sesión') ||
          message.includes('credenciales') ||
          message.includes('token') ||
          message.includes('expir');
        if (isAuthFailure) {
          console.warn('[jwtInterceptor] 401 with token attached, clearing session');
          localStorage.removeItem(TOKEN_KEY);
          localStorage.removeItem(USER_KEY);
          inject(Router).navigate(['/login']);
        }
      }

      return throwError(() => err);
    })
  );
};

