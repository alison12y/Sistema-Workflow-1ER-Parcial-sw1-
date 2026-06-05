package com.workflow.politicas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.politicas.dto.*;

import java.util.*;

@SuppressWarnings("unchecked")
public final class FormAssistResponseMapper {

    private FormAssistResponseMapper() {}

    public static AiFormAssistResponse fromMap(Map<String, Object> raw, ObjectMapper mapper) {
        if (raw == null) {
            return new AiFormAssistResponse();
        }
        try {
            AiFormAssistResponse response = mapper.convertValue(raw, AiFormAssistResponse.class);
            syncMaps(response);
            return response;
        } catch (IllegalArgumentException e) {
            AiFormAssistResponse response = new AiFormAssistResponse();
            response.setAiAvailable(bool(raw.get("aiAvailable")));
            response.setFallbackUsed(bool(raw.get("fallbackUsed")));
            response.setError(str(raw.get("error")));
            response.setExplanation(str(raw.get("explanation")));
            response.setSuggestedText(str(raw.get("suggestedText")));
            if (raw.get("confidence") instanceof Number n) {
                response.setConfidence(n.doubleValue());
            }
            response.setFieldSuggestions(mapSuggestions((List<Map<String, Object>>) raw.get("fieldSuggestions")));
            Object values = raw.get("suggestedValues");
            if (values instanceof Map<?, ?> map) {
                Map<String, String> sv = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        sv.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                    }
                }
                response.setSuggestedValues(sv);
            }
            response.setWarnings(stringList(raw.get("warnings")));
            response.setUnmatchedFields(stringList(raw.get("unmatchedFields")));
            syncMaps(response);
            return response;
        }
    }

    public static Map<String, Object> toAiServiceBody(AiFormAssistRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("report", request.getReport());
        body.put("prompt", request.getReport());
        body.put("policyId", request.getPolicyId());
        body.put("tramiteId", request.getTramiteId());
        body.put("workflowActivityId", request.getWorkflowActivityId());
        body.put("formId", request.getFormId());
        body.put("activityName", request.getActivityName());
        body.put("fields", request.getFields());
        body.put("currentValues", request.getCurrentValues());
        body.put("context", request.getContext());
        return body;
    }

    private static void syncMaps(AiFormAssistResponse response) {
        if (response.getSuggestedValues() == null) {
            response.setSuggestedValues(new LinkedHashMap<>());
        }
        if (response.getFieldSuggestions() == null) {
            response.setFieldSuggestions(new ArrayList<>());
            return;
        }
        for (AiFormFieldSuggestionDto s : response.getFieldSuggestions()) {
            if (Boolean.TRUE.equals(s.getApplicable()) && s.getFieldName() != null && s.getSuggestedValue() != null) {
                response.getSuggestedValues().putIfAbsent(s.getFieldName(), s.getSuggestedValue());
            }
        }
    }

    private static List<AiFormFieldSuggestionDto> mapSuggestions(List<Map<String, Object>> list) {
        List<AiFormFieldSuggestionDto> out = new ArrayList<>();
        if (list == null) return out;
        for (Map<String, Object> m : list) {
            AiFormFieldSuggestionDto dto = new AiFormFieldSuggestionDto();
            dto.setFieldName(str(m.get("fieldName")));
            dto.setFieldLabel(str(m.get("fieldLabel")));
            dto.setFieldType(str(m.get("fieldType")));
            dto.setSuggestedValue(str(m.get("suggestedValue")));
            dto.setMessage(str(m.get("message")));
            dto.setApplicable(bool(m.get("applicable")));
            if (m.get("confidence") instanceof Number n) {
                dto.setConfidence(n.doubleValue());
            }
            out.add(dto);
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

    private static Boolean bool(Object o) {
        if (o instanceof Boolean b) return b;
        return null;
    }
}
