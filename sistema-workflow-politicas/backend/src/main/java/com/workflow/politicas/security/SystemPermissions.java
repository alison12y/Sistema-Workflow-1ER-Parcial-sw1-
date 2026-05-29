package com.workflow.politicas.security;

import java.util.Set;

/**
 * Códigos de permiso del sistema. Se almacenan en Role.permissionIds
 * y se envían al frontend tras el login para filtrar menú y rutas.
 */
public final class SystemPermissions {

    public static final String USERS_MANAGE = "USERS_MANAGE";
    public static final String ROLES_MANAGE = "ROLES_MANAGE";
    public static final String DEPARTMENTS_MANAGE = "DEPARTMENTS_MANAGE";
    public static final String POLICIES_MANAGE = "POLICIES_MANAGE";
    public static final String WORKFLOW_MANAGE = "WORKFLOW_MANAGE";
    public static final String WORKFLOW_DESIGN = "WORKFLOW_DESIGN";
    public static final String WORKFLOW_VIEW = "WORKFLOW_VIEW";
    public static final String REPORTS_VIEW = "REPORTS_VIEW";
    public static final String AUDIT_VIEW = "AUDIT_VIEW";
    public static final String KPI_VIEW = "KPI_VIEW";
    public static final String MONITORING_VIEW = "MONITORING_VIEW";
    public static final String FORMS_MANAGE = "FORMS_MANAGE";
    public static final String TASKS_EXECUTE = "TASKS_EXECUTE";
    public static final String AI_ASSIST = "AI_ASSIST";
    public static final String SETTINGS_MANAGE = "SETTINGS_MANAGE";

    public static final Set<String> ALL = Set.of(
            USERS_MANAGE,
            ROLES_MANAGE,
            DEPARTMENTS_MANAGE,
            POLICIES_MANAGE,
            WORKFLOW_MANAGE,
            WORKFLOW_DESIGN,
            WORKFLOW_VIEW,
            REPORTS_VIEW,
            AUDIT_VIEW,
            KPI_VIEW,
            MONITORING_VIEW,
            FORMS_MANAGE,
            TASKS_EXECUTE,
            AI_ASSIST,
            SETTINGS_MANAGE
    );

    private SystemPermissions() {
    }
}
