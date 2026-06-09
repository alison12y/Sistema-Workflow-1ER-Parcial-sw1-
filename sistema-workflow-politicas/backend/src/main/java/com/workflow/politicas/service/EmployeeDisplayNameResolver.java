package com.workflow.politicas.service;

import com.workflow.politicas.dto.KpiLoadMetricDto;
import com.workflow.politicas.model.User;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resuelve el nombre visible de un funcionario evitando usar etiquetas de rol (p. ej. "Funcionario").
 */
public final class EmployeeDisplayNameResolver {

    private static final Set<String> GENERIC_LABELS = Set.of(
            "funcionario",
            "supervisor",
            "sin asignar",
            "sin departamento",
            "sin datos"
    );

    private EmployeeDisplayNameResolver() {
    }

    public static String fromUser(User user) {
        if (user == null) {
            return null;
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return null;
    }

    public static boolean isRoleLabel(String value, Collection<String> knownRoleNames) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalize(value);
        if (GENERIC_LABELS.contains(normalized)) {
            return true;
        }
        if (knownRoleNames == null || knownRoleNames.isEmpty()) {
            return false;
        }
        return knownRoleNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(EmployeeDisplayNameResolver::normalize)
                .anyMatch(normalized::equals);
    }

    /**
     * Prioridad: displayName válido → fullName vía username (key) → username → null.
     */
    public static String resolvePersonLabel(
            KpiLoadMetricDto load,
            Map<String, User> usersByUsername,
            Collection<String> knownRoleNames
    ) {
        if (load == null) {
            return null;
        }

        if (load.getDisplayName() != null && !load.getDisplayName().isBlank()
                && !isRoleLabel(load.getDisplayName(), knownRoleNames)) {
            return load.getDisplayName().trim();
        }

        if (load.getKey() != null && !load.getKey().isBlank() && usersByUsername != null) {
            User user = usersByUsername.get(load.getKey().trim().toLowerCase(Locale.ROOT));
            String fromUser = fromUser(user);
            if (fromUser != null && !isRoleLabel(fromUser, knownRoleNames)) {
                return fromUser;
            }
            if (!isRoleLabel(load.getKey(), knownRoleNames)) {
                return load.getKey().trim();
            }
        }

        return null;
    }

    public static boolean isRankableMetric(
            KpiLoadMetricDto load,
            Collection<String> knownRoleNames,
            Map<String, User> usersByUsername
    ) {
        String person = resolvePersonLabel(load, usersByUsername, knownRoleNames);
        return person != null && !isRoleLabel(person, knownRoleNames);
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").replaceAll("\\s+", " ");
    }
}
