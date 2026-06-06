package com.workflow.politicas.service;

import com.workflow.politicas.dto.DocumentRepositoryMigrationReport;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.repository.DocumentRepositoryStore;
import com.workflow.politicas.repository.TramiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Migración idempotente: garantiza un repositorio documental por trámite (CU17 retroactivo).
 */
@Service
public class DocumentRepositoryMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentRepositoryMigrationService.class);

    private final TramiteRepository tramiteRepository;
    private final DocumentRepositoryStore documentRepositoryStore;
    private final DocumentRepositoryService documentRepositoryService;

    public DocumentRepositoryMigrationService(
            TramiteRepository tramiteRepository,
            DocumentRepositoryStore documentRepositoryStore,
            DocumentRepositoryService documentRepositoryService
    ) {
        this.tramiteRepository = tramiteRepository;
        this.documentRepositoryStore = documentRepositoryStore;
        this.documentRepositoryService = documentRepositoryService;
    }

    public DocumentRepositoryMigrationReport migrateExistingTramites(String createdBy) {
        List<Tramite> tramites = tramiteRepository.findAll().stream()
                .filter(tramite -> tramite.getId() != null && !tramite.getId().isBlank())
                .sorted(Comparator.comparing(this::resolveTramiteCode))
                .toList();

        DocumentRepositoryMigrationReport report = new DocumentRepositoryMigrationReport();
        report.setTotalTramites(tramites.size());

        String actor = createdBy != null && !createdBy.isBlank() ? createdBy : "system-migration";
        int createdCount = 0;
        int existingCount = 0;

        for (Tramite tramite : tramites) {
            if (documentRepositoryStore.findByTramiteId(tramite.getId()).isPresent()) {
                existingCount++;
                continue;
            }

            documentRepositoryService.createForTramite(tramite, actor);
            String code = resolveTramiteCode(tramite);
            log.info("Repositorio creado para {}", code);
            report.addCreatedTramiteCode(code);
            createdCount++;
        }

        report.setRepositoriesCreated(createdCount);
        report.setRepositoriesAlreadyPresent(existingCount);

        if (createdCount > 0) {
            log.info(
                    "DocumentRepositoryMigration: {} repositorio(s) creado(s), {} ya existían ({} trámites revisados)",
                    createdCount,
                    existingCount,
                    tramites.size()
            );
        } else {
            log.info(
                    "DocumentRepositoryMigration: todos los trámites ya tenían repositorio ({} revisados)",
                    tramites.size()
            );
        }

        return report;
    }

    private String resolveTramiteCode(Tramite tramite) {
        if (tramite.getCode() != null && !tramite.getCode().isBlank()) {
            return tramite.getCode().trim();
        }
        return tramite.getId();
    }
}
