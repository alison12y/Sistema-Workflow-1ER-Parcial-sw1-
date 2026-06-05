package com.workflow.politicas.service;

import com.workflow.politicas.dto.WorkflowRoutingResult;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.repository.WorkflowTransitionRepository;
import com.workflow.politicas.support.WorkflowTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowRoutingServiceTest {

    @Mock
    private WorkflowActivityRepository workflowActivityRepository;
    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    private WorkflowRoutingService routingService;

    @BeforeEach
    void setUp() {
        routingService = new WorkflowRoutingService(
                workflowActivityRepository,
                workflowTransitionRepository,
                new WorkflowConditionEvaluator()
        );
    }

    @Test
    void resolveFirstWorkTasks_sequentialFlow() {
        stubPolicy(WorkflowTestFixtures.sequentialFlowActivities(), WorkflowTestFixtures.sequentialFlowTransitions());

        WorkflowRoutingResult result = routingService.resolveFirstWorkTasks("policy-1");

        assertEquals(WorkflowRoutingResult.Outcome.ACTIVATE_TASKS, result.getOutcome());
        assertEquals(1, result.getNextActivities().size());
        assertEquals("t1", result.getNextActivities().get(0).getId());
    }

    @Test
    void routeAfterCompletedActivity_advancesToNextTask() {
        stubPolicy(WorkflowTestFixtures.sequentialFlowActivities(), WorkflowTestFixtures.sequentialFlowTransitions());

        WorkflowRoutingResult result = routingService.routeAfterCompletedActivity(
                "policy-1",
                "t1",
                Map.of()
        );

        assertEquals(WorkflowRoutingResult.Outcome.ACTIVATE_TASKS, result.getOutcome());
        assertEquals("t2", result.getNextActivities().get(0).getId());
    }

    @Test
    void requireStartActivity_rejectsMultipleStart() {
        List<WorkflowActivity> activities = List.of(
                WorkflowTestFixtures.activity("s1", "START", "Inicio 1", null),
                WorkflowTestFixtures.activity("s2", "START", "Inicio 2", null),
                WorkflowTestFixtures.activity("end", "END", "Fin", null)
        );
        when(workflowActivityRepository.findByPolicyIdOrderByOrderIndexAsc("policy-1")).thenReturn(activities);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> routingService.requireStartActivity("policy-1")
        );
        assertTrue(ex.getMessage().contains("múltiples"));
    }

    @Test
    void routeAfterCompletedActivity_traverseDecisionWithStepData() {
        List<WorkflowActivity> activities = List.of(
                WorkflowTestFixtures.activity("start", "START", "Inicio", null),
                WorkflowTestFixtures.activity("t1", "TASK", "Recepción", "Ops"),
                WorkflowTestFixtures.activity("dec", "DECISION", "¿Válido?", null),
                WorkflowTestFixtures.activity("t2", "TASK", "Siguiente", "Ops"),
                WorkflowTestFixtures.activity("end", "END", "Fin", null)
        );
        List<com.workflow.politicas.model.WorkflowTransition> transitions = List.of(
                WorkflowTestFixtures.transition("a", "SEQUENTIAL", "start", "t1", null),
                WorkflowTestFixtures.transition("b", "SEQUENTIAL", "t1", "dec", null),
                WorkflowTestFixtures.transition("c", "CONDITIONAL", "dec", "t2", "valido == true"),
                WorkflowTestFixtures.transition("d", "SEQUENTIAL", "t2", "end", null)
        );
        transitions.get(2).setConditionExpression("valido == true");
        stubPolicy(activities, transitions);

        WorkflowRoutingResult result = routingService.routeAfterCompletedActivity(
                "policy-1",
                "t1",
                "Recepción",
                Map.of("valido", true)
        );

        assertEquals(WorkflowRoutingResult.Outcome.ACTIVATE_TASKS, result.getOutcome());
        assertEquals("t2", result.getNextActivities().get(0).getId());
    }

    @Test
    void parallelSplitFromDecision_afterTask() {
        List<WorkflowActivity> activities = List.of(
                WorkflowTestFixtures.activity("start", "START", "Inicio", null),
                WorkflowTestFixtures.activity("t1", "TASK", "T1", "Ops"),
                WorkflowTestFixtures.activity("dec", "DECISION", "Decisión", null),
                WorkflowTestFixtures.activity("b1", "TASK", "Rama A", "A"),
                WorkflowTestFixtures.activity("b2", "TASK", "Rama B", "B"),
                WorkflowTestFixtures.activity("join", "TASK", "Unión", "Ops"),
                WorkflowTestFixtures.activity("end", "END", "Fin", null)
        );
        List<com.workflow.politicas.model.WorkflowTransition> transitions = List.of(
                WorkflowTestFixtures.transition("a", "SEQUENTIAL", "start", "t1", null),
                WorkflowTestFixtures.transition("b", "SEQUENTIAL", "t1", "dec", null),
                WorkflowTestFixtures.transition("p1", "PARALLEL_SPLIT", "dec", "b1", null),
                WorkflowTestFixtures.transition("p2", "PARALLEL_SPLIT", "dec", "b2", null),
                WorkflowTestFixtures.transition("j1", "PARALLEL_JOIN", "b1", "join", null),
                WorkflowTestFixtures.transition("j2", "PARALLEL_JOIN", "b2", "join", null),
                WorkflowTestFixtures.transition("c", "SEQUENTIAL", "join", "end", null)
        );
        stubPolicy(activities, transitions);

        WorkflowRoutingResult result = routingService.routeAfterCompletedActivity("policy-1", "t1", Map.of());

        assertEquals(WorkflowRoutingResult.Outcome.ACTIVATE_TASKS, result.getOutcome());
        assertEquals(2, result.getNextActivities().size());
        assertEquals("join", result.getPendingJoinActivityId());
    }

    private void stubPolicy(
            List<WorkflowActivity> activities,
            List<com.workflow.politicas.model.WorkflowTransition> transitions
    ) {
        when(workflowActivityRepository.findByPolicyIdOrderByOrderIndexAsc("policy-1")).thenReturn(activities);
        when(workflowTransitionRepository.findByPolicyIdOrderByOrderIndexAsc("policy-1")).thenReturn(transitions);
        for (WorkflowActivity activity : activities) {
            when(workflowActivityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        }
    }
}
