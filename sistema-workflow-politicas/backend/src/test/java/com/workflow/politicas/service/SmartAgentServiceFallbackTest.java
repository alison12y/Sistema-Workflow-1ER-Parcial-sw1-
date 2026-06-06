package com.workflow.politicas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.politicas.dto.SmartAgentAnalyzeRequest;
import com.workflow.politicas.dto.SmartAgentAnalyzeResponse;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.repository.AiRequestRepository;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.DocumentRecordRepository;
import com.workflow.politicas.repository.DocumentRepositoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartAgentServiceFallbackTest {

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
        ReflectionTestUtils.setField(smartAgentService, "aiServiceUrl", "http://localhost:59999");
    }

    @Test
    void analyze_whenAiServiceDown_usesLocalFallbackWithWarning() {
        when(businessPolicyRepository.findAll()).thenReturn(List.of(
                activePolicy("p-draft-like", "Borrador interno", "No activa", "DRAFT"),
                activePolicy("p-medidor", "Solicitud de instalación de medidor", "Medidores de gas", "ACTIVE"),
                activePolicy("p-reclamo", "Reclamo de servicio", "Quejas", "ACTIVE")
        ));
        when(restTemplate.exchange(
                eq("http://localhost:59999/agent/analyze"),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new ResourceAccessException("Connection refused"));

        SmartAgentAnalyzeRequest request = new SmartAgentAnalyzeRequest();
        request.setMessage("Quiero instalar un medidor de gas");
        request.setRequesterName("Alison");

        SmartAgentAnalyzeResponse response = smartAgentService.analyze(request, null, "ana.rodriguez");

        assertEquals("LOCAL_FALLBACK", response.getSource());
        assertEquals("p-medidor", response.getRecommendedPolicyId());
        assertNotNull(response.getConfidenceScore());
        assertTrue(response.getWarnings().stream().anyMatch(w -> {
            String lower = w.toLowerCase();
            return lower.contains("fallback") || lower.contains("local") || lower.contains("no disponible");
        }));
        verify(bitacoraService).registrar(
                eq("ana.rodriguez"),
                eq("Inteligencia artificial"),
                eq("AGENT_REQUESTED"),
                any(),
                eq("SmartAgent"),
                eq(null)
        );
        verify(bitacoraService).registrar(
                eq("ana.rodriguez"),
                eq("Inteligencia artificial"),
                eq("AGENT_POLICY_RECOMMENDED"),
                any(),
                eq("BusinessPolicy"),
                eq("p-medidor")
        );
    }

    @Test
    void analyze_onlyConsidersActivePoliciesFromRepository() {
        when(businessPolicyRepository.findAll()).thenReturn(List.of(
                activePolicy("p-draft", "Política borrador medidor", "Medidor draft", "DRAFT"),
                activePolicy("p-active", "Gestión de bienes decomisados", "Bienes incautados", "ACTIVE")
        ));
        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new ResourceAccessException("down"));

        SmartAgentAnalyzeRequest request = new SmartAgentAnalyzeRequest();
        request.setMessage("gestionar bienes decomisados");

        SmartAgentAnalyzeResponse response = smartAgentService.analyze(request, null, "tester");

        assertEquals("p-active", response.getRecommendedPolicyId());
        assertEquals("Gestión de bienes decomisados", response.getRecommendedPolicyName());
    }

    private static BusinessPolicy activePolicy(String id, String name, String description, String status) {
        BusinessPolicy policy = new BusinessPolicy();
        policy.setId(id);
        policy.setName(name);
        policy.setDescription(description);
        policy.setStatus(status);
        return policy;
    }
}
