package com.workflow.politicas.service;

import com.workflow.politicas.dto.SmartAgentAnalyzeResponse;
import com.workflow.politicas.dto.SmartAgentSuggestedField;
import com.workflow.politicas.model.BusinessPolicy;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Motor local de recomendación de políticas (CU22 fallback) por palabras clave y coincidencia textual.
 */
public final class SmartAgentFallbackMatcher {

    private static final Map<String, List<String>> INTENT_KEYWORDS = Map.of(
            "INSTALACION_MEDIDOR", List.of("medidor", "instalacion", "instalar", "suministro", "electricidad", "gas", "contador"),
            "SOLICITUD_VACACIONES", List.of("vacaciones", "vacacion", "licencia anual", "descanso", "dias libres", "días libres", "ausencia planificada"),
            "PERMISO_LABORAL", List.of("permiso laboral", "permiso personal", "licencia medica", "ausencia justificada", "permiso especial"),
            "GESTION_DECOMISADOS", List.of("decomisado", "decomisados", "bienes", "confiscado", "incautacion", "incautados", "comiso"),
            "RECLAMO_SERVICIO", List.of("reclamo", "queja", "mal servicio", "defecto", "insatisfecho", "reclamar"),
            "REVISION_DOCUMENTAL", List.of("documento", "revision", "revisar", "aprobacion", "validar", "legal", "contrato"),
            "POLITICA_IA", List.of("inteligencia artificial", "ia", "chatbot", "asistente", "automatizar", "machine learning"),
            "SOLICITUD_GENERAL", List.of("solicitud", "tramite", "pedido", "requiero", "necesito", "ayuda")
    );

    private SmartAgentFallbackMatcher() {
    }

    public static SmartAgentAnalyzeResponse match(
            String combinedText,
            List<BusinessPolicy> activePolicies,
            String requesterName,
            String attachmentFileName
    ) {
        SmartAgentAnalyzeResponse response = new SmartAgentAnalyzeResponse();
        response.setSource("LOCAL_FALLBACK");

        if (activePolicies == null || activePolicies.isEmpty()) {
            response.setDetectedIntent("SIN_POLITICAS");
            response.setConfidenceScore(0.0);
            response.setExplanation("No hay políticas activas disponibles para recomendar.");
            response.getWarnings().add("Configure al menos una política activa en el sistema.");
            return response;
        }

        String normalized = normalize(combinedText);
        String intent = detectIntent(normalized);
        response.setDetectedIntent(intent);

        ScoredPolicy best = scorePolicies(normalized, intent, activePolicies)
                .stream()
                .max(Comparator.comparingDouble(ScoredPolicy::score))
                .orElse(new ScoredPolicy(activePolicies.get(0), 0.25, "Coincidencia por defecto"));

        BusinessPolicy policy = best.policy();
        response.setRecommendedPolicyId(policy.getId());
        response.setRecommendedPolicyName(policy.getName());
        response.setConfidenceScore(Math.min(0.95, Math.max(0.30, best.score())));
        response.setExplanation(buildExplanation(policy, intent, best.reason(), attachmentFileName));
        response.setRequiredDocuments(buildRequiredDocuments(intent, policy));
        response.setSuggestedFields(buildSuggestedFields(combinedText, requesterName));
        response.setAttachmentFileName(attachmentFileName);

        if (best.score() < 0.45) {
            response.getWarnings().add("La confianza es baja; verifique la recomendación antes de iniciar el trámite.");
        }
        if (attachmentFileName != null && !attachmentFileName.isBlank()) {
            response.getWarnings().add("Se consideró el documento adjunto: " + attachmentFileName);
        }

        return response;
    }

    private static String detectIntent(String normalizedText) {
        String bestIntent = "SOLICITUD_GENERAL";
        int bestHits = 0;
        for (Map.Entry<String, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            int hits = 0;
            for (String keyword : entry.getValue()) {
                if (normalizedText.contains(normalize(keyword))) {
                    hits++;
                }
            }
            if (hits > bestHits) {
                bestHits = hits;
                bestIntent = entry.getKey();
            }
        }
        return bestIntent;
    }

    private static List<ScoredPolicy> scorePolicies(String normalizedText, String intent, List<BusinessPolicy> policies) {
        List<ScoredPolicy> scored = new ArrayList<>();
        List<String> intentKeywords = INTENT_KEYWORDS.getOrDefault(intent, List.of());

        for (BusinessPolicy policy : policies) {
            double score = 0.20;
            StringBuilder reason = new StringBuilder();

            String policyText = normalize(
                    Optional.ofNullable(policy.getName()).orElse("")
                            + " "
                            + Optional.ofNullable(policy.getDescription()).orElse("")
                            + " "
                            + Optional.ofNullable(policy.getType()).orElse("")
            );

            for (String keyword : intentKeywords) {
                String nk = normalize(keyword);
                if (policyText.contains(nk)) {
                    score += 0.18;
                    reason.append("política contiene '").append(keyword).append("'; ");
                }
                if (normalizedText.contains(nk)) {
                    score += 0.08;
                }
            }

            for (String token : normalizedText.split("\\s+")) {
                if (token.length() >= 4 && policyText.contains(token)) {
                    score += 0.05;
                    reason.append("coincidencia '").append(token).append("'; ");
                }
            }

            if (reason.isEmpty()) {
                reason.append("mejor coincidencia disponible entre políticas activas");
            }
            scored.add(new ScoredPolicy(policy, score, reason.toString().trim()));
        }
        return scored;
    }

    private static String buildExplanation(BusinessPolicy policy, String intent, String reason, String attachmentFileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Se recomienda «").append(policy.getName()).append("» porque ");
        sb.append(reason).append(". ");
        sb.append("Intención detectada: ").append(humanIntent(intent)).append(".");
        if (attachmentFileName != null && !attachmentFileName.isBlank()) {
            sb.append(" El archivo «").append(attachmentFileName).append("» refuerza el contexto documental.");
        }
        if (policy.getDescription() != null && !policy.getDescription().isBlank()) {
            sb.append(" ").append(policy.getDescription());
        }
        return sb.toString().trim();
    }

    private static List<String> buildRequiredDocuments(String intent, BusinessPolicy policy) {
        List<String> docs = new ArrayList<>(switch (intent) {
            case "INSTALACION_MEDIDOR" -> List.of(
                    "Documento de identidad",
                    "Comprobante de domicilio",
                    "Plano o croquis del punto de instalación (opcional)"
            );
            case "SOLICITUD_VACACIONES" -> List.of(
                    "Formulario de solicitud de vacaciones firmado",
                    "Cronograma de actividades delegadas (opcional)"
            );
            case "PERMISO_LABORAL" -> List.of(
                    "Justificativo o certificado correspondiente",
                    "Aprobación previa del jefe inmediato"
            );
            case "GESTION_DECOMISADOS" -> List.of(
                    "Acta o informe de decomiso",
                    "Inventario de bienes",
                    "Documento de identidad del solicitante"
            );
            case "RECLAMO_SERVICIO" -> List.of(
                    "Descripción detallada del reclamo",
                    "Evidencia fotográfica o comprobantes (opcional)"
            );
            case "REVISION_DOCUMENTAL" -> List.of(
                    "Documento a revisar",
                    "Carta o memo de solicitud (opcional)"
            );
            case "POLITICA_IA" -> List.of(
                    "Descripción del caso de uso",
                    "Documentación técnica de referencia (opcional)"
            );
            default -> List.of(
                    "Documento de soporte (opcional)",
                    "Descripción de la solicitud"
            );
        });
        docs.add("Política: " + policy.getName());
        return docs;
    }

    private static List<SmartAgentSuggestedField> buildSuggestedFields(String combinedText, String requesterName) {
        List<SmartAgentSuggestedField> fields = new ArrayList<>();
        fields.add(new SmartAgentSuggestedField(
                "description",
                "Descripción de la solicitud",
                "TEXTAREA",
                true,
                combinedText != null && combinedText.length() > 500
                        ? combinedText.substring(0, 500) + "..."
                        : combinedText
        ));
        fields.add(new SmartAgentSuggestedField(
                "requestedBy",
                "Solicitante",
                "TEXT",
                true,
                requesterName
        ));
        fields.add(new SmartAgentSuggestedField(
                "priority",
                "Prioridad",
                "SELECT",
                false,
                "NORMAL"
        ));
        return fields;
    }

    private static String humanIntent(String intent) {
        return switch (intent) {
            case "INSTALACION_MEDIDOR" -> "instalación de medidor";
            case "SOLICITUD_VACACIONES" -> "solicitud de vacaciones";
            case "PERMISO_LABORAL" -> "permiso laboral";
            case "GESTION_DECOMISADOS" -> "gestión de bienes decomisados";
            case "RECLAMO_SERVICIO" -> "reclamo de servicio";
            case "REVISION_DOCUMENTAL" -> "revisión documental";
            case "POLITICA_IA" -> "solicitud relacionada con IA";
            default -> "solicitud general";
        };
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lowered = value.toLowerCase(Locale.ROOT).trim();
        String normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").replaceAll("\\s+", " ");
    }

    private record ScoredPolicy(BusinessPolicy policy, double score, String reason) {
    }
}
