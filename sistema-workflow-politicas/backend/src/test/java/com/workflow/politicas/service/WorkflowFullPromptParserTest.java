package com.workflow.politicas.service;

import com.workflow.politicas.dto.AiSuggestActivityItem;
import com.workflow.politicas.dto.AiSuggestTransitionItem;
import com.workflow.politicas.dto.AiWorkflowSuggestRequest;
import com.workflow.politicas.dto.AiWorkflowSuggestResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowFullPromptParserTest {

    private static final String PERMISO_LABORAL_PROMPT = """
            Crear un workflow para solicitud de permiso laboral.
            Carriles:
            - Funcionario
            - Recursos Humanos
            - Supervisor
            Flujo:
            Inicio.
            El funcionario registra la solicitud.
            Recursos Humanos revisa la documentación.
            Si la documentación está incompleta se solicita corrección.
            Si está completa pasa al supervisor.
            El supervisor aprueba o rechaza.
            Si aprueba termina el proceso.
            Si rechaza se notifica al funcionario y finaliza.
            """;

    @Test
    void canParse_detectsFullWorkflowPrompt() {
        assertTrue(WorkflowFullPromptParser.canParse(PERMISO_LABORAL_PROMPT));
    }

    @Test
    void parse_permisoLaboral_generatesCompleteWorkflow() {
        AiWorkflowSuggestRequest request = new AiWorkflowSuggestRequest();
        request.setPolicyId("policy-1");
        request.setPrompt(PERMISO_LABORAL_PROMPT);

        AiWorkflowSuggestResponse response = WorkflowFullPromptParser.parse(request);

        assertNotNull(response);
        assertEquals("GENERATE_FULL_WORKFLOW", response.getIntent());
        assertEquals(Boolean.TRUE, response.getFallbackUsed());
        assertEquals(10, response.getSuggestedActivities().size());
        assertEquals(11, response.getSuggestedTransitions().size());

        Set<String> activityNames = response.getSuggestedActivities().stream()
                .map(AiSuggestActivityItem::getName)
                .collect(Collectors.toSet());
        assertTrue(activityNames.containsAll(List.of(
                "Inicio",
                "Registrar solicitud",
                "Revisar documentación",
                "Documentación completa?",
                "Solicitar corrección",
                "Revisión supervisor",
                "Aprueba permiso?",
                "Aprobar permiso",
                "Notificar rechazo",
                "Fin"
        )));

        assertActivityType(response, "Inicio", "START");
        assertActivityType(response, "Fin", "END");
        assertActivityType(response, "Documentación completa?", "DECISION");
        assertActivityType(response, "Aprueba permiso?", "DECISION");
        assertActivityType(response, "Registrar solicitud", "TASK");

        assertLane(response, "Registrar solicitud", "Funcionario");
        assertLane(response, "Revisar documentación", "Recursos Humanos");
        assertLane(response, "Revisión supervisor", "Supervisor");

        assertTransition(response, "Inicio", "Registrar solicitud", "SEQUENTIAL", null);
        assertTransition(response, "Registrar solicitud", "Revisar documentación", "SEQUENTIAL", null);
        assertTransition(response, "Revisar documentación", "Documentación completa?", "SEQUENTIAL", null);
        assertTransition(response, "Documentación completa?", "Solicitar corrección", "CONDITIONAL", "completa == false");
        assertTransition(response, "Solicitar corrección", "Revisar documentación", "SEQUENTIAL", null);
        assertTransition(response, "Documentación completa?", "Revisión supervisor", "CONDITIONAL", "completa == true");
        assertTransition(response, "Revisión supervisor", "Aprueba permiso?", "SEQUENTIAL", null);
        assertTransition(response, "Aprueba permiso?", "Aprobar permiso", "CONDITIONAL", "aprobado == true");
        assertTransition(response, "Aprueba permiso?", "Notificar rechazo", "CONDITIONAL", "aprobado == false");
        assertTransition(response, "Aprobar permiso", "Fin", "SEQUENTIAL", null);
        assertTransition(response, "Notificar rechazo", "Fin", "SEQUENTIAL", null);

        assertEquals(3, response.getSuggestedResponsibles().size());
    }

    @Test
    void localParser_doesNotCollapsePromptIntoSingleActivity() {
        AiWorkflowSuggestRequest request = new AiWorkflowSuggestRequest();
        request.setPolicyId("policy-1");
        request.setPrompt(PERMISO_LABORAL_PROMPT);

        AiWorkflowSuggestResponse response = WorkflowSuggestLocalParser.parse(request);

        assertTrue(response.getSuggestedActivities().size() > 1);
        assertFalse(response.getSuggestedActivities().stream()
                .anyMatch(a -> a.getName().toLowerCase(Locale.ROOT).contains("workflow para solicitud")));
    }

    private static void assertActivityType(
            AiWorkflowSuggestResponse response,
            String name,
            String expectedType
    ) {
        AiSuggestActivityItem item = response.getSuggestedActivities().stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing activity: " + name));
        assertEquals(expectedType, item.getActivityType());
    }

    private static void assertLane(AiWorkflowSuggestResponse response, String activityName, String lane) {
        AiSuggestActivityItem item = response.getSuggestedActivities().stream()
                .filter(a -> activityName.equals(a.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing activity: " + activityName));
        assertEquals(lane, item.getResponsibleName());
    }

    private static void assertTransition(
            AiWorkflowSuggestResponse response,
            String from,
            String to,
            String type,
            String condition
    ) {
        List<AiSuggestTransitionItem> matches = response.getSuggestedTransitions().stream()
                .filter(t -> from.equals(t.getFromActivityName()) && to.equals(t.getToActivityName()))
                .toList();
        assertFalse(matches.isEmpty(), () -> "Missing transition " + from + " -> " + to);
        boolean ok = matches.stream().anyMatch(t ->
                type.equalsIgnoreCase(t.getTransitionType())
                        && (condition == null ? t.getConditionLabel() == null : condition.equals(t.getConditionLabel()))
        );
        assertTrue(ok, () -> "Transition " + from + " -> " + to + " type/condition mismatch");
    }
}
