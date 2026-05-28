import { Routes } from '@angular/router';
import { authGuard, loginGuard } from './guards/auth.guard';

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
      },
      {
        path: 'monitoring',
        loadComponent: () =>
          import('./pages/monitoring/monitoring.component').then((m) => m.MonitoringComponent),
      },
      {
        path: 'kpis',
        loadComponent: () =>
          import('./pages/kpi/kpi.component').then((m) => m.KpiComponent),
      },
      {
        path: 'kpi',
        redirectTo: 'kpis',
        pathMatch: 'full',
      },
      {
        path: 'workflow-designer/:id',
        loadComponent: () =>
          import('./pages/workflow-designer/workflow-designer.component').then((m) => m.WorkflowDesignerComponent),
      },
      {
        path: 'form-designer/:id',
        loadComponent: () =>
          import('./pages/form-designer/form-designer.component').then((m) => m.FormDesignerComponent),
      },
      {
        path: 'users',
        loadComponent: () => import('./pages/users/users.component').then((m) => m.UsersComponent),
      },
      {
        path: 'roles',
        loadComponent: () => import('./pages/roles/roles.component').then((m) => m.RolesComponent),
      },
      {
        path: 'departments',
        loadComponent: () =>
          import('./pages/departments/departments.component').then((m) => m.DepartmentsComponent),
      },
      {
        path: 'bitacora',
        loadComponent: () =>
          import('./pages/bitacora/bitacora.component').then((m) => m.BitacoraComponent),
      },
      {
        path: 'tramites',
        loadComponent: () =>
          import('./pages/tramites/tramites.component').then((m) => m.TramitesComponent),
      },
      {
        path: 'mis-actividades',
        loadComponent: () =>
          import('./pages/my-activities/my-activities.component').then((m) => m.MyActivitiesComponent),
      },
      {
        path: 'mis-actividades/:tramiteId/form',
        loadComponent: () =>
          import('./pages/form-execution/form-execution.component').then((m) => m.FormExecutionComponent),
      },
      {
        path: 'tramites/:id',
        loadComponent: () =>
          import('./pages/tramites/tramite-detail.component').then((m) => m.TramiteDetailComponent),
      },
      {
        path: 'ai-assistant',
        loadComponent: () =>
          import('./pages/ai-assistant/ai-assistant.component').then((m) => m.AiAssistantComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
