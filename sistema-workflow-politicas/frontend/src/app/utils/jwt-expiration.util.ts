import { TOKEN_KEY, USER_KEY } from '../core/constants';

export interface JwtPayload {
  exp?: number;
  sub?: string;
}

export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const parts = token.split('.');
    if (parts.length < 2) {
      return null;
    }
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const json = atob(padded);
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

export function getStoredTokenExpirationMs(): number | null {
  const raw = localStorage.getItem(TOKEN_KEY);
  if (!raw) {
    return null;
  }
  const payload = decodeJwtPayload(raw.trim());
  if (!payload?.exp) {
    return null;
  }
  return payload.exp * 1000;
}

export function isStoredTokenExpired(skewMs = 30_000): boolean {
  const expMs = getStoredTokenExpirationMs();
  if (expMs == null) {
    return true;
  }
  return Date.now() >= expMs - skewMs;
}

export function clearAuthStorage(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
