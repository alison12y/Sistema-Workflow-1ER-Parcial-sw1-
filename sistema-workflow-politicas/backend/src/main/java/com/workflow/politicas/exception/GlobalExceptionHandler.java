package com.workflow.politicas.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "No se pudo completar la operación.", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Solicitud inválida.", "Revise los datos enviados.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos", null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabled(DisabledException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Su cuenta está inactiva. Contacte al administrador.", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "No se pudo iniciar sesión. Verifique sus credenciales.", null);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElement(NoSuchElementException ex) {
        return error(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        if (ex instanceof AuthenticationException authenticationException) {
            return handleAuthentication(authenticationException);
        }
        log.error("Unhandled exception", ex);
        String details = ex.getMessage() != null ? ex.getMessage() : "Error inesperado.";
        if (details.toLowerCase().contains("not found")) {
            return error(HttpStatus.NOT_FOUND, "Recurso no encontrado", details);
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operación.", details);
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message, String details) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", message);
        if (details != null && !details.isBlank()) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
