package com.workflow.politicas.support;

import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.model.WorkflowTransition;

import java.util.ArrayList;
import java.util.List;

public final class WorkflowTestFixtures {

    private WorkflowTestFixtures() {
    }

    public static WorkflowActivity activity(String id, String type, String name, String responsibleName) {
        WorkflowActivity a = new WorkflowActivity();
        a.setId(id);
        a.setPolicyId("policy-1");
        a.setActivityType(type);
        a.setName(name);
        a.setResponsibleName(responsibleName);
        a.setResponsibleType(responsibleName != null ? "USER" : null);
        a.setResponsibleId(responsibleName);
        a.setActive(true);
        a.setOrderIndex(1);
        return a;
    }

    public static WorkflowTransition transition(
            String id,
            String type,
            String from,
            String to,
            String conditionLabel
    ) {
        WorkflowTransition t = new WorkflowTransition();
        t.setId(id);
        t.setPolicyId("policy-1");
        t.setTransitionType(type);
        t.setFromActivityId(from);
        t.setToActivityId(to);
        t.setFromActivityName(from);
        t.setToActivityName(to);
        t.setConditionLabel(conditionLabel);
        t.setActive(true);
        t.setOrderIndex(1);
        return t;
    }

    public static List<WorkflowActivity> sequentialFlowActivities() {
        List<WorkflowActivity> list = new ArrayList<>();
        list.add(activity("start", "START", "Inicio", null));
        list.add(activity("t1", "TASK", "Recepción", "Operaciones"));
        list.add(activity("t2", "TASK", "Cierre", "Operaciones"));
        list.add(activity("end", "END", "Fin", null));
        return list;
    }

    public static List<WorkflowTransition> sequentialFlowTransitions() {
        List<WorkflowTransition> list = new ArrayList<>();
        list.add(transition("tr1", "SEQUENTIAL", "start", "t1", null));
        list.add(transition("tr2", "SEQUENTIAL", "t1", "t2", null));
        list.add(transition("tr3", "SEQUENTIAL", "t2", "end", null));
        return list;
    }
}
