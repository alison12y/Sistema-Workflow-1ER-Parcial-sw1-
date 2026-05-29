const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Administrador del sistema',
  ADMINISTRADOR: 'Administrador del sistema',
  ADMINISTRADOR_DEL_SISTEMA: 'Administrador del sistema',
  DESIGNER: 'Diseñador de Políticas',
  POLICY_DESIGNER: 'Diseñador de Políticas',
  DISENADOR: 'Diseñador de Políticas',
  SUPERVISOR: 'Supervisor',
  ANALISTA: 'Analista',
  ANALYST: 'Analista',
  AUDITOR: 'Auditor',
  OFFICIAL: 'Funcionario',
  FUNCIONARIO: 'Funcionario',
  USUARIO_OPERATIVO: 'Funcionario',
  PROCESS_OWNER: 'Dueño de proceso',
  DUENO_DE_PROCESO: 'Dueño de proceso',
  RESPONSABLE_DE_PROCESO: 'Dueño de proceso',
  ATENCION_AL_CLIENTE: 'Atención al cliente',
  TECNICO: 'Técnico',
  LEGAL: 'Legal',
  CUSTOMER_SERVICE: 'Atención al cliente',
  TECHNICIAN: 'Técnico',
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
    .replace(/Ñ/g, 'N')
    .replace(/ /g, '_')
    .trim();

  if (ROLE_LABELS[normalized]) {
    return ROLE_LABELS[normalized];
  }
  if (normalized.includes('DISENADOR') || normalized.includes('DESIGNER') || normalized.includes('POLITIC')) {
    return 'Diseñador de Políticas';
  }
  if (normalized.includes('DUENO') && normalized.includes('PROCESO')) {
    return 'Dueño de proceso';
  }
  if (normalized.includes('ADMINISTRADOR')) {
    return 'Administrador del sistema';
  }
  if (normalized.includes('ATENCION') && normalized.includes('CLIENTE')) {
    return 'Atención al cliente';
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
