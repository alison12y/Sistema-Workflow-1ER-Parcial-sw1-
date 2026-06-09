package com.workflow.politicas.audit;

/** Códigos de acción para la bitácora centralizada (CU11). */
public final class AuditActions {

    private AuditActions() {
    }

    // Seguridad
    public static final String LOGIN_EXITOSO = "LOGIN_EXITOSO";
    public static final String LOGIN_FALLIDO = "LOGIN_FALLIDO";
    public static final String LOGOUT = "LOGOUT";
    public static final String CAMBIO_PASSWORD = "CAMBIO_PASSWORD";

    // Usuarios
    public static final String CREAR_USUARIO = "CREAR_USUARIO";
    public static final String EDITAR_USUARIO = "EDITAR_USUARIO";
    public static final String ELIMINAR_USUARIO = "ELIMINAR_USUARIO";
    public static final String ACTIVAR_USUARIO = "ACTIVAR_USUARIO";
    public static final String DESACTIVAR_USUARIO = "DESACTIVAR_USUARIO";

    // Roles y permisos
    public static final String CREAR_ROL = "CREAR_ROL";
    public static final String EDITAR_ROL = "EDITAR_ROL";
    public static final String ELIMINAR_ROL = "ELIMINAR_ROL";
    public static final String ASIGNAR_PERMISO = "ASIGNAR_PERMISO";
    public static final String QUITAR_PERMISO = "QUITAR_PERMISO";

    // Departamentos
    public static final String CREAR_DEPARTAMENTO = "CREAR_DEPARTAMENTO";
    public static final String EDITAR_DEPARTAMENTO = "EDITAR_DEPARTAMENTO";
    public static final String ELIMINAR_DEPARTAMENTO = "ELIMINAR_DEPARTAMENTO";

    // Políticas
    public static final String CREAR_POLITICA = "CREAR_POLITICA";
    public static final String EDITAR_POLITICA = "EDITAR_POLITICA";
    public static final String ACTIVAR_POLITICA = "ACTIVAR_POLITICA";
    public static final String DESACTIVAR_POLITICA = "DESACTIVAR_POLITICA";
    public static final String ELIMINAR_POLITICA = "ELIMINAR_POLITICA";

    // Workflow
    public static final String CREAR_ACTIVIDAD = "CREAR_ACTIVIDAD";
    public static final String EDITAR_ACTIVIDAD = "EDITAR_ACTIVIDAD";
    public static final String ELIMINAR_ACTIVIDAD = "ELIMINAR_ACTIVIDAD";
    public static final String CREAR_TRANSICION = "CREAR_TRANSICION";
    public static final String EDITAR_TRANSICION = "EDITAR_TRANSICION";
    public static final String ELIMINAR_TRANSICION = "ELIMINAR_TRANSICION";
    public static final String GUARDAR_WORKFLOW = "GUARDAR_WORKFLOW";

    // Formularios
    public static final String CREAR_FORMULARIO = "CREAR_FORMULARIO";
    public static final String EDITAR_FORMULARIO = "EDITAR_FORMULARIO";
    public static final String ELIMINAR_FORMULARIO = "ELIMINAR_FORMULARIO";
    public static final String CREAR_CAMPO = "CREAR_CAMPO";
    public static final String EDITAR_CAMPO = "EDITAR_CAMPO";
    public static final String ELIMINAR_CAMPO = "ELIMINAR_CAMPO";

    // Trámites
    public static final String INICIAR_TRAMITE = "INICIAR_TRAMITE";
    public static final String CANCELAR_TRAMITE = "CANCELAR_TRAMITE";
    public static final String ELIMINAR_TRAMITE = "ELIMINAR_TRAMITE";
    public static final String TOMAR_TAREA = "TOMAR_TAREA";
    public static final String COMPLETAR_ACTIVIDAD = "COMPLETAR_ACTIVIDAD";
    public static final String REASIGNAR_TAREA = "REASIGNAR_TAREA";

    // IA
    public static final String GENERAR_WORKFLOW_IA = "GENERAR_WORKFLOW_IA";
    public static final String ASISTENCIA_FORMULARIO_IA = "ASISTENCIA_FORMULARIO_IA";
    public static final String AI_TASK_ASSISTANT_USED = "AI_TASK_ASSISTANT_USED";
    public static final String AGENT_REQUESTED = "AGENT_REQUESTED";
    public static final String AGENT_POLICY_RECOMMENDED = "AGENT_POLICY_RECOMMENDED";
    public static final String AGENT_TRAMITE_STARTED = "AGENT_TRAMITE_STARTED";
    public static final String ANALYTICS_REPORT_REQUESTED = "ANALYTICS_REPORT_REQUESTED";
    public static final String ANALYTICS_RISK_ANALYZED = "ANALYTICS_RISK_ANALYZED";
    public static final String ANALYTICS_RECOMMENDATION_GENERATED = "ANALYTICS_RECOMMENDATION_GENERATED";
    public static final String OFFLINE_DATA_STORED = "OFFLINE_DATA_STORED";
    public static final String OFFLINE_SYNC_COMPLETED = "OFFLINE_SYNC_COMPLETED";

    // Colaboración
    public static final String ABRIR_WORKFLOW_COLABORATIVO = "ABRIR_WORKFLOW_COLABORATIVO";
    public static final String MODIFICAR_WORKFLOW_COLABORATIVO = "MODIFICAR_WORKFLOW_COLABORATIVO";

    // Documentos (CU17)
    public static final String DOCUMENT_UPLOADED = "DOCUMENT_UPLOADED";
    public static final String DOCUMENT_DOWNLOADED = "DOCUMENT_DOWNLOADED";
    public static final String DOCUMENT_DELETED = "DOCUMENT_DELETED";

    // Colaboración documental (CU19)
    public static final String DOCUMENT_OPENED = "DOCUMENT_OPENED";
    public static final String DOCUMENT_VIEWED = "DOCUMENT_VIEWED";
    public static final String DOCUMENT_LOCK_ACQUIRED = "DOCUMENT_LOCK_ACQUIRED";
    public static final String DOCUMENT_LOCK_RELEASED = "DOCUMENT_LOCK_RELEASED";
    public static final String DOCUMENT_EDIT_STARTED = "DOCUMENT_EDIT_STARTED";
    public static final String DOCUMENT_EDIT_SAVED = "DOCUMENT_EDIT_SAVED";
    public static final String DOCUMENT_VERSION_UPLOADED = "DOCUMENT_VERSION_UPLOADED";
    public static final String DOCUMENT_PERMISSION_GRANTED = "DOCUMENT_PERMISSION_GRANTED";
    public static final String DOCUMENT_PERMISSION_CHANGED = "DOCUMENT_PERMISSION_CHANGED";
    public static final String DOCUMENT_PERMISSION_DENIED = "DOCUMENT_PERMISSION_DENIED";
    public static final String DOCUMENT_PERMISSION_REMOVED = "DOCUMENT_PERMISSION_REMOVED";
}
