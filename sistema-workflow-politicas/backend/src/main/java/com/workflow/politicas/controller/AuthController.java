package com.workflow.politicas.controller;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.AuthResponse;
import com.workflow.politicas.dto.LoginRequest;
import com.workflow.politicas.dto.RegisterRequest;
import com.workflow.politicas.service.AuthService;
import com.workflow.politicas.service.BitacoraService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final BitacoraService bitacoraService;

    public AuthController(AuthService authService, BitacoraService bitacoraService) {
        this.authService = authService;
        this.bitacoraService = bitacoraService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = bitacoraService.resolveClientIp(httpRequest);
        String attemptedUser = request.getUsername() != null ? request.getUsername().trim() : "desconocido";
        try {
            AuthResponse response = authService.login(request);
            String display = response.getFullName() != null && !response.getFullName().isBlank()
                    ? response.getFullName()
                    : response.getUsername();
            bitacoraService.registrarExito(
                    response.getUsername(),
                    AuditModules.SEGURIDAD,
                    AuditActions.LOGIN_EXITOSO,
                    display + " inició sesión correctamente",
                    "User",
                    null,
                    clientIp
            );
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException | DisabledException ex) {
            bitacoraService.registrarError(
                    attemptedUser,
                    AuditModules.SEGURIDAD,
                    AuditActions.LOGIN_FALLIDO,
                    "Intento de inicio de sesión fallido para " + attemptedUser,
                    "User",
                    null,
                    clientIp
            );
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        String actor = bitacoraService.resolveActorDisplay();
        bitacoraService.registrarExito(
                null,
                AuditModules.SEGURIDAD,
                AuditActions.LOGOUT,
                actor + " cerró sesión",
                "User",
                null,
                bitacoraService.resolveClientIp(httpRequest)
        );
        return ResponseEntity.noContent().build();
    }
}
