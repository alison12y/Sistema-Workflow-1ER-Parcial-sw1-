import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { WorkflowDesignerService } from '../../services/workflow-designer.service';
import { WorkflowTransitionService } from '../../services/workflow-transition.service';
import { AuthService } from '../../services/auth.service';
import {
  ActivityNode,
  TransitionEdge,
  WorkflowDesignerData,
  WorkflowFlowValidationResponse,
  WorkflowLane,
} from '../../models/workflow.model';
import { activityStatusClass } from '../../utils/workflow-display.util';

const NODE_WIDTH = 200;
const NODE_HEIGHT = 100;
const LANE_HEIGHT = 140;
const LANE_LABEL_WIDTH = 180;

@Component({
  selector: 'app-workflow-designer',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './workflow-designer.component.html',
  styleUrl: './workflow-designer.component.scss',
})
export class WorkflowDesignerComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly designerService = inject(WorkflowDesignerService);
  private readonly transitionService = inject(WorkflowTransitionService);
  private readonly auth = inject(AuthService);

  policyId: string | null = null;
  data: WorkflowDesignerData | null = null;
  loading = true;
  error = '';
  message = '';
  validating = false;
  validationResult: WorkflowFlowValidationResponse | null = null;
  showValidation = false;

  readonly canEdit = this.auth.canEditWorkflowDesigner();
  readonly activityStatusClass = activityStatusClass;
  readonly NODE_WIDTH = NODE_WIDTH;
  readonly LANE_HEIGHT = LANE_HEIGHT;

  canvasWidth = 1200;
  canvasHeight = 400;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      this.policyId = params.get('id');
      if (!this.policyId) {
        this.loading = false;
        this.error = 'No se encontró la política asociada.';
        return;
      }
      if (!this.auth.canViewWorkflowDesigner()) {
        this.loading = false;
        this.error = 'No tiene permiso para ver el diseñador de workflow.';
        return;
      }
      this.loadDesigner();
    });
  }

  loadDesigner(): void {
    if (!this.policyId) return;
    this.loading = true;
    this.error = '';
    this.designerService.getByPolicy(this.policyId).subscribe({
      next: (d) => {
        this.data = d;
        this.validationResult = d.flowValidation ?? null;
        this.updateCanvasSize();
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 403) {
          this.error = 'Acceso denegado. No tiene permiso para ver el diseñador.';
        } else if (err.status === 404) {
          this.error = 'La política solicitada no existe.';
        } else {
          this.error = err.error?.message ?? 'No se pudo cargar el diseñador de workflow.';
        }
      },
    });
  }

  get hasActivities(): boolean {
    return (this.data?.activities?.length ?? 0) > 0;
  }

  get hasTransitions(): boolean {
    return (this.data?.transitions?.length ?? 0) > 0;
  }

  get emptyActivitiesMessage(): string {
    return 'Esta política aún no tiene actividades configuradas. Cree actividades antes de diseñar el workflow.';
  }

  get emptyTransitionsMessage(): string {
    return 'Esta política tiene actividades, pero aún no tiene conexiones entre ellas.';
  }

  policyStatusClass(): string {
    const s = (this.data?.policyStatus ?? '').toLowerCase();
    if (s.includes('activa')) return 'active';
    if (s.includes('inactiva') || s.includes('archiv')) return 'inactive';
    return 'draft';
  }

  validateFlow(): void {
    if (!this.policyId) return;
    this.validating = true;
    this.showValidation = true;
    this.transitionService.validatePolicyFlow(this.policyId).subscribe({
      next: (result) => {
        this.validationResult = result;
        this.validating = false;
        if (result.valid) {
          this.message = 'El flujo es válido.';
        } else {
          this.message = result.message;
        }
        setTimeout(() => (this.message = ''), 6000);
      },
      error: (err) => {
        this.validating = false;
        this.error = err.error?.message ?? 'No se pudo validar el flujo.';
      },
    });
  }

  closeValidation(): void {
    this.showValidation = false;
  }

  goBack(): void {
    if (this.policyId) {
      this.router.navigate(['/policies', this.policyId]);
    } else {
      this.router.navigate(['/policies']);
    }
  }

  nodeClass(type?: string): string {
    const t = (type ?? 'TASK').toUpperCase();
    if (t === 'START') return 'node-start';
    if (t === 'END') return 'node-end';
    if (t === 'DECISION') return 'node-decision';
    return 'node-task';
  }

  nodeById(id?: string): ActivityNode | undefined {
    return this.data?.activities.find((a) => a.id === id);
  }

  getEdgePath(edge: TransitionEdge): string {
    const from = this.nodeById(edge.fromActivityId);
    const to = this.nodeById(edge.toActivityId);
    if (!from || !to || from.x == null || from.y == null || to.x == null || to.y == null) {
      return '';
    }
    const sx = from.x + NODE_WIDTH;
    const sy = from.y + NODE_HEIGHT / 2;
    const tx = to.x;
    const ty = to.y + NODE_HEIGHT / 2;
    const mx = (sx + tx) / 2;
    return `M ${sx} ${sy} C ${mx} ${sy}, ${mx} ${ty}, ${tx} ${ty}`;
  }

  getEdgeLabelPosition(edge: TransitionEdge): { x: number; y: number } {
    const from = this.nodeById(edge.fromActivityId);
    const to = this.nodeById(edge.toActivityId);
    if (!from || !to || from.x == null || from.y == null || to.x == null || to.y == null) {
      return { x: 0, y: 0 };
    }
    const sx = from.x + NODE_WIDTH;
    const sy = from.y + NODE_HEIGHT / 2;
    const tx = to.x;
    const ty = to.y + NODE_HEIGHT / 2;
    return { x: (sx + tx) / 2, y: (sy + ty) / 2 - 10 };
  }

  edgeLabel(edge: TransitionEdge): string {
    if (edge.conditionLabel?.trim()) {
      return edge.conditionLabel.trim();
    }
    if (edge.transitionType?.toUpperCase() === 'CONDITIONAL') {
      return edge.transitionTypeLabel ?? 'Condicional';
    }
    return '';
  }

  updateCanvasSize(): void {
    if (!this.data?.activities?.length) {
      this.canvasWidth = 1200;
      this.canvasHeight = Math.max(400, (this.data?.lanes?.length ?? 1) * LANE_HEIGHT + 80);
      return;
    }
    let maxX = 900;
    let maxY = 400;
    for (const node of this.data.activities) {
      if (node.x != null && node.y != null) {
        maxX = Math.max(maxX, node.x + NODE_WIDTH + 80);
        maxY = Math.max(maxY, node.y + NODE_HEIGHT + 60);
      }
    }
    this.canvasWidth = Math.max(1200, maxX);
    this.canvasHeight = Math.max(
      (this.data.lanes?.length ?? 1) * LANE_HEIGHT + 80,
      maxY
    );
  }

  laneTop(index: number): number {
    return index * LANE_HEIGHT;
  }

  trackLane(_index: number, lane: WorkflowLane): string {
    return lane.laneName;
  }

  trackActivity(_index: number, activity: ActivityNode): string {
    return activity.id ?? activity.name;
  }

  trackEdge(_index: number, edge: TransitionEdge): string {
    return edge.id ?? `${edge.fromActivityId}-${edge.toActivityId}`;
  }
}
