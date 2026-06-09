package com.workflow.politicas.service;

import com.workflow.politicas.dto.TaskAssistantResponseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fallback local cuando el servicio IA no está disponible (Asistente Inteligente de Tarea).
 */
public final class TaskAssistantLocalFallback {

    private TaskAssistantLocalFallback() {
    }

    public static TaskAssistantResponseDto build(Map<String, Object> context) {
        TaskAssistantResponseDto response = new TaskAssistantResponseDto();
        response.setSource("LOCAL_FALLBACK");
        response.setSummary(
                "El funcionario debe revisar la actividad actual del trámite y continuar con el flujo correspondiente."
        );

        String tramiteName = asString(context.get("tramiteName"), "Trámite desconocido");
        String activityName = asString(context.get("activityName"), "Actividad desconocida");
        String taskStatus = asString(context.get("taskStatus"), "PENDIENTE");
        String assignedTo = asString(context.get("assignedTo"), "Sin asignar");

        List<String> important = new ArrayList<>();
        important.add("Trámite: " + tramiteName);
        important.add("Actividad actual: " + activityName);
        important.add("Estado de la tarea: " + taskStatus);
        important.add("Responsable asignado: " + assignedTo);
        response.setImportantData(important);

        List<String> missing = new ArrayList<>();
        Object formData = context.get("formData");
        if (!(formData instanceof Map<?, ?> map) || map.isEmpty()) {
            missing.add("Verificar si el formulario fue completado");
        }
        Object documents = context.get("documents");
        if (!(documents instanceof List<?> list) || list.isEmpty()) {
            missing.add("Verificar si existen documentos requeridos");
        }
        if (missing.isEmpty()) {
            missing.add("Confirmar que los datos del formulario son correctos antes de avanzar");
        }
        response.setMissingData(missing);
        response.setRecommendedAction(
                "Revisar la información disponible y completar la tarea si los datos son correctos. "
                        + "Si falta información, registrar una observación antes de continuar."
        );
        return response;
    }

    private static String asString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }
}
