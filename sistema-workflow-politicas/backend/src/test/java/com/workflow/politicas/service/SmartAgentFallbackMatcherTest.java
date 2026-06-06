package com.workflow.politicas.service;

import com.workflow.politicas.dto.SmartAgentAnalyzeResponse;
import com.workflow.politicas.model.BusinessPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartAgentFallbackMatcherTest {

    @Test
    void match_medidorIntent_recommendsMeterPolicyWithHigherConfidence() {
        List<BusinessPolicy> policies = List.of(
                policy("p-medidor", "Solicitud de instalación de medidor", "Flujo para medidores de gas y electricidad", "ACTIVE"),
                policy("p-reclamo", "Reclamo de servicio", "Atención de quejas", "ACTIVE")
        );

        SmartAgentAnalyzeResponse response = SmartAgentFallbackMatcher.match(
                "Necesito instalar un medidor nuevo en mi domicilio",
                policies,
                "Alison",
                null
        );

        assertEquals("LOCAL_FALLBACK", response.getSource());
        assertEquals("INSTALACION_MEDIDOR", response.getDetectedIntent());
        assertEquals("p-medidor", response.getRecommendedPolicyId());
        assertEquals("Solicitud de instalación de medidor", response.getRecommendedPolicyName());
        assertNotNull(response.getConfidenceScore());
        assertTrue(response.getConfidenceScore() >= 0.45);
        assertTrue(response.getExplanation().toLowerCase().contains("medidor"));
    }

    @Test
    void match_decomisadosIntent_recommendsAssetPolicy() {
        List<BusinessPolicy> policies = List.of(
                policy("p-decomisados", "Gestión de bienes decomisados", "Custodia y disposición de bienes incautados", "ACTIVE"),
                policy("p-medidor", "Solicitud de instalación de medidor", "Medidores", "ACTIVE")
        );

        SmartAgentAnalyzeResponse response = SmartAgentFallbackMatcher.match(
                "Quiero registrar bienes decomisados del operativo",
                policies,
                "Carlos",
                "acta_decomiso.pdf"
        );

        assertEquals("GESTION_DECOMISADOS", response.getDetectedIntent());
        assertEquals("p-decomisados", response.getRecommendedPolicyId());
        assertEquals("Gestión de bienes decomisados", response.getRecommendedPolicyName());
        assertTrue(response.getRequiredDocuments().stream().anyMatch(d -> d.toLowerCase().contains("decomiso")));
        assertTrue(response.getWarnings().stream().anyMatch(w -> w.contains("acta_decomiso.pdf")));
    }

    @Test
    void match_genericQuery_returnsGeneralIntentAndPolicy() {
        List<BusinessPolicy> policies = List.of(
                policy("p-general", "Solicitud de aprobación interna", "Solicitudes internas entre áreas", "ACTIVE"),
                policy("p-ia", "Política IA", "Casos de uso con inteligencia artificial", "ACTIVE")
        );

        SmartAgentAnalyzeResponse response = SmartAgentFallbackMatcher.match(
                "Necesito ayuda con una solicitud general",
                policies,
                null,
                null
        );

        assertEquals("SOLICITUD_GENERAL", response.getDetectedIntent());
        assertNotNull(response.getRecommendedPolicyId());
        assertNotNull(response.getConfidenceScore());
        assertTrue(response.getConfidenceScore() >= 0.30);
        assertTrue(response.getSuggestedFields().stream().anyMatch(f -> "description".equals(f.getName())));
    }

    @Test
    void match_onlyActivePoliciesInInput_draftPolicyNeverRecommended() {
        BusinessPolicy draft = policy("p-draft", "Borrador confidencial", "No debe recomendarse", "DRAFT");
        List<BusinessPolicy> activeOnly = List.of(
                policy("p-active", "Reclamo de servicio", "Gestión de reclamos de clientes", "ACTIVE")
        );

        SmartAgentAnalyzeResponse response = SmartAgentFallbackMatcher.match(
                "Tengo un reclamo por mal servicio",
                activeOnly,
                null,
                null
        );

        assertEquals("p-active", response.getRecommendedPolicyId());
        assertTrue(activeOnly.stream().noneMatch(p -> "DRAFT".equalsIgnoreCase(p.getStatus())));
        assertEquals("RECLAMO_SERVICIO", response.getDetectedIntent());

        List<BusinessPolicy> mixedIfPassedByMistake = List.of(draft, activeOnly.get(0));
        SmartAgentAnalyzeResponse mixed = SmartAgentFallbackMatcher.match(
                "reclamo por mal servicio",
                mixedIfPassedByMistake,
                null,
                null
        );
        assertEquals("p-active", mixed.getRecommendedPolicyId());
    }

    @Test
    void match_scoringProducesBoundedConfidence() {
        List<BusinessPolicy> policies = List.of(
                policy("p-weak", "Política genérica", "Procesos varios", "ACTIVE"),
                policy("p-strong", "Solicitud de instalación de medidor", "Instalación de medidores de suministro", "ACTIVE")
        );

        SmartAgentAnalyzeResponse strong = SmartAgentFallbackMatcher.match(
                "instalar medidor de gas en mi casa",
                policies,
                null,
                null
        );
        SmartAgentAnalyzeResponse weak = SmartAgentFallbackMatcher.match(
                "hola",
                policies,
                null,
                null
        );

        assertTrue(strong.getConfidenceScore() > weak.getConfidenceScore());
        assertTrue(strong.getConfidenceScore() <= 0.95);
        assertTrue(weak.getConfidenceScore() >= 0.30);
        assertTrue(strong.getConfidenceScore() >= 0.45);
    }

    @Test
    void match_noActivePolicies_returnsZeroConfidence() {
        SmartAgentAnalyzeResponse response = SmartAgentFallbackMatcher.match(
                "cualquier texto",
                List.of(),
                null,
                null
        );

        assertEquals("SIN_POLITICAS", response.getDetectedIntent());
        assertEquals(0.0, response.getConfidenceScore());
        assertTrue(response.getWarnings().stream().anyMatch(w -> w.toLowerCase().contains("política")));
    }

    private static BusinessPolicy policy(String id, String name, String description, String status) {
        BusinessPolicy policy = new BusinessPolicy();
        policy.setId(id);
        policy.setName(name);
        policy.setDescription(description);
        policy.setStatus(status);
        return policy;
    }
}
