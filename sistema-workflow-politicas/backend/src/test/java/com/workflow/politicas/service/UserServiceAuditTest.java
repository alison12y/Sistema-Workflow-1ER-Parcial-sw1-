package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.UserRequest;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceAuditTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BitacoraService bitacoraService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, bitacoraService);
        when(bitacoraService.resolveActorDisplay()).thenReturn("Admin Test");
        when(passwordEncoder.encode(any())).thenReturn("encoded");
    }

    @Test
    void create_registersAuditEvent() {
        UserRequest request = new UserRequest();
        request.setUsername("nuevo.user");
        request.setPassword("Secret123!");
        request.setEmail("nuevo@test.com");
        request.setFullName("Nuevo Usuario");
        request.setActive(true);

        User saved = new User();
        saved.setId("user-1");
        saved.setUsername("nuevo.user");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        userService.create(request);

        verify(bitacoraService).registrar(
                eq(AuditModules.USUARIOS),
                eq(AuditActions.CREAR_USUARIO),
                eq("Admin Test creó el usuario nuevo.user"),
                eq("User"),
                eq("user-1")
        );
    }

    @Test
    void update_passwordChange_registersAuditEvent() {
        User existing = new User();
        existing.setId("user-2");
        existing.setUsername("ana.user");
        existing.setActive(true);

        UserRequest request = new UserRequest();
        request.setUsername("ana.user");
        request.setPassword("NuevaClave123!");
        request.setActive(true);

        when(userRepository.findById("user-2")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.update("user-2", request);

        verify(bitacoraService).registrar(
                eq(AuditModules.USUARIOS),
                eq(AuditActions.CAMBIO_PASSWORD),
                eq("Admin Test cambió la contraseña del usuario ana.user"),
                eq("User"),
                eq("user-2")
        );
    }
}
