/** Pasarelas UML Fork / Join (Ciclo 1) — nodos técnicos de enrutamiento paralelo. */

export const GATEWAY_ACTIVITY_TYPES = ['FORK', 'JOIN'] as const;

export type GatewayActivityType = (typeof GATEWAY_ACTIVITY_TYPES)[number];

export function isGatewayActivityType(type?: string | null): boolean {
  const t = (type ?? '').toUpperCase();
  return t === 'FORK' || t === 'JOIN';
}

export function isForkGateway(type?: string | null): boolean {
  return (type ?? '').toUpperCase() === 'FORK';
}

export function isJoinGateway(type?: string | null): boolean {
  return (type ?? '').toUpperCase() === 'JOIN';
}

export function defaultGatewayName(type: string): string {
  return isForkGateway(type) ? 'Fork' : isJoinGateway(type) ? 'Join' : '';
}

/** Tipo de conexión sugerido según nodos origen/destino. */
export function suggestParallelTransitionType(
  fromActivityType?: string | null,
  toActivityType?: string | null,
): 'PARALLEL_SPLIT' | 'PARALLEL_JOIN' | null {
  if (isForkGateway(fromActivityType)) {
    return 'PARALLEL_SPLIT';
  }
  if (isJoinGateway(toActivityType)) {
    return 'PARALLEL_JOIN';
  }
  return null;
}

export function gatewayActivityTypeLabel(type?: string): string {
  if (isForkGateway(type)) return 'Fork (bifurcación)';
  if (isJoinGateway(type)) return 'Join (unión)';
  return type ?? '—';
}
