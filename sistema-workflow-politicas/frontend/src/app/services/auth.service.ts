import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, LoginRequest } from '../models/auth.model';
import { TOKEN_KEY, USER_KEY } from '../core/constants';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly api = `${environment.apiUrl}/api/auth`;

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/login`, credentials).pipe(
      tap((res) => {
        localStorage.setItem(TOKEN_KEY, res.token);
        localStorage.setItem(USER_KEY, JSON.stringify(res));
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getCurrentUser(): AuthResponse | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthResponse;
    } catch {
      return null;
    }
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  hasPermission(permission: string): boolean {
    const user = this.getCurrentUser();
    if (user?.role === 'ROLE_ADMIN') return true;
    return user?.permissions?.includes(permission) ?? false;
  }

  hasAnyPermission(permissions: string[]): boolean {
    if (!permissions.length) return true;
    const user = this.getCurrentUser();
    if (user?.role === 'ROLE_ADMIN') return true;
    return permissions.some((p) => this.hasPermission(p));
  }

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'ROLE_ADMIN' || this.hasPermission('USERS_MANAGE');
  }

  getRoleDisplayLabel(): string {
    const user = this.getCurrentUser();
    if (user?.roleName) return user.roleName;
    if (user?.roles?.length) return user.roles.join(', ');
    return 'Usuario';
  }
}
