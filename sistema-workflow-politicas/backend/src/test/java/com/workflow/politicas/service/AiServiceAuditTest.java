package com.workflow.politicas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.AiWorkflowSuggestRequest;
import com.workflow.politicas.repository.AiRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceAuditTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private AiRequestRepository aiRequestRepository;
    @Mock
    private BitacoraService bitacoraService;

    private AiService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiService(restTemplate, aiRequestRepository, new ObjectMapper(), bitacoraService);
        ReflectionTestUtils.setField(aiService, "aiServiceUrl", "http://localhost:8090");
        when(bitacoraService.resolveActorDisplay()).thenReturn("Diseñador Test");
    }

    @Test
    void suggestWorkflow_registersGenerarWorkflowIaAuditEvent() {
        AiWorkflowSuggestRequest request = new AiWorkflowSuggestRequest();
        request.setPolicyId("policy-1");
        request.setPrompt(
                "Crear actividad Validar documentos en Legal y conectarla después de Recepción de forma secuencial."
        );
        request.setUserId("designer");

        aiService.suggestWorkflow(request);

        verify(bitacoraService).registrar(
                eq(AuditModules.IA),
                eq(AuditActions.GENERAR_WORKFLOW_IA),
                org.mockito.ArgumentMatchers.contains("sugerencia"),
                eq("BusinessPolicy"),
                eq("policy-1")
        );
    }
}
