import { AuthService } from '../../services/auth.service';

export interface NavItemConfig {
  path: string;
  label: string;
  icon: string;
  /** Si se define, el usuario debe tener al menos uno de estos permisos */
  permissions?: string[];
  /** Módulo visible pero no accesible todavía */
  pending?: boolean;
  pendingMessage?: string;
}

export const NAV_ITEMS: NavItemConfig[] = [
  { path: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
  {
    path: '/policies',
    label: 'Políticas de negocio',
    icon: 'description',
    permissions: ['POLICIES_MANAGE'],
  },
  {
    path: '/workflow-designer',
    label: 'Diseñador workflow',
    icon: 'account_tree',
    permissions: ['WORKFLOW_MANAGE'],
    pending: true,
    pendingMessage: 'Módulo en desarrollo',
  },
  {
    path: '/tramites',
    label: 'Trámites',
    icon: 'assignment',
    permissions: ['TASKS_EXECUTE', 'POLICIES_MANAGE', 'MONITORING_VIEW', 'REPORTS_VIEW'],
  },
  {
    path: '/mis-actividades',
    label: 'Mis tareas',
    icon: 'assignment_turned_in',
    permissions: ['TASKS_EXECUTE'],
  },
  {
    path: '/monitoring',
    label: 'Monitoreo',
    icon: 'timeline',
    permissions: ['MONITORING_VIEW'],
  },
  {
    path: '/kpis',
    label: 'KPIs',
    icon: 'insert_chart',
    permissions: ['KPI_VIEW'],
  },
  {
    path: '/users',
    label: 'Usuarios',
    icon: 'people',
    permissions: ['USERS_MANAGE'],
  },
  {
    path: '/roles',
    label: 'Roles',
    icon: 'security',
    permissions: ['ROLES_MANAGE'],
  },
  {
    path: '/departments',
    label: 'Departamentos',
    icon: 'business',
    permissions: ['DEPARTMENTS_MANAGE'],
  },
  {
    path: '/bitacora',
    label: 'Bitácora',
    icon: 'history',
    permissions: ['AUDIT_VIEW'],
  },
  {
    path: '/ai-assistant',
    label: 'Asistente IA',
    icon: 'auto_awesome',
    permissions: ['AI_ASSIST'],
    pending: true,
    pendingMessage: 'Módulo en desarrollo',
  },
  {
    path: '/settings',
    label: 'Configuración',
    icon: 'settings',
    permissions: ['SETTINGS_MANAGE', 'USERS_MANAGE'],
  },
];

export interface VisibleNavItem extends NavItemConfig {
  disabled: boolean;
  tooltip: string;
}

export function getVisibleNavItems(auth: AuthService): VisibleNavItem[] {
  return NAV_ITEMS.filter((item) => auth.hasAnyPermission(item.permissions ?? [])).map((item) => ({
    ...item,
    disabled: !!item.pending,
    tooltip: item.pending ? (item.pendingMessage ?? 'Módulo en desarrollo') : item.label,
  }));
}

export function canAccessRoute(auth: AuthService, path: string): boolean {
  const item = NAV_ITEMS.find((nav) => nav.path === path || path.startsWith(nav.path + '/'));
  if (!item) return true;
  if (item.pending) return false;
  return auth.hasAnyPermission(item.permissions ?? []);
}
