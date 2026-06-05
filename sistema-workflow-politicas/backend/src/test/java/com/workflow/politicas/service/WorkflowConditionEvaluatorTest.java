package com.workflow.politicas.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowConditionEvaluatorTest {

    private final WorkflowConditionEvaluator evaluator = new WorkflowConditionEvaluator();

    @Test
    void evaluatesValidoTrueWithBooleanStepData() {
        assertTrue(evaluator.evaluate("valido == true", null, Map.of("valido", Boolean.TRUE)));
    }

    @Test
    void evaluatesValidoTrueWithStringStepData() {
        assertTrue(evaluator.evaluate("valido == true", null, Map.of("valido", "true")));
    }

    @Test
    void evaluatesValidoFalseWithBooleanStepData() {
        assertTrue(evaluator.evaluate("valido == false", null, Map.of("valido", Boolean.FALSE)));
    }

    @Test
    void evaluatesValidoFalseWithStringStepData() {
        assertTrue(evaluator.evaluate("valido == false", null, Map.of("valido", "false")));
    }

    @Test
    void evaluatesExpressionStoredInConditionLabelOnly() {
        assertTrue(evaluator.evaluate(null, "valido == true", Map.of("valido", Boolean.TRUE)));
        assertTrue(evaluator.evaluate(null, "valido == false", Map.of("valido", "false")));
    }

    @Test
    void failsWhenValidoMissing() {
        assertFalse(evaluator.evaluate("valido == true", null, Map.of()));
        assertFalse(evaluator.evaluate("valido == false", null, Map.of("llenar", true)));
    }
}
