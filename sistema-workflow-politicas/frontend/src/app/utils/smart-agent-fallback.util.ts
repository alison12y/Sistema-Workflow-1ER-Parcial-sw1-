import { BusinessPolicy } from '../models/auth.model';
import { SmartAgentAnalyzeResponse, SmartAgentSuggestedField } from '../models/smart-agent.model';

export const SMART_AGENT_FALLBACK_WARNING =
  'Servicio IA no disponible; se usó recomendación local.';

const INTENT_KEYWORDS: Record<string, string[]> = {
  INSTALACION_MEDIDOR: [
    'medidor',
    'instalacion',
    'instalar',
    'suministro',
    'electricidad',
    'gas',
    'contador',
  ],
  SOLICITUD_VACACIONES: [
    'vacaciones',
    'vacacion',
    'licencia anual',
    'descanso',
    'dias libres',
    'días libres',
    'ausencia planificada',
  ],
  PERMISO_LABORAL: [
    'permiso laboral',
    'permiso personal',
    'licencia medica',
    'ausencia justificada',
    'permiso especial',
  ],
  GESTION_DECOMISADOS: [
    'decomisado',
    'decomisados',
    'bienes',
    'confiscado',
    'incautacion',
    'incautados',
    'comiso',
  ],
  RECLAMO_SERVICIO: ['reclamo', 'queja', 'mal servicio', 'defecto', 'insatisfecho', 'reclamar'],
  REVISION_DOCUMENTAL: ['documento', 'revision', 'revisar', 'aprobacion', 'validar', 'legal', 'contrato'],
  POLITICA_IA: ['inteligencia artificial', 'ia', 'chatbot', 'asistente', 'automatizar', 'machine learning'],
  SOLICITUD_GENERAL: ['solicitud', 'tramite', 'pedido', 'requiero', 'necesito', 'ayuda'],
};

interface ScoredPolicy {
  policy: BusinessPolicy;
  score: number;
  reason: string;
}

function normalize(value: string | null | undefined): string {
  if (!value) return '';
  const lowered = value.toLowerCase().trim();
  return lowered
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .replace(/\s+/g, ' ');
}

function detectIntent(normalizedText: string): string {
  let bestIntent = 'SOLICITUD_GENERAL';
  let bestHits = 0;
  for (const [intent, keywords] of Object.entries(INTENT_KEYWORDS)) {
    let hits = 0;
    for (const keyword of keywords) {
      if (normalizedText.includes(normalize(keyword))) {
        hits++;
      }
    }
    if (hits > bestHits) {
      bestHits = hits;
      bestIntent = intent;
    }
  }
  return bestIntent;
}

function scorePolicies(
  normalizedText: string,
  intent: string,
  policies: BusinessPolicy[],
): ScoredPolicy[] {
  const intentKeywords = INTENT_KEYWORDS[intent] ?? [];
  return policies.map((policy) => {
    let score = 0.2;
    const reasonParts: string[] = [];
    const policyText = normalize(
      `${policy.name ?? ''} ${policy.description ?? ''} ${policy.type ?? ''}`,
    );

    for (const keyword of intentKeywords) {
      const nk = normalize(keyword);
      if (policyText.includes(nk)) {
        score += 0.18;
        reasonParts.push(`política contiene '${keyword}';`);
      }
      if (normalizedText.includes(nk)) {
        score += 0.08;
      }
    }

    for (const token of normalizedText.split(/\s+/)) {
      if (token.length >= 4 && policyText.includes(token)) {
        score += 0.05;
        reasonParts.push(`coincidencia '${token}';`);
      }
    }

    return {
      policy,
      score,
      reason: reasonParts.length
        ? reasonParts.join(' ').trim()
        : 'mejor coincidencia disponible entre políticas activas',
    };
  });
}

function humanIntent(intent: string): string {
  switch (intent) {
    case 'INSTALACION_MEDIDOR':
      return 'instalación de medidor';
    case 'SOLICITUD_VACACIONES':
      return 'solicitud de vacaciones';
    case 'PERMISO_LABORAL':
      return 'permiso laboral';
    case 'GESTION_DECOMISADOS':
      return 'gestión de bienes decomisados';
    case 'RECLAMO_SERVICIO':
      return 'reclamo de servicio';
    case 'REVISION_DOCUMENTAL':
      return 'revisión documental';
    case 'POLITICA_IA':
      return 'solicitud relacionada con IA';
    default:
      return 'solicitud general';
  }
}

function buildExplanation(
  policy: BusinessPolicy,
  intent: string,
  reason: string,
  attachmentFileName?: string | null,
): string {
  let text = `Se recomienda «${policy.name}» porque ${reason}. Intención detectada: ${humanIntent(intent)}.`;
  if (attachmentFileName) {
    text += ` El archivo «${attachmentFileName}» refuerza el contexto documental.`;
  }
  if (policy.description?.trim()) {
    text += ` ${policy.description.trim()}`;
  }
  return text.trim();
}

function buildRequiredDocuments(intent: string, policy: BusinessPolicy): string[] {
  let docs: string[];
  switch (intent) {
    case 'INSTALACION_MEDIDOR':
      docs = [
        'Documento de identidad',
        'Comprobante de domicilio',
        'Plano o croquis del punto de instalación (opcional)',
      ];
      break;
    case 'SOLICITUD_VACACIONES':
      docs = [
        'Formulario de solicitud de vacaciones firmado',
        'Cronograma de actividades delegadas (opcional)',
      ];
      break;
    case 'PERMISO_LABORAL':
      docs = ['Justificativo o certificado correspondiente', 'Aprobación previa del jefe inmediato'];
      break;
    case 'GESTION_DECOMISADOS':
      docs = [
        'Acta o informe de decomiso',
        'Inventario de bienes',
        'Documento de identidad del solicitante',
      ];
      break;
    case 'RECLAMO_SERVICIO':
      docs = ['Descripción detallada del reclamo', 'Evidencia fotográfica o comprobantes (opcional)'];
      break;
    case 'REVISION_DOCUMENTAL':
      docs = ['Documento a revisar', 'Carta o memo de solicitud (opcional)'];
      break;
    case 'POLITICA_IA':
      docs = ['Descripción del caso de uso', 'Documentación técnica de referencia (opcional)'];
      break;
    default:
      docs = ['Documento de soporte (opcional)', 'Descripción de la solicitud'];
  }
  return [...docs, `Política: ${policy.name}`];
}

function buildSuggestedFields(
  combinedText: string,
  requesterName?: string | null,
): SmartAgentSuggestedField[] {
  const description =
    combinedText.length > 500 ? `${combinedText.substring(0, 500)}...` : combinedText;
  return [
    {
      name: 'description',
      label: 'Descripción de la solicitud',
      type: 'TEXTAREA',
      required: true,
      suggestedValue: description,
    },
    {
      name: 'requestedBy',
      label: 'Solicitante',
      type: 'TEXT',
      required: true,
      suggestedValue: requesterName ?? undefined,
    },
    {
      name: 'priority',
      label: 'Prioridad',
      type: 'SELECT',
      required: false,
      suggestedValue: 'NORMAL',
    },
  ];
}

export function matchSmartAgentFallback(
  combinedText: string,
  activePolicies: BusinessPolicy[],
  requesterName?: string | null,
  attachmentFileName?: string | null,
): SmartAgentAnalyzeResponse {
  const response: SmartAgentAnalyzeResponse = {
    detectedIntent: 'SOLICITUD_GENERAL',
    source: 'LOCAL_FALLBACK',
    warnings: [SMART_AGENT_FALLBACK_WARNING],
  };

  if (!activePolicies.length) {
    response.detectedIntent = 'SIN_POLITICAS';
    response.confidenceScore = 0;
    response.explanation = 'No hay políticas activas disponibles para recomendar.';
    response.warnings = [
      SMART_AGENT_FALLBACK_WARNING,
      'Configure al menos una política activa en el sistema.',
    ];
    return response;
  }

  const normalized = normalize(combinedText);
  const intent = detectIntent(normalized);
  response.detectedIntent = intent;

  const scored = scorePolicies(normalized, intent, activePolicies);
  const best = scored.reduce((current, item) => (item.score > current.score ? item : current), scored[0]);

  const policy = best.policy;
  response.recommendedPolicyId = policy.id;
  response.recommendedPolicyName = policy.name;
  response.confidenceScore = Math.min(0.95, Math.max(0.3, best.score));
  response.explanation = buildExplanation(policy, intent, best.reason, attachmentFileName);
  response.requiredDocuments = buildRequiredDocuments(intent, policy);
  response.suggestedFields = buildSuggestedFields(combinedText, requesterName);
  response.attachmentFileName = attachmentFileName ?? undefined;

  if (best.score < 0.45) {
    response.warnings!.push(
      'La confianza es baja; verifique la recomendación antes de iniciar el trámite.',
    );
  }
  if (attachmentFileName) {
    response.warnings!.push(`Se consideró el documento adjunto: ${attachmentFileName}`);
  }

  return response;
}
