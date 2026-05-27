import { HttpInterceptorFn } from '@angular/common/http';
import { TOKEN_KEY } from '../core/constants';

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

  if (token) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req);
};
