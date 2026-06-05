import { Injectable } from '@angular/core';
import { AuthResponse } from '../models/auth.model';
import { DiagramEdge, DiagramNode } from '../models/activity-diagram.model';
import { FormDesignerFieldPayload } from '../models/form.model';
import { isFileFieldType } from '../utils/form-submission-display.util';

export const PROCESS_TYPE_OPTIONS = [
  'Solicitud de permiso laboral',
  'Aprobación de compra',
  'Revisión de documento',
  'Atención de reclamo',
  'Política de aprobación de solicitudes',
] as const;

export type ProcessType = (typeof PROCESS_TYPE_OPTIONS)[number];

export interface DiagramSuggestion {
  name: string;
  lanes: string[];
  nodes: DiagramNode[];
  edges: DiagramEdge[];
}

export interface FormFieldSuggestion {
  fieldId: string;
  fieldLabel: string;
  fieldType: string;
  suggestedValue: string | boolean | null;
  message?: string;
  applicable: boolean;
}

export interface FormSuggestionResult {
  suggestions: FormFieldSuggestion[];
  missingRequired: FormFieldSuggestion[];
}

const LANE_HEIGHT = 120;

@Injectable({ providedIn: 'root' })
export class AiAssistantService {
  suggestDiagram(processType: string, policyName?: string): DiagramSuggestion | null {
    const key = this.normalize(processType);
    const builder = DIAGRAM_TEMPLATES[key];
    if (!builder) {
      return null;
    }
    return builder(policyName?.trim() || processType);
  }

  suggestFormValues(
    activityName: string,
    fields: FormDesignerFieldPayload[],
    currentUser: AuthResponse | null,
    policyName?: string
  ): FormSuggestionResult {
    const activity = activityName.trim();
    const rules = this.resolveActivityRules(activity);
    const suggestions: FormFieldSuggestion[] = [];

    for (const field of fields) {
      const fieldId = field.name || field.label;
      const fieldType = (field.type || 'text').toLowerCase();
      const labelNorm = this.normalize(field.label);
      const nameNorm = this.normalize(field.name || '');

      if (isFileFieldType(fieldType)) {
        suggestions.push({
          fieldId,
          fieldLabel: field.label,
          fieldType,
          suggestedValue: null,
          message: 'Adjunte manualmente el documento de respaldo correspondiente.',
          applicable: false,
        });
        continue;
      }

      const suggested = this.inferFieldValue(
        field,
        fieldType,
        labelNorm,
        nameNorm,
        rules,
        currentUser,
        policyName
      );

      suggestions.push({
        fieldId,
        fieldLabel: field.label,
        fieldType,
        suggestedValue: suggested.value,
        message: suggested.message,
        applicable: suggested.value !== null && suggested.value !== '',
      });
    }

    const emptyValues: Record<string, string> = {};
    for (const field of fields) {
      const key = field.name || field.label;
      emptyValues[key] = field.type === 'checkbox' ? 'false' : '';
    }

    const missingRequired = this.detectMissingRequiredFields(fields, emptyValues)
      .map((field) => {
        const match = suggestions.find((s) => s.fieldId === (field.name || field.label));
        return match ?? {
          fieldId: field.name || field.label,
          fieldLabel: field.label,
          fieldType: field.type,
          suggestedValue: null,
          applicable: false,
        };
      });

    return { suggestions, missingRequired };
  }

  detectMissingRequiredFields(
    fields: FormDesignerFieldPayload[],
    formValues: Record<string, string>
  ): FormDesignerFieldPayload[] {
    return fields.filter((field) => {
      if (!field.required) {
        return false;
      }
      const key = field.name || field.label;
      if (isFileFieldType(field.type)) {
        return false;
      }
      const value = formValues[key];
      if (field.type === 'checkbox') {
        return value !== 'true';
      }
      return !value?.trim();
    });
  }

  applyFormSuggestions(
    fields: FormDesignerFieldPayload[],
    suggestions: FormFieldSuggestion[],
    currentValues: Record<string, string>
  ): { values: Record<string, string>; appliedCount: number; skippedCount: number } {
    const values = { ...currentValues };
    let appliedCount = 0;
    let skippedCount = 0;

    for (const suggestion of suggestions) {
      if (!suggestion.applicable || suggestion.suggestedValue === null) {
        continue;
      }

      const field = fields.find((f) => (f.name || f.label) === suggestion.fieldId);
      if (!field || isFileFieldType(field.type)) {
        continue;
      }

      const key = suggestion.fieldId;
      const current = values[key]?.trim() ?? '';
      const hasValue =
        field.type === 'checkbox' ? values[key] === 'true' : current.length > 0;

      if (hasValue) {
        skippedCount++;
        continue;
      }

      if (field.type === 'checkbox') {
        values[key] = suggestion.suggestedValue === true || suggestion.suggestedValue === 'true' ? 'true' : 'false';
        appliedCount++;
        continue;
      }

      if (field.type === 'select' || field.type === 'radio') {
        const options = this.parseOptions(field.options);
        const candidate = String(suggestion.suggestedValue);
        const match = options.find((opt) => this.normalize(opt) === this.normalize(candidate));
        if (match) {
          values[key] = match;
          appliedCount++;
        }
        continue;
      }

      if (field.type === 'number') {
        const num = String(suggestion.suggestedValue).replace(/[^\d.,-]/g, '').replace(',', '.');
        if (num) {
          values[key] = num;
          appliedCount++;
        }
        continue;
      }

      values[key] = String(suggestion.suggestedValue);
      appliedCount++;
    }

    return { values, appliedCount, skippedCount };
  }

  getGeneralAnswer(prompt: string): string {
    const p = this.normalize(prompt);

    if (
      p.includes('uml') ||
      p.includes('workflow') ||
      p.includes('diagrama') ||
      p.includes('carril') ||
      p.includes('swimlane') ||
      p.includes('transicion')
    ) {
      return [
        'Diseñador workflow UML 2.5 — elementos disponibles:',
        '• START, TASK, DECISION, END',
        '• Swimlanes / carriles por responsable',
        '• Transiciones: secuencial, condicional, iterativa, división paralela (Fork) y unión paralela (Join)',
        '',
        'Ruta: Políticas → Diseñar workflow → Modo edición → Validar flujo.',
      ].join('\n');
    }

    if (
      p.includes('dictado') ||
      p.includes('voz') ||
      (p.includes('ia') && (p.includes('disen') || p.includes('workflow') || p.includes('diagrama')))
    ) {
      return [
        'IA en el diseñador workflow (panel Asistente IA, columna izquierda):',
        '• Escriba un prompt o use Dictar por voz',
        '• Generar sugerencia → revisa actividades, decisiones, conexiones y carriles',
        '• Vista previa obligatoria antes de Aplicar sugerencia',
        '',
        'Ejemplo: "Crear un workflow para solicitud de permiso laboral con carriles Funcionario, Recursos Humanos y Supervisor, con decisión de aprobación y notificación al solicitante."',
        '',
        'La IA solo sugiere; usted confirma antes de modificar el diagrama.',
      ].join('\n');
    }

    if (p.includes('permiso') || p.includes('laboral')) {
      return [
        'Ejemplo de prompt para permiso laboral:',
        '"Crear un workflow para solicitud de permiso laboral con carriles Funcionario, Recursos Humanos y Supervisor, actividades de registro, revisión, validación, decisión de aprobación y notificación."',
        '',
        'En Diseñador workflow → Asistente IA → Generar sugerencia → Aplicar.',
        'En ejecución (Mis tareas → formulario) use Asistir formulario con informe libre o dictado.',
      ].join('\n');
    }

    if (
      p.includes('formulario') &&
      (p.includes('disen') || p.includes('campo') || p.includes('tecnico') || p.includes('dinamico'))
    ) {
      return [
        'Formularios dinámicos por actividad (activities/.../form):',
        '• Tipos: texto, número, fecha, checkbox, selección, archivo',
        '• Campos obligatorios y nombre técnico (variable)',
        '',
        'El nombre técnico se usa en condiciones del workflow.',
        'Ejemplo: campo técnico valido → condición en transición: valido == true',
      ].join('\n');
    }

    if (
      p.includes('asistir') ||
      p.includes('informe') ||
      (p.includes('ia') && p.includes('formulario')) ||
      p.includes('ayuda ia')
    ) {
      return [
        'Asistencia IA en formularios (Mis tareas → Completar formulario):',
        '• Escriba un informe libre o dicte por voz',
        '• Asistir formulario → sugerencia de valores y extracción de fechas',
        '• Aplicar sugerencias solo en campos vacíos',
        '• Campos FILE no se autocompletan: el funcionario debe adjuntar archivos manualmente',
      ].join('\n');
    }

    if (
      p.includes('tramite') ||
      p.includes('ejecut') ||
      p.includes('tomar') ||
      p.includes('completar') ||
      p.includes('motor') ||
      p.includes('avanzar')
    ) {
      return [
        'Ejecución del trámite:',
        '1. Trámites → Iniciar trámite (política ACTIVE)',
        '2. Mis tareas → Tomar tarea (si está pendiente)',
        '3. Completar formulario → Completar actividad',
        '4. El motor enruta automáticamente según transiciones y condiciones',
        '',
        'No use Avanzar manual salvo depuración o rol administrador.',
      ].join('\n');
    }

    if (p.includes('bandeja') || p.includes('tarea') || p.includes('mis actividad')) {
      return [
        'Bandeja del funcionario (Mis tareas):',
        '• Pendientes: aún no tomadas',
        '• En curso: asignadas al usuario activo',
        '• Finalizadas: completadas',
        '• Asignación por usuario, rol o departamento según responsable de la actividad',
      ].join('\n');
    }

    if (p.includes('monitore') || p.includes('traza') || p.includes('historial')) {
      return [
        'Monitoreo y trazabilidad:',
        '• Estado actual del trámite y actividad en curso',
        '• Responsable asignado',
        '• Historial de eventos y respuestas de formularios',
        '• Trazas: TRAMITE_CREADO, TAREA_TOMADA, FORMULARIO_ENVIADO, ACTIVIDAD_COMPLETADA',
        '• Actualización automática sin recargar (polling ~12 s)',
      ].join('\n');
    }

    if (p.includes('kpi') || p.includes('cuello') || p.includes('botella') || p.includes('metrica')) {
      return [
        'KPIs y cuellos de botella:',
        '• Trámites activos y finalizados',
        '• Tiempos promedio por actividad',
        '• Carga por funcionario y por departamento',
        '• Ranking de cuellos de botella (actividades con mayor demora)',
        '',
        'Relacione con estimatedTimeHours definido en el diseñador UML.',
      ].join('\n');
    }

    if (p.includes('colabor') || p.includes('conflicto') || p.includes('conectad')) {
      return [
        'Colaboración básica en el diseñador:',
        '• Usuarios conectados al mismo diagrama',
        '• Última modificación y actividad reciente',
        '• Si otro usuario guardó cambios: aviso de conflicto de edición',
        '• Recargar diagrama para ver la versión actual antes de guardar',
      ].join('\n');
    }

    if (p.includes('cancelar') || p.includes('eliminar')) {
      return [
        'Gestión de trámites:',
        '• Cancelar trámite según permisos del rol',
        '• Eliminar solo trámites en estado CANCELADO o COMPLETADO',
        '• No eliminar trámites EN_PROCESO',
      ].join('\n');
    }

    if (p.includes('compra') || p.includes('aprobacion')) {
      return 'Para aprobación de compras, use el Asistente IA del diseñador con un prompt como: "Flujo de aprobación de compra con carriles Solicitante, Compras y Gerencia, con cotización, verificación presupuestaria y decisión de aprobación."';
    }

    return [
      'Guía del sistema Workflow — Ciclo 1 implementado:',
      '',
      '1. Diseñador UML 2.5: START, TASK, DECISION, END, swimlanes, transiciones sec/cond/iter/paralelo',
      '2. IA diseño: prompt, dictado por voz, vista previa, aplicar sugerencia',
      '3. Formularios dinámicos: tipos de campo, obligatorios, nombre técnico para condiciones',
      '4. IA formularios: informe libre, dictado, sugerencias (FILE manual)',
      '5. Ejecución: iniciar trámite → tomar tarea → completar → motor automático',
      '6. Bandeja: pendientes / en curso / finalizadas',
      '7. Monitoreo y KPIs: trazas, tiempos, cuellos de botella',
      '8. Colaboración: usuarios conectados, conflictos, recargar diagrama',
      '9. Trámites: cancelar; eliminar solo CANCELADOS o COMPLETADOS',
      '',
      'Ciclo 2 (no implementado): S3, Flutter, offline, predictivo, reportes dinámicos IA.',
      '',
      'Use las sugerencias rápidas o pregunte por un módulo específico.',
    ].join('\n');
  }

  private inferFieldValue(
    field: FormDesignerFieldPayload,
    fieldType: string,
    labelNorm: string,
    nameNorm: string,
    rules: ActivityFormRules,
    currentUser: AuthResponse | null,
    policyName?: string
  ): { value: string | boolean | null; message?: string } {
    if (fieldType === 'date' || labelNorm.includes('fecha')) {
      return { value: this.todayIso() };
    }

    if (fieldType === 'checkbox') {
      if (labelNorm.includes('acept') || labelNorm.includes('confirm') || labelNorm.includes('declar')) {
        return { value: true };
      }
      return { value: null };
    }

    for (const [pattern, value] of Object.entries(rules.byFieldPattern)) {
      if (labelNorm.includes(pattern) || nameNorm.includes(pattern)) {
        if (fieldType === 'select') {
          const options = this.parseOptions(field.options);
          const match = options.find((opt) => this.normalize(opt).includes(this.normalize(String(value))));
          if (match) {
            return { value: match };
          }
        }
        return { value: String(value) };
      }
    }

    if (labelNorm.includes('nombre') && (labelNorm.includes('solicit') || labelNorm.includes('usuario'))) {
      const name = currentUser?.fullName || currentUser?.username || '';
      if (name) {
        return { value: name };
      }
    }

    if (labelNorm.includes('politica') || labelNorm.includes('tramite')) {
      if (policyName) {
        return { value: policyName };
      }
    }

    if (rules.defaultText && (fieldType === 'textarea' || labelNorm.includes('observ') || labelNorm.includes('motivo') || labelNorm.includes('mensaje') || labelNorm.includes('coment'))) {
      return { value: rules.defaultText };
    }

    return { value: null };
  }

  private resolveActivityRules(activityName: string): ActivityFormRules {
    const a = this.normalize(activityName);

    if (a.includes('registrar') && a.includes('solicitud')) {
      return {
        defaultText: 'Solicito permiso laboral por motivos personales para su revisión correspondiente.',
        byFieldPattern: {
          motivo: 'Solicito permiso laboral por motivos personales para su revisión correspondiente.',
          resultado: 'Registrado',
          estado: 'Pendiente de revisión',
        },
      };
    }
    if (a.includes('revisar') && a.includes('solicitud')) {
      return {
        defaultText: 'La solicitud fue revisada y cumple con los datos requeridos.',
        byFieldPattern: {
          observ: 'La solicitud fue revisada y cumple con los datos requeridos.',
          resultado: 'Procede para validación',
          estado: 'En revisión',
        },
      };
    }
    if (a.includes('validar')) {
      return {
        defaultText: 'La información proporcionada fue validada correctamente.',
        byFieldPattern: {
          observ: 'La información proporcionada fue validada correctamente.',
          estado: 'Información válida',
          resultado: 'Validado',
        },
      };
    }
    if (a.includes('aprobar')) {
      return {
        defaultText: 'Se aprueba la solicitud luego de la revisión correspondiente.',
        byFieldPattern: {
          decision: 'Aprobado',
          observ: 'Se aprueba la solicitud luego de la revisión correspondiente.',
          resultado: 'Aprobado',
        },
      };
    }
    if (a.includes('rechazar')) {
      return {
        defaultText: 'La solicitud no cumple con los criterios requeridos.',
        byFieldPattern: {
          decision: 'Rechazado',
          observ: 'La solicitud no cumple con los criterios requeridos.',
          resultado: 'Rechazado',
        },
      };
    }
    if (a.includes('notificar')) {
      return {
        defaultText: 'Se notifica el resultado del trámite al solicitante correspondiente.',
        byFieldPattern: {
          mensaje: 'Se notifica el resultado del trámite al solicitante correspondiente.',
          observ: 'Notificación enviada al solicitante.',
        },
      };
    }
    if (a.includes('cotiz')) {
      return {
        defaultText: 'Se registraron tres cotizaciones de proveedores autorizados.',
        byFieldPattern: { monto: '1500.00', proveedor: 'Proveedor autorizado' },
      };
    }
    if (a.includes('reclamo') || a.includes('queja')) {
      return {
        defaultText: 'El reclamo fue registrado y derivado al área correspondiente para su atención.',
        byFieldPattern: { prioridad: 'Media', estado: 'En atención' },
      };
    }

    return {
      defaultText: `Información registrada para la actividad "${activityName}".`,
      byFieldPattern: {},
    };
  }

  private parseOptions(options?: string): string[] {
    if (!options?.trim()) {
      return [];
    }
    return options.split(',').map((item) => item.trim()).filter(Boolean);
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private normalize(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }
}

interface ActivityFormRules {
  defaultText: string;
  byFieldPattern: Record<string, string>;
}

type DiagramBuilder = (name: string) => DiagramSuggestion;

function buildDiagram(
  name: string,
  lanes: string[],
  spec: Array<{ id: string; type: string; label: string; lane: string; col: number; rowOffset?: number }>,
  edgeSpec: Array<{ from: string; to: string; label?: string }>
): DiagramSuggestion {
  const nodes: DiagramNode[] = spec.map((item) => {
    const laneIndex = Math.max(0, lanes.indexOf(item.lane));
    const yBase = laneIndex * LANE_HEIGHT + 40 + (item.rowOffset ?? 0);
    const x = 40 + item.col * 170;
    return {
      id: item.id,
      type: item.type,
      label: item.label,
      lane: item.lane,
      x,
      y: yBase,
    };
  });

  const edges: DiagramEdge[] = edgeSpec.map((e, index) => ({
    id: `ai-e${index + 1}`,
    sourceId: e.from,
    targetId: e.to,
    label: e.label ?? '',
  }));

  return { name, lanes, nodes, edges };
}

const DIAGRAM_TEMPLATES: Record<string, DiagramBuilder> = {
  'solicitud de permiso laboral': (name) =>
    buildDiagram(
      name,
      ['Funcionario', 'Recursos Humanos', 'Supervisor'],
      [
        { id: 'ai-n1', type: 'INITIAL', label: 'Inicio', lane: 'Funcionario', col: 0 },
        { id: 'ai-n2', type: 'ACTION', label: 'Registrar solicitud', lane: 'Funcionario', col: 1 },
        { id: 'ai-n3', type: 'ACTION', label: 'Revisar solicitud', lane: 'Recursos Humanos', col: 2 },
        { id: 'ai-n4', type: 'ACTION', label: 'Validar información', lane: 'Recursos Humanos', col: 3 },
        { id: 'ai-n5', type: 'DECISION', label: '¿Aprueba?', lane: 'Supervisor', col: 4 },
        { id: 'ai-n6', type: 'ACTION', label: 'Aprobar permiso', lane: 'Supervisor', col: 5, rowOffset: -40 },
        { id: 'ai-n7', type: 'ACTION', label: 'Rechazar permiso', lane: 'Supervisor', col: 5, rowOffset: 60 },
        { id: 'ai-n8', type: 'MERGE', label: 'Unión', lane: 'Funcionario', col: 6 },
        { id: 'ai-n9', type: 'ACTION', label: 'Notificar resultado', lane: 'Funcionario', col: 7 },
        { id: 'ai-n10', type: 'FINAL', label: 'Fin', lane: 'Funcionario', col: 8 },
      ],
      [
        { from: 'ai-n1', to: 'ai-n2' },
        { from: 'ai-n2', to: 'ai-n3' },
        { from: 'ai-n3', to: 'ai-n4' },
        { from: 'ai-n4', to: 'ai-n5' },
        { from: 'ai-n5', to: 'ai-n6', label: '[Sí]' },
        { from: 'ai-n5', to: 'ai-n7', label: '[No]' },
        { from: 'ai-n6', to: 'ai-n8' },
        { from: 'ai-n7', to: 'ai-n8' },
        { from: 'ai-n8', to: 'ai-n9' },
        { from: 'ai-n9', to: 'ai-n10' },
      ]
    ),

  'aprobacion de compra': (name) =>
    buildDiagram(
      name,
      ['Solicitante', 'Compras', 'Gerencia'],
      [
        { id: 'ai-n1', type: 'INITIAL', label: 'Inicio', lane: 'Solicitante', col: 0 },
        { id: 'ai-n2', type: 'ACTION', label: 'Registrar requerimiento', lane: 'Solicitante', col: 1 },
        { id: 'ai-n3', type: 'ACTION', label: 'Cotizar materiales', lane: 'Compras', col: 2 },
        { id: 'ai-n4', type: 'ACTION', label: 'Verificar presupuesto', lane: 'Compras', col: 3 },
        { id: 'ai-n5', type: 'DECISION', label: '¿Presupuesto OK?', lane: 'Gerencia', col: 4 },
        { id: 'ai-n6', type: 'ACTION', label: 'Aprobar compra', lane: 'Gerencia', col: 5, rowOffset: -40 },
        { id: 'ai-n7', type: 'ACTION', label: 'Rechazar compra', lane: 'Gerencia', col: 5, rowOffset: 60 },
        { id: 'ai-n8', type: 'MERGE', label: 'Unión', lane: 'Compras', col: 6 },
        { id: 'ai-n9', type: 'ACTION', label: 'Emitir orden de compra', lane: 'Compras', col: 7 },
        { id: 'ai-n10', type: 'FINAL', label: 'Fin', lane: 'Solicitante', col: 8 },
      ],
      [
        { from: 'ai-n1', to: 'ai-n2' },
        { from: 'ai-n2', to: 'ai-n3' },
        { from: 'ai-n3', to: 'ai-n4' },
        { from: 'ai-n4', to: 'ai-n5' },
        { from: 'ai-n5', to: 'ai-n6', label: '[Sí]' },
        { from: 'ai-n5', to: 'ai-n7', label: '[No]' },
        { from: 'ai-n6', to: 'ai-n8' },
        { from: 'ai-n7', to: 'ai-n8' },
        { from: 'ai-n8', to: 'ai-n9' },
        { from: 'ai-n9', to: 'ai-n10' },
      ]
    ),

  'revision de documento': (name) =>
    buildDiagram(
      name,
      ['Autor', 'Revisor', 'Aprobador'],
      [
        { id: 'ai-n1', type: 'INITIAL', label: 'Inicio', lane: 'Autor', col: 0 },
        { id: 'ai-n2', type: 'ACTION', label: 'Elaborar documento', lane: 'Autor', col: 1 },
        { id: 'ai-n3', type: 'ACTION', label: 'Enviar a revisión', lane: 'Autor', col: 2 },
        { id: 'ai-n4', type: 'ACTION', label: 'Revisar contenido', lane: 'Revisor', col: 3 },
        { id: 'ai-n5', type: 'DECISION', label: '¿Conforme?', lane: 'Revisor', col: 4 },
        { id: 'ai-n6', type: 'ACTION', label: 'Corregir documento', lane: 'Autor', col: 5, rowOffset: 60 },
        { id: 'ai-n7', type: 'ACTION', label: 'Aprobar documento', lane: 'Aprobador', col: 6 },
        { id: 'ai-n8', type: 'ACTION', label: 'Publicar documento', lane: 'Autor', col: 7 },
        { id: 'ai-n9', type: 'FINAL', label: 'Fin', lane: 'Autor', col: 8 },
      ],
      [
        { from: 'ai-n1', to: 'ai-n2' },
        { from: 'ai-n2', to: 'ai-n3' },
        { from: 'ai-n3', to: 'ai-n4' },
        { from: 'ai-n4', to: 'ai-n5' },
        { from: 'ai-n5', to: 'ai-n6', label: '[No]' },
        { from: 'ai-n6', to: 'ai-n3' },
        { from: 'ai-n5', to: 'ai-n7', label: '[Sí]' },
        { from: 'ai-n7', to: 'ai-n8' },
        { from: 'ai-n8', to: 'ai-n9' },
      ]
    ),

  'atencion de reclamo': (name) =>
    buildDiagram(
      name,
      ['Cliente', 'Atención', 'Resolución'],
      [
        { id: 'ai-n1', type: 'INITIAL', label: 'Inicio', lane: 'Cliente', col: 0 },
        { id: 'ai-n2', type: 'ACTION', label: 'Registrar reclamo', lane: 'Cliente', col: 1 },
        { id: 'ai-n3', type: 'ACTION', label: 'Clasificar reclamo', lane: 'Atención', col: 2 },
        { id: 'ai-n4', type: 'ACTION', label: 'Investigar caso', lane: 'Atención', col: 3 },
        { id: 'ai-n5', type: 'DECISION', label: '¿Procede?', lane: 'Resolución', col: 4 },
        { id: 'ai-n6', type: 'ACTION', label: 'Resolver a favor', lane: 'Resolución', col: 5, rowOffset: -40 },
        { id: 'ai-n7', type: 'ACTION', label: 'Rechazar reclamo', lane: 'Resolución', col: 5, rowOffset: 60 },
        { id: 'ai-n8', type: 'MERGE', label: 'Unión', lane: 'Atención', col: 6 },
        { id: 'ai-n9', type: 'ACTION', label: 'Notificar al cliente', lane: 'Cliente', col: 7 },
        { id: 'ai-n10', type: 'FINAL', label: 'Fin', lane: 'Cliente', col: 8 },
      ],
      [
        { from: 'ai-n1', to: 'ai-n2' },
        { from: 'ai-n2', to: 'ai-n3' },
        { from: 'ai-n3', to: 'ai-n4' },
        { from: 'ai-n4', to: 'ai-n5' },
        { from: 'ai-n5', to: 'ai-n6', label: '[Sí]' },
        { from: 'ai-n5', to: 'ai-n7', label: '[No]' },
        { from: 'ai-n6', to: 'ai-n8' },
        { from: 'ai-n7', to: 'ai-n8' },
        { from: 'ai-n8', to: 'ai-n9' },
        { from: 'ai-n9', to: 'ai-n10' },
      ]
    ),

  'politica de aprobacion de solicitudes': (name) =>
    buildDiagram(
      name,
      ['Funcionario', 'Recursos Humanos', 'Supervisor'],
      [
        { id: 'ai-n1', type: 'INITIAL', label: 'Inicio', lane: 'Funcionario', col: 0 },
        { id: 'ai-n2', type: 'ACTION', label: 'Registrar solicitud', lane: 'Funcionario', col: 1 },
        { id: 'ai-n3', type: 'ACTION', label: 'Revisar solicitud', lane: 'Recursos Humanos', col: 2 },
        { id: 'ai-n4', type: 'ACTION', label: 'Validar información', lane: 'Recursos Humanos', col: 3 },
        { id: 'ai-n5', type: 'DECISION', label: '¿Aprueba?', lane: 'Supervisor', col: 4 },
        { id: 'ai-n6', type: 'ACTION', label: 'Aprobar solicitud', lane: 'Supervisor', col: 5, rowOffset: -40 },
        { id: 'ai-n7', type: 'ACTION', label: 'Rechazar solicitud', lane: 'Supervisor', col: 5, rowOffset: 60 },
        { id: 'ai-n8', type: 'MERGE', label: 'Unión', lane: 'Funcionario', col: 6 },
        { id: 'ai-n9', type: 'ACTION', label: 'Notificar resultado', lane: 'Funcionario', col: 7 },
        { id: 'ai-n10', type: 'FINAL', label: 'Fin', lane: 'Funcionario', col: 8 },
      ],
      [
        { from: 'ai-n1', to: 'ai-n2' },
        { from: 'ai-n2', to: 'ai-n3' },
        { from: 'ai-n3', to: 'ai-n4' },
        { from: 'ai-n4', to: 'ai-n5' },
        { from: 'ai-n5', to: 'ai-n6', label: '[Sí]' },
        { from: 'ai-n5', to: 'ai-n7', label: '[No]' },
        { from: 'ai-n6', to: 'ai-n8' },
        { from: 'ai-n7', to: 'ai-n8' },
        { from: 'ai-n8', to: 'ai-n9' },
        { from: 'ai-n9', to: 'ai-n10' },
      ]
    ),
};
