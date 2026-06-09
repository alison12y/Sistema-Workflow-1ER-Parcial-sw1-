package com.workflow.politicas.service;

import com.workflow.politicas.model.DocumentRecord;
import com.workflow.politicas.model.DocumentRepository;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.repository.DocumentRecordRepository;
import com.workflow.politicas.repository.DocumentRepositoryStore;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.storage.StorageProperties;
import com.workflow.politicas.storage.StorageService;
import com.workflow.politicas.storage.StoredObject;
import com.workflow.politicas.security.AuthenticatedActorResolver;
import com.workflow.politicas.security.AuthenticatedActorResolver.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentRepositoryServiceStorageTest {

    @Mock
    private DocumentRepositoryStore documentRepositoryStore;
    @Mock
    private DocumentRecordRepository documentRecordRepository;
    @Mock
    private TramiteRepository tramiteRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private BitacoraService bitacoraService;
    @Mock
    private DocumentCollaborationService documentCollaborationService;
    @Mock
    private AuthenticatedActorResolver actorResolver;

    private DocumentRepositoryService documentRepositoryService;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setBucket("workflow-docs");
        storageProperties.setPresignedUrlExpirationMinutes(15);
        documentRepositoryService = new DocumentRepositoryService(
                documentRepositoryStore,
                documentRecordRepository,
                tramiteRepository,
                storageService,
                storageProperties,
                bitacoraService,
                documentCollaborationService,
                actorResolver
        );
    }

    @Test
    void uploadDocument_firstVersion_usesTramiteCodigoRootPath() throws Exception {
        DocumentRepository repository = activeRepository();
        when(documentRepositoryStore.findById("repo-1")).thenReturn(Optional.of(repository));
        when(tramiteRepository.findById("tram-1")).thenReturn(Optional.of(new Tramite()));
        when(documentRecordRepository.findByRepositoryIdAndNombreOriginalAndEstadoNot(
                "repo-1", "informe.pdf", DocumentRecord.STATUS_ELIMINADO
        )).thenReturn(List.of());
        when(documentRecordRepository.findByDocumentFamilyIdAndEstadoNotOrderByVersionDesc(anyString(), anyString()))
                .thenReturn(List.of());
        when(storageService.upload(anyString(), any(), anyLong(), anyString(), anyMap()))
                .thenReturn(StoredObject.metadataOnly("TRM-007/informe.pdf", "workflow-docs", "application/pdf", 4L, "etag"));
        when(documentRecordRepository.save(any(DocumentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "informe.pdf", "application/pdf", "data".getBytes());
        var response = documentRepositoryService.uploadDocument("repo-1", file, "ana.rodriguez");
        assertEquals("TRM-007/informe.pdf", response.getS3Key());
        assertEquals(1, response.getVersion());
        assertEquals("TRM-007", response.getTramiteCodigo());
        assertEquals("informe.pdf", response.getNombreArchivo());
        assertEquals(response.getId(), response.getDocumentFamilyId());

        ArgumentCaptor<Map<String, String>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(storageService).upload(eq("TRM-007/informe.pdf"), any(), eq(4L), eq("application/pdf"), metadataCaptor.capture());
        Map<String, String> metadata = metadataCaptor.getValue();
        assertEquals("TRM-007", metadata.get("tramite-codigo"));
        assertEquals("TRM-007/informe.pdf", metadata.get("s3-key"));
        assertEquals("1", metadata.get("version"));
    }

    @Test
    void uploadDocument_secondVersion_supersedesPreviousAndUsesVersionesFolder() throws Exception {
        DocumentRepository repository = activeRepository();
        DocumentRecord previous = new DocumentRecord();
        previous.setId("doc-v1");
        previous.setDocumentFamilyId("family-1");
        previous.setVersion(1);
        previous.setNombreOriginal("informe.pdf");
        previous.setEstado(DocumentRecord.STATUS_ACTIVO);

        when(documentRepositoryStore.findById("repo-1")).thenReturn(Optional.of(repository));
        when(tramiteRepository.findById("tram-1")).thenReturn(Optional.of(new Tramite()));
        when(documentRecordRepository.findByRepositoryIdAndNombreOriginalAndEstadoNot(
                "repo-1", "informe.pdf", DocumentRecord.STATUS_ELIMINADO
        )).thenReturn(List.of(previous));
        when(documentRecordRepository.findByDocumentFamilyIdAndEstadoNotOrderByVersionDesc(
                "family-1", DocumentRecord.STATUS_ELIMINADO
        )).thenReturn(List.of(previous));
        when(storageService.upload(anyString(), any(), anyLong(), anyString(), anyMap()))
                .thenReturn(StoredObject.metadataOnly(
                        "TRM-007/versiones/informe_v2.pdf",
                        "workflow-docs",
                        "application/pdf",
                        4L,
                        "etag"
                ));
        when(documentRecordRepository.save(any(DocumentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actorResolver.requireCurrentActor()).thenReturn(new Actor("u1", "ana.rodriguez", "Ana Rodriguez"));

        MockMultipartFile file = new MockMultipartFile("file", "informe.pdf", "application/pdf", "data".getBytes());
        var response = documentRepositoryService.uploadDocument("repo-1", file, "ana.rodriguez");

        assertEquals("TRM-007/versiones/informe_v2.pdf", response.getS3Key());
        assertEquals(2, response.getVersion());
        assertEquals("family-1", response.getDocumentFamilyId());
        assertEquals("informe_v2.pdf", response.getNombreArchivo());

        ArgumentCaptor<DocumentRecord> savedCaptor = ArgumentCaptor.forClass(DocumentRecord.class);
        verify(documentRecordRepository, atLeastOnce()).save(savedCaptor.capture());
        boolean previousMarkedHistoric = savedCaptor.getAllValues().stream()
                .anyMatch(record -> "doc-v1".equals(record.getId())
                        && DocumentRecord.STATUS_HISTORICO.equals(record.getEstado()));
        org.junit.jupiter.api.Assertions.assertTrue(previousMarkedHistoric);
    }

    @Test
    void listDocuments_returnsOnlyCurrentVersions() {
        DocumentRepository repository = activeRepository();
        when(documentRepositoryStore.findById("repo-1")).thenReturn(Optional.of(repository));
        when(tramiteRepository.findById("tram-1")).thenReturn(Optional.of(new Tramite()));

        DocumentRecord current = new DocumentRecord();
        current.setId("doc-current");
        current.setDocumentFamilyId("family-1");
        current.setNombreOriginal("contrato.pdf");
        current.setVersion(3);
        current.setEstado(DocumentRecord.STATUS_ACTIVO);

        when(documentRecordRepository.findByRepositoryIdAndEstadoOrderByFechaSubidaDesc(
                "repo-1", DocumentRecord.STATUS_ACTIVO
        )).thenReturn(List.of(current));

        var documents = documentRepositoryService.listDocuments("repo-1");

        assertEquals(1, documents.size());
        assertEquals(3, documents.get(0).getVersion());
    }

    private DocumentRepository activeRepository() {
        DocumentRepository repository = new DocumentRepository();
        repository.setId("repo-1");
        repository.setTramiteId("tram-1");
        repository.setTramiteCodigo("TRM-007");
        repository.setEstado(DocumentRepository.STATUS_ACTIVO);
        return repository;
    }
}
