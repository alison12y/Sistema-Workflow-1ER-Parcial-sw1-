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

    // Colaboración
    public static final String ABRIR_WORKFLOW_COLABORATIVO = "ABRIR_WORKFLOW_COLABORATIVO";
    public static final String MODIFICAR_WORKFLOW_COLABORATIVO = "MODIFICAR_WORKFLOW_COLABORATIVO";
}
