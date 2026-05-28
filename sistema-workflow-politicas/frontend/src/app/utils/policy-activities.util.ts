import { ActivityDiagram } from '../models/activity-diagram.model';

export const DEFAULT_POLICY_ACTIVITIES = [
  'Registrar solicitud',
  'Revisar solicitud',
  'Validar información',
  'Aprobar permiso',
  'Rechazar permiso',
  'Notificar resultado',
];

export function extractActivitiesFromDiagram(diagram: ActivityDiagram | null | undefined): string[] {
  if (!diagram?.nodes?.length) {
    return [...DEFAULT_POLICY_ACTIVITIES];
  }

  const actions = diagram.nodes
    .filter((node) => (node.type ?? '').toUpperCase() === 'ACTION')
    .map((node) => node.label?.trim())
    .filter((label): label is string => !!label);

  const unique = [...new Set(actions)];
  return unique.length ? unique : [...DEFAULT_POLICY_ACTIVITIES];
}
