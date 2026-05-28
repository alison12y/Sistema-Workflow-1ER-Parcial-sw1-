import { Component, ElementRef, HostListener, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PolicyService } from '../../services/policy.service';
import { ActivityDiagramService } from '../../services/activity-diagram.service';
import {
  AiAssistantService,
  DiagramSuggestion,
  PROCESS_TYPE_OPTIONS,
} from '../../services/ai-assistant.service';
import { BusinessPolicy } from '../../models/auth.model';
import { DiagramEdge, DiagramNode } from '../../models/activity-diagram.model';

interface UmlTool {
  type: string;
  label: string;
  icon: string;
}

const LANE_HEIGHT = 120;
const DEFAULT_LANES = ['Funcionario', 'Recursos Humanos', 'Supervisor'];

@Component({
  selector: 'app-workflow-designer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './workflow-designer.component.html',
  styleUrl: './workflow-designer.component.scss',
})
export class WorkflowDesignerComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly policyService = inject(PolicyService);
  private readonly diagramService = inject(ActivityDiagramService);
  private readonly aiAssistant = inject(AiAssistantService);

  @ViewChild('canvasViewport') canvasViewportRef!: ElementRef<HTMLDivElement>;

  policyId: string | null = null;
  policy: BusinessPolicy | null = null;
  diagramId: string | null = null;
  diagramName = 'Diagrama de actividades UML 2.5';

  nodes: DiagramNode[] = [];
  edges: DiagramEdge[] = [];
  lanes = [...DEFAULT_LANES];

  canvasWidth = 1600;
  canvasHeight = 480;

  loading = true;
  saving = false;
  message = '';
  error = '';

  selectedNodeId: string | null = null;
  selectedEdgeId: string | null = null;
  connectSourceId: string | null = null;

  showAiModal = false;
  aiProcessType = PROCESS_TYPE_OPTIONS[0];
  aiGenerating = false;
  aiSuggestion: DiagramSuggestion | null = null;
  aiError = '';
  readonly processTypeOptions = PROCESS_TYPE_OPTIONS;

  newLaneName = '';
  editingLaneIndex: number | null = null;
  editingLaneName = '';

  private draggingNodeId: string | null = null;
  private dragOffsetX = 0;
  private dragOffsetY = 0;

  tools: UmlTool[] = [
    { type: 'INITIAL', label: 'Nodo inicial', icon: 'trip_origin' },
    { type: 'ACTION', label: 'Actividad / Acción', icon: 'crop_16_9' },
    { type: 'DECISION', label: 'Decisión', icon: 'change_history' },
    { type: 'MERGE', label: 'Merge / Unión', icon: 'merge_type' },
    { type: 'FORK_JOIN', label: 'Fork / Join', icon: 'density_small' },
    { type: 'FINAL', label: 'Nodo final', icon: 'stop_circle' },
  ];

  ngOnInit(): void {
    this.policyId = this.route.snapshot.paramMap.get('id');
    if (!this.policyId) {
      this.loading = false;
      this.error = 'No se encontró la política asociada';
      return;
    }

    this.policyService.getById(this.policyId).subscribe({
      next: (p) => {
        this.policy = p;
        this.diagramName = `Diagrama - ${p.name}`;
      },
      error: () => (this.error = 'No se pudo cargar la política'),
    });

    this.loadDiagram();
  }

  loadDiagram(): void {
    if (!this.policyId) return;

    this.diagramService.getByPolicy(this.policyId).subscribe({
      next: (diagram) => {
        this.diagramId = diagram.id ?? null;
        if (diagram.nodes?.length) {
          this.nodes = diagram.nodes.map((n) => ({ ...n }));
          this.edges = (diagram.edges ?? []).map((e) => ({ ...e }));
          this.lanes = this.resolveLanes(diagram.lanes, this.nodes);
          if (diagram.name) {
            this.diagramName = diagram.name;
          }
        } else {
          this.loadExampleTemplate();
        }
        this.updateCanvasSize();
        this.loading = false;
        setTimeout(() => this.centerDiagram(), 0);
      },
      error: () => {
        this.loadExampleTemplate();
        this.updateCanvasSize();
        this.loading = false;
        setTimeout(() => this.centerDiagram(), 0);
      },
    });
  }

  resolveLanes(savedLanes: string[] | undefined, nodes: DiagramNode[]): string[] {
    if (savedLanes?.length) {
      return [...savedLanes];
    }
    const fromNodes = nodes
      .map((n) => n.lane?.trim())
      .filter((lane): lane is string => !!lane);
    const unique = [...new Set(fromNodes)];
    return unique.length ? unique : [...DEFAULT_LANES];
  }

  loadExampleTemplate(): void {
    this.lanes = [...DEFAULT_LANES];
    this.nodes = [
      { id: 'n1', type: 'INITIAL', label: 'Inicio', x: 40, y: 210, lane: 'Funcionario' },
      { id: 'n2', type: 'ACTION', label: 'Registrar solicitud', x: 130, y: 190, lane: 'Funcionario' },
      { id: 'n3', type: 'ACTION', label: 'Revisar solicitud', x: 320, y: 190, lane: 'Recursos Humanos' },
      { id: 'n4', type: 'ACTION', label: 'Validar información', x: 510, y: 190, lane: 'Recursos Humanos' },
      { id: 'n5', type: 'DECISION', label: '¿Aprueba?', x: 700, y: 185, lane: 'Supervisor' },
      { id: 'n6', type: 'ACTION', label: 'Aprobar permiso', x: 880, y: 100, lane: 'Supervisor' },
      { id: 'n7', type: 'ACTION', label: 'Rechazar permiso', x: 880, y: 280, lane: 'Supervisor' },
      { id: 'n8', type: 'MERGE', label: 'Unión', x: 1060, y: 185, lane: 'Funcionario' },
      { id: 'n9', type: 'ACTION', label: 'Notificar resultado', x: 1180, y: 190, lane: 'Funcionario' },
      { id: 'n10', type: 'FINAL', label: 'Fin', x: 1350, y: 210, lane: 'Funcionario' },
    ];
    this.edges = [
      { id: 'e1', sourceId: 'n1', targetId: 'n2', label: '' },
      { id: 'e2', sourceId: 'n2', targetId: 'n3', label: '' },
      { id: 'e3', sourceId: 'n3', targetId: 'n4', label: '' },
      { id: 'e4', sourceId: 'n4', targetId: 'n5', label: '' },
      { id: 'e5', sourceId: 'n5', targetId: 'n6', label: '[Sí]' },
      { id: 'e6', sourceId: 'n5', targetId: 'n7', label: '[No]' },
      { id: 'e7', sourceId: 'n6', targetId: 'n8', label: '' },
      { id: 'e8', sourceId: 'n7', targetId: 'n8', label: '' },
      { id: 'e9', sourceId: 'n8', targetId: 'n9', label: '' },
      { id: 'e10', sourceId: 'n9', targetId: 'n10', label: '' },
    ];
    this.diagramName = 'Política de aprobación de solicitudes';
    this.updateCanvasSize();
  }

  openAiModal(): void {
    this.showAiModal = true;
    this.aiSuggestion = null;
    this.aiError = '';
    this.aiProcessType = PROCESS_TYPE_OPTIONS[0];
  }

  closeAiModal(): void {
    this.showAiModal = false;
    this.aiSuggestion = null;
    this.aiError = '';
    this.aiGenerating = false;
  }

  generateAiSuggestion(): void {
    this.aiGenerating = true;
    this.aiError = '';
    this.aiSuggestion = null;

    const suggestion = this.aiAssistant.suggestDiagram(
      this.aiProcessType,
      this.policy?.name ?? this.diagramName
    );

    this.aiGenerating = false;
    if (!suggestion) {
      this.aiError = 'No se pudo generar la sugerencia';
      return;
    }

    this.aiSuggestion = suggestion;
    this.message = 'Sugerencia generada correctamente';
    setTimeout(() => (this.message = ''), 4000);
  }

  insertAiSuggestion(): void {
    if (!this.aiSuggestion) return;

    if (this.nodes.length > 0) {
      const confirmed = confirm('Ya existe un diagrama. ¿Desea reemplazarlo con la sugerencia de IA?');
      if (!confirmed) {
        return;
      }
    }

    const timestamp = Date.now();
    const idMap = new Map<string, string>();

    this.lanes = [...this.aiSuggestion.lanes];
    this.nodes = this.aiSuggestion.nodes.map((node) => {
      const newId = `ai-${timestamp}-${node.id}`;
      idMap.set(node.id, newId);
      return { ...node, id: newId };
    });
    this.edges = this.aiSuggestion.edges.map((edge, index) => ({
      ...edge,
      id: `ai-edge-${timestamp}-${index}`,
      sourceId: idMap.get(edge.sourceId) ?? edge.sourceId,
      targetId: idMap.get(edge.targetId) ?? edge.targetId,
    }));

    if (this.aiSuggestion.name) {
      this.diagramName = this.aiSuggestion.name;
    }

    this.selectedNodeId = null;
    this.selectedEdgeId = null;
    this.updateCanvasSize();
    this.closeAiModal();
    this.message = 'Sugerencia insertada en el diagrama';
    setTimeout(() => (this.message = ''), 5000);
    setTimeout(() => this.centerDiagram(), 0);
  }

  addLane(): void {
    const name = this.newLaneName.trim();
    if (!name) {
      this.error = 'Ingrese un nombre para el carril';
      return;
    }
    if (this.lanes.some((l) => l.toLowerCase() === name.toLowerCase())) {
      this.error = 'Ya existe un carril con ese nombre';
      return;
    }
    this.lanes.push(name);
    this.newLaneName = '';
    this.error = '';
    this.updateCanvasSize();
  }

  startEditLane(index: number): void {
    this.editingLaneIndex = index;
    this.editingLaneName = this.lanes[index];
  }

  saveEditLane(): void {
    if (this.editingLaneIndex === null) return;
    const newName = this.editingLaneName.trim();
    const oldName = this.lanes[this.editingLaneIndex];
    if (!newName) {
      this.error = 'El nombre del carril no puede estar vacío';
      return;
    }
    if (this.lanes.some((l, i) => i !== this.editingLaneIndex && l.toLowerCase() === newName.toLowerCase())) {
      this.error = 'Ya existe un carril con ese nombre';
      return;
    }
    this.lanes[this.editingLaneIndex] = newName;
    for (const node of this.nodes) {
      if (node.lane === oldName) {
        node.lane = newName;
      }
    }
    this.editingLaneIndex = null;
    this.editingLaneName = '';
    this.error = '';
  }

  cancelEditLane(): void {
    this.editingLaneIndex = null;
    this.editingLaneName = '';
  }

  removeLane(index: number): void {
    const lane = this.lanes[index];
    const assigned = this.nodes.filter((n) => n.lane === lane);
    if (assigned.length) {
      const ok = confirm(
        `El carril "${lane}" tiene ${assigned.length} nodo(s) asignado(s). ¿Eliminarlo y mover los nodos al primer carril?`
      );
      if (!ok) return;
      const fallback = this.lanes.find((_, i) => i !== index) ?? 'General';
      for (const node of assigned) {
        node.lane = fallback;
        this.snapNodeToLane(node);
      }
    } else if (!confirm(`¿Eliminar el carril "${lane}"?`)) {
      return;
    }

    this.lanes.splice(index, 1);
    if (!this.lanes.length) {
      this.lanes.push('General');
    }
    this.updateCanvasSize();
  }

  moveLaneUp(index: number): void {
    if (index <= 0) return;
    [this.lanes[index - 1], this.lanes[index]] = [this.lanes[index], this.lanes[index - 1]];
    this.repositionNodesByLane();
    this.updateCanvasSize();
  }

  moveLaneDown(index: number): void {
    if (index >= this.lanes.length - 1) return;
    [this.lanes[index + 1], this.lanes[index]] = [this.lanes[index], this.lanes[index + 1]];
    this.repositionNodesByLane();
    this.updateCanvasSize();
  }

  onNodeLaneChange(node: DiagramNode): void {
    this.snapNodeToLane(node);
    this.updateCanvasSize();
  }

  snapNodeToLane(node: DiagramNode): void {
    const laneIndex = Math.max(0, this.lanes.indexOf(node.lane ?? ''));
    const lane = this.lanes[laneIndex] ?? this.lanes[0];
    node.lane = lane;
    const size = this.getNodeSize(node);
    node.y = laneIndex * LANE_HEIGHT + Math.max(20, (LANE_HEIGHT - size.height) / 2);
  }

  repositionNodesByLane(): void {
    for (const node of this.nodes) {
      this.snapNodeToLane(node);
    }
  }

  addNode(type: string): void {
    const id = `node-${Date.now()}`;
    const lane = this.lanes[0];
    const defaults: Record<string, string> = {
      INITIAL: 'Inicio',
      FINAL: 'Fin',
      DECISION: '¿Condición?',
      MERGE: 'Unión',
      FORK_JOIN: 'Paralelismo',
      ACTION: 'Nueva actividad',
    };

    const node: DiagramNode = {
      id,
      type,
      label: defaults[type] ?? 'Nodo',
      x: 60 + (this.nodes.length * 35) % 500,
      y: 120,
      lane,
    };
    this.snapNodeToLane(node);
    this.nodes.push(node);
    this.selectedNodeId = id;
    this.error = '';
    this.updateCanvasSize();
  }

  selectNode(nodeId: string, event?: MouseEvent): void {
    event?.stopPropagation();
    if (this.connectSourceId) {
      if (this.connectSourceId !== nodeId) {
        this.addEdge(this.connectSourceId, nodeId);
      }
      this.connectSourceId = null;
      this.message = '';
      return;
    }
    this.selectedNodeId = nodeId;
    this.selectedEdgeId = null;
  }

  selectEdge(edgeId: string, event: MouseEvent): void {
    event.stopPropagation();
    this.selectedEdgeId = edgeId;
    this.selectedNodeId = null;
  }

  startConnect(nodeId: string, event: MouseEvent): void {
    event.stopPropagation();
    this.connectSourceId = nodeId;
    this.message = 'Seleccione el nodo destino para crear la conexión';
  }

  addEdge(sourceId: string, targetId: string): void {
    const exists = this.edges.some((e) => e.sourceId === sourceId && e.targetId === targetId);
    if (exists) {
      this.error = 'Ya existe una conexión entre esos nodos';
      return;
    }
    this.edges.push({
      id: `edge-${Date.now()}`,
      sourceId,
      targetId,
      label: '',
    });
    this.error = '';
  }

  removeSelectedNode(): void {
    if (!this.selectedNodeId) return;
    const id = this.selectedNodeId;
    this.nodes = this.nodes.filter((n) => n.id !== id);
    this.edges = this.edges.filter((e) => e.sourceId !== id && e.targetId !== id);
    this.selectedNodeId = null;
    this.updateCanvasSize();
  }

  removeSelectedEdge(): void {
    if (!this.selectedEdgeId) return;
    this.edges = this.edges.filter((e) => e.id !== this.selectedEdgeId);
    this.selectedEdgeId = null;
  }

  onNodeMouseDown(event: MouseEvent, node: DiagramNode): void {
    event.stopPropagation();
    this.selectNode(node.id);
    this.draggingNodeId = node.id;
    const viewport = this.canvasViewportRef.nativeElement;
    const rect = viewport.getBoundingClientRect();
    this.dragOffsetX = event.clientX - rect.left + viewport.scrollLeft - node.x;
    this.dragOffsetY = event.clientY - rect.top + viewport.scrollTop - node.y;
  }

  @HostListener('document:mousemove', ['$event'])
  onDocumentMouseMove(event: MouseEvent): void {
    if (!this.draggingNodeId || !this.canvasViewportRef) return;
    const viewport = this.canvasViewportRef.nativeElement;
    const rect = viewport.getBoundingClientRect();
    const node = this.nodes.find((n) => n.id === this.draggingNodeId);
    if (!node) return;
    node.x = Math.max(0, event.clientX - rect.left + viewport.scrollLeft - this.dragOffsetX);
    node.y = Math.max(0, event.clientY - rect.top + viewport.scrollTop - this.dragOffsetY);
  }

  @HostListener('document:mouseup')
  onDocumentMouseUp(): void {
    if (this.draggingNodeId) {
      this.updateCanvasSize();
    }
    this.draggingNodeId = null;
  }

  getNodeSize(node: DiagramNode): { width: number; height: number } {
    const type = node.type.toUpperCase();
    if (type === 'INITIAL' || type === 'FINAL') {
      return { width: 56, height: 56 };
    }
    if (type === 'DECISION' || type === 'MERGE') {
      return { width: 88, height: 88 };
    }
    if (type === 'FORK_JOIN') {
      return { width: 120, height: 48 };
    }
    return { width: 150, height: 78 };
  }

  updateCanvasSize(): void {
    const padding = 160;
    let maxX = 900;
    let maxY = 400;

    for (const node of this.nodes) {
      const size = this.getNodeSize(node);
      maxX = Math.max(maxX, node.x + size.width);
      maxY = Math.max(maxY, node.y + size.height);
    }

    this.canvasWidth = Math.max(1200, maxX + padding);
    this.canvasHeight = Math.max(this.lanes.length * LANE_HEIGHT + 40, maxY + padding);
  }

  centerDiagram(): void {
    if (!this.canvasViewportRef || !this.nodes.length) return;
    this.updateCanvasSize();
    const viewport = this.canvasViewportRef.nativeElement;

    let minX = Infinity;
    let minY = Infinity;
    let maxX = 0;
    let maxY = 0;

    for (const node of this.nodes) {
      const size = this.getNodeSize(node);
      minX = Math.min(minX, node.x);
      minY = Math.min(minY, node.y);
      maxX = Math.max(maxX, node.x + size.width);
      maxY = Math.max(maxY, node.y + size.height);
    }

    const contentCenterX = (minX + maxX) / 2;
    const contentCenterY = (minY + maxY) / 2;

    viewport.scrollLeft = Math.max(0, contentCenterX - viewport.clientWidth / 2);
    viewport.scrollTop = Math.max(0, contentCenterY - viewport.clientHeight / 2);
  }

  showFullDiagram(): void {
    if (!this.canvasViewportRef || !this.nodes.length) return;
    this.updateCanvasSize();
    const viewport = this.canvasViewportRef.nativeElement;

    let minX = Infinity;
    let minY = Infinity;

    for (const node of this.nodes) {
      minX = Math.min(minX, node.x);
      minY = Math.min(minY, node.y);
    }

    viewport.scrollLeft = Math.max(0, minX - 48);
    viewport.scrollTop = Math.max(0, minY - 24);
  }

  getNodeCenter(node: DiagramNode): { x: number; y: number } {
    const type = node.type.toUpperCase();
    if (type === 'INITIAL' || type === 'FINAL') {
      return { x: node.x + 28, y: node.y + 28 };
    }
    if (type === 'DECISION' || type === 'MERGE') {
      return { x: node.x + 44, y: node.y + 44 };
    }
    if (type === 'FORK_JOIN') {
      return { x: node.x + 60, y: node.y + 8 };
    }
    return { x: node.x + 70, y: node.y + 32 };
  }

  getEdgePath(edge: DiagramEdge): string {
    const source = this.nodes.find((n) => n.id === edge.sourceId);
    const target = this.nodes.find((n) => n.id === edge.targetId);
    if (!source || !target) return '';
    const s = this.getNodeCenter(source);
    const t = this.getNodeCenter(target);
    const mx = (s.x + t.x) / 2;
    return `M ${s.x} ${s.y} C ${mx} ${s.y}, ${mx} ${t.y}, ${t.x} ${t.y}`;
  }

  getEdgeLabelPosition(edge: DiagramEdge): { x: number; y: number } {
    const source = this.nodes.find((n) => n.id === edge.sourceId);
    const target = this.nodes.find((n) => n.id === edge.targetId);
    if (!source || !target) return { x: 0, y: 0 };
    const s = this.getNodeCenter(source);
    const t = this.getNodeCenter(target);
    return { x: (s.x + t.x) / 2, y: (s.y + t.y) / 2 - 8 };
  }

  nodeClass(type: string): string {
    return type.toLowerCase().replace('_', '-');
  }

  nodeTypeLabel(type: string): string {
    const map: Record<string, string> = {
      INITIAL: 'Nodo inicial',
      ACTION: 'Actividad',
      DECISION: 'Decisión',
      MERGE: 'Merge',
      FORK_JOIN: 'Fork/Join',
      FINAL: 'Nodo final',
    };
    return map[type.toUpperCase()] ?? type;
  }

  selectedNode(): DiagramNode | undefined {
    return this.nodes.find((n) => n.id === this.selectedNodeId);
  }

  selectedEdge(): DiagramEdge | undefined {
    return this.edges.find((e) => e.id === this.selectedEdgeId);
  }

  validateLocal(): string | null {
    if (!this.nodes.length) return 'Debe agregar al menos un nodo al diagrama';
    if (!this.nodes.some((n) => n.type === 'INITIAL')) return 'Debe existir un nodo inicial';
    if (!this.nodes.some((n) => n.type === 'FINAL')) return 'Debe existir un nodo final';
    const needsLabel = this.nodes.filter((n) =>
      ['ACTION', 'DECISION', 'MERGE', 'FORK_JOIN'].includes(n.type.toUpperCase())
    );
    if (needsLabel.some((n) => !n.label?.trim())) return 'Todas las actividades deben tener nombre';
    return null;
  }

  save(): void {
    this.error = '';
    this.message = '';

    if (!this.policyId) {
      this.error = 'No se encontró la política asociada';
      return;
    }

    const validationError = this.validateLocal();
    if (validationError) {
      this.error = validationError;
      return;
    }

    this.saving = true;
    const payload = {
      policyId: this.policyId,
      name: this.diagramName,
      lanes: this.lanes,
      nodes: this.nodes,
      edges: this.edges,
    };

    this.diagramService.save(payload).subscribe({
      next: (saved) => {
        this.saving = false;
        this.diagramId = saved.id ?? null;
        this.message = 'Diagrama guardado correctamente';
        setTimeout(() => (this.message = ''), 5000);
      },
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message ?? 'No se pudo guardar el diagrama';
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/policies']);
  }

  clearCanvas(): void {
    if (!confirm('¿Limpiar el lienzo? Esta acción no guarda cambios.')) return;
    this.nodes = [];
    this.edges = [];
    this.selectedNodeId = null;
    this.selectedEdgeId = null;
    this.updateCanvasSize();
  }
}
