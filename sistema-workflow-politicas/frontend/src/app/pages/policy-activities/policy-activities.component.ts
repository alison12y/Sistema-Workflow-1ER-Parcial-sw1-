import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { WorkflowActivityService } from '../../services/workflow-activity.service';
import { PolicyService } from '../../services/policy.service';
import { WorkflowActivity } from '../../models/workflow.model';

@Component({
  selector: 'app-policy-activities',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './policy-activities.component.html',
  styleUrl: './policy-activities.component.scss',
})
export class PolicyActivitiesComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly activityService = inject(WorkflowActivityService);
  private readonly policyService = inject(PolicyService);

  policyId = '';
  policyName = '';
  activities: WorkflowActivity[] = [];
  loading = true;
  error = '';

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      this.policyId = params.get('id') ?? '';
      if (this.policyId) {
        this.loadPolicyName();
        this.loadActivities();
      }
    });
  }

  private loadPolicyName(): void {
    this.policyService.getById(this.policyId).subscribe({
      next: (p) => (this.policyName = p.name),
    });
  }

  loadActivities(): void {
    this.loading = true;
    this.activityService.getByPolicy(this.policyId).subscribe({
      next: (data) => {
        this.activities = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar las actividades del workflow.';
        this.loading = false;
      },
    });
  }
}
