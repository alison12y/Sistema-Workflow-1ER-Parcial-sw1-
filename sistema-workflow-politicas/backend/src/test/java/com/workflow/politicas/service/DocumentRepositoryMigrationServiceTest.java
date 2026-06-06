package com.workflow.politicas.service;

import com.workflow.politicas.dto.DocumentRepositoryMigrationReport;
import com.workflow.politicas.model.DocumentRepository;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.repository.DocumentRepositoryStore;
import com.workflow.politicas.repository.TramiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentRepositoryMigrationServiceTest {

    @Mock
    private TramiteRepository tramiteRepository;
    @Mock
    private DocumentRepositoryStore documentRepositoryStore;
    @Mock
    private DocumentRepositoryService documentRepositoryService;

    private DocumentRepositoryMigrationService migrationService;

    @BeforeEach
    void setUp() {
        migrationService = new DocumentRepositoryMigrationService(
                tramiteRepository,
                documentRepositoryStore,
                documentRepositoryService
        );
    }

    @Test
    void migrateExistingTramites_createsOnlyMissingRepositories() {
        Tramite tramite1 = tramite("t1", "TRM-001");
        Tramite tramite2 = tramite("t2", "TRM-002");

        when(tramiteRepository.findAll()).thenReturn(List.of(tramite2, tramite1));
        when(documentRepositoryStore.findByTramiteId("t1")).thenReturn(Optional.of(new DocumentRepository()));
        when(documentRepositoryStore.findByTramiteId("t2")).thenReturn(Optional.empty());
        when(documentRepositoryService.createForTramite(tramite2, "admin")).thenReturn(new DocumentRepository());

        DocumentRepositoryMigrationReport report = migrationService.migrateExistingTramites("admin");

        assertEquals(2, report.getTotalTramites());
        assertEquals(1, report.getRepositoriesCreated());
        assertEquals(1, report.getRepositoriesAlreadyPresent());
        assertEquals(List.of("TRM-002"), report.getCreatedTramiteCodes());
        verify(documentRepositoryService).createForTramite(tramite2, "admin");
        verify(documentRepositoryService, never()).createForTramite(eq(tramite1), any());
    }

    @Test
    void migrateExistingTramites_isIdempotentWhenAllExist() {
        Tramite tramite1 = tramite("t1", "TRM-001");
        when(tramiteRepository.findAll()).thenReturn(List.of(tramite1));
        when(documentRepositoryStore.findByTramiteId("t1")).thenReturn(Optional.of(new DocumentRepository()));

        DocumentRepositoryMigrationReport report = migrationService.migrateExistingTramites("admin");

        assertEquals(1, report.getTotalTramites());
        assertEquals(0, report.getRepositoriesCreated());
        assertEquals(1, report.getRepositoriesAlreadyPresent());
        assertTrue(report.getCreatedTramiteCodes().isEmpty());
        verify(documentRepositoryService, never()).createForTramite(any(), any());
    }

    private Tramite tramite(String id, String code) {
        Tramite tramite = new Tramite();
        tramite.setId(id);
        tramite.setCode(code);
        return tramite;
    }
}
