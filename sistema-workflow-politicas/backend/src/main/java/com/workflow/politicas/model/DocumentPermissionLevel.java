package com.workflow.politicas.model;

import java.util.Locale;

public enum DocumentPermissionLevel {
    READ,
    EDIT,
    ADMIN;

    public boolean satisfies(DocumentPermissionLevel required) {
        return ordinal() >= required.ordinal();
    }

    public static DocumentPermissionLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Nivel de permiso requerido");
        }
        return DocumentPermissionLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public static DocumentPermissionLevel max(DocumentPermissionLevel a, DocumentPermissionLevel b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
