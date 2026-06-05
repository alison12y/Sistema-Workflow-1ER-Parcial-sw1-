package com.workflow.politicas.service;

import com.workflow.politicas.util.Cu7WorkflowDebugLog;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evalúa condiciones simples para transiciones CONDITIONAL del Ciclo 1.
 * Ejemplos: {@code monto > 1000}, {@code aprobado == true}, {@code tipo == "EMPRESA"}.
 */
@Component
public class WorkflowConditionEvaluator {

    private static final Pattern COMPARISON = Pattern.compile(
            "^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*(==|!=|>=|<=|>|<)\\s*(.+?)\\s*$"
    );

    public boolean evaluate(String expression, String conditionLabel, Map<String, Object> stepData) {
        if (expression != null && !expression.isBlank()) {
            boolean result = evaluateExpression(expression.trim(), stepData);
            Cu7WorkflowDebugLog.log(
                    "evaluate expr='{}' stepData={} valido={} -> {}",
                    expression.trim(),
                    Cu7WorkflowDebugLog.stepDataSummary(stepData),
                    resolveFieldValueForDebug("valido", stepData),
                    result
            );
            if (result) {
                return true;
            }
        }
        if (conditionLabel != null && !conditionLabel.isBlank()) {
            String label = conditionLabel.trim();
            if (looksLikeExpression(label)) {
                boolean result = evaluateExpression(label, stepData);
                Cu7WorkflowDebugLog.log(
                        "evaluate label-as-expr='{}' stepData={} valido={} -> {}",
                        label,
                        Cu7WorkflowDebugLog.stepDataSummary(stepData),
                        resolveFieldValueForDebug("valido", stepData),
                        result
                );
                if (result) {
                    return true;
                }
            }
            boolean labelResult = evaluateLabel(label, stepData);
            Cu7WorkflowDebugLog.log(
                    "evaluate label='{}' stepData={} -> {}",
                    label,
                    Cu7WorkflowDebugLog.stepDataSummary(stepData),
                    labelResult
            );
            return labelResult;
        }
        Cu7WorkflowDebugLog.log("evaluate sin expresión ni etiqueta stepData={}", Cu7WorkflowDebugLog.stepDataSummary(stepData));
        return false;
    }

    private static boolean looksLikeExpression(String text) {
        return text.contains("==")
                || text.contains("!=")
                || text.contains(">=")
                || text.contains("<=")
                || text.contains(">")
                || text.contains("<");
    }

    private Object resolveFieldValueForDebug(String field, Map<String, Object> stepData) {
        if (stepData == null) {
            return null;
        }
        if (stepData.containsKey(field)) {
            return stepData.get(field);
        }
        for (Map.Entry<String, Object> entry : stepData.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(field)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean evaluateLabel(String label, Map<String, Object> stepData) {
        if (stepData == null || stepData.isEmpty()) {
            return false;
        }
        String normalizedLabel = normalize(label);
        for (Map.Entry<String, Object> entry : stepData.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String valueStr = String.valueOf(value).trim();
            if (normalize(valueStr).equals(normalizedLabel)) {
                return true;
            }
            if (truthy(value) && matchesPositiveLabel(normalizedLabel)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPositiveLabel(String normalizedLabel) {
        return normalizedLabel.contains("APROB")
                || normalizedLabel.contains("ACEPT")
                || normalizedLabel.contains("SI")
                || normalizedLabel.equals("OK")
                || normalizedLabel.equals("TRUE");
    }

    private boolean evaluateExpression(String expression, Map<String, Object> stepData) {
        Matcher matcher = COMPARISON.matcher(expression);
        if (!matcher.matches()) {
            return evaluateLabel(expression, stepData);
        }
        String field = matcher.group(1);
        String operator = matcher.group(2);
        String rawRhs = matcher.group(3).trim();
        Object lhsValue = resolveFieldValue(field, stepData);
        if (lhsValue == null) {
            return false;
        }
        return compare(lhsValue, operator, parseRhs(rawRhs));
    }

    private Object resolveFieldValue(String field, Map<String, Object> stepData) {
        if (stepData == null) {
            return null;
        }
        if (stepData.containsKey(field)) {
            return stepData.get(field);
        }
        String fieldNorm = normalize(field);
        for (Map.Entry<String, Object> entry : stepData.entrySet()) {
            if (normalize(entry.getKey()).equals(fieldNorm)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Object parseRhs(String raw) {
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
            return raw.substring(1, raw.length() - 1);
        }
        if (raw.startsWith("'") && raw.endsWith("'") && raw.length() >= 2) {
            return raw.substring(1, raw.length() - 1);
        }
        if ("true".equalsIgnoreCase(raw)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return Boolean.FALSE;
        }
        try {
            if (raw.contains(".")) {
                return Double.parseDouble(raw);
            }
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return raw;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean compare(Object lhs, String operator, Object rhs) {
        if ("==".equals(operator)) {
            return valuesEqual(lhs, rhs);
        }
        if ("!=".equals(operator)) {
            return !valuesEqual(lhs, rhs);
        }
        Double leftNum = toNumber(lhs);
        Double rightNum = toNumber(rhs);
        if (leftNum == null || rightNum == null) {
            return false;
        }
        return switch (operator) {
            case ">" -> leftNum > rightNum;
            case "<" -> leftNum < rightNum;
            case ">=" -> leftNum >= rightNum;
            case "<=" -> leftNum <= rightNum;
            default -> false;
        };
    }

    private boolean valuesEqual(Object lhs, Object rhs) {
        if (lhs == null && rhs == null) {
            return true;
        }
        if (lhs == null || rhs == null) {
            return false;
        }
        if (lhs instanceof Boolean || rhs instanceof Boolean || isBooleanLike(lhs) || isBooleanLike(rhs)) {
            return toBoolean(lhs) == toBoolean(rhs);
        }
        Double leftNum = toNumber(lhs);
        Double rightNum = toNumber(rhs);
        if (leftNum != null && rightNum != null) {
            return Double.compare(leftNum, rightNum) == 0;
        }
        return normalize(String.valueOf(lhs)).equals(normalize(String.valueOf(rhs)));
    }

    private boolean isBooleanLike(Object value) {
        if (value instanceof Boolean) {
            return true;
        }
        String s = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return truthy(value);
    }

    private Double toNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean truthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return false;
        }
        return !"false".equalsIgnoreCase(s) && !"0".equals(s) && !"no".equalsIgnoreCase(s);
    }

    private String normalize(String value) {
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ñ", "N");
    }
}
