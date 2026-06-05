package com.workflow.politicas.service;

import com.workflow.politicas.dto.AiWorkflowSuggestRequest;
import com.workflow.politicas.dto.AiWorkflowSuggestResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cubre el fallback local usado por {@link AiService#suggestWorkflow} cuando ai-service no responde.
 */
class AiServiceFallbackTest {

    @Test
    void localParser_marksFallbackWhenAiUnavailable() {
        AiWorkflowSuggestRequest request = new AiWorkflowSuggestRequest();
        request.setPolicyId("policy-1");
        request.setPrompt("Agregar actividad Validar documentos");

        AiWorkflowSuggestResponse response = WorkflowSuggestLocalParser.parse(request);

        assertNotNull(response);
        assertEquals(Boolean.TRUE, response.getFallbackUsed());
        assertEquals(Boolean.FALSE, response.getAiAvailable());
        assertNotNull(response.getError());
    }
}
