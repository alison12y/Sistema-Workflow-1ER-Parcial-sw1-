export interface BitacoraEntry {
  username: string;
  module: string;
  action: string;
  description: string;
  createdAt: string;
}

export const BITACORA_MODULES = [
  'Políticas',
  'Formularios',
  'Diagramas UML',
  'Trámites',
  'Usuarios',
] as const;

export const BITACORA_ACTION_LABELS: Record<string, string> = {
  CREAR_POLITICA: 'Crear política',
  EDITAR_POLITICA: 'Editar política',
  ELIMINAR_POLITICA: 'Eliminar política',
  GUARDAR_FORMULARIO: 'Guardar formulario',
  GUARDAR_DIAGRAMA: 'Guardar diagrama',
  CREAR_TRAMITE: 'Crear trámite',
  AVANZAR_TRAMITE: 'Avanzar trámite',
  CANCELAR_TRAMITE: 'Cancelar trámite',
  CREAR_USUARIO: 'Crear usuario',
  EDITAR_USUARIO: 'Editar usuario',
  ELIMINAR_USUARIO: 'Eliminar usuario',
};
