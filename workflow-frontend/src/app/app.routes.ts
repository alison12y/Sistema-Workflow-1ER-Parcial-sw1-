import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { Policies } from './pages/policies/policies';
import { WorkflowDesigner } from './pages/workflow-designer/workflow-designer';
import { Forms } from './pages/forms/forms';
import { Kpis } from './pages/kpis/kpis';
import { Monitoring } from './pages/monitoring/monitoring';
import { AiAssistant } from './pages/ai-assistant/ai-assistant';

export const routes: Routes = [
  { path: '', component: Login },
  { path: 'dashboard', component: Dashboard },
  { path: 'policies', component: Policies },
  { path: 'workflow-designer', component: WorkflowDesigner },
  { path: 'forms', component: Forms },
  { path: 'kpis', component: Kpis },
  { path: 'monitoring', component: Monitoring },
  { path: 'ai-assistant', component: AiAssistant },
  { path: '**', redirectTo: '' }
];