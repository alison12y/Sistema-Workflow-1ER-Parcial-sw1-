import { Routes } from '@angular/router';
import { authGuard, loginGuard, permissionGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
    canActivate: [loginGuard],
  },
  {
    path: '',
    loadComponent: () => import('./shared/layout/layout.component').then((m) => m.LayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'policies',
        loadComponent: () =>
          import('./pages/policies/policies.component').then((m) => m.PoliciesComponent),
        canActivate: [permissionGuard('POLICIES_MANAGE')],
      },
      {
        path: 'policies/:id/actividades',
        loadComponent: () =>
          import('./pages/policy-activities/policy-activities.component').then((m) => m.PolicyActivitiesComponent),
        canActivate: [permissionGuard('POLICIES_MANAGE', 'WORKFLOW_MANAGE', 'WORKFLOW_VIEW')],
      },
      {
        path: 'policies/:id/transiciones',
        loadComponent: () =>
          import('./pages/policy-transitions/policy-transitions.component').then((m) => m.PolicyTransitionsComponent),
        canActivate: [permissionGuard('POLICIES_MANAGE', 'WORKFLOW_MANAGE', 'WORKFLOW_VIEW')],
      },
      {
        path: 'policies/:id',
        loadComponent: () =>
          import('./pages/policy-detail/policy-detail.component').then((m) => m.PolicyDetailComponent),
        canActivate: [permissionGuard('POLICIES_MANAGE', 'WORKFLOW_VIEW')],
      },
      {
        path: 'seguimiento',
        loadComponent: () =>
          import('./pages/tramite-tracking/tramite-tracking.component').then((m) => m.TramiteTrackingComponent),
        canActivate: [permissionGuard('MONITORING_VIEW', 'REPORTS_VIEW', 'POLICIES_MANAGE')],
      },
      {
        path: 'monitoring',
        loadComponent: () =>
          import('./pages/monitoring/monitoring.component').then((m) => m.MonitoringComponent),
        canActivate: [permissionGuard('MONITORING_VIEW')],
      },
      {
        path: 'kpis',
        loadComponent: () =>
          import('./pages/kpi/kpi.component').then((m) => m.KpiComponent),
        canActivate: [permissionGuard('KPI_VIEW')],
      },
      {
        path: 'kpi',
        redirectTo: 'kpis',
        pathMatch: 'full',
      },
      {
        path: 'workflow-designer',
        redirectTo: 'policies',
        pathMatch: 'full',
      },
      {
        path: 'workflow-designer/:id',
        loadComponent: () =>
          import('./pages/workflow-designer/workflow-designer.component').then((m) => m.WorkflowDesignerComponent),
        canActivate: [permissionGuard('WORKFLOW_MANAGE', 'WORKFLOW_DESIGN', 'WORKFLOW_VIEW', 'POLICIES_MANAGE')],
      },
      {
        path: 'activities/:activityId/form',
        loadComponent: () =>
          import('./pages/activity-form-designer/activity-form-designer.component').then(
            (m) => m.ActivityFormDesignerComponent,
          ),
        canActivate: [
          permissionGuard(
            'WORKFLOW_MANAGE',
            'WORKFLOW_DESIGN',
            'WORKFLOW_VIEW',
            'POLICIES_MANAGE',
            'FORMS_MANAGE',
            'TASKS_EXECUTE',
          ),
        ],
      },
      {
        path: 'form-designer/:id',
        redirectTo: 'workflow-designer/:id',
        pathMatch: 'full',
      },
      {
        path: 'users',
        loadComponent: () => import('./pages/users/users.component').then((m) => m.UsersComponent),
        canActivate: [permissionGuard('USERS_MANAGE')],
      },
      {
        path: 'roles',
        loadComponent: () => import('./pages/roles/roles.component').then((m) => m.RolesComponent),
        canActivate: [permissionGuard('ROLES_MANAGE')],
      },
      {
        path: 'departments',
        loadComponent: () =>
          import('./pages/departments/departments.component').then((m) => m.DepartmentsComponent),
        canActivate: [permissionGuard('DEPARTMENTS_MANAGE')],
      },
      {
        path: 'bitacora',
        loadComponent: () =>
          import('./pages/bitacora/bitacora.component').then((m) => m.BitacoraComponent),
        canActivate: [permissionGuard('AUDIT_VIEW')],
      },
      {
        path: 'tramites',
        loadComponent: () =>
          import('./pages/tramites/tramites.component').then((m) => m.TramitesComponent),
        canActivate: [permissionGuard('TASKS_EXECUTE', 'POLICIES_MANAGE', 'MONITORING_VIEW', 'REPORTS_VIEW')],
      },
      {
        path: 'my-activities',
        redirectTo: 'mis-actividades',
        pathMatch: 'full',
      },
      {
        path: 'mis-actividades',
        loadComponent: () =>
          import('./pages/my-activities/my-activities.component').then((m) => m.MyActivitiesComponent),
        canActivate: [permissionGuard('TASKS_EXECUTE')],
      },
      {
        path: 'mis-actividades/:tramiteId/form',
        loadComponent: () =>
          import('./pages/form-execution/form-execution.component').then((m) => m.FormExecutionComponent),
        canActivate: [permissionGuard('TASKS_EXECUTE')],
      },
      {
        path: 'tramites/:id',
        loadComponent: () =>
          import('./pages/tramites/tramite-detail.component').then((m) => m.TramiteDetailComponent),
        canActivate: [permissionGuard('TASKS_EXECUTE', 'POLICIES_MANAGE', 'MONITORING_VIEW', 'REPORTS_VIEW')],
      },
      {
        path: 'tramites/:tramiteId/documentos/:documentId/editar',
        loadComponent: () =>
          import('./pages/tramites/document-edit.component').then((m) => m.DocumentEditComponent),
        canActivate: [permissionGuard('DOCUMENTS_VIEW', 'DOCUMENTS_UPLOAD', 'DOCUMENTS_DELETE', 'TASKS_EXECUTE', 'POLICIES_MANAGE', 'MONITORING_VIEW')],
      },
      {
        path: 'ai-assistant',
        loadComponent: () =>
          import('./pages/ai-assistant/ai-assistant.component').then((m) => m.AiAssistantComponent),
        canActivate: [permissionGuard('AI_ASSIST')],
      },
      {
        path: 'smart-agent',
        loadComponent: () =>
          import('./pages/smart-agent/smart-agent.component').then((m) => m.SmartAgentComponent),
        canActivate: [permissionGuard('AI_AGENT_USE')],
      },
      {
        path: 'intelligent-analytics',
        loadComponent: () =>
          import('./pages/intelligent-analytics/intelligent-analytics.component').then(
            (m) => m.IntelligentAnalyticsComponent,
          ),
        canActivate: [permissionGuard('INTELLIGENT_ANALYTICS_VIEW')],
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./pages/settings/settings.component').then((m) => m.SettingsComponent),
        canActivate: [permissionGuard('SETTINGS_MANAGE', 'USERS_MANAGE')],
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
