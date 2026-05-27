const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Administrador',
  ADMINISTRADOR: 'Administrador',
  DESIGNER: 'Diseñador de Políticas',
  POLICY_DESIGNER: 'Diseñador de Políticas',
  DISENADOR: 'Diseñador de Políticas',
  SUPERVISOR: 'Supervisor',
  ANALISTA: 'Analista',
  ANALYST: 'Analista',
  AUDITOR: 'Auditor',
  OFFICIAL: 'Usuario Operativo',
  FUNCIONARIO: 'Usuario Operativo',
  USUARIO_OPERATIVO: 'Usuario Operativo',
  PROCESS_OWNER: 'Responsable de Proceso',
  RESPONSABLE_DE_PROCESO: 'Responsable de Proceso',
};

function isMongoId(value: string): boolean {
  return /^[a-f0-9]{24}$/i.test(value);
}

export function getRoleDisplayName(name?: string | null): string {
  if (!name || isMongoId(name)) {
    return '—';
  }

  const normalized = name
    .toUpperCase()
    .replace(/^ROLE_/, '')
    .replace(/Á/g, 'A')
    .replace(/É/g, 'E')
    .replace(/Í/g, 'I')
    .replace(/Ó/g, 'O')
    .replace(/Ú/g, 'U')
    .replace(/ /g, '_')
    .trim();

  if (ROLE_LABELS[normalized]) {
    return ROLE_LABELS[normalized];
  }
  if (normalized.includes('DISENADOR') || normalized.includes('DESIGNER') || normalized.includes('POLITIC')) {
    return 'Diseñador de Políticas';
  }
  if (normalized.includes('RESPONSABLE') && normalized.includes('PROCESO')) {
    return 'Responsable de Proceso';
  }

  return name
    .replace(/^ROLE_/i, '')
    .replace(/_/g, ' ')
    .toLowerCase()
    .split(' ')
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}
