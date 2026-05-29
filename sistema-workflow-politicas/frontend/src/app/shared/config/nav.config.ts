import { AuthService } from '../../services/auth.service';

export interface NavItemConfig {
  path: string;
  label: string;
  icon: string;
  permissions?: string[];
  pending?: boolean;
  pendingMessage?: string;
  section?: 'workflow' | 'admin';
}

/** Menú orientado a workflow — Fase 2 */
export const NAV_ITEMS: NavItemConfig[] = [
  { path: '/dashboard', label: 'Dashboard', icon: 'dashboard', section: 'workflow' },
  {
    path: '/policies',
    label: 'Políticas de negocio',
    icon: 'description',
    permissions: ['POLICIES_MANAGE'],
    section: 'workflow',
  },
  {
    path: '/workflow-designer',
    label: 'Diseñador workflow',
    icon: 'account_tree',
    permissions: ['WORKFLOW_MANAGE', 'WORKFLOW_DESIGN', 'WORKFLOW_VIEW', 'POLICIES_MANAGE'],
    section: 'workflow',
  },
  {
    path: '/tramites',
    label: 'Trámites',
    icon: 'assignment',
    permissions: ['TASKS_EXECUTE', 'POLICIES_MANAGE', 'MONITORING_VIEW', 'REPORTS_VIEW'],
    section: 'workflow',
  },
  {
    path: '/mis-actividades',
    label: 'Mis tareas',
    icon: 'assignment_turned_in',
    permissions: ['TASKS_EXECUTE'],
    section: 'workflow',
  },
  {
    path: '/seguimiento',
    label: 'Seguimiento de trámites',
    icon: 'track_changes',
    permissions: ['MONITORING_VIEW', 'REPORTS_VIEW', 'POLICIES_MANAGE'],
    section: 'workflow',
  },
  {
    path: '/monitoring',
    label: 'Monitoreo',
    icon: 'timeline',
    permissions: ['MONITORING_VIEW'],
    section: 'workflow',
  },
  {
    path: '/kpis',
    label: 'KPIs / Cuellos de botella',
    icon: 'insert_chart',
    permissions: ['KPI_VIEW'],
    section: 'workflow',
  },
  {
    path: '/users',
    label: 'Usuarios',
    icon: 'people',
    permissions: ['USERS_MANAGE'],
    section: 'admin',
  },
  {
    path: '/roles',
    label: 'Roles',
    icon: 'security',
    permissions: ['ROLES_MANAGE'],
    section: 'admin',
  },
  {
    path: '/departments',
    label: 'Departamentos',
    icon: 'business',
    permissions: ['DEPARTMENTS_MANAGE'],
    section: 'admin',
  },
  {
    path: '/bitacora',
    label: 'Bitácora',
    icon: 'history',
    permissions: ['AUDIT_VIEW'],
    section: 'admin',
  },
  {
    path: '/settings',
    label: 'Configuración',
    icon: 'settings',
    permissions: ['SETTINGS_MANAGE', 'USERS_MANAGE'],
    section: 'admin',
  },
  {
    path: '/ai-assistant',
    label: 'Asistente IA',
    icon: 'auto_awesome',
    permissions: ['AI_ASSIST'],
    pending: true,
    pendingMessage: 'Módulo en desarrollo',
    section: 'workflow',
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
  if (path.startsWith('/policies/') && (path.includes('/actividades') || path.includes('/transiciones'))) {
    return (
      auth.hasPermission('POLICIES_MANAGE') ||
      auth.hasPermission('WORKFLOW_MANAGE') ||
      auth.hasPermission('WORKFLOW_VIEW')
    );
  }
  if (path.match(/^\/policies\/[^/]+$/)) {
    return auth.hasPermission('POLICIES_MANAGE') || auth.hasPermission('WORKFLOW_VIEW');
  }
  if (path.startsWith('/workflow-designer')) {
    return auth.canViewWorkflowDesigner();
  }
  const item = NAV_ITEMS.find((nav) => nav.path === path || (nav.path !== '/dashboard' && path.startsWith(nav.path + '/')));
  if (!item) return true;
  if (item.pending) return false;
  return auth.hasAnyPermission(item.permissions ?? []);
}

export function getWelcomeMessage(roleName?: string | null): string {
  const role = (roleName ?? '').toLowerCase();
  if (role.includes('administrador')) {
    return 'Panel de administración y control general del sistema workflow.';
  }
  if (role.includes('dueño') || role.includes('dueno') || role.includes('proceso')) {
    return 'Gestiona políticas de negocio y prepara flujos de trabajo.';
  }
  if (role.includes('funcionario')) {
    return 'Consulta y atiende tus tareas asignadas.';
  }
  if (role.includes('supervisor')) {
    return 'Supervisa trámites, seguimiento y rendimiento del workflow.';
  }
  if (role.includes('atención') || role.includes('atencion') || role.includes('cliente')) {
    return 'Gestiona trámites y el seguimiento de solicitudes de clientes.';
  }
  if (role.includes('técnico') || role.includes('tecnico')) {
    return 'Atiende tus tareas técnicas dentro de los flujos de trabajo.';
  }
  if (role.includes('legal')) {
    return 'Revisa documentación y tareas asignadas de cumplimiento normativo.';
  }
  return 'Bienvenido al sistema workflow para políticas de negocio.';
}
