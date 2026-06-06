import { AuthService } from '../../services/auth.service';

export interface MobileNavItem {
  path: string;
  label: string;
  icon: string;
  permissions?: string[];
}

/** Navegación inferior optimizada para móvil / PWA (CU27). */
export const MOBILE_NAV_ITEMS: MobileNavItem[] = [
  { path: '/dashboard', label: 'Inicio', icon: 'dashboard' },
  {
    path: '/mis-actividades',
    label: 'Tareas',
    icon: 'assignment_turned_in',
    permissions: ['TASKS_EXECUTE'],
  },
  {
    path: '/tramites',
    label: 'Trámites',
    icon: 'assignment',
    permissions: ['TASKS_EXECUTE', 'POLICIES_MANAGE', 'MONITORING_VIEW', 'REPORTS_VIEW'],
  },
  {
    path: '/smart-agent',
    label: 'Agente',
    icon: 'smart_toy',
    permissions: ['AI_AGENT_USE'],
  },
];

export function getVisibleMobileNavItems(auth: AuthService): MobileNavItem[] {
  return MOBILE_NAV_ITEMS.filter((item) => auth.hasAnyPermission(item.permissions ?? []));
}
