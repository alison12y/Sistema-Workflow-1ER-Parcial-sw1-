import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PolicyService } from '../../services/policy.service';
import { AuthService } from '../../services/auth.service';
import { PolicyDetail } from '../../models/workflow.model';
import { tramiteStatusLabel } from '../../utils/tramite-display.util';
import {
  activityStatusLabel,
  activityTypeLabel,
} from '../../utils/workflow-display.util';

@Component({
  selector: 'app-policy-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './policy-detail.component.html',
  styleUrl: './policy-detail.component.scss',
})
export class PolicyDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly policyService = inject(PolicyService);
  private readonly auth = inject(AuthService);

  policy: PolicyDetail | null = null;
  loading = true;
  error = '';
  message = '';
  actionLoading = false;

  readonly tramiteStatusLabel = tramiteStatusLabel;
  readonly activityTypeLabel = activityTypeLabel;
  readonly activityStatusLabel = activityStatusLabel;
  readonly canManageActivities = this.auth.canManageWorkflowActivities();
  readonly canViewDesigner = this.auth.canViewWorkflowDesigner();
  readonly canEditDesigner = this.auth.canEditWorkflowDesigner();

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) this.load(id);
    });
  }

  load(id: string): void {
    this.loading = true;
    this.error = '';
    this.policyService.getDetail(id).subscribe({
      next: (p) => {
        this.policy = p;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudo cargar el detalle de la política.';
        this.loading = false;
      },
    });
  }

  statusLabel(status?: string): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'ACTIVE') return 'Activa';
    if (s === 'INACTIVE') return 'Inactiva';
    if (s === 'DRAFT' || s === 'BORRADOR') return 'Borrador';
    return s || 'Borrador';
  }

  statusClass(status?: string): string {
    const s = (status ?? '').toUpperCase();
    if (s === 'ACTIVE') return 'active';
    if (s === 'INACTIVE') return 'inactive';
    return 'draft';
  }

  get previewActivities() {
    return (this.policy?.activities ?? []).slice(0, 3);
  }

  activate(): void {
    if (!this.policy?.id) return;
    this.actionLoading = true;
    this.policyService.activate(this.policy.id).subscribe({
      next: () => {
        this.message = 'Política activada correctamente';
        this.load(this.policy!.id!);
        this.actionLoading = false;
      },
      error: () => {
        this.error = 'No se pudo activar la política';
        this.actionLoading = false;
      },
    });
  }

  deactivate(): void {
    if (!this.policy?.id) return;
    this.actionLoading = true;
    this.policyService.deactivate(this.policy.id).subscribe({
      next: () => {
        this.message = 'Política desactivada correctamente';
        this.load(this.policy!.id!);
        this.actionLoading = false;
      },
      error: () => {
        this.error = 'No se pudo desactivar la política';
        this.actionLoading = false;
      },
    });
  }
}
