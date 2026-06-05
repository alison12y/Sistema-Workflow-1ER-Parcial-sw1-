package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.model.Role;
import com.workflow.politicas.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceAuditTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private BitacoraService bitacoraService;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleRepository, bitacoraService);
        when(bitacoraService.resolveActorDisplay()).thenReturn("Admin Test");
    }

    @Test
    void create_registersAuditEvent() {
        Role role = new Role();
        role.setName("Supervisor");
        role.setPermissionIds(Set.of("TASKS_EXECUTE"));

        Role saved = new Role();
        saved.setId("role-1");
        saved.setName("Supervisor");
        saved.setPermissionIds(Set.of("TASKS_EXECUTE"));
        when(roleRepository.save(any(Role.class))).thenReturn(saved);

        roleService.create(role);

        verify(bitacoraService).registrar(
                eq(AuditModules.ROLES),
                eq(AuditActions.CREAR_ROL),
                eq("Admin Test creó el rol Supervisor"),
                eq("Role"),
                eq("role-1")
        );
        verify(bitacoraService).registrar(
                eq(AuditModules.ROLES),
                eq(AuditActions.ASIGNAR_PERMISO),
                eq("Admin Test asignó el permiso TASKS_EXECUTE al rol Supervisor"),
                eq("Role"),
                eq("role-1")
        );
    }
}
