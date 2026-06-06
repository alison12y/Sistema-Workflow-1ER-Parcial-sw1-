export interface BitacoraEntry {
  id?: string;
  userId?: string;
  username: string;
  fullName?: string;
  module: string;
  action: string;
  description: string;
  entityType?: string;
  entityId?: string;
  resultado?: string;
  ip?: string;
  createdAt: string;
}

export interface BitacoraFilter {
  userId?: string;
  username?: string;
  module?: string;
  action?: string;
  dateFrom?: string;
  dateTo?: string;
}

export const BITACORA_MODULES = [
  'Seguridad',
  'Usuarios',
  'Roles y permisos',
  'Departamentos',
  'Políticas',
  'Workflow',
  'Formularios',
  'Trámites',
  'Mis actividades',
  'Inteligencia artificial',
  'Colaboración',
  'Modo offline',
  'Analítica inteligente',
  'Documentos',
] as const;

export const BITACORA_ACTION_LABELS: Record<string, string> = {
  LOGIN_EXITOSO: 'Inicio de sesión exitoso',
  LOGIN_FALLIDO: 'Inicio de sesión fallido',
  LOGOUT: 'Cierre de sesión',
  CAMBIO_PASSWORD: 'Cambio de contraseña',
  CREAR_USUARIO: 'Crear usuario',
  EDITAR_USUARIO: 'Editar usuario',
  ELIMINAR_USUARIO: 'Eliminar usuario',
  ACTIVAR_USUARIO: 'Activar usuario',
  DESACTIVAR_USUARIO: 'Desactivar usuario',
  CREAR_ROL: 'Crear rol',
  EDITAR_ROL: 'Editar rol',
  ELIMINAR_ROL: 'Eliminar rol',
  ASIGNAR_PERMISO: 'Asignar permiso',
  QUITAR_PERMISO: 'Quitar permiso',
  CREAR_DEPARTAMENTO: 'Crear departamento',
  EDITAR_DEPARTAMENTO: 'Editar departamento',
  ELIMINAR_DEPARTAMENTO: 'Eliminar departamento',
  CREAR_POLITICA: 'Crear política',
  EDITAR_POLITICA: 'Editar política',
  ACTIVAR_POLITICA: 'Activar política',
  DESACTIVAR_POLITICA: 'Desactivar política',
  ELIMINAR_POLITICA: 'Eliminar política',
  CREAR_ACTIVIDAD: 'Crear actividad',
  EDITAR_ACTIVIDAD: 'Editar actividad',
  ELIMINAR_ACTIVIDAD: 'Eliminar actividad',
  CREAR_TRANSICION: 'Crear transición',
  EDITAR_TRANSICION: 'Editar transición',
  ELIMINAR_TRANSICION: 'Eliminar transición',
  GUARDAR_WORKFLOW: 'Guardar workflow',
  CREAR_FORMULARIO: 'Crear formulario',
  EDITAR_FORMULARIO: 'Editar formulario',
  ELIMINAR_FORMULARIO: 'Eliminar formulario',
  CREAR_CAMPO: 'Crear campo',
  EDITAR_CAMPO: 'Editar campo',
  ELIMINAR_CAMPO: 'Eliminar campo',
  INICIAR_TRAMITE: 'Iniciar trámite',
  CANCELAR_TRAMITE: 'Cancelar trámite',
  ELIMINAR_TRAMITE: 'Eliminar trámite',
  TOMAR_TAREA: 'Tomar tarea',
  COMPLETAR_ACTIVIDAD: 'Completar actividad',
  REASIGNAR_TAREA: 'Reasignar tarea',
  GENERAR_WORKFLOW_IA: 'Generar workflow con IA',
  ASISTENCIA_FORMULARIO_IA: 'Asistencia IA en formulario',
  ABRIR_WORKFLOW_COLABORATIVO: 'Abrir workflow colaborativo',
  MODIFICAR_WORKFLOW_COLABORATIVO: 'Modificar workflow colaborativo',
  AVANZAR_TRAMITE: 'Avanzar trámite',
  CONFLICTO_EDICION: 'Conflicto de edición',
  OFFLINE_DATA_STORED: 'Datos guardados en modo offline',
  OFFLINE_SYNC_COMPLETED: 'Sincronización offline completada',
};
