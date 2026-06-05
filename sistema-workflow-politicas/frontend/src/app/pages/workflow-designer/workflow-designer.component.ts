import {
  Component,
  ChangeDetectorRef,
  ElementRef,
  HostListener,
  NgZone,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
} from '@angular/core';

import { CommonModule } from '@angular/common';

import { FormsModule } from '@angular/forms';

import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, interval, Observable, of, Subscription } from 'rxjs';
import { concatMap, map, switchMap } from 'rxjs/operators';

import { WorkflowDesignerService } from '../../services/workflow-designer.service';
import { WorkflowCollaborationService } from '../../services/workflow-collaboration.service';
import {
  WorkflowCollaborationActiveEdit,
  WorkflowCollaborationConnectedUser,
  WorkflowCollaborationRecentAction,
  WorkflowCollaborationState,
} from '../../models/workflow-collaboration.model';

import { WorkflowActivityService } from '../../services/workflow-activity.service';

import { WorkflowTransitionService } from '../../services/workflow-transition.service';

import { AuthService } from '../../services/auth.service';
import { AiService } from '../../services/ai.service';
import {
  AiWorkflowSuggestRequest,
  AiWorkflowSuggestResponse,
} from '../../models/ai-workflow-suggest.model';

import {
  ActivityNode,
  TransitionEdge,
  WorkflowActivity,
  WorkflowActivityRequest,
  WorkflowDesignerData,
  WorkflowFlowValidationResponse,
  WorkflowLane,
  WorkflowTransition,
  WorkflowTransitionRequest,
} from '../../models/workflow.model';

import {
  ACTIVITY_STATUS_OPTIONS,
  ACTIVITY_TYPE_OPTIONS,
  RESPONSIBLE_OPTIONS,
  RESPONSIBLE_TYPE_OPTIONS,
  activityStatusClass,
  activityStatusLabel,
  activityTypeLabel,
} from '../../utils/workflow-display.util';

import {
  applyGatewayTransitionTypeHint,
  edgeCanvasLabel,
  TransitionFormLike,
  TRANSITION_TYPE_OPTIONS,
  transitionConditionRequired,
  transitionShowsConditionField,
  transitionStatusClass,
  transitionStatusLabel,
  transitionTypeHint,
  transitionTypeLabel,
  transitionWizardSkipsConditionStep,
  validateTransitionFormInput,
} from '../../utils/transition-display.util';
import {
  defaultGatewayName,
  gatewayActivityTypeLabel,
  isGatewayActivityType,
  isForkGateway,
  isJoinGateway,
} from '../../utils/workflow-gateway.util';
import {
  activityTypeHelp,
  buildSequentialEdges,
  CONDITION_LABEL_EXAMPLES,
  DEFAULT_QUICK_FLOW_ROWS,
  friendlyValidationMessage,
  FlowTemplateActivity,
  FlowTemplateEdge,
  GUIDED_ACTIVITY_TYPE_OPTIONS,
  GUIDED_TRANSITION_TYPE_OPTIONS,
  QuickFlowRow,
  SUGGESTED_LANE_NAMES,
  UML_TOOLBOX_ITEMS,
  UmlToolboxItem,
} from '../../utils/workflow-designer-guided.util';
import { isVisibleActivity, isVisibleTransition } from '../../utils/workflow-visibility.util';
import {
  appendDictationText,
  isVoiceDictationSupported,
  VoiceDictationController,
  VoiceDictationHandlers,
} from '../../utils/voice-dictation.util';

type UmlVisualType = 'START' | 'END' | 'DECISION' | 'TASK' | 'FORK' | 'JOIN';

interface NodeSize {
  width: number;

  height: number;
}

interface Point {
  x: number;

  y: number;
}

interface DragState {
  nodeId: string;
  offsetX: number;
  offsetY: number;
}

interface ValidationCheckItem {
  id: string;
  label: string;
  status: 'pass' | 'fail' | 'warn';
  detail?: string;
}

const LANE_LABEL_WIDTH = 200;

const LANE_HEIGHT = 200;

const NODE_SLOT_WIDTH = 280;

const NODE_TOP_PADDING_SLOT = 50;

const AUTO_LAYOUT_H_GAP = 220;

const CANVAS_PADDING = 60;

const COLLABORATION_POLL_MS = 11_000;
const COLLABORATION_CONFLICT_FALLBACK =
  'Este workflow fue modificado por otro usuario. Recargue antes de guardar.';

@Component({
  selector: 'app-workflow-designer',

  standalone: true,

  imports: [CommonModule, FormsModule, RouterLink],

  templateUrl: './workflow-designer.component.html',

  styleUrl: './workflow-designer.component.scss',
})
export class WorkflowDesignerComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);

  private readonly router = inject(Router);

  private readonly designerService = inject(WorkflowDesignerService);
  private readonly collaborationService = inject(WorkflowCollaborationService);

  private readonly activityService = inject(WorkflowActivityService);

  private readonly transitionService = inject(WorkflowTransitionService);

  private readonly auth = inject(AuthService);
  private readonly aiService = inject(AiService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly zone = inject(NgZone);

  aiPrompt = '';
  aiLoading = false;
  aiApplying = false;
  aiError = '';
  aiSuggestion: AiWorkflowSuggestResponse | null = null;
  voiceListening = false;
  voiceStatus = '';
  readonly aiExamplePrompt =
    'Crear una actividad Validar documentación en el departamento Legal y conectarla después de Recepción de solicitud de forma secuencial.';
  readonly voiceSupported = isVoiceDictationSupported();

  private voiceDictation: VoiceDictationController | null = null;

  policyId: string | null = null;

  collaborationConflictMessage = COLLABORATION_CONFLICT_FALLBACK;
  collaborationSessionId = '';
  collaborationState: WorkflowCollaborationState | null = null;
  collaborationConflict = false;
  collaborationPolling = false;
  private loadedRevision = 0;
  private collaborationStarted = false;
  private conflictReported = false;
  private collaborationPollSub?: Subscription;
  /** Usuario JWT al que pertenece la sesión colaborativa activa (evita mezclar cuentas). */
  private collaborationOwnerUsername = '';
  private trackedEditingElementId: string | null = null;

  data: WorkflowDesignerData | null = null;

  loading = true;

  error = '';

  message = '';

  validating = false;

  validationResult: WorkflowFlowValidationResponse | null = null;

  showValidation = false;

  editMode = false;

  saving = false;

  modalError = '';

  crudActivities: WorkflowActivity[] = [];

  allTransitions: WorkflowTransition[] = [];

  activityModalOpen = false;

  activityViewOpen = false;

  editingActivityId: string | null = null;

  viewingActivity: WorkflowActivity | ActivityNode | null = null;

  activityForm: WorkflowActivityRequest = this.emptyActivityForm();

  transitionModalOpen = false;

  transitionViewOpen = false;

  editingTransitionId: string | null = null;

  viewingTransition: WorkflowTransition | null = null;

  transitionForm: WorkflowTransitionRequest = this.emptyTransitionForm();

  selectedNodeId: string | null = null;

  selectedEdgeId: string | null = null;

  activityModalGuided = false;

  quickFlowOpen = false;

  quickFlowStep = 1;

  quickFlowName = '';

  quickFlowRows: QuickFlowRow[] = [];

  readonly guidedActivityTypeOptions = GUIDED_ACTIVITY_TYPE_OPTIONS;

  readonly guidedTransitionTypeOptions = GUIDED_TRANSITION_TYPE_OPTIONS;
  transitionFormWarning = '';

  readonly conditionLabelExamples = CONDITION_LABEL_EXAMPLES;

  readonly umlToolboxItems = UML_TOOLBOX_ITEMS;

  readonly suggestedLaneNames = SUGGESTED_LANE_NAMES;

  canvasZoom = 100;

  transitionWizardOpen = false;

  transitionWizardStep = 1;

  transitionWizardForm: WorkflowTransitionRequest = this.emptyTransitionForm();

  propertiesSaving = false;

  propertiesActivityForm: WorkflowActivityRequest | null = null;

  propertiesTransitionForm: WorkflowTransitionRequest | null = null;

  propertiesEditingTransitionId: string | null = null;

  connectionsPanelOpen = false;

  lanesPanelOpen = false;

  flowTextPanelOpen = true;

  helpPanelOpen = false;

  propertiesPanelVisible = true;

  customLaneNames: string[] = [];

  newLaneName = '';

  laneModalOpen = false;

  laneModalName = '';

  laneRemoveBlockedOpen = false;

  laneReassignModalOpen = false;

  laneActionTarget: string | null = null;

  laneReassignTarget = '';

  reassigningLane = false;

  cleaningDuplicates = false;

  private logConnectionsAfterCleanup = false;

  preselectedLaneForActivity: string | null = null;

  dragging = false;

  dragState: DragState | null = null;

  savingVisual = false;

  pendingVisualChanges = new Map<string, { x: number; y: number }>();

  propertiesPositionX: number | null = null;

  propertiesPositionY: number | null = null;

  private outgoingEdgeIndex = new Map<string, number>();

  private incomingEdgeIndex = new Map<string, number>();

  private outgoingEdgeTotal = new Map<string, number>();

  private incomingEdgeTotal = new Map<string, number>();

  @ViewChild('diagramViewport') diagramViewport?: ElementRef<HTMLDivElement>;

  readonly canEdit = this.auth.canEditWorkflowDesigner();
  readonly canViewForms = this.auth.canViewDynamicForms();

  readonly activityStatusClass = activityStatusClass;

  readonly activityTypeLabel = activityTypeLabel;

  readonly activityStatusLabel = activityStatusLabel;

  readonly activityTypeOptions = ACTIVITY_TYPE_OPTIONS;

  readonly activityStatusOptions = ACTIVITY_STATUS_OPTIONS;

  readonly responsibleOptions = RESPONSIBLE_OPTIONS;

  readonly responsibleTypeOptions = RESPONSIBLE_TYPE_OPTIONS;

  readonly transitionTypeOptions = TRANSITION_TYPE_OPTIONS;

  readonly transitionTypeLabel = transitionTypeLabel;

  readonly transitionStatusLabel = transitionStatusLabel;

  readonly transitionStatusClass = transitionStatusClass;

  readonly LANE_HEIGHT = LANE_HEIGHT;

  readonly LANE_LABEL_WIDTH = LANE_LABEL_WIDTH;

  canvasWidth = 1200;

  canvasHeight = 400;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const nextPolicyId = params.get('id');

      if (!nextPolicyId) {
        this.loading = false;

        this.error = 'No se encontró la política asociada.';

        return;
      }

      if (!this.auth.canViewWorkflowDesigner()) {
        this.loading = false;

        this.error = 'No tiene permiso para ver el diseñador de workflow.';

        return;
      }

      if (this.policyId && this.policyId !== nextPolicyId) {
        this.resetCollaborationSession();
        this.abortVoiceDictation();
      }

      this.policyId = nextPolicyId;
      this.ensureCollaborationIdentity();
      this.loadDesigner();
    });
  }

  ngOnDestroy(): void {
    this.abortVoiceDictation();
    this.resetCollaborationSession();
  }

  loadDesigner(showLoading = true): void {
    if (!this.policyId) return;

    if (showLoading) {
      this.loading = true;
    }

    this.error = '';

    forkJoin({
      designer: this.designerService.getByPolicy(this.policyId),
      transitions: this.transitionService.getByPolicy(this.policyId),
      activities: this.activityService.getByPolicy(this.policyId),
    }).subscribe({
      next: ({ designer, transitions, activities }) => {
        this.applyDesignerSnapshot(designer, transitions, activities);
        if (showLoading) {
          this.loading = false;
        }
        this.startCollaborationIfNeeded();
        this.cdr.detectChanges();
      },

      error: (err) => {
        this.loading = false;

        if (err.status === 403) {
          this.error =
            'Acceso denegado. No tiene permiso para ver el diseñador.';
        } else if (err.status === 404) {
          this.error = 'La política solicitada no existe.';
        } else {
          this.error =
            err.error?.message ?? 'No se pudo cargar el diseñador de workflow.';
        }
        this.cdr.detectChanges();
      },
    });
  }

  private applyDesignerSnapshot(
    designer: WorkflowDesignerData,
    transitions: WorkflowTransition[],
    activities: WorkflowActivity[],
  ): void {
    this.data = designer;
    this.allTransitions = [...transitions];
    this.crudActivities = activities.filter(isVisibleActivity);

    this.pruneSelectionsAfterReload();
    this.enrichNodes();
    this.rebuildEdgeMaps();
    this.logEdgeDrawDiagnostics();

    this.validationResult = designer.flowValidation ?? null;
    this.loadLanePrefs();
    this.updateCanvasSize();
    this.syncPropertiesPanel();

    if (this.logConnectionsAfterCleanup) {
      console.log('[cleanup] conexiones después de loadDesigner():', transitions.length);
      this.logConnectionsAfterCleanup = false;
    }

    if (this.collaborationStarted && !this.collaborationConflict) {
      this.syncRevisionAfterOwnSave();
    }
  }

  private pruneSelectionsAfterReload(): void {
    if (this.selectedNodeId && !this.nodeById(this.selectedNodeId)) {
      this.selectedNodeId = null;
    }

    if (
      this.selectedEdgeId &&
      !this.canvasActiveTransitions.some((edge) => edge.id === this.selectedEdgeId)
    ) {
      this.selectedEdgeId = null;
    }
  }

  loadCrudData(): void {
    this.loadCrudActivities();

    this.loadConnectionsList();
  }

  loadCrudActivities(): void {
    if (!this.policyId) return;

    this.activityService.getByPolicy(this.policyId).subscribe({
      next: (list) => {
        this.crudActivities = list.filter(isVisibleActivity);
        this.cdr.detectChanges();
      },
    });
  }

  loadConnectionsList(): void {
    if (!this.policyId) return;

    this.transitionService.getByPolicy(this.policyId).subscribe({
      next: (list) => {
        this.allTransitions = [...list];
        this.rebuildEdgeMaps();
        this.enrichNodes();
        this.logEdgeDrawDiagnostics();
        this.pruneSelectionsAfterReload();
        this.cdr.detectChanges();
      },
    });
  }

  get connectionsPanelRows(): WorkflowTransition[] {
    const raw = this.connectionsSourceRows();
    return this.dedupeTransitionsForDisplay(raw.filter(isVisibleTransition));
  }

  private connectionsSourceRows(): WorkflowTransition[] {
    if (this.allTransitions.length) {
      return this.allTransitions;
    }

    return (this.data?.transitions ?? []).map((edge) => ({
      id: edge.id,
      fromActivityId: edge.fromActivityId,
      fromActivityName: edge.fromActivityName,
      toActivityId: edge.toActivityId,
      toActivityName: edge.toActivityName,
      transitionType: edge.transitionType,
      transitionTypeLabel: edge.transitionTypeLabel,
      conditionLabel: edge.conditionLabel,
      active: edge.active,
    }));
  }

  get duplicateConnectionsCount(): number {
    const source = this.allTransitions.length
      ? this.allTransitions.filter((t) => t.active !== false)
      : (this.data?.transitions ?? [])
          .filter((e) => e.active !== false)
          .map((e) => ({
          fromActivityId: e.fromActivityId,
          toActivityId: e.toActivityId,
          fromActivityName: e.fromActivityName,
          toActivityName: e.toActivityName,
        }));
    const keys = new Set<string>();
    let duplicates = 0;
    for (const t of source) {
      const key = this.transitionPairKey(t);
      if (!key) continue;
      if (keys.has(key)) duplicates += 1;
      else keys.add(key);
    }
    return duplicates;
  }

  private normalizeTransitionName(name?: string | null): string {
    return (name ?? '').trim().toLowerCase().replace(/\s+/g, ' ');
  }

  private transitionPairKey(transition: {
    fromActivityId?: string;
    toActivityId?: string;
    fromActivityName?: string;
    toActivityName?: string;
  }): string | null {
    const fromName = this.normalizeTransitionName(transition.fromActivityName);
    const toName = this.normalizeTransitionName(transition.toActivityName);
    if (fromName && toName) {
      return `${fromName}->${toName}`;
    }
    if (transition.fromActivityId && transition.toActivityId) {
      return `${transition.fromActivityId}->${transition.toActivityId}`;
    }
    return null;
  }

  private dedupeTransitionsForDisplay(list: WorkflowTransition[]): WorkflowTransition[] {
    const byPair = new Map<string, WorkflowTransition>();

    for (const transition of list) {
      const key = this.transitionPairKey(transition);
      if (!key) continue;
      const existing = byPair.get(key);
      if (!existing) {
        byPair.set(key, transition);
        continue;
      }
      byPair.set(key, this.preferredTransition(existing, transition));
    }

    return [...byPair.values()].sort(
      (a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0),
    );
  }

  private preferredTransition(
    a: WorkflowTransition,
    b: WorkflowTransition,
  ): WorkflowTransition {
    const aActive = a.active !== false;
    const bActive = b.active !== false;
    if (aActive && !bActive) return a;
    if (bActive && !aActive) return b;
    const aTime = a.updatedAt ?? a.createdAt ?? '';
    const bTime = b.updatedAt ?? b.createdAt ?? '';
    return bTime >= aTime ? b : a;
  }

  cleanDuplicateConnections(): void {
    if (!this.ensureCanEdit() || !this.policyId) return;
    const beforeCount = this.allTransitions.length;
    console.log('[cleanup] conexiones antes:', beforeCount);
    this.cleaningDuplicates = true;
    this.allTransitions = [];
    this.transitionService.cleanupByPolicy(this.policyId).subscribe({
      next: (result) => {
        console.log('[cleanup] respuesta:', result);
        this.cleaningDuplicates = false;
        this.message = result.message || 'Conexiones duplicadas limpiadas correctamente.';
        if (result.warning) {
          this.error = result.warning;
        } else if (result.duplicateActivitiesDetected) {
          this.error =
            'Existen actividades duplicadas con nombres similares. Revise las actividades.';
        } else {
          this.error = '';
        }
        this.logConnectionsAfterCleanup = true;
        this.loadDesigner(false);
        this.flashMessage();
      },
      error: (err: HttpErrorResponse) => {
        this.cleaningDuplicates = false;
        if (err.status === 403) {
          this.error = 'No tienes permiso para limpiar conexiones.';
          return;
        }
        if (err.status === 401) {
          this.error = 'Sesión no válida. Inicie sesión nuevamente.';
          return;
        }
        this.error = this.resolveMutationError(
          err,
          'No se pudieron limpiar las conexiones duplicadas.',
        );
      },
    });
  }

  private handleTransitionCreateSuccess(
    result: WorkflowTransition,
    onDone?: () => void,
  ): void {
    this.message = result.reactivated
      ? 'Conexión reactivada correctamente.'
      : 'Conexión creada correctamente.';
    this.saving = false;
    onDone?.();
    this.loadDesigner(false);
    this.flashMessage();
  }

  private enrichNodes(): void {
    if (!this.data?.activities?.length) return;

    const incoming = new Map<string, number>();

    const outgoing = new Map<string, number>();

    const outgoingConditional = new Map<string, number>();

    for (const edge of this.canvasActiveTransitions) {
      if (edge.active === false || !edge.fromActivityId || !edge.toActivityId)
        continue;

      outgoing.set(
        edge.fromActivityId,
        (outgoing.get(edge.fromActivityId) ?? 0) + 1,
      );

      incoming.set(
        edge.toActivityId,
        (incoming.get(edge.toActivityId) ?? 0) + 1,
      );

      if (
        edge.transitionType?.toUpperCase() === 'CONDITIONAL' ||
        edge.conditionLabel?.trim()
      ) {
        outgoingConditional.set(
          edge.fromActivityId,

          (outgoingConditional.get(edge.fromActivityId) ?? 0) + 1,
        );
      }
    }

    for (const node of this.data.activities) {
      if (!node.id) continue;

      node.incomingCount = incoming.get(node.id) ?? node.incomingCount ?? 0;

      node.outgoingCount = outgoing.get(node.id) ?? node.outgoingCount ?? 0;

      node.outgoingConditionalCount =
        outgoingConditional.get(node.id) ?? node.outgoingConditionalCount ?? 0;

      node.decisionNode =
        node.decisionNode ??
        (node.activityType?.toUpperCase() === 'DECISION' ||
          (node.outgoingConditionalCount ?? 0) > 1);
    }
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

  get diagramHint(): string {
    if (this.editMode && this.canEdit) {
      return 'Arrastre los nodos para ordenar el diagrama. Use Autoacomodar si desea reorganizar el flujo automáticamente.';
    }

    if (this.canEdit) {
      return 'Diagrama en modo solo lectura. Active «Modo edición» para mover nodos y modificar actividades y conexiones.';
    }

    return 'Diagrama de solo lectura según notación UML 2.5. El avance del proceso se lee de izquierda a derecha; cada canal representa un responsable.';
  }

  get hasPendingVisualChanges(): boolean {
    return this.pendingVisualChanges.size > 0;
  }

  policyStatusClass(): string {
    const s = (this.data?.policyStatus ?? '').toLowerCase();

    if (s.includes('activa')) return 'active';

    if (s.includes('inactiva') || s.includes('archiv')) return 'inactive';

    return 'draft';
  }

  toggleEditMode(): void {
    if (!this.canEdit) {
      this.message = 'No tienes permiso para modificar el diseño.';

      this.flashMessage();

      return;
    }

    this.editMode = !this.editMode;

    if (this.editMode) {
      this.loadCrudData();
    } else {
      this.clearDiagramSelection();
    }

    this.syncPropertiesPanel();
  }

  cancelEditMode(): void {
    this.editMode = false;
    this.clearDiagramSelection();

    this.closeActivityModal();

    this.closeTransitionModal();
  }

  validateFlow(): void {
    if (!this.policyId) return;

    this.validating = true;
    this.showValidation = true;
    this.message = '';

    this.transitionService.validatePolicyFlow(this.policyId).subscribe({
      next: (result) => {
        this.validationResult = {
          ...result,
          errors: (result.errors ?? []).map(friendlyValidationMessage),
          warnings: (result.warnings ?? []).map(friendlyValidationMessage),
          message: friendlyValidationMessage(result.message),
        };
        this.validating = false;
        this.cdr.detectChanges();
      },

      error: (err) => {
        this.validating = false;
        this.showValidation = false;
        this.error = err.error?.message ?? 'No se pudo validar el flujo.';
        this.cdr.detectChanges();
      },
    });
  }

  closeValidation(): void {
    this.showValidation = false;
  }

  validationStatusTitle(): string {
    if (!this.validationResult) return '';
    return this.validationResult.valid ? 'Flujo válido' : 'Flujo inválido';
  }

  validationErrorCount(): number {
    return this.validationResult?.errors?.length ?? 0;
  }

  validationWarningCount(): number {
    return this.validationResult?.warnings?.length ?? 0;
  }

  validationPassedCheckCount(): number {
    return this.validationChecklist().filter((item) => item.status === 'pass').length;
  }

  validationChecklist(): ValidationCheckItem[] {
    const errors = (this.validationResult?.errors ?? []).map((e) => e.toLowerCase());
    const warnings = (this.validationResult?.warnings ?? []).map((w) => w.toLowerCase());
    const matches = (patterns: string[]) =>
      patterns.some((p) => errors.some((e) => e.includes(p)) || warnings.some((w) => w.includes(p)));

    const startFail = matches(['inicio', 'start']);
    const endFail = matches(['fin', 'end']);
    const connectivityWarn = matches([
      'aislad',
      'sin conexiones',
      'sin salida',
      'sin entrada',
      'conectar las actividades',
    ]);
    const responsibleFail = matches(['responsable']);
    const transitionFail = matches([
      'conexión',
      'conexion',
      'transición',
      'transicion',
      'condicional',
      'paralel',
      'iterativ',
      'origen',
      'destino',
    ]);

    const activities = this.crudActivities.filter(isVisibleActivity);
    const transitions = this.allTransitions.filter(isVisibleTransition);
    const startCount = activities.filter((a) => (a.activityType ?? '').toUpperCase() === 'START').length;
    const endCount = activities.filter((a) => (a.activityType ?? '').toUpperCase() === 'END').length;
    const tasks = activities.filter((a) => (a.activityType ?? '').toUpperCase() === 'TASK');
    const tasksWithResponsible = tasks.filter(
      (a) => !!(a.responsibleName?.trim() || a.responsibleType?.trim()),
    );

    return [
      {
        id: 'start',
        label: 'Nodo Inicio',
        status: startFail ? 'fail' : startCount === 1 ? 'pass' : startCount === 0 ? 'fail' : 'warn',
        detail:
          startCount === 0
            ? 'Falta actividad START'
            : startCount > 1
              ? 'Hay más de un inicio'
              : undefined,
      },
      {
        id: 'end',
        label: 'Nodo Fin',
        status: endFail ? 'fail' : endCount >= 1 ? 'pass' : 'fail',
        detail: endCount === 0 ? 'Falta actividad END' : undefined,
      },
      {
        id: 'connected',
        label: 'Actividades conectadas',
        status: connectivityWarn ? 'warn' : activities.length <= 1 || transitions.length > 0 ? 'pass' : 'warn',
        detail: connectivityWarn ? 'Revise actividades aisladas o sin enlace' : undefined,
      },
      {
        id: 'responsible',
        label: 'Responsables definidos',
        status: responsibleFail
          ? 'fail'
          : tasks.length === 0 || tasksWithResponsible.length === tasks.length
            ? 'pass'
            : 'warn',
        detail:
          tasks.length > tasksWithResponsible.length
            ? `${tasks.length - tasksWithResponsible.length} tarea(s) sin responsable`
            : undefined,
      },
      {
        id: 'transitions',
        label: 'Transiciones válidas',
        status: transitionFail ? 'fail' : transitions.length > 0 || activities.length <= 1 ? 'pass' : 'warn',
        detail: transitionFail ? 'Revise conexiones y tipos de transición' : undefined,
      },
    ];
  }

  validationCheckIcon(status: ValidationCheckItem['status']): string {
    if (status === 'pass') return '✓';
    if (status === 'fail') return '✕';
    return '!';
  }

  goBack(): void {
    if (this.policyId) {
      this.router.navigate(['/policies', this.policyId]);
    } else {
      this.router.navigate(['/policies']);
    }
  }

  selectNode(node: ActivityNode): void {
    if (!node.id) return;
    if (!this.confirmSoftLock('ACTIVITY', node.id)) return;
    this.clearActiveEditingRegistration();
    this.selectedNodeId = node.id;
    this.selectedEdgeId = null;
    this.syncPropertiesPanel();
    this.reportActiveEditing('ACTIVITY', node.id, node.name ?? 'Actividad', 'SELECTING');
  }

  selectEdge(edge: TransitionEdge | WorkflowTransition): void {
    if (!edge.id) return;
    if (!this.confirmSoftLock('TRANSITION', edge.id)) return;
    this.clearActiveEditingRegistration();
    this.selectedEdgeId = edge.id;
    this.selectedNodeId = null;
    this.syncPropertiesPanel();
    const label =
      edge.fromActivityName && edge.toActivityName
        ? `${edge.fromActivityName} → ${edge.toActivityName}`
        : 'Conexión';
    this.reportActiveEditing('TRANSITION', edge.id, label, 'SELECTING');
  }

  private clearDiagramSelection(): void {
    this.clearActiveEditingRegistration();
    this.selectedNodeId = null;
    this.selectedEdgeId = null;
  }

  get zoomScale(): number {
    return this.canvasZoom / 100;
  }

  get zoomLabel(): string {
    return `${this.canvasZoom}%`;
  }

  zoomIn(): void {
    this.canvasZoom = Math.min(150, this.canvasZoom + 10);
  }

  zoomOut(): void {
    this.canvasZoom = Math.max(50, this.canvasZoom - 10);
  }

  resetZoom(): void {
    this.canvasZoom = 100;
  }

  centerCanvas(): void {
    const el = this.diagramViewport?.nativeElement;
    if (!el) return;
    const scrollLeft = (el.scrollWidth - el.clientWidth) / 2;
    const scrollTop = (el.scrollHeight - el.clientHeight) / 2;
    el.scrollTo({ left: Math.max(0, scrollLeft), top: Math.max(0, scrollTop), behavior: 'smooth' });
  }

  fitToFlow(): void {
    const el = this.diagramViewport?.nativeElement;
    if (!el || !this.canvasWidth) return;
    const padding = 48;
    const scaleW = (el.clientWidth - padding) / this.canvasWidth;
    const scaleH = (el.clientHeight - padding) / this.canvasHeight;
    const scale = Math.min(1.5, Math.max(0.5, Math.min(scaleW, scaleH)));
    this.canvasZoom = Math.round(scale * 100);
    setTimeout(() => this.centerCanvas(), 50);
  }

  onToolboxClick(item: UmlToolboxItem): void {
    if (!item.enabled) return;
    if (!this.ensureCanEdit()) return;
    this.ensureEditModeForGuided();
    if (item.kind === 'transition') {
      this.openTransitionWizard();
      return;
    }
    if (item.activityType) {
      this.openCreateActivityFromToolbox(item.activityType);
    }
  }

  openCreateActivityFromToolbox(activityType: string): void {
    this.editingActivityId = null;
    this.preselectedLaneForActivity = null;
    const normalized = activityType.toUpperCase();
    const gateway = isGatewayActivityType(normalized);
    this.activityForm = {
      ...this.emptyActivityForm(),
      activityType: normalized,
      name: gateway ? defaultGatewayName(normalized) : '',
      responsibleName: '',
      estimatedTimeHours: gateway ? undefined : 1,
    };
    this.activityModalGuided = true;
    this.modalError = '';
    this.activityModalOpen = true;
  }

  isGatewayActivityForm(): boolean {
    return isGatewayActivityType(this.activityForm.activityType);
  }

  gatewayActivityFormHint(): string {
    if (isForkGateway(this.activityForm.activityType)) {
      return 'Pasarela de bifurcación: no genera tarea humana. Conecte entrada secuencial y al menos dos salidas PARALLEL_SPLIT.';
    }
    if (isJoinGateway(this.activityForm.activityType)) {
      return 'Pasarela de unión: no genera tarea humana. Conecte al menos dos entradas PARALLEL_JOIN y una salida hacia la siguiente actividad.';
    }
    return '';
  }

  isGatewayPropertiesNode(): boolean {
    return isGatewayActivityType(this.propertiesActivityForm?.activityType);
  }

  gatewayPropertiesTypeHint(): string {
    return gatewayActivityTypeLabel(this.propertiesActivityForm?.activityType);
  }

  openTransitionWizard(fromActivityId?: string): void {
    if (!this.ensureCanEdit()) return;
    this.ensureEditModeForGuided();
    this.transitionWizardForm = this.emptyTransitionForm();
    if (fromActivityId) {
      this.transitionWizardForm.fromActivityId = fromActivityId;
      this.transitionWizardStep = 2;
    } else {
      this.transitionWizardStep = 1;
    }
    this.modalError = '';
    this.transitionWizardOpen = true;
  }

  closeTransitionWizard(): void {
    this.transitionWizardOpen = false;
    this.transitionWizardStep = 1;
    this.modalError = '';
  }

  transitionWizardNext(): void {
    if (this.transitionWizardStep === 1 && !this.transitionWizardForm.fromActivityId) {
      this.modalError = 'Seleccione la actividad de origen.';
      return;
    }
    if (this.transitionWizardStep === 2 && !this.transitionWizardForm.toActivityId) {
      this.modalError = 'Seleccione la actividad de destino.';
      return;
    }
    if (this.transitionWizardStep === 4) {
      const validation = validateTransitionFormInput(
        this.transitionWizardForm,
        this.allTransitions,
      );
      if (validation.error) {
        this.modalError = validation.error;
        return;
      }
      this.transitionFormWarning = validation.warning ?? '';
    }
    this.modalError = '';
    if (this.transitionWizardStep < 5) {
      if (this.transitionWizardStep === 2) {
        this.applyGatewayTransitionHintsToForm(this.transitionWizardForm);
      }
      if (
        this.transitionWizardStep === 3 &&
        transitionWizardSkipsConditionStep(this.transitionWizardForm.transitionType)
      ) {
        this.transitionWizardStep = 5;
      } else {
        this.transitionWizardStep += 1;
      }
    }
  }

  transitionWizardPrev(): void {
    this.modalError = '';
    if (this.transitionWizardStep > 1) {
      if (
        this.transitionWizardStep === 5 &&
        transitionWizardSkipsConditionStep(this.transitionWizardForm.transitionType)
      ) {
        this.transitionWizardStep = 3;
      } else {
        this.transitionWizardStep -= 1;
      }
    }
  }

  confirmTransitionWizard(): void {
    if (
      this.transitionWizardForm.fromActivityId === this.transitionWizardForm.toActivityId
    ) {
      this.modalError = 'Origen y destino no pueden ser iguales.';
      return;
    }
    const clientValidation = validateTransitionFormInput(
      this.transitionWizardForm,
      this.allTransitions,
    );
    if (clientValidation.error) {
      this.modalError = clientValidation.error;
      return;
    }
    this.transitionFormWarning = clientValidation.warning ?? '';
    this.saving = true;
    const payload: WorkflowTransitionRequest = {
      policyId: this.policyId ?? undefined,
      fromActivityId: this.transitionWizardForm.fromActivityId,
      toActivityId: this.transitionWizardForm.toActivityId,
      transitionType: (this.transitionWizardForm.transitionType ?? 'SEQUENTIAL').toUpperCase(),
      conditionLabel: this.transitionWizardForm.conditionLabel?.trim() || undefined,
      active: true,
    };
    this.transitionService.create(payload).subscribe({
      next: (result) => {
        this.closeTransitionWizard();
        this.handleTransitionCreateSuccess(result);
      },
      error: (err: HttpErrorResponse) => {
        this.modalError = this.resolveMutationError(err, 'No se pudo guardar la conexión.');
        this.saving = false;
      },
    });
  }

  get selectedTransition(): WorkflowTransition | undefined {
    if (!this.selectedEdgeId) return undefined;
    return this.allTransitions.find((t) => t.id === this.selectedEdgeId);
  }

  get primaryToolboxItems(): UmlToolboxItem[] {
    return this.umlToolboxItems.filter((i) => !i.soon);
  }

  get connectionsCountLabel(): string {
    const n = this.connectionsPanelRows.length;
    return n === 1 ? '1 conexión configurada' : `${n} conexiones configuradas`;
  }

  get lanesCountLabel(): string {
    const n = this.displayLaneNames.length;
    return n === 1 ? '1 carril detectado' : `${n} carriles detectados`;
  }

  togglePropertiesPanel(): void {
    this.propertiesPanelVisible = !this.propertiesPanelVisible;
  }

  get displayLaneNames(): string[] {
    const fromData = (this.data?.lanes ?? []).map((l) => l.laneName);
    const merged = [...new Set([...fromData, ...this.customLaneNames])];
    return merged.filter(Boolean);
  }

  get displayLanes(): WorkflowLane[] {
    const byName = new Map<string, WorkflowLane>();
    for (const lane of this.data?.lanes ?? []) {
      byName.set(lane.laneName.trim(), lane);
    }
    return this.displayLaneNames.map(
      (name) => byName.get(name.trim()) ?? { laneName: name, activities: [] },
    );
  }

  get showDesignerWorkspace(): boolean {
    return this.hasActivities || this.displayLaneNames.length > 0;
  }

  private laneStorageKey(): string {
    return `workflow-designer-lanes-${this.policyId ?? ''}`;
  }

  loadLanePrefs(): void {
    if (!this.policyId) return;
    try {
      const raw = sessionStorage.getItem(this.laneStorageKey());
      this.customLaneNames = raw ? (JSON.parse(raw) as string[]) : [];
    } catch {
      this.customLaneNames = [];
    }
  }

  private saveLanePrefs(): void {
    if (!this.policyId) return;
    sessionStorage.setItem(this.laneStorageKey(), JSON.stringify(this.customLaneNames));
  }

  activitiesInLane(laneName: string): number {
    return (this.data?.activities ?? []).filter(
      (a) => (a.responsibleName ?? '').trim() === laneName.trim(),
    ).length;
  }

  isLaneEmpty(laneName: string): boolean {
    return this.activitiesInLane(laneName) === 0;
  }

  get laneActionActivityCount(): number {
    return this.laneActionTarget ? this.activitiesInLane(this.laneActionTarget) : 0;
  }

  get laneReassignTargetOptions(): string[] {
    if (!this.laneActionTarget) return [];
    return this.displayLaneNames.filter(
      (n) => n.trim().toLowerCase() !== this.laneActionTarget!.trim().toLowerCase(),
    );
  }

  private activitiesInLaneRecords(laneName: string): WorkflowActivity[] {
    const lane = laneName.trim();
    const byId = new Map<string, WorkflowActivity>();

    for (const act of this.crudActivities) {
      if ((act.responsibleName ?? '').trim() === lane && act.id) {
        byId.set(act.id, act);
      }
    }

    for (const node of this.data?.activities ?? []) {
      if (!node.id || byId.has(node.id)) continue;
      if ((node.responsibleName ?? '').trim() !== lane) continue;
      byId.set(node.id, {
        id: node.id,
        name: node.name,
        description: node.description,
        policyId: this.policyId ?? undefined,
        responsibleType: 'ROLE',
        responsibleName: node.responsibleName,
        activityType: node.activityType,
        status: node.status,
        orderIndex: node.orderIndex,
        estimatedTimeHours: node.estimatedTimeHours,
      });
    }

    return [...byId.values()];
  }

  openAddLaneModal(): void {
    if (!this.ensureCanEdit()) return;
    this.ensureEditModeForGuided();
    this.laneModalName = '';
    this.modalError = '';
    this.laneModalOpen = true;
  }

  closeAddLaneModal(): void {
    this.laneModalOpen = false;
    this.laneModalName = '';
    this.modalError = '';
  }

  saveAddLaneModal(): void {
    if (!this.ensureCanEdit()) return;
    const name = this.laneModalName.trim();
    if (!name) {
      this.modalError = 'Indique el nombre del carril.';
      return;
    }
    if (this.displayLaneNames.some((n) => n.toLowerCase() === name.toLowerCase())) {
      this.modalError = 'Ese carril ya existe.';
      return;
    }
    this.customLaneNames = [...this.customLaneNames, name];
    this.saveLanePrefs();
    this.updateCanvasSize();
    this.closeAddLaneModal();
    this.message = 'Carril agregado correctamente.';
    this.flashMessage();
  }

  private registerLaneName(name?: string): void {
    const trimmed = (name ?? '').trim();
    if (!trimmed) return;
    if (this.displayLaneNames.some((n) => n.toLowerCase() === trimmed.toLowerCase())) {
      return;
    }
    this.customLaneNames = [...this.customLaneNames, trimmed];
    this.saveLanePrefs();
  }

  openCreateActivityInLane(laneName: string): void {
    if (!this.ensureCanEdit()) return;
    this.ensureEditModeForGuided();
    this.editingActivityId = null;
    this.preselectedLaneForActivity = laneName;
    this.activityForm = {
      ...this.emptyActivityForm(),
      responsibleName: laneName,
    };
    this.activityModalGuided = false;
    this.modalError = '';
    this.activityModalOpen = true;
  }

  addCustomLane(): void {
    const name = this.newLaneName.trim();
    if (!name) return;
    if (!this.displayLaneNames.some((n) => n.toLowerCase() === name.toLowerCase())) {
      this.customLaneNames = [...this.customLaneNames, name];
      this.saveLanePrefs();
      this.updateCanvasSize();
      this.message = 'Carril agregado correctamente.';
      this.flashMessage();
    }
    this.newLaneName = '';
  }

  renameLane(oldName: string, newName: string): void {
    const trimmed = newName.trim();
    if (!trimmed || trimmed === oldName || !this.ensureCanEdit()) return;
    const toUpdate = this.crudActivities.filter(
      (a) => (a.responsibleName ?? '').trim() === oldName.trim(),
    );
    if (!toUpdate.length) {
      this.customLaneNames = this.customLaneNames.map((n) => (n === oldName ? trimmed : n));
      this.saveLanePrefs();
      return;
    }
    this.saving = true;
    let chain$ = of(null as WorkflowActivity | null);
    for (const act of toUpdate) {
      if (!act.id) continue;
      chain$ = chain$.pipe(
        concatMap(() =>
          this.activityService.update(act.id!, {
            policyId: this.policyId!,
            name: act.name,
            description: act.description,
            responsibleType: act.responsibleType ?? 'ROLE',
            responsibleName: trimmed,
            activityType: act.activityType,
            status: act.status,
            orderIndex: act.orderIndex,
            estimatedTimeHours: act.estimatedTimeHours,
          }),
        ),
      );
    }
    chain$.subscribe({
      next: () => {
        this.saving = false;
        this.message = 'Carril actualizado correctamente.';
        this.loadDesigner(false);
        this.loadCrudActivities();
        this.flashMessage();
      },
      error: () => {
        this.saving = false;
        this.error = 'No se pudo renombrar el carril.';
      },
    });
  }

  removeCustomLane(laneName: string): void {
    if (!this.ensureCanEdit()) return;

    const count = this.activitiesInLane(laneName);
    if (count > 0) {
      this.laneActionTarget = laneName;
      this.modalError = '';
      this.laneRemoveBlockedOpen = true;
      return;
    }

    this.performRemoveLane(laneName);
  }

  closeLaneRemoveBlockedModal(): void {
    this.laneRemoveBlockedOpen = false;
    this.laneActionTarget = null;
    this.modalError = '';
  }

  openLaneReassignModal(): void {
    const options = this.laneReassignTargetOptions;
    if (!options.length) {
      this.modalError =
        'Debe existir al menos otro carril. Cree otro carril antes de reasignar las actividades.';
      return;
    }
    this.modalError = '';
    this.laneRemoveBlockedOpen = false;
    this.laneReassignModalOpen = true;
    this.laneReassignTarget = options[0];
  }

  closeLaneReassignModal(): void {
    this.laneReassignModalOpen = false;
    this.laneActionTarget = null;
    this.laneReassignTarget = '';
    this.modalError = '';
  }

  confirmLaneReassign(): void {
    if (!this.ensureCanEdit() || !this.laneActionTarget) return;

    const toLane = this.laneReassignTarget.trim();
    if (!toLane) {
      this.modalError = 'Seleccione el carril destino.';
      return;
    }
    if (toLane.toLowerCase() === this.laneActionTarget.trim().toLowerCase()) {
      this.modalError = 'Seleccione un carril distinto al actual.';
      return;
    }

    this.reassignActivitiesToLane(this.laneActionTarget, toLane);
  }

  private reassignActivitiesToLane(fromLane: string, toLane: string): void {
    const toUpdate = this.activitiesInLaneRecords(fromLane);
    if (!toUpdate.length) {
      this.modalError = 'No se encontraron actividades en este carril.';
      return;
    }

    this.reassigningLane = true;
    this.modalError = '';

    let chain$ = of(null as WorkflowActivity | null);
    for (const act of toUpdate) {
      if (!act.id) continue;
      chain$ = chain$.pipe(
        concatMap(() =>
          this.activityService.update(act.id!, {
            policyId: this.policyId!,
            name: act.name,
            description: act.description,
            responsibleType: act.responsibleType ?? 'ROLE',
            responsibleName: toLane,
            activityType: act.activityType,
            status: act.status,
            orderIndex: act.orderIndex,
            estimatedTimeHours: act.estimatedTimeHours,
          }),
        ),
      );
    }

    const sourceLane = fromLane;
    chain$.subscribe({
      next: () => {
        this.reassigningLane = false;
        this.closeLaneReassignModal();
        this.performRemoveLane(sourceLane, false);
        this.message = 'Actividades reasignadas correctamente.';
        this.loadDesigner(false);
        this.loadCrudActivities();
        this.flashMessage();
      },
      error: (err: HttpErrorResponse) => {
        this.reassigningLane = false;
        this.error = this.resolveMutationError(err, 'No se pudieron reasignar las actividades.');
      },
    });
  }

  private performRemoveLane(laneName: string, showMessage = true): void {
    this.customLaneNames = this.customLaneNames.filter((n) => n !== laneName);
    this.saveLanePrefs();
    this.updateCanvasSize();
    if (showMessage) {
      this.message = 'Carril quitado correctamente.';
      this.flashMessage();
    }
  }

  syncPropertiesPanel(): void {
    this.propertiesActivityForm = null;
    this.propertiesTransitionForm = null;
    this.propertiesEditingTransitionId = null;
    this.propertiesPositionX = null;
    this.propertiesPositionY = null;

    if (this.selectedNodeId) {
      const full = this.crudActivities.find((a) => a.id === this.selectedNodeId);
      const node = this.nodeById(this.selectedNodeId);
      if (node) {
        this.propertiesPositionX = Math.round(node.x ?? 0);
        this.propertiesPositionY = Math.round(node.y ?? 0);
      }
      if (full) {
        this.propertiesActivityForm = {
          policyId: this.policyId ?? undefined,
          name: full.name,
          description: full.description ?? '',
          responsibleType: full.responsibleType ?? 'ROLE',
          responsibleName: full.responsibleName ?? '',
          activityType: full.activityType ?? 'TASK',
          status: full.status ?? 'BORRADOR',
          orderIndex: full.orderIndex,
          estimatedTimeHours: full.estimatedTimeHours ?? 1,
        };
      } else if (node) {
        this.propertiesActivityForm = {
          policyId: this.policyId ?? undefined,
          name: node.name,
          description: node.description ?? '',
          responsibleType: 'ROLE',
          responsibleName: node.responsibleName ?? '',
          activityType: node.activityType ?? 'TASK',
          status: node.status ?? 'BORRADOR',
          orderIndex: node.orderIndex,
          estimatedTimeHours: node.estimatedTimeHours ?? 1,
        };
      }
      return;
    }

    const tr = this.selectedTransition;
    if (tr?.id) {
      this.propertiesEditingTransitionId = tr.id;
      this.propertiesTransitionForm = {
        policyId: this.policyId ?? undefined,
        fromActivityId: tr.fromActivityId,
        toActivityId: tr.toActivityId,
        transitionType: (tr.transitionType ?? 'SEQUENTIAL').toUpperCase(),
        conditionLabel: tr.conditionLabel ?? '',
        conditionExpression: tr.conditionExpression ?? '',
        orderIndex: tr.orderIndex,
        active: tr.active !== false,
      };
    }
  }

  savePropertiesPanel(): void {
    if (!this.canEdit || !this.editMode) return;
    if (this.propertiesActivityForm && this.selectedNodeId) {
      if (!this.confirmSoftLock('ACTIVITY', this.selectedNodeId)) return;
      if (!this.propertiesActivityForm.name?.trim()) {
        this.error = 'El nombre es obligatorio.';
        return;
      }
      this.reportActiveEditing(
        'ACTIVITY',
        this.selectedNodeId,
        this.propertiesActivityForm.name.trim(),
        'EDITING',
      );
      this.registerLaneName(this.propertiesActivityForm.responsibleName);
      this.propertiesSaving = true;
      this.activityService
        .update(this.selectedNodeId, {
          ...this.propertiesActivityForm,
          policyId: this.policyId ?? undefined,
          name: this.propertiesActivityForm.name.trim(),
          responsibleName: this.propertiesActivityForm.responsibleName?.trim() || undefined,
        })
        .subscribe({
          next: () => {
            this.propertiesSaving = false;
            this.message = 'Actividad actualizada correctamente.';
            this.loadDesigner(false);
            this.loadCrudActivities();
            this.flashMessage();
          },
          error: (err: HttpErrorResponse) => {
            this.propertiesSaving = false;
            this.error = this.resolveMutationError(err, 'No se pudo guardar la actividad.');
          },
        });
      return;
    }

    if (this.propertiesTransitionForm && this.propertiesEditingTransitionId) {
      if (!this.confirmSoftLock('TRANSITION', this.propertiesEditingTransitionId)) return;
      const clientValidation = validateTransitionFormInput(
        this.propertiesTransitionForm,
        this.allTransitions,
        this.propertiesEditingTransitionId,
      );
      if (clientValidation.error) {
        this.error = clientValidation.error;
        return;
      }
      this.reportActiveEditing(
        'TRANSITION',
        this.propertiesEditingTransitionId,
        'Conexión',
        'EDITING',
      );
      this.propertiesSaving = true;
      const payload = {
        ...this.propertiesTransitionForm,
        policyId: this.policyId ?? undefined,
        transitionType: (
          this.propertiesTransitionForm.transitionType ?? 'SEQUENTIAL'
        ).toUpperCase(),
        conditionLabel: this.propertiesTransitionForm.conditionLabel?.trim() || undefined,
        conditionExpression:
          this.propertiesTransitionForm.conditionExpression?.trim() || undefined,
      };
      this.transitionService.update(this.propertiesEditingTransitionId, payload).subscribe({
        next: () => {
          this.propertiesSaving = false;
          this.message = clientValidation.warning
            ? `Conexión actualizada correctamente. ${clientValidation.warning}`
            : 'Conexión actualizada correctamente.';
          this.loadDesigner(false);
          this.flashMessage();
        },
        error: (err: HttpErrorResponse) => {
          this.propertiesSaving = false;
          this.error = this.resolveMutationError(err, 'No se pudo guardar la conexión.');
        },
      });
    }
  }

  cancelPropertiesPanel(): void {
    this.syncPropertiesPanel();
  }

  canEditProperties(): boolean {
    return this.canEdit && this.editMode;
  }

  isNodeSelected(node: ActivityNode): boolean {
    return !!node.id && node.id === this.selectedNodeId;
  }

  isEdgeSelected(edge: TransitionEdge): boolean {
    return !!edge.id && edge.id === this.selectedEdgeId;
  }

  isTransitionRowSelected(t: WorkflowTransition): boolean {
    return !!t.id && t.id === this.selectedEdgeId;
  }

  // ——— Actividades ———

  private emptyActivityForm(): WorkflowActivityRequest {
    return {
      policyId: this.policyId ?? undefined,

      name: '',

      description: '',

      responsibleType: 'ROLE',

      responsibleName: '',

      activityType: 'TASK',

      status: 'BORRADOR',

      orderIndex: undefined,

      estimatedTimeHours: 1,
    };
  }

  openCreateActivity(guided = false): void {
    if (!this.ensureCanEdit()) return;

    this.editingActivityId = null;
    this.preselectedLaneForActivity = null;

    this.activityForm = this.emptyActivityForm();

    this.activityModalGuided = guided;

    this.modalError = '';

    this.activityModalOpen = true;
  }

  openFirstActivityGuided(): void {
    this.ensureEditModeForGuided();
    this.openCreateActivity(true);
  }

  private ensureEditModeForGuided(): void {
    if (!this.canEdit) return;
    if (!this.editMode) {
      this.editMode = true;
      this.loadCrudData();
    }
  }

  openQuickFlowWizard(): void {
    if (!this.ensureCanEdit()) return;
    if (this.hasActivities) {
      const ok = confirm(
        'Esta política ya tiene actividades. ¿Desea agregar este flujo al existente?',
      );
      if (!ok) return;
    }
    this.ensureEditModeForGuided();
    this.quickFlowName = this.data?.policyName ?? '';
    this.quickFlowRows = DEFAULT_QUICK_FLOW_ROWS.map((r) => ({ ...r }));
    this.quickFlowStep = 1;
    this.quickFlowOpen = true;
    this.modalError = '';
  }

  closeQuickFlowWizard(): void {
    this.quickFlowOpen = false;
    this.quickFlowStep = 1;
    this.modalError = '';
  }

  quickFlowNextStep(): void {
    if (this.quickFlowStep === 1 && !this.quickFlowName.trim()) {
      this.modalError = 'Indique un nombre para el flujo.';
      return;
    }
    if (this.quickFlowStep === 2) {
      const invalid = this.quickFlowRows.find((r) => !r.name.trim() || !r.responsible);
      if (invalid) {
        this.modalError = 'Cada actividad debe tener nombre y responsable.';
        return;
      }
    }
    this.modalError = '';
    if (this.quickFlowStep < 4) {
      this.quickFlowStep += 1;
    }
  }

  quickFlowPrevStep(): void {
    this.modalError = '';
    if (this.quickFlowStep > 1) this.quickFlowStep -= 1;
  }

  addQuickFlowRow(): void {
    const nextOrder =
      this.quickFlowRows.reduce((max, r) => Math.max(max, r.orderIndex), 0) + 1;
    this.quickFlowRows = [
      ...this.quickFlowRows,
      {
        orderIndex: nextOrder,
        name: '',
        responsible: 'Funcionario',
        activityType: 'TASK',
      },
    ];
  }

  removeQuickFlowRow(index: number): void {
    if (this.quickFlowRows.length <= 1) return;
    this.quickFlowRows = this.quickFlowRows.filter((_, i) => i !== index);
  }

  get quickFlowPreviewLines(): string[] {
    const sorted = [...this.quickFlowRows].sort((a, b) => a.orderIndex - b.orderIndex);
    const lines: string[] = [];
    for (let i = 0; i < sorted.length - 1; i++) {
      lines.push(`${sorted[i].name} → ${sorted[i + 1].name}`);
    }
    return lines;
  }

  confirmQuickFlow(): void {
    const sorted = [...this.quickFlowRows].sort((a, b) => a.orderIndex - b.orderIndex);
    const activities: FlowTemplateActivity[] = sorted.map((r, i) => ({
      orderIndex: i + 1,
      name: r.name.trim(),
      responsible: r.responsible,
      activityType: r.activityType as FlowTemplateActivity['activityType'],
    }));
    const edges = buildSequentialEdges(activities.length);
    this.persistGuidedFlow(
      activities,
      edges,
      'Flujo creado correctamente. Ya puede ver el diagrama.',
      () => this.closeQuickFlowWizard(),
    );
  }

  guidedActivityLabel(type: string): string {
    return (
      GUIDED_ACTIVITY_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? type
    );
  }

  private persistGuidedFlow(
    activities: FlowTemplateActivity[],
    edges: FlowTemplateEdge[],
    successMessage: string,
    onDone?: () => void,
  ): void {
    if (!this.policyId) return;
    this.saving = true;
    this.modalError = '';
    this.error = '';

    const sorted = [...activities].sort((a, b) => a.orderIndex - b.orderIndex);
    const idByOrder = new Map<number, string>();

    let pipeline$: Observable<WorkflowActivity | null> = of(null);

    for (const row of sorted) {
      pipeline$ = pipeline$.pipe(
        concatMap(() =>
          this.activityService
            .create({
              policyId: this.policyId!,
              name: row.name,
              responsibleType: 'ROLE',
              responsibleName: row.responsible,
              activityType: row.activityType,
              status: 'ACTIVA',
              orderIndex: row.orderIndex,
              estimatedTimeHours: 1,
            })
            .pipe(
              map((created) => {
                if (created.id) idByOrder.set(row.orderIndex, created.id);
                return created;
              }),
            ),
        ),
      );
    }

    pipeline$
      .pipe(
        switchMap(() => {
          const requests = edges
            .map((edge, index) => {
              const fromId = idByOrder.get(edge.fromOrder);
              const toId = idByOrder.get(edge.toOrder);
              if (!fromId || !toId) return null;
              return this.transitionService.create({
                policyId: this.policyId!,
                fromActivityId: fromId,
                toActivityId: toId,
                transitionType: edge.transitionType ?? 'SEQUENTIAL',
                conditionLabel: edge.conditionLabel,
                orderIndex: index + 1,
                active: true,
              });
            })
            .filter((r): r is Observable<WorkflowTransition> => r !== null);

          return requests.length ? forkJoin(requests) : of([]);
        }),
      )
      .subscribe({
        next: () => {
          this.saving = false;
          this.message = successMessage;
          this.editMode = true;
          onDone?.();
          this.loadDesigner(false);
          this.flashMessage();
        },
        error: (err: HttpErrorResponse) => {
          this.saving = false;
          this.modalError = this.resolveMutationError(
            err,
            'No se pudo crear el flujo. Intente de nuevo.',
          );
          this.error = this.modalError;
        },
      });
  }

  get selectedNode(): ActivityNode | undefined {
    if (!this.selectedNodeId || !this.data) return undefined;
    return this.data.activities.find((n) => n.id === this.selectedNodeId);
  }

  openConnectFromSelectedNode(): void {
    const node = this.selectedNode;
    if (!node?.id) return;
    this.openConnectFromNode(node);
  }

  openConnectFromNode(node: ActivityNode): void {
    if (!this.ensureCanEdit() || !node.id) return;
    this.editingTransitionId = null;
    this.transitionForm = this.emptyTransitionForm();
    this.transitionForm.fromActivityId = node.id;
    this.modalError = '';
    this.transitionModalOpen = true;
  }

  activityTypeHelpText(): string {
    return activityTypeHelp(this.activityForm.activityType);
  }

  friendlyValidationMessage = friendlyValidationMessage;

  openEditActivityNode(node: ActivityNode): void {
    if (!this.ensureCanEdit() || !node.id) return;

    const full = this.crudActivities.find((a) => a.id === node.id);

    if (full) {
      this.openEditActivity(full);

      return;
    }

    this.activityService.getById(node.id).subscribe({
      next: (a) => this.openEditActivity(a),

      error: () => {
        this.modalError = 'No se pudo cargar la actividad.';

        this.openEditActivityFromNode(node);
      },
    });
  }

  private openEditActivityFromNode(node: ActivityNode): void {
    this.editingActivityId = node.id ?? null;

    this.activityForm = {
      policyId: this.policyId ?? undefined,

      name: node.name,

      description: node.description ?? '',

      responsibleType: 'ROLE',

      responsibleName: node.responsibleName ?? '',

      activityType: node.activityType ?? 'TASK',

      status: node.status ?? 'BORRADOR',

      orderIndex: node.orderIndex,

      estimatedTimeHours: node.estimatedTimeHours ?? 1,
    };

    this.modalError = '';

    this.activityModalOpen = true;
  }

  openEditActivity(activity: WorkflowActivity): void {
    if (!this.ensureCanEdit()) return;

    this.editingActivityId = activity.id ?? null;

    this.activityForm = {
      policyId: this.policyId ?? undefined,

      name: activity.name,

      description: activity.description ?? '',

      responsibleType: activity.responsibleType ?? 'ROLE',

      responsibleId: activity.responsibleId,

      responsibleName: activity.responsibleName ?? '',

      activityType: activity.activityType ?? 'TASK',

      status: activity.status ?? 'BORRADOR',

      orderIndex: activity.orderIndex,

      estimatedTimeHours: activity.estimatedTimeHours ?? 1,
    };

    this.modalError = '';

    this.activityModalOpen = true;
  }

  openViewActivityNode(node: ActivityNode): void {
    const full = this.crudActivities.find((a) => a.id === node.id);

    this.viewingActivity = full ?? node;

    this.activityViewOpen = true;
  }

  openViewActivity(activity: WorkflowActivity): void {
    this.viewingActivity = activity;

    this.activityViewOpen = true;
  }

  closeActivityModal(): void {
    this.activityModalOpen = false;

    this.activityModalGuided = false;
    this.preselectedLaneForActivity = null;

    this.modalError = '';
  }

  closeActivityView(): void {
    this.activityViewOpen = false;

    this.viewingActivity = null;
  }

  saveActivity(): void {
    if (!this.ensureCanEdit()) return;

    if (!this.activityForm.name?.trim()) {
      this.modalError = 'El nombre de la actividad es obligatorio.';

      return;
    }

    this.saving = true;

    this.modalError = '';
    this.ensureCollaborationIdentity();
    this.logCollaborationActor(this.editingActivityId ? 'editar actividad' : 'crear actividad', {
      activityName: this.activityForm.name?.trim(),
    });

    const payload: WorkflowActivityRequest = {
      ...this.activityForm,

      policyId: this.policyId ?? undefined,

      name: this.activityForm.name.trim(),
      responsibleName: this.activityForm.responsibleName?.trim() || undefined,
    };

    this.registerLaneName(payload.responsibleName);

    const request$ = this.editingActivityId
      ? this.activityService.update(this.editingActivityId, payload)
      : this.activityService.create(payload);

    request$.subscribe({
      next: () => {
        this.message = this.editingActivityId
          ? 'Actividad actualizada correctamente.'
          : 'Actividad creada correctamente.';

        this.activityModalOpen = false;

        this.saving = false;

        this.loadDesigner(false);

        this.flashMessage();
      },

      error: (err: HttpErrorResponse) => {
        this.modalError = this.resolveMutationError(
          err,
          'No se pudo guardar la actividad.',
        );

        this.saving = false;
      },
    });
  }

  deactivateActivityNode(node: ActivityNode): void {
    if (!this.ensureCanEdit() || !node.id) return;

    const full = this.crudActivities.find((a) => a.id === node.id);

    if (full) {
      this.deactivateActivity(full);
    } else {
      this.activityService.deactivate(node.id).subscribe({
        next: () => {
          this.message = 'Actividad desactivada correctamente.';
          this.patchLocalActivityRemoved(node.id);
          this.loadDesigner(false);

          this.flashMessage();
        },

        error: (err: HttpErrorResponse) => {
          this.error = this.resolveMutationError(
            err,
            'No se pudo desactivar la actividad.',
          );
        },
      });
    }
  }

  deactivateActivity(activity: WorkflowActivity): void {
    if (!this.ensureCanEdit() || !activity.id) return;

    const isActive = activity.status === 'ACTIVA' && activity.active !== false;

    const action$ = isActive
      ? this.activityService.deactivate(activity.id)
      : this.activityService.activate(activity.id);

    action$.subscribe({
      next: () => {
        this.message = isActive
          ? 'Actividad desactivada correctamente.'
          : 'Actividad activada correctamente.';

        if (isActive && activity.id) {
          this.patchLocalActivityRemoved(activity.id);
        }

        this.loadDesigner(false);

        this.flashMessage();
      },

      error: (err: HttpErrorResponse) => {
        this.error = this.resolveMutationError(
          err,
          'No se pudo cambiar el estado de la actividad.',
        );
      },
    });
  }

  removeActivityNode(node: ActivityNode): void {
    if (!this.ensureCanEdit() || !node.id) return;
    if (!this.confirmSoftLock('ACTIVITY', node.id)) return;

    if (
      !confirm(
        '¿Está seguro de eliminar esta actividad? Las conexiones relacionadas pueden verse afectadas.',
      )
    ) {
      return;
    }

    this.activityService.delete(node.id).subscribe({
      next: (result) => {
        this.message = result.message || 'Actividad desactivada correctamente.';
        this.patchLocalActivityRemoved(node.id);
        this.loadDesigner(false);
        this.flashMessage();
      },

      error: (err: HttpErrorResponse) => {
        this.error = this.resolveMutationError(
          err,
          'No se pudo eliminar la actividad.',
        );
      },
    });
  }

  // ——— Conexiones ———

  private emptyTransitionForm(): WorkflowTransitionRequest {
    return {
      policyId: this.policyId ?? undefined,

      fromActivityId: '',

      toActivityId: '',

      transitionType: 'SEQUENTIAL',

      conditionLabel: '',

      conditionExpression: '',

      orderIndex: undefined,

      active: true,
    };
  }

  openCreateTransition(fromActivityId?: string): void {
    if (!this.ensureCanEdit()) return;

    this.editingTransitionId = null;

    this.transitionForm = this.emptyTransitionForm();

    if (fromActivityId) {
      this.transitionForm.fromActivityId = fromActivityId;
      this.applyGatewayTransitionHintsToForm(this.transitionForm);
    }

    this.modalError = '';

    this.transitionModalOpen = true;
  }

  onTransitionFormEndpointChange(): void {
    this.applyGatewayTransitionHintsToForm(this.transitionForm);
    this.onCollaborationPropertyEdit();
  }

  onPropertiesTransitionEndpointChange(): void {
    if (this.propertiesTransitionForm) {
      this.applyGatewayTransitionHintsToForm(this.propertiesTransitionForm);
    }
    this.onCollaborationPropertyEdit();
  }

  private applyGatewayTransitionHintsToForm(form: TransitionFormLike): void {
    const from = this.nodeById(form.fromActivityId);
    const to = this.nodeById(form.toActivityId);
    applyGatewayTransitionTypeHint(form, from?.activityType, to?.activityType);
  }

  openEditTransition(transition: WorkflowTransition): void {
    if (!this.ensureCanEdit()) return;

    this.editingTransitionId = transition.id ?? null;

    this.transitionForm = {
      policyId: this.policyId ?? undefined,

      fromActivityId: transition.fromActivityId,

      toActivityId: transition.toActivityId,

      transitionType: (transition.transitionType ?? 'SEQUENTIAL').toUpperCase(),

      conditionLabel: transition.conditionLabel ?? '',

      conditionExpression: transition.conditionExpression ?? '',

      orderIndex: transition.orderIndex,

      active: transition.active !== false,
    };

    this.transitionFormWarning = '';
    this.modalError = '';

    this.transitionModalOpen = true;
  }

  openViewTransition(transition: WorkflowTransition): void {
    this.viewingTransition = transition;

    this.transitionViewOpen = true;
  }

  closeTransitionModal(): void {
    this.transitionModalOpen = false;

    this.modalError = '';
  }

  closeTransitionView(): void {
    this.transitionViewOpen = false;

    this.viewingTransition = null;
  }

  saveTransition(): void {
    if (!this.ensureCanEdit()) return;

    if (
      !this.transitionForm.fromActivityId ||
      !this.transitionForm.toActivityId
    ) {
      this.modalError = 'Debe seleccionar actividad origen y destino.';

      return;
    }

    if (
      this.transitionForm.fromActivityId === this.transitionForm.toActivityId
    ) {
      this.modalError = 'La actividad origen y destino no pueden ser iguales.';

      return;
    }

    const clientValidation = validateTransitionFormInput(
      this.transitionForm,
      this.allTransitions,
      this.editingTransitionId,
    );
    if (clientValidation.error) {
      this.modalError = clientValidation.error;
      this.transitionFormWarning = '';
      return;
    }
    this.transitionFormWarning = clientValidation.warning ?? '';

    this.saving = true;

    this.modalError = '';
    this.ensureCollaborationIdentity();
    this.logCollaborationActor(
      this.editingTransitionId ? 'editar conexión' : 'crear conexión',
    );

    const payload: WorkflowTransitionRequest = {
      policyId: this.policyId ?? undefined,
      fromActivityId: this.transitionForm.fromActivityId,
      toActivityId: this.transitionForm.toActivityId,
      transitionType: (this.transitionForm.transitionType ?? 'SEQUENTIAL').toUpperCase(),
      conditionLabel: this.transitionForm.conditionLabel?.trim() || undefined,
      conditionExpression:
        this.transitionForm.conditionExpression?.trim() || undefined,
      orderIndex: this.transitionForm.orderIndex,
      active: this.transitionForm.active ?? true,
    };

    const request$ = this.editingTransitionId
      ? this.transitionService.update(this.editingTransitionId, payload)
      : this.transitionService.create(payload);

    request$.subscribe({
      next: (result) => {
        if (this.editingTransitionId) {
          this.message = this.transitionFormWarning
            ? `Conexión actualizada correctamente. ${this.transitionFormWarning}`
            : 'Conexión actualizada correctamente.';
          this.transitionFormWarning = '';
          this.transitionModalOpen = false;
          this.saving = false;
          this.loadDesigner(false);
          this.flashMessage();
          return;
        }
        this.transitionModalOpen = false;
        this.transitionFormWarning = '';
        this.handleTransitionCreateSuccess(result);
      },

      error: (err: HttpErrorResponse) => {
        this.modalError = this.resolveMutationError(
          err,
          this.editingTransitionId
            ? 'No se pudo guardar la conexión.'
            : 'No se pudo crear la conexión. Verifique que las actividades seleccionadas sean válidas.',
        );

        this.saving = false;
      },
    });
  }

  deactivateTransition(transition: WorkflowTransition): void {
    if (!this.ensureCanEdit() || !transition.id) return;

    const action$ =
      transition.active !== false
        ? this.transitionService.deactivate(transition.id)
        : this.transitionService.activate(transition.id);

    action$.subscribe({
      next: () => {
        this.message =
          transition.active !== false
            ? 'Conexión desactivada correctamente.'
            : 'Conexión activada correctamente.';

        if (transition.id) {
          const wasActive = transition.active !== false;
          this.patchLocalTransitionActiveState(transition.id, !wasActive);
        }

        this.loadDesigner(false);

        this.flashMessage();
      },

      error: (err: HttpErrorResponse) => {
        this.error = this.resolveMutationError(
          err,
          'No se pudo cambiar el estado de la conexión.',
        );
      },
    });
  }

  removeTransition(transition: WorkflowTransition): void {
    if (!this.ensureCanEdit() || !transition.id) return;

    const from = transition.fromActivityName ?? 'origen';

    const to = transition.toActivityName ?? 'destino';

    if (!confirm(`¿Eliminar la conexión de "${from}" hacia "${to}"?`)) return;

    this.transitionService.delete(transition.id).subscribe({
      next: (result) => {
        this.message = result.message || 'Conexión eliminada correctamente.';
        this.patchLocalTransitionRemoved(transition.id);
        this.loadDesigner(false);
        this.flashMessage();
      },

      error: (err: HttpErrorResponse) => {
        this.error = this.resolveMutationError(
          err,
          'No se pudo eliminar la conexión.',
        );
      },
    });
  }

  isConditionalTransitionType(): boolean {
    return transitionShowsConditionField(this.transitionForm.transitionType);
  }

  transitionConditionRequired(): boolean {
    return transitionConditionRequired(this.transitionForm.transitionType);
  }

  propertiesTransitionShowsCondition(): boolean {
    return transitionShowsConditionField(this.propertiesTransitionForm?.transitionType);
  }

  propertiesTransitionConditionRequired(): boolean {
    return transitionConditionRequired(this.propertiesTransitionForm?.transitionType);
  }

  currentTransitionFormHint(): string {
    return transitionTypeHint(this.transitionForm.transitionType);
  }

  propertiesTransitionTypeHint(): string {
    return transitionTypeHint(this.propertiesTransitionForm?.transitionType);
  }

  wizardTransitionTypeHint(): string {
    return transitionTypeHint(this.transitionWizardForm.transitionType);
  }

  wizardShowsConditionStep(): boolean {
    return transitionShowsConditionField(this.transitionWizardForm.transitionType);
  }

  wizardConditionRequired(): boolean {
    return transitionConditionRequired(this.transitionWizardForm.transitionType);
  }

  activityOptionsForSelect(): WorkflowActivity[] {
    const source = this.crudActivities.length
      ? this.crudActivities
      : (this.data?.activities ?? []).map((n) => ({
          id: n.id,
          name: n.name,
          policyId: this.policyId ?? undefined,
          active: true,
        }));

    return source.filter(isVisibleActivity);
  }

  activityNameById(id?: string): string {
    if (!id) return '—';
    return this.activityOptionsForSelect().find((a) => a.id === id)?.name ?? '—';
  }

  activityHasForm(node: ActivityNode): boolean {
    if (!node.id) return false;
    return !!this.crudActivities.find((activity) => activity.id === node.id)?.formId;
  }

  private ensureCanEdit(): boolean {
    if (!this.canEdit) {
      this.message = 'No tienes permiso para modificar el diseño.';

      this.flashMessage();

      return false;
    }

    if (this.collaborationConflict) {
      this.error = this.collaborationConflictMessage;
      this.cdr.detectChanges();
      return false;
    }

    return true;
  }

  reloadAfterConflict(): void {
    if (!this.policyId) return;
    this.collaborationConflict = false;
    this.conflictReported = false;
    this.error = '';
    this.loadDesigner(true);
  }

  connectedCollaborators(): WorkflowCollaborationConnectedUser[] {
    const state = this.collaborationState;
    if (!state) {
      return [];
    }
    if (state.connectedUsers?.length) {
      return state.connectedUsers;
    }
    return (state.connectedEditors ?? []).map((editor) => ({
      userId: editor.sessionId,
      username: editor.username,
      fullName: editor.displayName || editor.username,
      currentUser: editor.currentUser,
    }));
  }

  activeEditsNow(): WorkflowCollaborationActiveEdit[] {
    const edits = this.collaborationState?.activeEdits ?? [];
    return edits.filter((edit) => !edit.currentUser);
  }

  recentCollaborationActivity(): WorkflowCollaborationRecentAction[] {
    return this.collaborationState?.recentActions ?? [];
  }

  editingNowLabel(edit: WorkflowCollaborationActiveEdit): string {
    const name = edit.fullName?.trim() || edit.username || 'Usuario';
    const action = (edit.action ?? 'SELECTING').toUpperCase();
    let verb = 'está revisando';
    if (action === 'EDITING') verb = 'está editando';
    if (action === 'MOVING') verb = 'está moviendo';
    const element = edit.elementName?.trim() || edit.elementId || 'elemento';
    const you = edit.currentUser ? ' (usted)' : '';
    return `${name}${you} ${verb} “${element}”`;
  }

  collaborationLastModifiedLabel(): string {
    const state = this.collaborationState;
    if (!state?.lastModifiedAt) {
      return 'Sin modificaciones registradas';
    }
    if (state.lastModifiedSummary?.trim()) {
      const when = new Date(state.lastModifiedAt).toLocaleString();
      return `${state.lastModifiedSummary.trim()} — ${when}`;
    }
    const who =
      state.lastModifiedByName?.trim() ||
      state.lastModifiedByDisplayName?.trim() ||
      state.lastModifiedByUsername?.trim() ||
      '—';
    const verb = state.lastModifiedActionLabel?.trim() || 'modificó el workflow';
    const element = state.lastModifiedElementName?.trim();
    const kind =
      state.lastModifiedElementType === 'ACTIVITY'
        ? 'actividad'
        : state.lastModifiedElementType === 'TRANSITION'
          ? 'conexión'
          : 'elemento';
    const detail = element ? `${verb} ${kind} “${element}”` : verb;
    const when = new Date(state.lastModifiedAt).toLocaleString();
    return `${who} ${detail} — ${when}`;
  }

  recentActivityLabel(action: WorkflowCollaborationRecentAction): string {
    if (action.summary?.trim()) {
      return action.summary.trim();
    }
    const who =
      action.modifiedByName?.trim() ||
      action.modifiedByDisplayName?.trim() ||
      action.modifiedByUsername?.trim() ||
      'Usuario';
    const verb = action.actionLabel?.trim() || 'modificó';
    const element = action.elementName?.trim();
    const kind =
      action.elementType === 'ACTIVITY'
        ? 'actividad'
        : action.elementType === 'TRANSITION'
          ? 'conexión'
          : 'elemento';
    return element ? `${who} ${verb} ${kind} “${element}”` : `${who} ${verb}`;
  }

  recentActivityTime(action: WorkflowCollaborationRecentAction): string {
    if (!action.modifiedAt) return '';
    return new Date(action.modifiedAt).toLocaleTimeString([], {
      hour: 'numeric',
      minute: '2-digit',
    });
  }

  private updateCollaborationConflictMessage(): void {
    const state = this.collaborationState;
    if (!state?.staleForClient) {
      this.collaborationConflictMessage = COLLABORATION_CONFLICT_FALLBACK;
      return;
    }
    const who =
      state.lastModifiedByName?.trim() ||
      state.lastModifiedByDisplayName?.trim() ||
      state.lastModifiedByUsername?.trim() ||
      'Otro usuario';
    const when = state.lastModifiedAt
      ? new Date(state.lastModifiedAt).toLocaleTimeString([], {
          hour: 'numeric',
          minute: '2-digit',
        })
      : '';
    this.collaborationConflictMessage = when
      ? `${who} modificó el workflow a las ${when}. Recargue para ver los cambios antes de guardar.`
      : `${who} modificó el workflow. Recargue para ver los cambios antes de guardar.`;
  }

  isElementLockedByOther(
    elementType: 'ACTIVITY' | 'TRANSITION',
    elementId: string | null | undefined,
  ): boolean {
    return !!this.findOtherUserEditing(elementType, elementId);
  }

  private findOtherUserEditing(
    elementType: 'ACTIVITY' | 'TRANSITION',
    elementId: string | null | undefined,
  ): WorkflowCollaborationActiveEdit | null {
    if (!elementId) return null;
    const me = this.auth.getCurrentUser()?.username?.trim().toLowerCase() ?? '';
    return (
      this.activeEditsNow().find(
        (edit) =>
          edit.elementId === elementId &&
          (edit.elementType ?? '').toUpperCase() === elementType &&
          !edit.currentUser &&
          (edit.username?.trim().toLowerCase() ?? '') !== me,
      ) ?? null
    );
  }

  private confirmSoftLock(
    elementType: 'ACTIVITY' | 'TRANSITION',
    elementId: string | null | undefined,
  ): boolean {
    const other = this.findOtherUserEditing(elementType, elementId);
    if (!other) return true;
    const who = other.fullName?.trim() || other.username || 'otro usuario';
    return confirm(
      `Este elemento está siendo editado por ${who}. ¿Desea continuar de todos modos?`,
    );
  }

  onCollaborationPropertyEdit(): void {
    if (!this.canEdit || !this.editMode) return;
    if (this.selectedNodeId && this.propertiesActivityForm?.name) {
      this.reportActiveEditing(
        'ACTIVITY',
        this.selectedNodeId,
        this.propertiesActivityForm.name.trim() || 'Actividad',
        'EDITING',
      );
      return;
    }
    if (this.selectedEdgeId) {
      const tr = this.selectedTransition;
      const label =
        tr?.fromActivityName && tr?.toActivityName
          ? `${tr.fromActivityName} → ${tr.toActivityName}`
          : 'Conexión';
      this.reportActiveEditing('TRANSITION', this.selectedEdgeId, label, 'EDITING');
    }
  }

  private reportActiveEditing(
    elementType: 'ACTIVITY' | 'TRANSITION',
    elementId: string,
    elementName: string,
    action: 'SELECTING' | 'EDITING' | 'MOVING',
  ): void {
    if (!this.policyId || !this.collaborationSessionId || !this.editMode) return;
    this.ensureCollaborationIdentity();
    this.logCollaborationActor('reportEditing', { elementId, elementName, action });
    this.trackedEditingElementId = elementId;
    this.collaborationService
      .reportEditing(this.policyId, {
        sessionId: this.collaborationSessionId,
        elementType,
        elementId,
        elementName,
        action,
      })
      .subscribe({
        next: (state) => {
          this.collaborationState = this.normalizeCollaborationState(state);
          this.cdr.detectChanges();
        },
        error: () => undefined,
      });
  }

  private clearActiveEditingRegistration(): void {
    if (!this.policyId || !this.collaborationSessionId || !this.trackedEditingElementId) {
      this.trackedEditingElementId = null;
      return;
    }
    const elementId = this.trackedEditingElementId;
    this.trackedEditingElementId = null;
    this.collaborationService
      .clearEditing(this.policyId, this.collaborationSessionId, elementId)
      .subscribe({
        next: (state) => {
          this.collaborationState = this.normalizeCollaborationState(state);
          this.cdr.detectChanges();
        },
        error: () => undefined,
      });
  }

  private createCollaborationSessionId(): string {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
      return crypto.randomUUID();
    }
    return `sess-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  }

  private collaborationStorageKey(): string {
    const username = this.auth.getCurrentUser()?.username?.trim() ?? 'anon';
    return `workflow-collaboration-session:${this.policyId ?? ''}:${username}`;
  }

  private logCollaborationActor(context: string, extra?: Record<string, unknown>): void {
    const user = this.auth.getCurrentUser();
    console.log(`[CU16] ${context}`, {
      username: user?.username ?? null,
      fullName: user?.fullName ?? null,
      sessionId: this.collaborationSessionId || null,
      tokenPresent: !!this.auth.getToken(),
      ...extra,
    });
  }

  /** Asegura que la sesión colaborativa pertenece al usuario JWT actual (no reutiliza otro usuario). */
  private ensureCollaborationIdentity(): void {
    const user = this.auth.getCurrentUser();
    const username = user?.username?.trim() ?? '';
    if (!username || !this.policyId) {
      return;
    }

    let userChanged = false;
    if (
      this.collaborationOwnerUsername &&
      this.collaborationOwnerUsername !== username
    ) {
      userChanged = true;
      this.logCollaborationActor('usuario autenticado cambió — reiniciando colaboración', {
        previous: this.collaborationOwnerUsername,
        current: username,
      });
      this.resetCollaborationSession();
    }

    this.collaborationOwnerUsername = username;

    const storageKey = this.collaborationStorageKey();
    const stored = sessionStorage.getItem(storageKey);
    if (stored) {
      this.collaborationSessionId = stored;
    } else if (!this.collaborationSessionId) {
      this.collaborationSessionId = this.createCollaborationSessionId();
      sessionStorage.setItem(storageKey, this.collaborationSessionId);
    }

    if (userChanged && this.collaborationSessionId) {
      this.startCollaborationIfNeeded();
    }
  }

  private startCollaborationIfNeeded(): void {
    if (!this.policyId || this.collaborationStarted) return;
    this.ensureCollaborationIdentity();
    if (!this.collaborationSessionId) {
      this.collaborationSessionId = this.createCollaborationSessionId();
      sessionStorage.setItem(this.collaborationStorageKey(), this.collaborationSessionId);
    }
    this.collaborationStarted = true;
    this.logCollaborationActor('abriendo colaboración');

    this.collaborationService.open(this.policyId, this.collaborationSessionId).subscribe({
      next: (state) => {
        this.applyCollaborationState(state, false);
        this.startCollaborationPolling();
        this.cdr.detectChanges();
      },
      error: () => {
        this.collaborationPolling = false;
      },
    });
  }

  private startCollaborationPolling(): void {
    if (this.collaborationPollSub || !this.policyId) return;
    this.collaborationPolling = true;
    this.collaborationPollSub = interval(COLLABORATION_POLL_MS).subscribe(() => {
      this.pollCollaboration();
    });
    setTimeout(() => this.pollCollaboration(), 2_000);
  }

  private stopCollaborationPolling(): void {
    this.collaborationPollSub?.unsubscribe();
    this.collaborationPollSub = undefined;
    this.collaborationPolling = false;
  }

  private pollCollaboration(): void {
    if (!this.policyId || !this.collaborationSessionId) return;
    if (this.voiceListening) return;
    this.ensureCollaborationIdentity();

    this.collaborationService
      .heartbeat(this.policyId, this.collaborationSessionId, this.loadedRevision)
      .subscribe({
        next: (state) => {
          this.applyCollaborationState(state, true);
          this.cdr.detectChanges();
        },
        error: () => undefined,
      });
  }

  private applyCollaborationState(state: WorkflowCollaborationState, fromPoll: boolean): void {
    const normalized = this.normalizeCollaborationState(state);
    this.collaborationState = normalized;
    this.updateCollaborationConflictMessage();

    const currentUser = this.auth.getCurrentUser()?.username?.trim().toLowerCase() ?? '';
    const modifiedBySelf =
      !!normalized.lastModifiedByUsername &&
      !!currentUser &&
      normalized.lastModifiedByUsername.trim().toLowerCase() === currentUser;

    if (!fromPoll || !normalized.staleForClient || modifiedBySelf) {
      if (!this.collaborationConflict) {
        this.loadedRevision = normalized.revision;
      }
    }

    if (normalized.staleForClient && !modifiedBySelf && !this.collaborationConflict) {
      this.collaborationConflict = true;
      this.error = this.collaborationConflictMessage;
      if (this.editMode) {
        this.editMode = false;
      }
      this.reportCollaborationConflictOnce();
    }
  }

  private reportCollaborationConflictOnce(): void {
    if (this.conflictReported || !this.policyId) return;
    this.conflictReported = true;
    this.collaborationService
      .reportConflict(this.policyId, this.collaborationSessionId, this.loadedRevision)
      .subscribe({ error: () => undefined });
  }

  private syncRevisionAfterOwnSave(): void {
    if (!this.policyId || !this.collaborationSessionId) return;
    this.ensureCollaborationIdentity();
    this.logCollaborationActor('refrescando colaboración tras guardar', {
      baseRevision: this.loadedRevision,
    });
    this.collaborationService
      .heartbeat(this.policyId, this.collaborationSessionId, this.loadedRevision)
      .subscribe({
        next: (state) => {
          const normalized = this.normalizeCollaborationState(state);
          this.loadedRevision = normalized.revision;
          this.collaborationState = normalized;
          const latest = normalized.recentActions?.[0];
          if (latest) {
            console.log('[CU16] evento recibido backend', {
              modifiedBy: latest.modifiedByDisplayName ?? latest.modifiedByUsername,
              summary: latest.summary,
            });
          }
          this.cdr.detectChanges();
        },
        error: () => undefined,
      });
  }

  private normalizeCollaborationState(
    state: WorkflowCollaborationState,
  ): WorkflowCollaborationState {
    const currentUsername =
      this.auth.getCurrentUser()?.username?.trim().toLowerCase() ?? '';
    const connectedUsers =
      state.connectedUsers?.length
        ? state.connectedUsers.map((user) => ({
            ...user,
            currentUser:
              !!currentUsername &&
              user.username?.trim().toLowerCase() === currentUsername,
          }))
        : (state.connectedEditors ?? []).map((editor) => ({
            userId: editor.sessionId,
            username: editor.username,
            fullName: editor.displayName || editor.username,
            currentUser:
              !!currentUsername &&
              editor.username?.trim().toLowerCase() === currentUsername,
          }));
    const activeEdits = (state.activeEdits ?? []).map((edit) => ({
      ...edit,
      currentUser:
        !!currentUsername &&
        edit.username?.trim().toLowerCase() === currentUsername,
    }));
    return {
      ...state,
      revision: state.currentRevision ?? state.revision,
      connectedUsers,
      activeEdits,
    };
  }

  private resetCollaborationSession(): void {
    this.stopCollaborationPolling();
    const policyId = this.policyId;
    const sessionId = this.collaborationSessionId;
    if (this.collaborationOwnerUsername && policyId) {
      sessionStorage.removeItem(
        `workflow-collaboration-session:${policyId}:${this.collaborationOwnerUsername}`,
      );
    }
    if (policyId && sessionId) {
      this.collaborationService.close(policyId, sessionId).subscribe({
        error: () => undefined,
      });
    }
    this.collaborationStarted = false;
    this.collaborationConflict = false;
    this.conflictReported = false;
    this.collaborationConflictMessage = COLLABORATION_CONFLICT_FALLBACK;
    this.collaborationState = null;
    this.collaborationSessionId = '';
    this.trackedEditingElementId = null;
    this.loadedRevision = 0;
    this.collaborationPolling = false;
  }

  private resolveMutationError(
    err: HttpErrorResponse,
    fallback: string,
  ): string {
    if (err.status === 403)
      return 'No tienes permiso para modificar el diseño.';

    if (err.status === 404) return 'El recurso solicitado no existe.';

    const rawError = err.error;
    if (typeof rawError === 'string') {
      if (rawError.includes('<!doctype html') || rawError.includes('HTTP Status')) {
        return err.status === 500
          ? 'No se pudo crear la conexión. Verifique que las actividades seleccionadas sean válidas.'
          : fallback;
      }
      return rawError;
    }

    const apiMessage = rawError?.message;
    const apiDetails = rawError?.details;

    if (apiMessage && apiDetails && apiMessage !== apiDetails) {
      return apiDetails;
    }

    if (apiMessage) return apiMessage;

    if (err.status === 500) {
      return 'No se pudo crear la conexión. Verifique que las actividades seleccionadas sean válidas.';
    }

    if (err.status === 400) return fallback;

    return fallback;
  }

  private flashMessage(): void {
    setTimeout(() => {
      if (
        this.message.startsWith('Actividad') ||
        this.message.startsWith('Conexión') ||
        this.message.startsWith('Se limpiaron') ||
        this.message.includes('duplicadas limpiadas') ||
        this.message.includes('reactivada') ||
        this.message.startsWith('Flujo') ||
        this.message.startsWith('Plantilla') ||
        this.message.startsWith('Actividades reasignadas') ||
        this.message.startsWith('Carril quitado') ||
        this.message.startsWith('Carril') ||
        this.message.startsWith('Posición') ||
        this.message.startsWith('Cambios visuales') ||
        this.message.includes('correctamente configurado') ||
        this.message.includes('restablecidas')
      ) {
        this.message = '';
      }
    }, 6000);
  }

  onNodePointerDown(event: MouseEvent, node: ActivityNode): void {
    if (!this.editMode || !this.canEdit || !node.id) return;
    event.preventDefault();
    event.stopPropagation();
    this.selectNode(node);
    this.reportActiveEditing('ACTIVITY', node.id, node.name ?? 'Actividad', 'MOVING');
    const pt = this.canvasPointFromEvent(event);
    this.dragging = true;
    this.dragState = {
      nodeId: node.id,
      offsetX: pt.x - this.nodeLeft(node),
      offsetY: pt.y - this.nodeTop(node),
    };
  }

  @HostListener('document:mousemove', ['$event'])
  onDocumentMouseMove(event: MouseEvent): void {
    if (!this.dragging || !this.dragState || !this.data) return;
    const node = this.nodeById(this.dragState.nodeId);
    if (!node) return;

    const pt = this.canvasPointFromEvent(event);
    const size = this.nodeSize(node);
    const offset = this.nodeOffsetInSlot(node);

    let left = pt.x - this.dragState.offsetX;
    let top = pt.y - this.dragState.offsetY;

    left = Math.max(
      CANVAS_PADDING,
      Math.min(left, this.canvasWidth - size.width - CANVAS_PADDING),
    );
    top = Math.max(
      CANVAS_PADDING,
      Math.min(top, this.canvasHeight - size.height - CANVAS_PADDING),
    );

    node.x = left - offset.x;
    node.y = top - offset.y;
    this.updateCanvasSize();
  }

  @HostListener('document:mouseup')
  onDocumentMouseUp(): void {
    if (!this.dragging || !this.dragState) return;
    const nodeId = this.dragState.nodeId;
    const node = this.nodeById(nodeId);
    this.dragging = false;
    this.dragState = null;

    if (node?.id != null && node.x != null && node.y != null) {
      this.pendingVisualChanges.set(node.id, { x: node.x, y: node.y });
      this.syncPropertiesPanel();
      this.saveSingleNodePosition(node);
      this.reportActiveEditing('ACTIVITY', node.id, node.name ?? 'Actividad', 'SELECTING');
    }
  }

  private canvasPointFromEvent(event: MouseEvent): Point {
    const canvas = this.diagramViewport?.nativeElement.querySelector(
      '.diagram-canvas',
    ) as HTMLElement | null;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    const scale = this.zoomScale || 1;
    return {
      x: (event.clientX - rect.left) / scale,
      y: (event.clientY - rect.top) / scale,
    };
  }

  private saveSingleNodePosition(node: ActivityNode): void {
    if (!node.id || node.x == null || node.y == null) return;
    this.savingVisual = true;
    this.activityService.updatePosition(node.id, Math.round(node.x), Math.round(node.y)).subscribe({
      next: () => {
        this.savingVisual = false;
        this.pendingVisualChanges.delete(node.id!);
        this.message = 'Posición guardada correctamente.';
        this.syncRevisionAfterOwnSave();
        this.flashMessage();
      },
      error: (err: HttpErrorResponse) => {
        this.savingVisual = false;
        this.error = this.resolveMutationError(err, 'No se pudo guardar la posición.');
      },
    });
  }

  saveVisualChanges(): void {
    if (!this.canEdit || !this.editMode || !this.pendingVisualChanges.size) return;
    this.ensureCollaborationIdentity();
    this.logCollaborationActor('guardar cambios visuales', {
      count: this.pendingVisualChanges.size,
    });
    this.savingVisual = true;
    const entries = Array.from(this.pendingVisualChanges.entries());
    let chain$ = of(null as WorkflowActivity | null);
    for (const [id, pos] of entries) {
      chain$ = chain$.pipe(
        concatMap(() => this.activityService.updatePosition(id, pos.x, pos.y)),
      );
    }
    chain$.subscribe({
      next: () => {
        this.savingVisual = false;
        this.pendingVisualChanges.clear();
        this.message = 'Cambios visuales guardados.';
        this.syncRevisionAfterOwnSave();
        this.flashMessage();
      },
      error: (err: HttpErrorResponse) => {
        this.savingVisual = false;
        this.error = this.resolveMutationError(err, 'No se pudieron guardar los cambios visuales.');
      },
    });
  }

  autoArrangeNodes(): void {
    if (!this.canEdit || !this.editMode || !this.data?.activities?.length) return;
    this.applyAutoLayoutToNodes();
    this.updateCanvasSize();
    for (const node of this.data.activities) {
      if (node.id && node.x != null && node.y != null) {
        this.pendingVisualChanges.set(node.id, { x: node.x, y: node.y });
      }
    }
    this.message =
      'Diagrama autoacomodado. Use «Guardar cambios visuales» para persistir las posiciones.';
    this.flashMessage();
  }

  resetPositions(): void {
    if (!this.canEdit || !this.editMode || !this.data?.activities?.length) return;
    if (!confirm('¿Desea restablecer las posiciones del diagrama?')) return;

    const ids = this.data.activities.map((n) => n.id).filter((id): id is string => !!id);
    if (!ids.length) return;

    this.savingVisual = true;
    let chain$ = of(null as WorkflowActivity | null);
    for (const id of ids) {
      chain$ = chain$.pipe(concatMap(() => this.activityService.clearPosition(id)));
    }
    chain$.subscribe({
      next: () => {
        this.savingVisual = false;
        this.pendingVisualChanges.clear();
        this.message = 'Posiciones restablecidas.';
        this.loadDesigner(false);
        this.flashMessage();
      },
      error: (err: HttpErrorResponse) => {
        this.savingVisual = false;
        this.error = this.resolveMutationError(err, 'No se pudieron restablecer las posiciones.');
      },
    });
  }

  applyManualPositionFromPanel(): void {
    if (!this.selectedNodeId || this.propertiesPositionX == null || this.propertiesPositionY == null) {
      this.propertiesSaving = false;
      this.message = 'Actividad actualizada correctamente.';
      this.loadDesigner(false);
      this.loadCrudActivities();
      this.flashMessage();
      return;
    }

    const node = this.nodeById(this.selectedNodeId);
    if (node) {
      node.x = this.propertiesPositionX;
      node.y = this.propertiesPositionY;
      this.updateCanvasSize();
    }

    this.activityService
      .updatePosition(
        this.selectedNodeId,
        Math.round(this.propertiesPositionX),
        Math.round(this.propertiesPositionY),
      )
      .subscribe({
        next: () => {
          this.propertiesSaving = false;
          this.message = 'Actividad y posición actualizadas correctamente.';
          this.loadCrudActivities();
          this.flashMessage();
        },
        error: (err: HttpErrorResponse) => {
          this.propertiesSaving = false;
          this.error = this.resolveMutationError(err, 'No se pudo guardar la posición.');
        },
      });
  }

  onPropertiesPositionChange(): void {
    if (!this.canEditProperties() || !this.selectedNodeId) return;
    const node = this.nodeById(this.selectedNodeId);
    if (!node || this.propertiesPositionX == null || this.propertiesPositionY == null) return;
    node.x = this.propertiesPositionX;
    node.y = this.propertiesPositionY;
    if (node.id) {
      this.pendingVisualChanges.set(node.id, { x: node.x, y: node.y });
    }
    this.updateCanvasSize();
  }

  private laneIndexForNode(node: ActivityNode): number {
    const laneName = (node.responsibleName ?? '').trim();
    const idx = this.displayLanes.findIndex((l) => l.laneName.trim() === laneName);
    return idx >= 0 ? idx : 0;
  }

  private applyAutoLayoutToNodes(): void {
    if (!this.data?.activities?.length) return;
    for (const node of this.data.activities) {
      const laneIndex = this.laneIndexForNode(node);
      const orderIndex = Math.max(1, node.orderIndex ?? 1);
      node.x = LANE_LABEL_WIDTH + (orderIndex - 1) * AUTO_LAYOUT_H_GAP;
      node.y = laneIndex * LANE_HEIGHT + NODE_TOP_PADDING_SLOT;
    }
  }

  private rebuildEdgeMaps(): void {
    this.outgoingEdgeIndex.clear();
    this.incomingEdgeIndex.clear();
    this.outgoingEdgeTotal.clear();
    this.incomingEdgeTotal.clear();

    const activeEdges = this.canvasActiveTransitions;
    if (!activeEdges.length) return;

    const byFrom = new Map<string, TransitionEdge[]>();
    const byTo = new Map<string, TransitionEdge[]>();

    for (const edge of activeEdges) {
      const fromId = edge.fromActivityId!;
      const toId = edge.toActivityId!;
      if (!byFrom.has(fromId)) byFrom.set(fromId, []);
      if (!byTo.has(toId)) byTo.set(toId, []);
      byFrom.get(fromId)!.push(edge);
      byTo.get(toId)!.push(edge);
    }

    for (const [nodeId, edges] of byFrom.entries()) {
      edges.sort((a, b) => (a.id ?? '').localeCompare(b.id ?? ''));
      this.outgoingEdgeTotal.set(nodeId, edges.length);
      edges.forEach((edge, index) => {
        if (edge.id) this.outgoingEdgeIndex.set(edge.id, index);
      });
    }

    for (const [nodeId, edges] of byTo.entries()) {
      edges.sort((a, b) => (a.id ?? '').localeCompare(b.id ?? ''));
      this.incomingEdgeTotal.set(nodeId, edges.length);
      edges.forEach((edge, index) => {
        if (edge.id) this.incomingEdgeIndex.set(edge.id, index);
      });
    }
  }

  get canvasActiveTransitions(): TransitionEdge[] {
    if (this.allTransitions.length) {
      return this.allTransitions
        .filter(
          (transition) =>
            isVisibleTransition(transition) &&
            transition.fromActivityId &&
            transition.toActivityId,
        )
        .map((transition) => this.toTransitionEdge(transition));
    }

    return (this.data?.transitions ?? []).filter(
      (edge) =>
        isVisibleTransition(edge) && edge.fromActivityId && edge.toActivityId,
    );
  }

  private syncDiagramViewAfterMutation(): void {
    this.rebuildEdgeMaps();
    this.enrichNodes();
    this.updateCanvasSize();
    this.pruneSelectionsAfterReload();
    this.cdr.detectChanges();
  }

  private patchLocalActivityRemoved(activityId: string): void {
    if (this.data) {
      this.data = {
        ...this.data,
        activities: (this.data.activities ?? []).filter((node) => node.id !== activityId),
        transitions: (this.data.transitions ?? []).filter(
          (edge) =>
            edge.fromActivityId !== activityId && edge.toActivityId !== activityId,
        ),
        lanes: (this.data.lanes ?? []).map((lane) => ({
          ...lane,
          activities: (lane.activities ?? []).filter((node) => node.id !== activityId),
        })),
      };
    }

    this.crudActivities = this.crudActivities.filter((activity) => activity.id !== activityId);
    this.allTransitions = this.allTransitions.filter(
      (transition) =>
        transition.fromActivityId !== activityId && transition.toActivityId !== activityId,
    );

    if (this.selectedNodeId === activityId) {
      this.selectedNodeId = null;
    }

    this.syncDiagramViewAfterMutation();
  }

  private patchLocalTransitionRemoved(transitionId: string): void {
    this.allTransitions = this.allTransitions.filter(
      (transition) => transition.id !== transitionId,
    );

    if (this.data?.transitions) {
      this.data = {
        ...this.data,
        transitions: this.data.transitions.filter((edge) => edge.id !== transitionId),
      };
    }

    if (this.selectedEdgeId === transitionId) {
      this.selectedEdgeId = null;
    }

    this.syncDiagramViewAfterMutation();
  }

  private patchLocalTransitionActiveState(transitionId: string, active: boolean): void {
    this.allTransitions = this.allTransitions.map((transition) =>
      transition.id === transitionId ? { ...transition, active } : transition,
    );

    if (this.data?.transitions) {
      this.data = {
        ...this.data,
        transitions: this.data.transitions.map((edge) =>
          edge.id === transitionId ? { ...edge, active } : edge,
        ),
      };
    }

    this.syncDiagramViewAfterMutation();
  }

  get hasDuplicateActivityNames(): boolean {
    const counts = new Map<string, number>();
    for (const activity of this.data?.activities ?? []) {
      const key = this.normalizeTransitionName(activity.name);
      if (!key) continue;
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }
    return [...counts.values()].some((count) => count > 1);
  }

  canDrawTransition(edge: Pick<TransitionEdge, 'fromActivityId' | 'toActivityId'>): boolean {
    return !!this.nodeById(edge.fromActivityId) && !!this.nodeById(edge.toActivityId);
  }

  getTransitionDrawWarning(transition: WorkflowTransition): string | null {
    if (transition.active === false) return null;
    if (!transition.fromActivityId || !transition.toActivityId) {
      return 'Esta conexión está activa, pero no se puede dibujar porque la actividad origen o destino no coincide con los nodos del diagrama.';
    }
    if (!this.canDrawTransition(transition)) {
      return 'Esta conexión está activa, pero no se puede dibujar porque la actividad origen o destino no coincide con los nodos del diagrama.';
    }
    return null;
  }

  private toTransitionEdge(transition: WorkflowTransition): TransitionEdge {
    return {
      id: transition.id,
      fromActivityId: transition.fromActivityId,
      fromActivityName: transition.fromActivityName,
      toActivityId: transition.toActivityId,
      toActivityName: transition.toActivityName,
      transitionType: transition.transitionType,
      transitionTypeLabel: transition.transitionTypeLabel,
      conditionLabel: transition.conditionLabel,
      active: transition.active,
    };
  }

  private logEdgeDrawDiagnostics(): void {
    console.log(
      '[diagram] actividades cargadas:',
      (this.data?.activities ?? []).map((activity) => ({
        id: activity.id,
        name: activity.name,
      })),
    );
    console.log(
      '[diagram] transiciones activas:',
      this.canvasActiveTransitions.map((edge) => ({
        id: edge.id,
        fromActivityId: edge.fromActivityId,
        toActivityId: edge.toActivityId,
        conditionLabel: edge.conditionLabel,
      })),
    );
    for (const edge of this.canvasActiveTransitions) {
      if (!this.nodeById(edge.fromActivityId)) {
        console.warn('No se encontró nodo origen para transición', edge);
      }
      if (!this.nodeById(edge.toActivityId)) {
        console.warn('No se encontró nodo destino para transición', edge);
      }
    }
  }

  visualType(node: ActivityNode): UmlVisualType {
    const type = (node.activityType ?? 'TASK').toUpperCase();

    if (type === 'START') return 'START';

    if (type === 'END') return 'END';

    if (type === 'FORK') return 'FORK';

    if (type === 'JOIN') return 'JOIN';

    if (type === 'DECISION' || node.decisionNode) return 'DECISION';

    return 'TASK';
  }

  nodeClass(node: ActivityNode): string {
    const classes = [`uml-${this.visualType(node).toLowerCase()}`];

    if (this.isNodeSelected(node)) classes.push('selected');

    if (node.id && this.isElementLockedByOther('ACTIVITY', node.id)) classes.push('peer-editing');

    if (this.dragging && node.id === this.dragState?.nodeId) classes.push('dragging');

    return classes.join(' ');
  }

  nodeById(id?: string): ActivityNode | undefined {
    return this.data?.activities.find((a) => a.id === id);
  }

  nodeSize(node: ActivityNode): NodeSize {
    const type = this.visualType(node);

    switch (type) {
      case 'START':

      case 'END':
        return { width: 88, height: 100 };

      case 'DECISION':
        return { width: 110, height: 110 };

      case 'FORK':
      case 'JOIN':
        return { width: 96, height: 44 };

      default:
        return { width: 200, height: 88 };
    }
  }

  nodeOffsetInSlot(node: ActivityNode): Point {
    const slotW = NODE_SLOT_WIDTH;

    const slotH = LANE_HEIGHT - NODE_TOP_PADDING_SLOT;

    const size = this.nodeSize(node);

    return {
      x: Math.max(0, (slotW - size.width) / 2),

      y: Math.max(0, (slotH - size.height) / 2),
    };
  }

  nodeLeft(node: ActivityNode): number {
    return (node.x ?? 0) + this.nodeOffsetInSlot(node).x;
  }

  nodeTop(node: ActivityNode): number {
    return (node.y ?? 0) + this.nodeOffsetInSlot(node).y;
  }

  getAnchor(
    node: ActivityNode,
    side: 'out' | 'in',
    edge?: TransitionEdge,
  ): Point {
    const left = this.nodeLeft(node);

    const top = this.nodeTop(node);

    const size = this.nodeSize(node);

    const cx = left + size.width / 2;

    const cy = top + size.height / 2;

    const type = this.visualType(node);

    let spreadOffset = 0;
    if (edge?.id) {
      const total =
        side === 'out'
          ? this.outgoingEdgeTotal.get(node.id ?? '') ?? 1
          : this.incomingEdgeTotal.get(node.id ?? '') ?? 1;
      const index =
        side === 'out'
          ? this.outgoingEdgeIndex.get(edge.id) ?? 0
          : this.incomingEdgeIndex.get(edge.id) ?? 0;
      if (total > 1) {
        spreadOffset = (index - (total - 1) / 2) * 12;
      }
    }

    if (type === 'DECISION') {
      if (side === 'out') {
        return { x: left + size.width, y: cy + spreadOffset };
      }

      return { x: left, y: cy + spreadOffset };
    }

    if (type === 'START') {
      return side === 'out'
        ? { x: left + size.width / 2 + 18, y: cy + spreadOffset }
        : { x: left + size.width / 2 - 18, y: cy + spreadOffset };
    }

    if (type === 'END') {
      return side === 'out'
        ? { x: left + size.width / 2 + 22, y: cy + spreadOffset }
        : { x: left + size.width / 2 - 22, y: cy + spreadOffset };
    }

    return side === 'out'
      ? { x: left + size.width, y: cy + spreadOffset }
      : { x: left, y: cy + spreadOffset };
  }

  getEdgePath(edge: TransitionEdge): string {
    const from = this.nodeById(edge.fromActivityId);

    const to = this.nodeById(edge.toActivityId);

    if (!from || !to) return '';

    const s = this.getAnchor(from, 'out', edge);

    const t = this.getAnchor(to, 'in', edge);

    const dx = t.x - s.x;
    const dy = t.y - s.y;

    if (dx >= 20) {
      const bend = Math.min(140, Math.max(50, Math.abs(dx) * 0.4));
      const c1x = s.x + bend;
      const c2x = t.x - bend;
      const lift = dy !== 0 ? dy * 0.15 : 0;
      return `M ${s.x} ${s.y} C ${c1x} ${s.y + lift}, ${c2x} ${t.y - lift}, ${t.x} ${t.y}`;
    }

    const loopOffset = Math.max(40, Math.abs(dy) * 0.35 + 30);
    const midX = (s.x + t.x) / 2;
    const c1y = s.y - loopOffset;
    const c2y = t.y - loopOffset;
    return `M ${s.x} ${s.y} C ${midX} ${c1y}, ${midX} ${c2y}, ${t.x} ${t.y}`;
  }

  getEdgeLabelPosition(edge: TransitionEdge): Point {
    const from = this.nodeById(edge.fromActivityId);

    const to = this.nodeById(edge.toActivityId);

    if (!from || !to) return { x: 0, y: 0 };

    const s = this.getAnchor(from, 'out', edge);

    const t = this.getAnchor(to, 'in', edge);

    const dx = t.x - s.x;

    if (dx >= 20) {
      const labelY = (s.y + t.y) / 2 - 12;
      return { x: (s.x + t.x) / 2, y: labelY };
    }

    const loopOffset = Math.max(40, Math.abs(t.y - s.y) * 0.35 + 30);
    return { x: (s.x + t.x) / 2, y: Math.min(s.y, t.y) - loopOffset - 6 };
  }

  edgeLabel(edge: TransitionEdge): string {
    return edgeCanvasLabel(
      edge.transitionType,
      edge.conditionLabel,
      edge.transitionTypeLabel,
    );
  }

  edgeLabelWidth(label: string): number {
    return Math.max(48, label.length * 7 + 16);
  }

  updateCanvasSize(): void {
    const laneCount = Math.max(1, this.displayLanes.length);

    if (!this.data?.activities?.length) {
      this.canvasWidth = 1400;

      this.canvasHeight = Math.max(
        480,
        laneCount * LANE_HEIGHT + CANVAS_PADDING * 2,
      );

      return;
    }

    let maxX = LANE_LABEL_WIDTH + 400;

    let maxY = laneCount * LANE_HEIGHT + CANVAS_PADDING;

    for (const node of this.data.activities) {
      if (node.x == null || node.y == null) continue;

      const size = this.nodeSize(node);

      const offset = this.nodeOffsetInSlot(node);

      maxX = Math.max(maxX, node.x + NODE_SLOT_WIDTH + offset.x + CANVAS_PADDING);

      maxY = Math.max(maxY, node.y + size.height + offset.y + CANVAS_PADDING);
    }

    this.canvasWidth = Math.max(1400, maxX);

    this.canvasHeight = Math.max(
      laneCount * LANE_HEIGHT + CANVAS_PADDING * 2,
      maxY,
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

  trackTransition(_index: number, t: WorkflowTransition): string {
    return t.id ?? `${t.fromActivityName}-${t.toActivityName}`;
  }

  // ——— IA diseño workflow (F6) ———

  useAiExamplePrompt(): void {
    this.aiPrompt = this.aiExamplePrompt;
  }

  generateAiSuggestion(): void {
    if (!this.canEdit) {
      this.aiError = 'No tiene permiso para diseñar con IA.';
      return;
    }
    if (!this.policyId || !this.aiPrompt.trim()) {
      this.aiError = 'Escriba o dicte un prompt antes de generar la sugerencia.';
      return;
    }
    this.ensureEditModeForGuided();
    this.aiLoading = true;
    this.aiError = '';
    this.aiSuggestion = null;

    this.aiService.suggestWorkflow(this.buildAiSuggestRequest()).subscribe({
      next: (response) => {
        this.aiLoading = false;
        this.aiSuggestion = response;
        if (response.fallbackUsed) {
          this.message =
            'Sugerencia generada con parser local (IA externa no disponible). Revise antes de aplicar.';
          this.flashMessage();
        }
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.aiLoading = false;
        if (err.status === 403) {
          this.aiError = 'No tiene permiso para usar IA en el diseñador.';
        } else if (err.status === 400) {
          this.aiError = 'El prompt es obligatorio.';
        } else {
          this.aiError =
            'No se pudo obtener la sugerencia. El diseñador sigue operativo; intente de nuevo o reformule el prompt.';
        }
        this.cdr.detectChanges();
      },
    });
  }

  private buildAiSuggestRequest(): AiWorkflowSuggestRequest {
    const activities = this.activityOptionsForSelect().map((a) => ({
      id: a.id,
      name: a.name,
      activityType: a.activityType,
      responsibleName: a.responsibleName,
      responsibleType: a.responsibleType,
      orderIndex: a.orderIndex,
    }));
    const transitions = this.connectionsPanelRows.map((t) => ({
      id: t.id,
      fromActivityId: t.fromActivityId,
      fromActivityName: t.fromActivityName,
      toActivityId: t.toActivityId,
      toActivityName: t.toActivityName,
      transitionType: t.transitionType,
      conditionLabel: t.conditionLabel,
    }));
    const lanes = this.displayLaneNames.map((name) => ({
      name,
      activityCount: this.activitiesInLane(name),
    }));
    return {
      policyId: this.policyId!,
      prompt: this.aiPrompt.trim(),
      activities,
      transitions,
      lanes,
    };
  }

  get canApplyAiSuggestion(): boolean {
    if (!this.aiSuggestion || !this.canEdit) return false;
    if (this.aiSuggestion.intent === 'VALIDATE_DIAGRAM') return true;
    const acts = this.aiSuggestion.suggestedActivities?.length ?? 0;
    const trans = this.aiSuggestion.suggestedTransitions?.length ?? 0;
    return acts > 0 || trans > 0;
  }

  applyAiSuggestion(): void {
    if (!this.ensureCanEdit() || !this.aiSuggestion || !this.policyId) return;

    if (this.aiSuggestion.intent === 'VALIDATE_DIAGRAM') {
      this.validateFlow();
      return;
    }

    const activities = (this.aiSuggestion.suggestedActivities ?? []).filter(
      (a) => (a.operation ?? 'CREATE').toUpperCase() === 'CREATE' && a.name?.trim(),
    );
    const transitions = (this.aiSuggestion.suggestedTransitions ?? []).filter(
      (t) =>
        (t.operation ?? 'CREATE').toUpperCase() === 'CREATE' &&
        t.fromActivityName?.trim() &&
        t.toActivityName?.trim(),
    );

    if (!activities.length && !transitions.length) {
      this.aiError = 'La sugerencia no incluye actividades ni conexiones para aplicar.';
      return;
    }

    if (
      !confirm(
        '¿Aplicar la sugerencia de IA al diagrama? Los cambios se guardarán en actividades y conexiones del workflow.',
      )
    ) {
      return;
    }

    this.aiApplying = true;
    this.aiError = '';
    this.ensureEditModeForGuided();

    const nameToId = this.buildActivityNameIndex();
    const maxOrder = this.maxActivityOrderIndex();

    let chain$ = of(null as WorkflowActivity | null);
    let order = maxOrder;

    for (const item of activities) {
      const norm = this.normalizeActivityName(item.name);
      if (nameToId.has(norm)) continue;
      order += 1;
      const responsible = item.responsibleName?.trim();
      if (responsible) this.registerLaneName(responsible);
      const payload: WorkflowActivityRequest = {
        policyId: this.policyId,
        name: item.name.trim(),
        responsibleType: item.responsibleType ?? (responsible ? 'DEPARTMENT' : 'ROLE'),
        responsibleName: responsible || undefined,
        activityType: (item.activityType ?? 'TASK').toUpperCase(),
        status: 'ACTIVA',
        orderIndex: item.orderIndex ?? order,
        estimatedTimeHours: 1,
      };
      chain$ = chain$.pipe(
        concatMap(() =>
          this.activityService.create(payload).pipe(
            map((created) => {
              if (created.id) {
                nameToId.set(this.normalizeActivityName(created.name), created.id);
              }
              return created;
            }),
          ),
        ),
      );
    }

    chain$
      .pipe(
        switchMap(() => {
          const requests = transitions
            .map((edge, index) => {
              const fromId = this.resolveActivityIdByName(edge.fromActivityName, nameToId);
              const toId = this.resolveActivityIdByName(edge.toActivityName, nameToId);
              if (!fromId || !toId || fromId === toId) return null;
              const type = (edge.transitionType ?? 'SEQUENTIAL').toUpperCase();
              return this.transitionService.create({
                policyId: this.policyId!,
                fromActivityId: fromId,
                toActivityId: toId,
                transitionType: type,
                conditionLabel: edge.conditionLabel?.trim() || undefined,
                orderIndex: index + 1,
                active: true,
              });
            })
            .filter((r): r is Observable<WorkflowTransition> => r !== null);
          return requests.length ? forkJoin(requests) : of([]);
        }),
      )
      .subscribe({
        next: () => {
          this.aiApplying = false;
          this.message = 'Sugerencia de IA aplicada. Validando flujo...';
          this.aiSuggestion = null;
          this.loadDesigner(false);
          this.syncRevisionAfterOwnSave();
          this.validateFlow();
          this.flashMessage();
        },
        error: (err: HttpErrorResponse) => {
          this.aiApplying = false;
          this.aiError = this.resolveMutationError(
            err,
            'No se pudo aplicar la sugerencia completa. Revise actividades y conexiones creadas.',
          );
          this.loadDesigner(false);
        },
      });
  }

  private buildActivityNameIndex(): Map<string, string> {
    const map = new Map<string, string>();
    for (const a of this.activityOptionsForSelect()) {
      if (a.id && a.name) {
        map.set(this.normalizeActivityName(a.name), a.id);
      }
    }
    return map;
  }

  private maxActivityOrderIndex(): number {
    return this.activityOptionsForSelect().reduce(
      (max, a) => Math.max(max, a.orderIndex ?? 0),
      0,
    );
  }

  private normalizeActivityName(name: string): string {
    return name.trim().toLowerCase().replace(/\s+/g, ' ');
  }

  private resolveActivityIdByName(name: string, index: Map<string, string>): string | undefined {
    const norm = this.normalizeActivityName(name);
    if (index.has(norm)) return index.get(norm);
    for (const [key, id] of index.entries()) {
      if (key.includes(norm) || norm.includes(key)) return id;
    }
    return undefined;
  }

  toggleVoiceDictation(): void {
    if (this.voiceListening) {
      this.stopVoiceDictation();
      return;
    }
    this.startVoiceDictation();
  }

  startVoiceDictation(): void {
    if (!this.voiceSupported) {
      this.aiError = 'El navegador no soporta dictado por voz.';
      this.voiceStatus = '';
      this.cdr.detectChanges();
      return;
    }

    this.voiceDictation?.abort();
    this.voiceDictation = new VoiceDictationController(this.buildVoiceDictationHandlers());

    this.aiError = '';
    this.voiceStatus = '';
    this.voiceDictation.start();
  }

  stopVoiceDictation(): void {
    this.voiceDictation?.stop();
    this.voiceListening = false;
    this.cdr.detectChanges();
  }

  private abortVoiceDictation(): void {
    this.voiceDictation?.abort();
    this.voiceDictation = null;
    this.voiceListening = false;
    this.voiceStatus = '';
    this.cdr.detectChanges();
  }

  private buildVoiceDictationHandlers(): VoiceDictationHandlers {
    return {
      onTranscript: (text) => {
        this.zone.run(() => {
          console.log('[CU14] onresult transcript:', text);
          this.aiPrompt = appendDictationText(this.aiPrompt, text);
          this.cdr.detectChanges();
        });
      },
      onListeningChange: (listening) => {
        this.zone.run(() => {
          this.voiceListening = listening;
          this.cdr.detectChanges();
        });
      },
      onStatus: (message) => {
        this.zone.run(() => {
          this.voiceStatus = message;
          if (message !== 'Escuchando...') {
            this.aiError = '';
          }
          this.cdr.detectChanges();
        });
      },
      onError: (message) => {
        this.zone.run(() => {
          console.log('[CU14] onerror:', message);
          this.aiError = message;
          this.voiceStatus = '';
          this.cdr.detectChanges();
        });
      },
      onDebug: (event, detail) => {
        if (event === 'start') {
          console.log('[CU14] onstart');
        } else if (event === 'end') {
          console.log('[CU14] onend');
        } else if (event === 'result') {
          console.log('[CU14] onresult raw:', detail);
        } else if (event === 'error') {
          console.log('[CU14] onerror raw:', detail);
        }
      },
    };
  }

  get voiceButtonLabel(): string {
    if (this.voiceListening) {
      return 'Detener dictado';
    }
    return 'Dictar por voz';
  }

  clearAiSuggestion(): void {
    this.aiSuggestion = null;
    this.aiError = '';
  }
}
