package com.workflow.politicas.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae fechas y rangos en español desde informes libres (CU15).
 */
public final class SpanishDateExtraction {

    public record DateRange(LocalDate start, LocalDate end) {
        public String startIso() {
            return start.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        public String endIso() {
            return end.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    private static final Map<String, Integer> MONTHS = buildMonths();

    private static final Pattern RANGE_TEXT_MONTHS = Pattern.compile(
            "(?:desde|del)\\s+(?:el\\s+)?(\\d{1,2})\\s+(?:de\\s+)?([a-záéíóúñ]+)"
                    + "\\s+(?:hasta|al)\\s+(?:el\\s+)?(\\d{1,2})\\s+(?:de\\s+)?([a-záéíóúñ]+)"
                    + "(?:\\s+de\\s+(\\d{4}))?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern RANGE_SLASH = Pattern.compile(
            "(?:desde|del)\\s+(\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?)"
                    + "\\s+(?:hasta|al)\\s+(\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RANGE_INICIA_TERMINA = Pattern.compile(
            "inicia\\s+(?:el\\s+)?(\\d{1,2})\\s+(?:de\\s+)?([a-záéíóúñ]+)"
                    + "\\s+y\\s+termina\\s+(?:el\\s+)?(\\d{1,2})\\s+(?:de\\s+)?([a-záéíóúñ]+)"
                    + "(?:\\s+de\\s+(\\d{4}))?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern SINGLE_TEXT_MONTH = Pattern.compile(
            "(?:el\\s+)?(\\d{1,2})\\s+de\\s+([a-záéíóúñ]+)(?:\\s+de\\s+(\\d{4}))?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern SLASH_DATE = Pattern.compile("\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?");

    private SpanishDateExtraction() {}

    public static Optional<DateRange> extractRange(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher inicia = RANGE_INICIA_TERMINA.matcher(text);
        if (inicia.find()) {
            return buildTextMonthRange(
                    inicia.group(1), inicia.group(2), inicia.group(3), inicia.group(4), inicia.group(5)
            );
        }

        Matcher slash = RANGE_SLASH.matcher(text);
        if (slash.find()) {
            return buildSlashRange(slash.group(1), slash.group(2));
        }

        Matcher textMonths = RANGE_TEXT_MONTHS.matcher(text);
        if (textMonths.find()) {
            return buildTextMonthRange(
                    textMonths.group(1), textMonths.group(2),
                    textMonths.group(3), textMonths.group(4), textMonths.group(5)
            );
        }

        return Optional.empty();
    }

    public static boolean containsAnyDate(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return extractRange(text).isPresent()
                || ISO_DATE.matcher(text).find()
                || SLASH_DATE.matcher(text).find()
                || SINGLE_TEXT_MONTH.matcher(text).find();
    }

    public static Optional<String> extractFirstIsoDate(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Optional<DateRange> range = extractRange(text);
        if (range.isPresent()) {
            return Optional.of(range.get().startIso());
        }

        Matcher iso = ISO_DATE.matcher(text);
        if (iso.find()) {
            return Optional.of(iso.group());
        }

        Matcher slash = SLASH_DATE.matcher(text);
        if (slash.find()) {
            return parseSlashDate(slash.group()).map(LocalDate::toString);
        }

        Matcher single = SINGLE_TEXT_MONTH.matcher(text);
        if (single.find()) {
            return parseTextMonthDate(single.group(1), single.group(2), resolveYear(single.group(3)))
                    .map(LocalDate::toString);
        }

        return Optional.empty();
    }

    public static boolean isStartDateField(String name, String label) {
        String n = normalizeToken(name);
        String l = normalizeToken(label);
        return n.contains("inicio") || n.contains("start") || n.equals("fecha_inicio")
                || l.contains("fecha de inicio") || l.equals("inicio") || l.startsWith("inicio ");
    }

    public static boolean isEndDateField(String name, String label) {
        String n = normalizeToken(name);
        String l = normalizeToken(label);
        return (n.contains("fin") && !n.contains("inicio")) || n.contains("end") || n.equals("fecha_fin")
                || l.contains("fecha de fin") || l.equals("fin") || l.endsWith(" fin");
    }

    private static Optional<DateRange> buildTextMonthRange(
            String startDay,
            String startMonth,
            String endDay,
            String endMonth,
            String yearGroup
    ) {
        int year = resolveYear(yearGroup);
        Optional<LocalDate> start = parseTextMonthDate(startDay, startMonth, year);
        int endMonthNum = monthNumber(endMonth);
        if (endMonthNum < 0) {
            endMonthNum = monthNumber(startMonth);
        }
        Optional<LocalDate> end = parseDayMonthYear(endDay, endMonthNum, year);
        if (start.isEmpty() || end.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DateRange(start.get(), end.get()));
    }

    private static Optional<DateRange> buildSlashRange(String startRaw, String endRaw) {
        Optional<LocalDate> start = parseSlashDate(startRaw);
        Optional<LocalDate> end = parseSlashDate(endRaw);
        if (start.isEmpty() || end.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DateRange(start.get(), end.get()));
    }

    private static Optional<LocalDate> parseSlashDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String[] parts = raw.trim().split("/");
        if (parts.length < 2) {
            return Optional.empty();
        }
        int day = parseInt(parts[0]);
        int month = parseInt(parts[1]);
        int year = parts.length >= 3 ? normalizeYear(parseInt(parts[2])) : LocalDate.now().getYear();
        if (day < 1 || month < 1 || month > 12) {
            return Optional.empty();
        }
        return safeDate(year, month, day);
    }

    private static Optional<LocalDate> parseTextMonthDate(String dayRaw, String monthRaw, int year) {
        int month = monthNumber(monthRaw);
        if (month < 0) {
            return Optional.empty();
        }
        return parseDayMonthYear(dayRaw, month, year);
    }

    private static Optional<LocalDate> parseDayMonthYear(String dayRaw, int month, int year) {
        int day = parseInt(dayRaw);
        if (day < 1) {
            return Optional.empty();
        }
        return safeDate(year, month, day);
    }

    private static Optional<LocalDate> safeDate(int year, int month, int day) {
        try {
            return Optional.of(LocalDate.of(year, month, day));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static int resolveYear(String yearRaw) {
        if (yearRaw == null || yearRaw.isBlank()) {
            return LocalDate.now().getYear();
        }
        return normalizeYear(parseInt(yearRaw));
    }

    private static int normalizeYear(int year) {
        if (year < 100) {
            return 2000 + year;
        }
        return year;
    }

    private static int parseInt(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static int monthNumber(String monthRaw) {
        if (monthRaw == null) {
            return -1;
        }
        String key = normalizeToken(monthRaw);
        return MONTHS.getOrDefault(key, -1);
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[áàä]", "a")
                .replaceAll("[éèë]", "e")
                .replaceAll("[íìï]", "i")
                .replaceAll("[óòö]", "o")
                .replaceAll("[úùü]", "u");
    }

    private static Map<String, Integer> buildMonths() {
        Map<String, Integer> months = new LinkedHashMap<>();
        months.put("enero", 1);
        months.put("febrero", 2);
        months.put("marzo", 3);
        months.put("abril", 4);
        months.put("mayo", 5);
        months.put("junio", 6);
        months.put("julio", 7);
        months.put("agosto", 8);
        months.put("septiembre", 9);
        months.put("setiembre", 9);
        months.put("octubre", 10);
        months.put("noviembre", 11);
        months.put("diciembre", 12);
        return months;
    }
}
