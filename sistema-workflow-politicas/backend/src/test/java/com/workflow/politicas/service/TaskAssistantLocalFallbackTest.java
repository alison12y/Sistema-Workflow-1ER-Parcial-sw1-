package com.workflow.politicas.service;

import com.workflow.politicas.dto.TaskAssistantResponseDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskAssistantLocalFallbackTest {

    @Test
    void build_returnsExpectedFallbackStructure() {
        TaskAssistantResponseDto response = TaskAssistantLocalFallback.build(Map.of(
                "tramiteName", "TRM-001",
                "activityName", "Validación",
                "taskStatus", "EN_CURSO",
                "assignedTo", "Ana Rodríguez Paz"
        ));

        assertEquals("LOCAL_FALLBACK", response.getSource());
        assertTrue(response.getSummary().contains("revisar la actividad"));
        assertEquals(4, response.getImportantData().size());
        assertTrue(response.getImportantData().stream().anyMatch(s -> s.contains("Ana Rodríguez Paz")));
        assertFalse(response.getMissingData().isEmpty());
        assertTrue(response.getRecommendedAction().contains("Revisar la información disponible"));
    }

    @Test
    void build_detectsMissingFormAndDocuments() {
        TaskAssistantResponseDto response = TaskAssistantLocalFallback.build(Map.of(
                "tramiteName", "TRM-002",
                "activityName", "Aprobación",
                "taskStatus", "PENDIENTE",
                "assignedTo", "Luis Herrera",
                "formData", Map.of(),
                "documents", List.of()
        ));

        assertTrue(response.getMissingData().contains("Verificar si el formulario fue completado"));
        assertTrue(response.getMissingData().contains("Verificar si existen documentos requeridos"));
    }
}
