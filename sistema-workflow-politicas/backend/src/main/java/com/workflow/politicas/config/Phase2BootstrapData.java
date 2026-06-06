package com.workflow.politicas.config;

import java.util.List;

public final class Phase2BootstrapData {

    private Phase2BootstrapData() {
    }

    public record PolicySeed(
            String name,
            String description,
            String type,
            String status,
            String responsible,
            String createdBy
    ) {
    }

    public static final List<PolicySeed> SAMPLE_POLICIES = List.of(
            new PolicySeed(
                    "Solicitud de instalación de medidor",
                    "Gestiona el flujo de solicitud, revisión técnica y aprobación para instalación de medidores.",
                    "GENERAL_REQUEST",
                    "ACTIVE",
                    "Dueño de proceso — Operaciones",
                    "carlos.mendoza"
            ),
            new PolicySeed(
                    "Reclamo de servicio",
                    "Define el proceso de registro, atención y seguimiento de reclamos de clientes.",
                    "CLAIM_ATTENTION",
                    "ACTIVE",
                    "Atención al cliente",
                    "carlos.mendoza"
            ),
            new PolicySeed(
                    "Solicitud de aprobación interna",
                    "Permite solicitar y aprobar documentos o decisiones internas entre áreas.",
                    "DOCUMENT_APPROVAL",
                    "DRAFT",
                    "Dueño de proceso — Dirección",
                    "carlos.mendoza"
            ),
            new PolicySeed(
                    "Solicitud de revisión documental",
                    "Establece el flujo de carga, revisión legal y validación de documentación.",
                    "DOCUMENT_APPROVAL",
                    "DRAFT",
                    "Legal",
                    "carlos.mendoza"
            ),
            new PolicySeed(
                    "Gestión de bienes decomisados",
                    "Administra el registro, custodia, evaluación y disposición de bienes decomisados.",
                    "ASSET_MANAGEMENT",
                    "ACTIVE",
                    "Operaciones",
                    "carlos.mendoza"
            ),
            new PolicySeed(
                    "Política IA",
                    "Gestiona solicitudes de automatización, asistencia inteligente y casos de uso con IA.",
                    "AI_POLICY",
                    "ACTIVE",
                    "Tecnología e Información",
                    "carlos.mendoza"
            )
    );
}
