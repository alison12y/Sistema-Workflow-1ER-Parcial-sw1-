package com.workflow.politicas.config;

import java.util.List;

public final class Phase4BootstrapData {

    private Phase4BootstrapData() {
    }

    public record TransitionSeed(
            String fromActivityName,
            String toActivityName,
            String transitionType,
            String conditionLabel,
            int orderIndex
    ) {
    }

    public static final String METER_POLICY_NAME = Phase3BootstrapData.METER_POLICY_NAME;

    public static final List<TransitionSeed> METER_INSTALLATION_TRANSITIONS = List.of(
            new TransitionSeed("Recepción de solicitud", "Revisión de requisitos", "SEQUENTIAL", null, 1),
            new TransitionSeed("Revisión de requisitos", "Validación técnica", "SEQUENTIAL", null, 2),
            new TransitionSeed("Validación técnica", "Revisión legal", "CONDITIONAL", "Aprobado", 3),
            new TransitionSeed("Validación técnica", "Revisión de requisitos", "CONDITIONAL", "Observado", 4),
            new TransitionSeed("Revisión legal", "Aprobación final", "SEQUENTIAL", null, 5)
    );
}
