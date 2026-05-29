import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { canAccessRoute } from '../shared/config/nav.config';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};

export const loginGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) {
    return router.createUrlTree(['/dashboard']);
  }
  return true;
};

export function permissionGuard(...permissions: string[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isLoggedIn()) {
      return router.createUrlTree(['/login']);
    }

    const path = state.url.split('?')[0];
    if (!canAccessRoute(auth, path) && permissions.length === 0) {
      return router.createUrlTree(['/dashboard'], { queryParams: { acceso: 'denegado' } });
    }

    if (permissions.length > 0 && !auth.hasAnyPermission(permissions)) {
      return router.createUrlTree(['/dashboard'], { queryParams: { acceso: 'denegado' } });
    }

    return true;
  };
}
