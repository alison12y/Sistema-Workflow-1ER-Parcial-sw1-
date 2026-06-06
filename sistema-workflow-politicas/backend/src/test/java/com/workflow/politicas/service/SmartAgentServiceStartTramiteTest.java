package com.workflow.politicas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.politicas.dto.DocumentRecordResponse;
import com.workflow.politicas.dto.SmartAgentStartTramiteRequest;
import com.workflow.politicas.dto.SmartAgentStartTramiteResponse;
import com.workflow.politicas.dto.TramiteCreateRequest;
import com.workflow.politicas.model.DocumentRepository;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.repository.AiRequestRepository;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.DocumentRecordRepository;
import com.workflow.politicas.repository.DocumentRepositoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartAgentServiceStartTramiteTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private AiRequestRepository aiRequestRepository;
    @Mock
    private BusinessPolicyRepository businessPolicyRepository;
    @Mock
    private DocumentRecordRepository documentRecordRepository;
    @Mock
    private DocumentRepositoryStore documentRepositoryStore;
    @Mock
    private DocumentRepositoryService documentRepositoryService;
    @Mock
    private TramiteService tramiteService;
    @Mock
    private BitacoraService bitacoraService;

    private SmartAgentService smartAgentService;

    @BeforeEach
    void setUp() {
        smartAgentService = new SmartAgentService(
                restTemplate,
                new ObjectMapper(),
                businessPolicyRepository,
                documentRecordRepository,
                documentRepositoryStore,
                documentRepositoryService,
                tramiteService,
                bitacoraService
        );
    }

    @Test
    void startTramite_createsTramiteAndUploadsAttachmentToDocumentRepository() throws Exception {
        Tramite tramite = new Tramite();
        tramite.setId("tram-42");
        tramite.setCode("TRM-042");
        tramite.setPolicyName("Solicitud de instalación de medidor");
        when(tramiteService.create(any(TramiteCreateRequest.class), eq("ana.rodriguez"))).thenReturn(tramite);

        DocumentRepository repository = new DocumentRepository();
        repository.setId("repo-42");
        repository.setTramiteId("tram-42");
        when(documentRepositoryStore.findByTramiteId("tram-42")).thenReturn(Optional.of(repository));

        DocumentRecordResponse uploaded = new DocumentRecordResponse();
        uploaded.setId("doc-1");
        uploaded.setNombreOriginal("solicitud.pdf");
        when(documentRepositoryService.uploadDocument(eq("repo-42"), any(), eq("ana.rodriguez"))).thenReturn(uploaded);

        SmartAgentStartTramiteRequest request = new SmartAgentStartTramiteRequest();
        request.setPolicyId("policy-medidor");
        request.setDescription("Instalar medidor de gas");
        request.setRequestedBy("ana.rodriguez");
        request.setPriority("NORMAL");
        request.setDetectedIntent("INSTALACION_MEDIDOR");

        MockMultipartFile attachment = new MockMultipartFile(
                "attachment",
                "solicitud.pdf",
                "application/pdf",
                "pdf-data".getBytes()
        );

        SmartAgentStartTramiteResponse response = smartAgentService.startTramite(request, attachment, "ana.rodriguez");

        assertNotNull(response.getTramite());
        assertEquals("TRM-042", response.getTramite().getCode());
        assertNotNull(response.getAttachedDocument());
        assertEquals("solicitud.pdf", response.getAttachedDocument().getNombreOriginal());

        ArgumentCaptor<TramiteCreateRequest> createCaptor = ArgumentCaptor.forClass(TramiteCreateRequest.class);
        verify(tramiteService).create(createCaptor.capture(), eq("ana.rodriguez"));
        assertEquals("policy-medidor", createCaptor.getValue().getPolicyId());
        verify(documentRepositoryStore).findByTramiteId("tram-42");
        verify(documentRepositoryService).uploadDocument(eq("repo-42"), eq(attachment), eq("ana.rodriguez"));
        verify(bitacoraService).registrar(
                eq("ana.rodriguez"),
                eq("Inteligencia artificial"),
                eq("AGENT_TRAMITE_STARTED"),
                any(),
                eq("Tramite"),
                eq("tram-42")
        );
    }

    @Test
    void startTramite_withoutAttachment_onlyCreatesTramite() {
        Tramite tramite = new Tramite();
        tramite.setId("tram-43");
        tramite.setCode("TRM-043");
        when(tramiteService.create(any(TramiteCreateRequest.class), eq("carlos.mendoza"))).thenReturn(tramite);

        SmartAgentStartTramiteRequest request = new SmartAgentStartTramiteRequest();
        request.setPolicyId("policy-1");
        request.setDescription("Solicitud general");
        request.setRequestedBy("carlos.mendoza");

        SmartAgentStartTramiteResponse response = smartAgentService.startTramite(request, null, "carlos.mendoza");

        assertEquals("TRM-043", response.getTramite().getCode());
        verify(tramiteService).create(any(TramiteCreateRequest.class), eq("carlos.mendoza"));
        verify(documentRepositoryService, org.mockito.Mockito.never()).uploadDocument(any(), any(), any());
    }
}
