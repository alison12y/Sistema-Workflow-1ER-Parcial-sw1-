package com.workflow.politicas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.politicas.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class AiWorkflowSuggestMapper {

    private AiWorkflowSuggestMapper() {}

    public static AiWorkflowSuggestResponse fromMap(Map<String, Object> raw, ObjectMapper mapper) {
        if (raw == null) {
            return WorkflowSuggestLocalParser.parse(emptyRequest());
        }
        try {
            return mapper.convertValue(raw, AiWorkflowSuggestResponse.class);
        } catch (IllegalArgumentException e) {
            AiWorkflowSuggestResponse response = new AiWorkflowSuggestResponse();
            response.setAiAvailable(bool(raw.get("aiAvailable")));
            response.setFallbackUsed(bool(raw.get("fallbackUsed")));
            response.setError(str(raw.get("error")));
            response.setExplanation(str(raw.get("explanation")));
            response.setFlowType(str(raw.get("flowType")));
            response.setIntent(str(raw.get("intent")));
            response.setRequiresConfirmation(
                    raw.get("requiresConfirmation") == null || Boolean.TRUE.equals(raw.get("requiresConfirmation")));
            response.setSuggestedActivities(mapActivities((List<Map<String, Object>>) raw.get("suggestedActivities")));
            response.setSuggestedTransitions(mapTransitions((List<Map<String, Object>>) raw.get("suggestedTransitions")));
            response.setSuggestedResponsibles(mapResponsibles((List<Map<String, Object>>) raw.get("suggestedResponsibles")));
            response.setSuggestions(stringList(raw.get("suggestions")));
            response.setWarnings(stringList(raw.get("warnings")));
            return response;
        }
    }

    public static Map<String, Object> toAiServiceBody(AiWorkflowSuggestRequest request) {
        return Map.of(
                "policyId", request.getPolicyId() != null ? request.getPolicyId() : "",
                "prompt", request.getPrompt(),
                "activities", request.getActivities() != null ? request.getActivities() : List.of(),
                "transitions", request.getTransitions() != null ? request.getTransitions() : List.of(),
                "lanes", request.getLanes() != null ? request.getLanes() : List.of()
        );
    }

    private static AiWorkflowSuggestRequest emptyRequest() {
        AiWorkflowSuggestRequest r = new AiWorkflowSuggestRequest();
        r.setPrompt("");
        return r;
    }

    private static List<AiSuggestActivityItem> mapActivities(List<Map<String, Object>> list) {
        List<AiSuggestActivityItem> out = new ArrayList<>();
        if (list == null) return out;
        for (Map<String, Object> m : list) {
            AiSuggestActivityItem item = new AiSuggestActivityItem();
            item.setOperation(strOr(m.get("operation"), "CREATE"));
            item.setName(str(m.get("name")));
            item.setActivityType(strOr(m.get("activityType"), "TASK"));
            item.setResponsibleName(str(m.get("responsibleName")));
            item.setResponsibleType(strOr(m.get("responsibleType"), "ROLE"));
            if (m.get("orderIndex") instanceof Number n) {
                item.setOrderIndex(n.intValue());
            }
            item.setConnectAfterActivityName(str(m.get("connectAfterActivityName")));
            out.add(item);
        }
        return out;
    }

    private static List<AiSuggestTransitionItem> mapTransitions(List<Map<String, Object>> list) {
        List<AiSuggestTransitionItem> out = new ArrayList<>();
        if (list == null) return out;
        for (Map<String, Object> m : list) {
            AiSuggestTransitionItem item = new AiSuggestTransitionItem();
            item.setOperation(strOr(m.get("operation"), "CREATE"));
            item.setFromActivityName(str(m.get("fromActivityName")));
            item.setToActivityName(str(m.get("toActivityName")));
            item.setTransitionType(strOr(m.get("transitionType"), "SEQUENTIAL"));
            item.setConditionLabel(str(m.get("conditionLabel")));
            out.add(item);
        }
        return out;
    }

    private static List<AiSuggestResponsibleItem> mapResponsibles(List<Map<String, Object>> list) {
        List<AiSuggestResponsibleItem> out = new ArrayList<>();
        if (list == null) return out;
        for (Map<String, Object> m : list) {
            AiSuggestResponsibleItem item = new AiSuggestResponsibleItem();
            item.setName(str(m.get("name")));
            item.setType(strOr(m.get("type"), "ROLE"));
            out.add(item);
        }
        return out;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o != null) out.add(String.valueOf(o));
        }
        return out;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static String strOr(Object o, String def) {
        String s = str(o);
        return s == null || s.isBlank() ? def : s;
    }

    private static Boolean bool(Object o) {
        if (o instanceof Boolean b) return b;
        return null;
    }
}
