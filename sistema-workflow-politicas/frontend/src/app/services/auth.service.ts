import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, of, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, LoginRequest } from '../models/auth.model';
import { TOKEN_KEY, USER_KEY } from '../core/constants';
import { isStoredTokenExpired } from '../utils/jwt-expiration.util';

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
    const token = this.getToken();
    if (token) {
      this.http.post<void>(`${this.api}/logout`, {}).pipe(
        catchError(() => of(void 0)),
      ).subscribe({
        complete: () => this.clearSession(),
      });
      return;
    }
    this.clearSession();
  }

  private clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    const raw = localStorage.getItem(TOKEN_KEY);
    if (!raw) return null;
    const token = raw.trim();
    if (!token || token === 'undefined' || token === 'null') return null;
    return token;
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
    if (!this.getToken()) {
      return false;
    }
    if (isStoredTokenExpired()) {
      this.clearSession();
      return false;
    }
    return true;
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

  canManageWorkflowActivities(): boolean {
    return this.isAdmin() || this.hasPermission('WORKFLOW_MANAGE');
  }

  canViewWorkflowActivities(): boolean {
    return (
      this.canManageWorkflowActivities() ||
      this.hasPermission('WORKFLOW_VIEW') ||
      this.hasPermission('POLICIES_MANAGE')
    );
  }

  canViewWorkflowDesigner(): boolean {
    return (
      this.isAdmin() ||
      this.hasPermission('WORKFLOW_VIEW') ||
      this.hasPermission('WORKFLOW_MANAGE') ||
      this.hasPermission('WORKFLOW_DESIGN') ||
      this.hasPermission('POLICIES_MANAGE')
    );
  }

  canEditWorkflowDesigner(): boolean {
    return (
      this.isAdmin() ||
      this.hasPermission('WORKFLOW_MANAGE') ||
      this.hasPermission('WORKFLOW_DESIGN') ||
      this.hasPermission('POLICIES_MANAGE')
    );
  }

  canManageDynamicForms(): boolean {
    return (
      this.isAdmin() ||
      this.hasPermission('WORKFLOW_MANAGE') ||
      this.hasPermission('WORKFLOW_DESIGN') ||
      this.hasPermission('POLICIES_MANAGE') ||
      this.hasPermission('FORMS_MANAGE')
    );
  }

  canViewDynamicForms(): boolean {
    return (
      this.canManageDynamicForms() ||
      this.hasPermission('WORKFLOW_VIEW') ||
      this.hasPermission('TASKS_EXECUTE')
    );
  }

  canExecuteTasks(): boolean {
    return (
      this.isAdmin() ||
      this.hasPermission('TASKS_EXECUTE') ||
      this.hasPermission('WORKFLOW_MANAGE') ||
      this.hasPermission('POLICIES_MANAGE')
    );
  }

  canViewDocuments(): boolean {
    return (
      this.isAdmin() ||
      this.hasPermission('DOCUMENTS_VIEW') ||
      this.hasPermission('DOCUMENTS_UPLOAD') ||
      this.hasPermission('DOCUMENTS_DELETE')
    );
  }

  canUploadDocuments(): boolean {
    return this.isAdmin() || this.hasPermission('DOCUMENTS_UPLOAD');
  }

  canDeleteDocuments(): boolean {
    return this.isAdmin() || this.hasPermission('DOCUMENTS_DELETE');
  }

  canUseSmartAgent(): boolean {
    return this.isAdmin() || this.hasPermission('AI_AGENT_USE');
  }

  canViewIntelligentAnalytics(): boolean {
    return this.isAdmin() || this.hasPermission('INTELLIGENT_ANALYTICS_VIEW');
  }
}
