package com.workflow.politicas.config;

import com.workflow.politicas.security.SystemPermissions;

import java.util.List;
import java.util.Set;

/**
 * Datos canónicos Usados por DatabaseSeeder y Phase1MigrationService.
 */
public final class Phase1BootstrapData {

    private Phase1BootstrapData() {
    }

    public static final List<RoleSeed> ROLES = List.of(
            new RoleSeed(
                    "Administrador del sistema",
                    "Acceso completo a usuarios, roles, configuración y administración general",
                    SystemPermissions.ALL
            ),
            new RoleSeed(
                    "Dueño de proceso",
                    "Gestiona políticas de negocio y diseño de workflows de su área",
                    Set.of(
                            SystemPermissions.POLICIES_MANAGE,
                            SystemPermissions.WORKFLOW_MANAGE,
                            SystemPermissions.WORKFLOW_DESIGN,
                            SystemPermissions.FORMS_MANAGE,
                            SystemPermissions.REPORTS_VIEW,
                            SystemPermissions.MONITORING_VIEW,
                            SystemPermissions.KPI_VIEW,
                            SystemPermissions.TASKS_EXECUTE,
                            SystemPermissions.DOCUMENTS_VIEW,
                            SystemPermissions.DOCUMENTS_UPLOAD,
                            SystemPermissions.DOCUMENTS_DELETE,
                            SystemPermissions.AI_AGENT_USE,
                            SystemPermissions.INTELLIGENT_ANALYTICS_VIEW
                    )
            ),
            new RoleSeed(
                    "Funcionario",
                    "Ejecuta tareas asignadas dentro de los procesos de negocio",
                    Set.of(
                            SystemPermissions.TASKS_EXECUTE,
                            SystemPermissions.DOCUMENTS_VIEW,
                            SystemPermissions.DOCUMENTS_UPLOAD,
                            SystemPermissions.AI_AGENT_USE
                    )
            ),
            new RoleSeed(
                    "Supervisor",
                    "Supervisa trámites, monitoreo e indicadores de su equipo",
                    Set.of(
                            SystemPermissions.TASKS_EXECUTE,
                            SystemPermissions.MONITORING_VIEW,
                            SystemPermissions.KPI_VIEW,
                            SystemPermissions.REPORTS_VIEW,
                            SystemPermissions.AUDIT_VIEW,
                            SystemPermissions.WORKFLOW_VIEW,
                            SystemPermissions.DOCUMENTS_VIEW,
                            SystemPermissions.INTELLIGENT_ANALYTICS_VIEW
                    )
            ),
            new RoleSeed(
                    "Atención al cliente",
                    "Atiende solicitudes y gestiona trámites de clientes",
                    Set.of(
                            SystemPermissions.TASKS_EXECUTE,
                            SystemPermissions.REPORTS_VIEW,
                            SystemPermissions.MONITORING_VIEW,
                            SystemPermissions.WORKFLOW_VIEW,
                            SystemPermissions.DOCUMENTS_VIEW,
                            SystemPermissions.DOCUMENTS_UPLOAD,
                            SystemPermissions.AI_AGENT_USE,
                            SystemPermissions.AI_ASSIST
                    )
            ),
            new RoleSeed(
                    "Técnico",
                    "Configura formularios y apoya la ejecución técnica de procesos",
                    Set.of(SystemPermissions.TASKS_EXECUTE, SystemPermissions.FORMS_MANAGE)
            ),
            new RoleSeed(
                    "Legal",
                    "Revisa políticas, auditoría y cumplimiento normativo",
                    Set.of(
                            SystemPermissions.POLICIES_MANAGE,
                            SystemPermissions.AUDIT_VIEW,
                            SystemPermissions.REPORTS_VIEW,
                            SystemPermissions.TASKS_EXECUTE,
                            SystemPermissions.DOCUMENTS_VIEW,
                            SystemPermissions.DOCUMENTS_DELETE
                    )
            )
    );

    public static final List<DepartmentSeed> DEPARTMENTS = List.of(
            new DepartmentSeed("Tecnología e Información", "Departamento de sistemas y soporte tecnológico"),
            new DepartmentSeed("Recursos Humanos", "Gestión de personal, permisos y vacaciones"),
            new DepartmentSeed("Operaciones", "Procesos operativos y atención interna"),
            new DepartmentSeed("Dirección General", "Dirección estratégica y aprobaciones finales")
    );

    public static final List<UserSeed> USERS = List.of(
            new UserSeed(
                    "sistema.admin",
                    "Admin.Sistema2024!",
                    "María Elena Vargas",
                    "maria.vargas@empresa.com",
                    "Administrador del sistema",
                    "Tecnología e Información"
            ),
            new UserSeed(
                    "carlos.mendoza",
                    "Carlos.M2024!",
                    "Carlos Mendoza Ríos",
                    "carlos.mendoza@empresa.com",
                    "Dueño de proceso",
                    "Tecnología e Información"
            ),
            new UserSeed(
                    "ana.rodriguez",
                    "Ana.R2024!",
                    "Ana Rodríguez Paz",
                    "ana.rodriguez@empresa.com",
                    "Funcionario",
                    "Operaciones"
            ),
            new UserSeed(
                    "luis.supervisor",
                    "Luis.S2024!",
                    "Luis Herrera Mora",
                    "luis.supervisor@empresa.com",
                    "Supervisor",
                    "Operaciones"
            ),
            new UserSeed(
                    "patricia.cliente",
                    "Patricia.C2024!",
                    "Patricia Solís Vega",
                    "patricia.cliente@empresa.com",
                    "Atención al cliente",
                    "Operaciones"
            )
    );

    public record RoleSeed(String name, String description, Set<String> permissions) {
    }

    public record DepartmentSeed(String name, String description) {
    }

    public record UserSeed(
            String username,
            String rawPassword,
            String fullName,
            String email,
            String roleName,
            String departmentName
    ) {
    }
}
