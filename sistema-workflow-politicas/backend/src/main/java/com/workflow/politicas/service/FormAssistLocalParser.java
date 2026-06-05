package com.workflow.politicas.service;

import com.workflow.politicas.dto.*;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser local para mapear informe libre a campos de formulario. */
public final class FormAssistLocalParser {

    private static final String FALLBACK_MSG =
            "Sugerencia generada con parser local (IA externa no disponible).";

    private FormAssistLocalParser() {}

    public static AiFormAssistResponse parse(AiFormAssistRequest request) {
        String report = request.getReport() != null ? request.getReport().trim() : "";
        AiFormAssistResponse response = new AiFormAssistResponse();
        response.setAiAvailable(false);
        response.setFallbackUsed(true);
        response.setError(FALLBACK_MSG);
        response.setConfidence(0.55);

        List<AiFormFieldDefinitionDto> fields = request.getFields() != null ? request.getFields() : List.of();
        Map<String, String> current = request.getCurrentValues() != null ? request.getCurrentValues() : Map.of();

        if (report.isBlank()) {
            response.setExplanation("Indique un informe o descripción en lenguaje natural.");
            return response;
        }

        List<AiFormFieldSuggestionDto> suggestions = new ArrayList<>();
        Map<String, String> values = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        int matched = 0;

        Optional<SpanishDateExtraction.DateRange> dateRange = SpanishDateExtraction.extractRange(report);
        boolean reportHasDates = SpanishDateExtraction.containsAnyDate(report);

        for (AiFormFieldDefinitionDto field : fields) {
            String type = normalizeType(field.getType());
            String name = field.getName() != null ? field.getName() : slug(field.getLabel());
            String label = field.getLabel() != null ? field.getLabel() : name;

            if ("FILE".equals(type)) {
                suggestions.add(suggestion(name, label, type, null, false,
                        "Los campos FILE no se autocompletan; adjunte el archivo manualmente."));
                continue;
            }

            String extracted = extractForField(report, field, type, name, label, dateRange, reportHasDates);
            if (extracted == null || extracted.isBlank()) {
                if (Boolean.TRUE.equals(field.getRequired())) {
                    unmatched.add(label);
                }
                suggestions.add(suggestion(name, label, type, null, false,
                        "No se detectó valor en el informe para este campo."));
                continue;
            }

            String normalized = normalizeValue(type, extracted, field);
            if (normalized == null) {
                warnings.add("Valor no válido para " + label + " (" + type + ")");
                unmatched.add(label);
                suggestions.add(suggestion(name, label, type, null, false, "Valor no compatible con el tipo de campo."));
                continue;
            }

            matched++;
            values.put(name, normalized);
            suggestions.add(suggestion(name, label, type, normalized, true, null));
        }

        response.setFieldSuggestions(suggestions);
        response.setSuggestedValues(values);
        response.setUnmatchedFields(unmatched);
        response.setWarnings(warnings);
        response.setSuggestedText(report.length() > 200 ? report.substring(0, 200) + "…" : report);
        response.setExplanation(
                FALLBACK_MSG + " Se mapearon " + matched + " de " + fields.size() + " campos según etiquetas y patrones del informe.");
        if (matched == 0) {
            response.getWarnings().add("Revise que el informe mencione datos alineados con las etiquetas del formulario.");
        }
        if (!current.isEmpty()) {
            response.getWarnings().add("Los campos ya completados no se sobrescribirán sin confirmación en el cliente.");
        }
        return response;
    }

    private static String extractForField(
            String report,
            AiFormFieldDefinitionDto field,
            String type,
            String name,
            String label,
            Optional<SpanishDateExtraction.DateRange> dateRange,
            boolean reportHasDates
    ) {
        if ("DATE".equals(type)) {
            return extractDateForField(report, name, label, dateRange, reportHasDates);
        }

        String lowReport = report.toLowerCase(Locale.ROOT);
        String normLabel = normalize(label);
        String normName = normalize(name);

        Pattern labeled = Pattern.compile(
                "(?:" + Pattern.quote(label) + "|" + Pattern.quote(name) + ")\\s*[:=]\\s*([^\\n.;]+)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m = labeled.matcher(report);
        if (m.find()) {
            return m.group(1).trim();
        }

        if (lowReport.contains(normLabel) || lowReport.contains(normName)) {
            if ("CHECKBOX".equals(type)) {
                if (containsAny(lowReport, "acepto", "aceptar", "confirmo", "sí acepto", "si acepto")) {
                    return "true";
                }
            }
            if ("NUMBER".equals(type)) {
                Matcher nm = Pattern.compile("(?:monto|cantidad|total|valor)\\s*[:=]?\\s*([\\d.,]+)", Pattern.CASE_INSENSITIVE)
                        .matcher(report);
                if (nm.find()) return nm.group(1).replace(",", ".");
            }
            if ("SELECT".equals(type) || "RADIO".equals(type)) {
                return matchOption(report, field.getOptions());
            }
            if ("TEXTAREA".equals(type) || normLabel.contains("observ") || normLabel.contains("motivo")
                    || normLabel.contains("coment") || normLabel.contains("descripcion")) {
                return report.length() > 500 ? report.substring(0, 500) : report;
            }
        }

        if (("TEXTAREA".equals(type) || "TEXT".equals(type))
                && (normLabel.contains("observ") || normLabel.contains("motivo") || normLabel.contains("informe"))) {
            return report.length() > 400 ? report.substring(0, 400) : report;
        }

        return null;
    }

    private static String extractDateForField(
            String report,
            String name,
            String label,
            Optional<SpanishDateExtraction.DateRange> dateRange,
            boolean reportHasDates
    ) {
        if (dateRange.isPresent()) {
            if (SpanishDateExtraction.isStartDateField(name, label)) {
                return dateRange.get().startIso();
            }
            if (SpanishDateExtraction.isEndDateField(name, label)) {
                return dateRange.get().endIso();
            }
        }

        Optional<String> firstIso = SpanishDateExtraction.extractFirstIsoDate(report);
        if (firstIso.isPresent()) {
            if (SpanishDateExtraction.isEndDateField(name, label) && dateRange.isPresent()) {
                return dateRange.get().endIso();
            }
            return firstIso.get();
        }

        String normLabel = normalize(label);
        if (normLabel.contains("fecha") && !reportHasDates) {
            return LocalDate.now().toString();
        }

        return null;
    }

    private static String matchOption(String report, String optionsCsv) {
        if (optionsCsv == null || optionsCsv.isBlank()) return null;
        String low = report.toLowerCase(Locale.ROOT);
        for (String opt : optionsCsv.split(",")) {
            String trimmed = opt.trim();
            if (!trimmed.isEmpty() && low.contains(trimmed.toLowerCase(Locale.ROOT))) {
                return trimmed;
            }
        }
        return null;
    }

    private static String normalizeValue(String type, String raw, AiFormFieldDefinitionDto field) {
        String v = raw.trim();
        return switch (type) {
            case "CHECKBOX" -> {
                String low = v.toLowerCase(Locale.ROOT);
                yield (low.contains("si") || low.contains("sí") || low.contains("true") || low.contains("acept"))
                        ? "true" : "false";
            }
            case "NUMBER" -> {
                String num = v.replaceAll("[^\\d.,-]", "").replace(",", ".");
                yield num.isBlank() ? null : num;
            }
            case "DATE" -> {
                if (v.matches("\\d{4}-\\d{2}-\\d{2}")) yield v;
                yield null;
            }
            case "SELECT", "RADIO" -> {
                String opt = matchOption(v, field.getOptions());
                yield opt != null ? opt : v;
            }
            case "FILE" -> null;
            default -> v;
        };
    }

    private static AiFormFieldSuggestionDto suggestion(
            String name,
            String label,
            String type,
            String value,
            boolean applicable,
            String message
    ) {
        AiFormFieldSuggestionDto dto = new AiFormFieldSuggestionDto();
        dto.setFieldName(name);
        dto.setFieldLabel(label);
        dto.setFieldType(type);
        dto.setSuggestedValue(value);
        dto.setApplicable(applicable);
        dto.setConfidence(applicable ? 0.6 : 0.0);
        dto.setMessage(message);
        return dto;
    }

    private static String normalizeType(String type) {
        if (type == null) return "TEXT";
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalize(String s) {
        return s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replaceAll("[áàä]", "a")
                .replaceAll("[éèë]", "e")
                .replaceAll("[íìï]", "i")
                .replaceAll("[óòö]", "o")
                .replaceAll("[úùü]", "u");
    }

    private static String slug(String label) {
        return label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private static boolean containsAny(String text, String... parts) {
        for (String p : parts) {
            if (text.contains(p)) return true;
        }
        return false;
    }
}
