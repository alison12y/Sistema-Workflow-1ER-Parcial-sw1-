/** Utilidades de modo guiado para el diseñador workflow (Fase 8). */

export interface GuidedActivityTypeOption {
  value: string;
  label: string;
  help: string;
}

export interface GuidedTransitionTypeOption {
  value: string;
  label: string;
  help: string;
  enabled: boolean;
}

export interface FlowTemplateActivity {
  orderIndex: number;
  name: string;
  responsible: string;
  activityType: 'START' | 'TASK' | 'DECISION' | 'END';
}

export interface FlowTemplateEdge {
  fromOrder: number;
  toOrder: number;
  transitionType?: 'SEQUENTIAL' | 'CONDITIONAL';
  conditionLabel?: string;
}

export interface FlowTemplate {
  id: string;
  title: string;
  description: string;
  activities: FlowTemplateActivity[];
  edges: FlowTemplateEdge[];
}

export interface QuickFlowRow {
  orderIndex: number;
  name: string;
  responsible: string;
  activityType: string;
}

export const GUIDED_ACTIVITY_TYPE_OPTIONS: GuidedActivityTypeOption[] = [
  {
    value: 'START',
    label: 'Inicio',
    help: 'Primera actividad del trámite. Marca el comienzo del flujo.',
  },
  {
    value: 'TASK',
    label: 'Tarea normal',
    help: 'Actividad habitual realizada por un responsable.',
  },
  {
    value: 'DECISION',
    label: 'Decisión / condición',
    help: 'Punto donde el flujo puede tomar más de un camino.',
  },
  {
    value: 'END',
    label: 'Fin del trámite',
    help: 'Cierre del trámite cuando termina el proceso.',
  },
];

export const GUIDED_TRANSITION_TYPE_OPTIONS: GuidedTransitionTypeOption[] = [
  {
    value: 'SEQUENTIAL',
    label: 'Secuencial',
    help: 'Pasa directamente a la siguiente actividad.',
    enabled: true,
  },
  {
    value: 'CONDITIONAL',
    label: 'Condicional',
    help: 'Depende de una respuesta como Aprobado, Observado o Rechazado.',
    enabled: true,
  },
  {
    value: 'ITERATIVE',
    label: 'Iterativa',
    help: 'Disponible en una fase posterior.',
    enabled: false,
  },
  {
    value: 'PARALLEL_SPLIT',
    label: 'División paralela',
    help: 'Disponible en una fase posterior.',
    enabled: false,
  },
  {
    value: 'PARALLEL_JOIN',
    label: 'Unión paralela',
    help: 'Disponible en una fase posterior.',
    enabled: false,
  },
];

export const CONDITION_LABEL_EXAMPLES = ['Aprobado', 'Observado', 'Rechazado'];

export interface UmlToolboxItem {
  id: string;
  label: string;
  kind: 'activity' | 'transition';
  activityType?: 'START' | 'TASK' | 'DECISION' | 'END';
  symbol: 'initial' | 'action' | 'decision' | 'final' | 'arrow' | 'fork' | 'join';
  enabled: boolean;
  soon?: boolean;
}

export const UML_TOOLBOX_ITEMS: UmlToolboxItem[] = [
  { id: 'start', label: 'Inicio', kind: 'activity', activityType: 'START', symbol: 'initial', enabled: true },
  { id: 'task', label: 'Actividad', kind: 'activity', activityType: 'TASK', symbol: 'action', enabled: true },
  { id: 'decision', label: 'Decisión', kind: 'activity', activityType: 'DECISION', symbol: 'decision', enabled: true },
  { id: 'fork', label: 'Fork', kind: 'activity', symbol: 'fork', enabled: false, soon: true },
  { id: 'join', label: 'Join', kind: 'activity', symbol: 'join', enabled: false, soon: true },
  { id: 'end', label: 'Fin', kind: 'activity', activityType: 'END', symbol: 'final', enabled: true },
  { id: 'transition', label: 'Transición', kind: 'transition', symbol: 'arrow', enabled: true },
];

export const SUGGESTED_LANE_NAMES = [
  'Atención al cliente',
  'Técnico',
  'Legal',
  'Supervisor',
  'Recursos Humanos',
  'Jefe inmediato',
  'Funcionario',
  'Dueño de proceso',
];

export const FLOW_TEMPLATES: FlowTemplate[] = [
  {
    id: 'sequential-basic',
    title: 'Flujo secuencial básico',
    description: 'Cuatro pasos en línea: inicio, revisión, aprobación y fin.',
    activities: [
      { orderIndex: 1, name: 'Inicio', responsible: 'Atención al cliente', activityType: 'START' },
      { orderIndex: 2, name: 'Revisión', responsible: 'Supervisor', activityType: 'TASK' },
      { orderIndex: 3, name: 'Aprobación', responsible: 'Dueño de proceso', activityType: 'TASK' },
      { orderIndex: 4, name: 'Fin', responsible: 'Atención al cliente', activityType: 'END' },
    ],
    edges: [
      { fromOrder: 1, toOrder: 2 },
      { fromOrder: 2, toOrder: 3 },
      { fromOrder: 3, toOrder: 4 },
    ],
  },
  {
    id: 'with-decision',
    title: 'Flujo con decisión',
    description: 'Incluye un punto de decisión con caminos aprobado y observado.',
    activities: [
      { orderIndex: 1, name: 'Inicio', responsible: 'Funcionario', activityType: 'START' },
      { orderIndex: 2, name: 'Revisión', responsible: 'Supervisor', activityType: 'TASK' },
      { orderIndex: 3, name: 'Decisión', responsible: 'Supervisor', activityType: 'DECISION' },
      { orderIndex: 4, name: 'Fin', responsible: 'Atención al cliente', activityType: 'END' },
    ],
    edges: [
      { fromOrder: 1, toOrder: 2 },
      { fromOrder: 2, toOrder: 3 },
      { fromOrder: 3, toOrder: 4, transitionType: 'CONDITIONAL', conditionLabel: 'Aprobado' },
      { fromOrder: 3, toOrder: 2, transitionType: 'CONDITIONAL', conditionLabel: 'Observado' },
    ],
  },
  {
    id: 'leave-request',
    title: 'Solicitud de permiso laboral',
    description: 'Ejemplo típico de permiso con revisión y evaluación.',
    activities: [
      { orderIndex: 1, name: 'Registrar solicitud', responsible: 'Funcionario', activityType: 'START' },
      { orderIndex: 2, name: 'Revisar jefe inmediato', responsible: 'Supervisor', activityType: 'TASK' },
      { orderIndex: 3, name: 'Evaluar RRHH', responsible: 'Dueño de proceso', activityType: 'DECISION' },
      { orderIndex: 4, name: 'Aprobar permiso', responsible: 'Dueño de proceso', activityType: 'TASK' },
      { orderIndex: 5, name: 'Finalizar', responsible: 'Funcionario', activityType: 'END' },
    ],
    edges: [
      { fromOrder: 1, toOrder: 2 },
      { fromOrder: 2, toOrder: 3 },
      { fromOrder: 3, toOrder: 4, transitionType: 'CONDITIONAL', conditionLabel: 'Aprobado' },
      { fromOrder: 4, toOrder: 5 },
      { fromOrder: 3, toOrder: 2, transitionType: 'CONDITIONAL', conditionLabel: 'Rechazado' },
    ],
  },
  {
    id: 'document-review',
    title: 'Revisión documental',
    description: 'Recepción, verificación, decisión y corrección de documentos.',
    activities: [
      { orderIndex: 1, name: 'Recibir documento', responsible: 'Atención al cliente', activityType: 'START' },
      { orderIndex: 2, name: 'Verificar requisitos', responsible: 'Técnico', activityType: 'TASK' },
      { orderIndex: 3, name: 'Decidir aprobación', responsible: 'Supervisor', activityType: 'DECISION' },
      { orderIndex: 4, name: 'Corregir observación', responsible: 'Funcionario', activityType: 'TASK' },
      { orderIndex: 5, name: 'Finalizar', responsible: 'Atención al cliente', activityType: 'END' },
    ],
    edges: [
      { fromOrder: 1, toOrder: 2 },
      { fromOrder: 2, toOrder: 3 },
      { fromOrder: 3, toOrder: 5, transitionType: 'CONDITIONAL', conditionLabel: 'Aprobado' },
      { fromOrder: 3, toOrder: 4, transitionType: 'CONDITIONAL', conditionLabel: 'Observado' },
      { fromOrder: 4, toOrder: 2 },
    ],
  },
];

export const DEFAULT_QUICK_FLOW_ROWS: QuickFlowRow[] = [
  { orderIndex: 1, name: 'Registrar solicitud', responsible: 'Funcionario', activityType: 'START' },
  { orderIndex: 2, name: 'Revisar solicitud', responsible: 'Supervisor', activityType: 'TASK' },
  { orderIndex: 3, name: 'Evaluar resultado', responsible: 'Dueño de proceso', activityType: 'DECISION' },
  { orderIndex: 4, name: 'Aprobar permiso', responsible: 'Dueño de proceso', activityType: 'END' },
];

export function activityTypeHelp(type?: string): string {
  const opt = GUIDED_ACTIVITY_TYPE_OPTIONS.find(
    (o) => o.value === (type ?? 'TASK').toUpperCase()
  );
  return opt?.help ?? '';
}

export function friendlyValidationMessage(raw: string): string {
  const text = raw.trim();
  if (!text) return text;

  const lower = text.toLowerCase();

  if (lower.includes('no tiene actividad de inicio')) {
    return 'Debe existir una actividad de inicio.';
  }
  if (lower.includes('no tiene actividad de fin')) {
    return 'Debe existir una actividad de fin.';
  }
  if (lower.includes('está aislada') || lower.includes('sin conexiones')) {
    const match = text.match(/"([^"]+)"/);
    const name = match?.[1] ?? 'esa actividad';
    return `La actividad ${name} no está conectada.`;
  }
  if (lower.includes('no tiene salida')) {
    const match = text.match(/"([^"]+)"/);
    const name = match?.[1] ?? 'esa actividad';
    if (lower.includes('decisión') || name.toLowerCase().includes('decisión')) {
      return `La decisión ${name} necesita al menos dos salidas.`;
    }
    return `La actividad ${name} no tiene una conexión de salida.`;
  }
  if (lower.includes('no tiene entrada')) {
    const match = text.match(/"([^"]+)"/);
    const name = match?.[1] ?? 'esa actividad';
    return `La actividad ${name} no está conectada con el paso anterior.`;
  }
  if (lower.includes('no tiene conexiones activas')) {
    return 'Debe conectar las actividades para formar el flujo.';
  }
  if (lower.includes('no tiene actividades configuradas')) {
    return 'Agregue al menos una actividad para diseñar el flujo.';
  }
  if (lower.includes('transición duplicada')) {
    return 'Hay una conexión repetida entre dos actividades. Revise las conexiones.';
  }
  if (lower.includes('no tiene etiqueta de condición')) {
    return 'Las conexiones condicionales deben indicar una etiqueta (por ejemplo: Aprobado).';
  }

  return text
    .replace(/\bSEQUENTIAL\b/gi, 'Secuencial')
    .replace(/\bCONDITIONAL\b/gi, 'Condicional')
    .replace(/\bSTART\b/gi, 'Inicio')
    .replace(/\bEND\b/gi, 'Fin')
    .replace(/\bTASK\b/gi, 'Tarea')
    .replace(/\bDECISION\b/gi, 'Decisión');
}

export function buildSequentialEdges(count: number): FlowTemplateEdge[] {
  const edges: FlowTemplateEdge[] = [];
  for (let i = 1; i < count; i++) {
    edges.push({ fromOrder: i, toOrder: i + 1 });
  }
  return edges;
}

export function cloneTemplate(template: FlowTemplate): FlowTemplate {
  return {
    ...template,
    activities: template.activities.map((a) => ({ ...a })),
    edges: template.edges.map((e) => ({ ...e })),
  };
}
