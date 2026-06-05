package com.workflow.politicas.service;

import com.workflow.politicas.model.WorkflowTransition;
import com.workflow.politicas.repository.WorkflowTransitionRepository;
import com.workflow.politicas.util.FormFieldKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Copia {@code conditionLabel} → {@code conditionExpression} cuando el diseñador
 * guardó la condición solo como etiqueta visible (CU7).
 */
@Service
public class WorkflowTransitionConditionMigrationService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTransitionConditionMigrationService.class);

    private final WorkflowTransitionRepository workflowTransitionRepository;

    public WorkflowTransitionConditionMigrationService(WorkflowTransitionRepository workflowTransitionRepository) {
        this.workflowTransitionRepository = workflowTransitionRepository;
    }

    public void migrateConditionalExpressionsFromLabels() {
        List<WorkflowTransition> transitions = workflowTransitionRepository.findAll();
        int updated = 0;
        for (WorkflowTransition transition : transitions) {
            if (!transition.isActive()) {
                continue;
            }
            if (!isConditional(transition.getTransitionType())) {
                continue;
            }
            String label = transition.getConditionLabel();
            if (label == null || label.isBlank()) {
                continue;
            }
            String expression = transition.getConditionExpression();
            if (expression != null && !expression.isBlank()) {
                continue;
            }
            if (!looksLikeExpression(label)) {
                continue;
            }
            transition.setConditionExpression(label.trim());
            transition.setUpdatedAt(LocalDateTime.now());
            workflowTransitionRepository.save(transition);
            updated++;
        }
        if (updated > 0) {
            log.info(
                    "WorkflowTransitionConditionMigrationService: conditionExpression copiada desde label en {} transición(es)",
                    updated
            );
        }
    }

    private static boolean isConditional(String transitionType) {
        if (transitionType == null || transitionType.isBlank()) {
            return false;
        }
        String type = transitionType.trim().toUpperCase();
        return "CONDITIONAL".equals(type) || type.contains("COND");
    }

    private static boolean looksLikeExpression(String text) {
        return !FormFieldKeyUtil.extractVariablesFromCondition(text).isEmpty()
                || text.contains("==")
                || text.contains("!=");
    }
}
