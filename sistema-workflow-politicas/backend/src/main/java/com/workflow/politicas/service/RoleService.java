package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.model.Role;
import com.workflow.politicas.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final BitacoraService bitacoraService;

    public RoleService(RoleRepository roleRepository, BitacoraService bitacoraService) {
        this.roleRepository = roleRepository;
        this.bitacoraService = bitacoraService;
    }

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Optional<Role> findById(String id) {
        return roleRepository.findById(id);
    }

    public Role create(Role role) {
        Role saved = roleRepository.save(role);
        String actor = bitacoraService.resolveActorDisplay();
        bitacoraService.registrar(
                AuditModules.ROLES,
                AuditActions.CREAR_ROL,
                actor + " creó el rol " + saved.getName(),
                "Role",
                saved.getId()
        );
        auditPermissionAssignments(saved, Set.of(), saved.getPermissionIds(), actor);
        return saved;
    }

    public Optional<Role> update(String id, Role roleDetails) {
        return roleRepository.findById(id).map(role -> {
            Set<String> previousPermissions = copyPermissions(role.getPermissionIds());
            role.setName(roleDetails.getName());
            role.setDescription(roleDetails.getDescription());
            role.setPermissionIds(roleDetails.getPermissionIds());
            Role saved = roleRepository.save(role);
            String actor = bitacoraService.resolveActorDisplay();
            bitacoraService.registrar(
                    AuditModules.ROLES,
                    AuditActions.EDITAR_ROL,
                    actor + " editó el rol " + saved.getName(),
                    "Role",
                    saved.getId()
            );
            auditPermissionAssignments(saved, previousPermissions, saved.getPermissionIds(), actor);
            return saved;
        });
    }

    public void deleteById(String id) {
        roleRepository.findById(id).ifPresent(role -> {
            String actor = bitacoraService.resolveActorDisplay();
            bitacoraService.registrar(
                    AuditModules.ROLES,
                    AuditActions.ELIMINAR_ROL,
                    actor + " eliminó el rol " + role.getName(),
                    "Role",
                    role.getId()
            );
            roleRepository.deleteById(id);
        });
    }

    /** Compatibilidad con código existente. */
    public Role save(Role role) {
        if (role.getId() == null || role.getId().isBlank()) {
            return create(role);
        }
        return update(role.getId(), role).orElseGet(() -> roleRepository.save(role));
    }

    private void auditPermissionAssignments(
            Role role,
            Set<String> previous,
            Set<String> current,
            String actor
    ) {
        Set<String> before = previous != null ? previous : Set.of();
        Set<String> after = current != null ? current : Set.of();

        for (String permission : after) {
            if (!before.contains(permission)) {
                bitacoraService.registrar(
                        AuditModules.ROLES,
                        AuditActions.ASIGNAR_PERMISO,
                        actor + " asignó el permiso " + permission + " al rol " + role.getName(),
                        "Role",
                        role.getId()
                );
            }
        }
        for (String permission : before) {
            if (!after.contains(permission)) {
                bitacoraService.registrar(
                        AuditModules.ROLES,
                        AuditActions.QUITAR_PERMISO,
                        actor + " quitó el permiso " + permission + " del rol " + role.getName(),
                        "Role",
                        role.getId()
                );
            }
        }
    }

    private static Set<String> copyPermissions(Set<String> permissions) {
        return permissions != null ? new HashSet<>(permissions) : new HashSet<>();
    }
}
