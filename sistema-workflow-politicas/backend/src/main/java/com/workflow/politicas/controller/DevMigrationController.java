package com.workflow.politicas.controller;

import com.workflow.politicas.dto.MigrationReport;
import com.workflow.politicas.service.Phase1MigrationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint temporal para migrar datos de la Fase 1 en bases MongoDB ya existentes.
 * Requiere cabecera X-Migration-Key. No elimina datos de otras colecciones.
 */
@RestController
@RequestMapping("/api/dev")
public class DevMigrationController {

    private final Phase1MigrationService migrationService;

    @Value("${app.migration.key:phase1-local-dev}")
    private String migrationKey;

    public DevMigrationController(Phase1MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/migrate-phase1")
    public ResponseEntity<?> migratePhase1(
            @RequestHeader(value = "X-Migration-Key", required = false) String providedKey
    ) {
        if (providedKey == null || !providedKey.equals(migrationKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Clave de migración inválida o ausente (header X-Migration-Key)."));
        }

        MigrationReport report = migrationService.migratePhase1();
        return ResponseEntity.ok(Map.of(
                "message", "Migración Fase 1 completada. No se eliminaron usuarios, roles ni departamentos previos.",
                "report", report
        ));
    }
}
