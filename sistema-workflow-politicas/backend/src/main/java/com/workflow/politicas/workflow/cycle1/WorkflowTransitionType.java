package com.workflow.politicas.workflow.cycle1;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Tipos de arista UML soportados por el modelo oficial del Ciclo 1.
 * Alineado a {@link com.workflow.politicas.model.WorkflowTransition#transitionType}.
 * <p>
 * La resolución automática en runtime se implementará en F1 ({@link Cycle1WorkflowModel#ROUTING_SERVICE_PLANNED}).
 * </p>
 */
public enum WorkflowTransitionType {

    SEQUENTIAL("Secuencial"),
    CONDITIONAL("Condicional"),
    ITERATIVE("Iterativa"),
    PARALLEL_SPLIT("División paralela"),
    PARALLEL_JOIN("Unión paralela");

    private static final Set<String> KNOWN = Set.of(
            "SEQUENTIAL", "CONDITIONAL", "ITERATIVE", "PARALLEL_SPLIT", "PARALLEL_JOIN"
    );

    private final String displayLabel;

    WorkflowTransitionType(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getCode() {
        return name();
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public static Optional<WorkflowTransitionType> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(code.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static boolean isKnownCode(String code) {
        return code != null && KNOWN.contains(code.trim().toUpperCase(Locale.ROOT));
    }

    public static String defaultCode() {
        return SEQUENTIAL.getCode();
    }
}
