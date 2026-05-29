package com.workflow.politicas.security;

import com.workflow.politicas.model.Role;
import com.workflow.politicas.repository.RoleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resuelve referencias legacy de roles (IDs Mongo, nombres o códigos ROLE_ADMIN).
 */
public final class RoleReferenceResolver {

    private RoleReferenceResolver() {
    }

    public static Optional<Role> resolve(RoleRepository roleRepository, String roleIdOrName) {
        if (roleIdOrName == null || roleIdOrName.isBlank()) {
            return Optional.empty();
        }

        Optional<Role> byId = roleRepository.findById(roleIdOrName);
        if (byId.isPresent()) {
            return byId;
        }

        Optional<Role> byName = roleRepository.findByNameIgnoreCase(roleIdOrName);
        if (byName.isPresent()) {
            return byName;
        }

        for (String alias : legacyNameAliases(roleIdOrName)) {
            Optional<Role> match = roleRepository.findByNameIgnoreCase(alias);
            if (match.isPresent()) {
                return match;
            }
        }

        return Optional.empty();
    }

    public static List<String> legacyNameAliases(String roleIdOrName) {
        List<String> aliases = new ArrayList<>();
        String normalized = roleIdOrName.toUpperCase()
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace("Ñ", "N")
                .replace(" ", "_")
                .trim();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        switch (normalized) {
            case "ADMIN", "ADMINISTRADOR", "ADMINISTRADOR_DEL_SISTEMA" -> aliases.addAll(List.of(
                    "Administrador del sistema",
                    "ADMINISTRADOR",
                    "Administrador"
            ));
            case "POLICY_DESIGNER", "DESIGNER", "DISENADOR_DE_POLITICAS", "DISENADOR" -> aliases.addAll(List.of(
                    "Dueño de proceso",
                    "Diseñador de Políticas",
                    "Diseñador de politicas"
            ));
            case "USER", "FUNCIONARIO", "USUARIO_OPERATIVO", "OFFICIAL" -> aliases.add("Funcionario");
            case "SUPERVISOR" -> aliases.add("Supervisor");
            case "PROCESS_OWNER", "DUENO_DE_PROCESO" -> aliases.add("Dueño de proceso");
            case "CUSTOMER_SERVICE", "ATENCION_AL_CLIENTE" -> aliases.add("Atención al cliente");
            case "TECHNICIAN", "TECNICO" -> aliases.add("Técnico");
            case "LEGAL" -> aliases.add("Legal");
            default -> {
            }
        }

        return aliases;
    }
}
