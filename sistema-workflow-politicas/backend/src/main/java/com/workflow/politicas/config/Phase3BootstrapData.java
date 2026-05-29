package com.workflow.politicas.config;

import java.util.List;

public final class Phase3BootstrapData {

    private Phase3BootstrapData() {
    }

    public record ActivitySeed(
            String name,
            String description,
            String responsibleType,
            String responsibleName,
            String activityType,
            int orderIndex,
            int estimatedTimeHours,
            String status
    ) {
    }

    public static final String METER_POLICY_NAME = "Solicitud de instalación de medidor";

    public static final List<ActivitySeed> METER_INSTALLATION_ACTIVITIES = List.of(
            new ActivitySeed(
                    "Recepción de solicitud",
                    "Registro inicial de la solicitud del cliente y validación de datos básicos.",
                    "ROLE",
                    "Atención al cliente",
                    "START",
                    1,
                    2,
                    "ACTIVA"
            ),
            new ActivitySeed(
                    "Revisión de requisitos",
                    "Verificación de documentación y requisitos mínimos para la instalación.",
                    "ROLE",
                    "Atención al cliente",
                    "TASK",
                    2,
                    4,
                    "ACTIVA"
            ),
            new ActivitySeed(
                    "Validación técnica",
                    "Evaluación técnica de viabilidad e inspección del punto de instalación.",
                    "ROLE",
                    "Técnico",
                    "TASK",
                    3,
                    24,
                    "ACTIVA"
            ),
            new ActivitySeed(
                    "Revisión legal",
                    "Revisión de aspectos normativos y documentación legal aplicable.",
                    "ROLE",
                    "Legal",
                    "TASK",
                    4,
                    24,
                    "ACTIVA"
            ),
            new ActivitySeed(
                    "Aprobación final",
                    "Aprobación del supervisor y cierre del proceso de solicitud.",
                    "ROLE",
                    "Supervisor",
                    "END",
                    5,
                    8,
                    "ACTIVA"
            )
    );
}
