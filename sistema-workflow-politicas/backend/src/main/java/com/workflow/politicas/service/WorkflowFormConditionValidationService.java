package com.workflow.politicas.service;

import com.workflow.politicas.model.FormField;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.model.WorkflowTransition;
import com.workflow.politicas.repository.DynamicFormRepository;
import com.workflow.politicas.repository.FormFieldRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.repository.WorkflowTransitionRepository;
import com.workflow.politicas.util.FormFieldKeyUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cruza formularios dinámicos con condiciones CONDITIONAL del workflow (Ciclo 1).
 */
@Service
public class WorkflowFormConditionValidationService {

    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowActivityRepository workflowActivityRepository;
    private final DynamicFormRepository dynamicFormRepository;
    private final FormFieldRepository formFieldRepository;

    public WorkflowFormConditionValidationService(
            WorkflowTransitionRepository workflowTransitionRepository,
            WorkflowActivityRepository workflowActivityRepository,
            DynamicFormRepository dynamicFormRepository,
            FormFieldRepository formFieldRepository
    ) {
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.workflowActivityRepository = workflowActivityRepository;
        this.dynamicFormRepository = dynamicFormRepository;
        this.formFieldRepository = formFieldRepository;
    }

    public void appendFormConditionWarnings(String policyId, List<String> warnings) {
        List<WorkflowActivity> activities = workflowActivityRepository.findByPolicyIdOrderByOrderIndexAsc(policyId);
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByPolicyIdOrderByOrderIndexAsc(policyId)
                .stream()
                .filter(WorkflowTransition::isActive)
                .toList();

        Map<String, WorkflowActivity> byId = activities.stream()
                .filter(a -> a.getId() != null && a.isActive())
                .collect(Collectors.toMap(WorkflowActivity::getId, a -> a, (a, b) -> a));

        for (WorkflowActivity decision : activities) {
            if (!decision.isActive() || !"DECISION".equalsIgnoreCase(decision.getActivityType())) {
                continue;
            }
            List<WorkflowTransition> incoming = transitions.stream()
                    .filter(t -> decision.getId().equals(t.getToActivityId()))
                    .toList();
            List<WorkflowTransition> conditionalOut = transitions.stream()
                    .filter(t -> decision.getId().equals(t.getFromActivityId()))
                    .filter(this::isConditional)
                    .toList();
            if (conditionalOut.isEmpty()) {
                continue;
            }

            Set<String> requiredVars = new LinkedHashSet<>();
            for (WorkflowTransition transition : conditionalOut) {
                requiredVars.addAll(extractRequiredVariables(transition));
            }
            if (requiredVars.isEmpty()) {
                continue;
            }

            for (WorkflowTransition in : incoming) {
                WorkflowActivity source = byId.get(in.getFromActivityId());
                if (source == null || !"TASK".equalsIgnoreCase(source.getActivityType())) {
                    continue;
                }
                Set<String> formKeys = loadFormFieldKeys(source.getId());
                for (String variable : requiredVars) {
                    if (!formKeys.contains(variable)) {
                        for (WorkflowTransition cond : conditionalOut) {
                            String expression = cond.getConditionExpression();
                            if (expression != null
                                    && FormFieldKeyUtil.extractVariablesFromCondition(expression).contains(variable)) {
                                warnings.add(
                                        "La condición '"
                                                + expression.trim()
                                                + "' requiere un campo de formulario llamado '"
                                                + variable
                                                + "' en la actividad "
                                                + source.getName()
                                                + ".");
                            }
                        }
                    }
                }
            }
        }
    }

    public void validateStepDataForCompletion(
            String workflowActivityId,
            String policyId,
            Map<String, Object> stepData,
            boolean hasConfiguredForm
    ) {
        List<String> required = requiredVariablesBeforeDecision(workflowActivityId, policyId);
        if (required.isEmpty()) {
            return;
        }

        Set<String> present = new HashSet<>();
        if (stepData != null) {
            for (Map.Entry<String, Object> entry : stepData.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank()) {
                    present.add(entry.getKey().trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        List<String> missing = required.stream()
                .filter(v -> !present.contains(v))
                .toList();

        if (missing.isEmpty()) {
            return;
        }

        if (!hasConfiguredForm) {
            throw new IllegalArgumentException(
                    "Esta actividad lleva a una decisión que requiere datos del formulario: "
                            + String.join(", ", missing));
        }

        throw new IllegalArgumentException(
                "Complete el formulario con los campos requeridos para la decisión: "
                        + String.join(", ", missing));
    }

    public List<String> requiredVariablesBeforeDecision(String completedActivityId, String policyId) {
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByPolicyIdOrderByOrderIndexAsc(policyId)
                .stream()
                .filter(WorkflowTransition::isActive)
                .toList();

        List<String> required = new ArrayList<>();
        for (WorkflowTransition outgoing : transitions) {
            if (!completedActivityId.equals(outgoing.getFromActivityId())) {
                continue;
            }
            WorkflowActivity target = workflowActivityRepository.findById(outgoing.getToActivityId()).orElse(null);
            if (target == null || !target.isActive() || !"DECISION".equalsIgnoreCase(target.getActivityType())) {
                continue;
            }
            for (WorkflowTransition decisionOut : transitions) {
                if (!target.getId().equals(decisionOut.getFromActivityId()) || !isConditional(decisionOut)) {
                    continue;
                }
                required.addAll(extractRequiredVariables(decisionOut));
            }
        }
        return required.stream().distinct().toList();
    }

    private Set<String> loadFormFieldKeys(String workflowActivityId) {
        return dynamicFormRepository.findByActivityIdAndActiveTrue(workflowActivityId).stream()
                .findFirst()
                .map(form -> formFieldRepository.findByFormIdOrderByOrderAsc(form.getId()).stream()
                        .filter(FormField::isActive)
                        .map(FormField::getName)
                        .filter(Objects::nonNull)
                        .map(name -> name.trim().toLowerCase(Locale.ROOT))
                        .filter(name -> !name.isBlank())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private List<String> extractRequiredVariables(WorkflowTransition transition) {
        if (transition.getConditionExpression() != null && !transition.getConditionExpression().isBlank()) {
            return FormFieldKeyUtil.extractVariablesFromCondition(transition.getConditionExpression());
        }
        return List.of();
    }

    private boolean isConditional(WorkflowTransition transition) {
        if (transition.getTransitionType() == null) {
            return false;
        }
        String type = transition.getTransitionType().trim().toUpperCase(Locale.ROOT);
        return "CONDITIONAL".equals(type) || type.contains("COND");
    }
}
