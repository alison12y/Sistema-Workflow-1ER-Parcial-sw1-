package com.workflow.politicas.service;

import com.workflow.politicas.dto.WorkflowFlowValidationResponse;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.support.WorkflowTestFixtures;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.repository.WorkflowTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowFlowValidationHelperTest {

    @Mock
    private WorkflowActivityRepository workflowActivityRepository;
    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    private WorkflowRoutingService workflowRoutingService;

    @BeforeEach
    void setUp() {
        workflowRoutingService = new WorkflowRoutingService(
                workflowActivityRepository,
                workflowTransitionRepository,
                new WorkflowConditionEvaluator()
        );
    }

    @Test
    void validate_detectsMultipleStartAndMissingResponsible() {
        List<WorkflowActivity> activities = new ArrayList<>(WorkflowTestFixtures.sequentialFlowActivities());
        activities.add(WorkflowTestFixtures.activity("start2", "START", "Inicio 2", null));
        WorkflowActivity noResp = WorkflowTestFixtures.activity("bad", "TASK", "Sin resp", null);
        noResp.setResponsibleName(null);
        noResp.setResponsibleId(null);
        noResp.setResponsibleType(null);
        activities.add(noResp);

        WorkflowFlowValidationResponse result = WorkflowFlowValidationHelper.validate(
                "policy-1",
                activities,
                WorkflowTestFixtures.sequentialFlowTransitions(),
                workflowRoutingService
        );

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("múltiples")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("responsable")));
    }

    @Test
    void validate_parallelWithoutJoin_isError() {
        List<WorkflowActivity> activities = List.of(
                WorkflowTestFixtures.activity("start", "START", "Inicio", null),
                WorkflowTestFixtures.activity("t1", "TASK", "T1", "Ops"),
                WorkflowTestFixtures.activity("b1", "TASK", "A", "A"),
                WorkflowTestFixtures.activity("b2", "TASK", "B", "B"),
                WorkflowTestFixtures.activity("end", "END", "Fin", null)
        );
        List<com.workflow.politicas.model.WorkflowTransition> transitions = List.of(
                WorkflowTestFixtures.transition("1", "SEQUENTIAL", "start", "t1", null),
                WorkflowTestFixtures.transition("2", "PARALLEL_SPLIT", "t1", "b1", null),
                WorkflowTestFixtures.transition("3", "PARALLEL_SPLIT", "t1", "b2", null)
        );
        when(workflowTransitionRepository.findByPolicyIdOrderByOrderIndexAsc("policy-1")).thenReturn(transitions);

        WorkflowFlowValidationResponse result = WorkflowFlowValidationHelper.validate(
                "policy-1",
                activities,
                transitions,
                workflowRoutingService
        );

        assertTrue(result.getErrors().stream().anyMatch(e ->
                e.contains("PARALLEL_JOIN") || e.contains("sin PARALLEL_JOIN")));
    }

    @Test
    void validate_conditionalWithoutCondition_isError() {
        List<WorkflowActivity> activities = WorkflowTestFixtures.sequentialFlowActivities();
        List<com.workflow.politicas.model.WorkflowTransition> transitions = new ArrayList<>();
        transitions.add(WorkflowTestFixtures.transition("c", "CONDITIONAL", "t1", "t2", null));

        WorkflowFlowValidationResponse result = WorkflowFlowValidationHelper.validate(
                "policy-1",
                activities,
                transitions,
                workflowRoutingService
        );

        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("condicional")));
    }
}
