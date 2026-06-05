package com.workflow.politicas.workflow.cycle1;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Tipos de responsable en swimlanes / actividades UML (Ciclo 1).
 * Alineado a {@link com.workflow.politicas.model.WorkflowActivity#responsibleType}.
 */
public enum WorkflowResponsibleType {

    ROLE("Rol del sistema"),
    DEPARTMENT("Departamento"),
    USER("Usuario específico");

    private static final Set<String> KNOWN = Set.of("ROLE", "DEPARTMENT", "USER");

    private final String displayLabel;

    WorkflowResponsibleType(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getCode() {
        return name();
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public static Optional<WorkflowResponsibleType> fromCode(String code) {
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
}
