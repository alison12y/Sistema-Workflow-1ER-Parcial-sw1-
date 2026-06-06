import {
  decodeJwtPayload,
  isStoredTokenExpired,
} from './jwt-expiration.util';

describe('jwt-expiration.util', () => {
  function buildToken(expSeconds: number): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({ sub: 'user', exp: expSeconds }));
    return `${header}.${payload}.signature`;
  }

  it('decodeJwtPayload reads exp claim', () => {
    const exp = Math.floor(Date.now() / 1000) + 3600;
    const payload = decodeJwtPayload(buildToken(exp));
    expect(payload?.exp).toBe(exp);
  });

  it('isStoredTokenExpired returns true for expired token in storage', () => {
    const expired = Math.floor(Date.now() / 1000) - 60;
    localStorage.setItem('auth_token', buildToken(expired));
    expect(isStoredTokenExpired()).toBeTrue();
    localStorage.removeItem('auth_token');
  });

  it('isStoredTokenExpired returns false for valid token', () => {
    const valid = Math.floor(Date.now() / 1000) + 3600;
    localStorage.setItem('auth_token', buildToken(valid));
    expect(isStoredTokenExpired()).toBeFalse();
    localStorage.removeItem('auth_token');
  });
});
