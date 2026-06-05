package com.workflow.politicas.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nombres técnicos de campos de formulario y variables en condiciones del workflow.
 */
public final class FormFieldKeyUtil {

    public static final Pattern TECHNICAL_NAME = Pattern.compile("^[a-z][a-z0-9_]*$");
    private static final Pattern CONDITION_VARIABLE = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s*(==|!=|>=|<=|>|<)"
    );

    private FormFieldKeyUtil() {
    }

    public static String generateFromLabel(String label) {
        if (label == null || label.isBlank()) {
            return "campo";
        }
        String base = Normalizer.normalize(label.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return base.isBlank() ? "campo" : base;
    }

    public static String normalizeTechnicalName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static void validateTechnicalName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre técnico del campo es obligatorio");
        }
        String normalized = normalizeTechnicalName(name);
        if (normalized.contains(" ")) {
            throw new IllegalArgumentException("El nombre técnico no puede contener espacios");
        }
        if (!TECHNICAL_NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "El nombre técnico solo puede usar letras minúsculas, números y guion bajo (ej.: valido)");
        }
    }

    public static String resolveTechnicalName(String requestedName, String label, java.util.function.Predicate<String> nameTaken) {
        String candidate;
        if (requestedName != null && !requestedName.isBlank()) {
            candidate = normalizeTechnicalName(requestedName);
            validateTechnicalName(candidate);
        } else {
            candidate = generateFromLabel(label);
            int suffix = 1;
            String base = candidate;
            while (nameTaken.test(candidate)) {
                suffix++;
                candidate = base + "_" + suffix;
            }
        }
        if (nameTaken.test(candidate)) {
            throw new IllegalArgumentException("El nombre técnico ya existe en el formulario: " + candidate);
        }
        return candidate;
    }

    public static List<String> extractVariablesFromCondition(String expression) {
        if (expression == null || expression.isBlank()) {
            return List.of();
        }
        Set<String> variables = new LinkedHashSet<>();
        Matcher matcher = CONDITION_VARIABLE.matcher(expression.trim());
        while (matcher.find()) {
            variables.add(matcher.group(1).trim().toLowerCase(Locale.ROOT));
        }
        return new ArrayList<>(variables);
    }
}
