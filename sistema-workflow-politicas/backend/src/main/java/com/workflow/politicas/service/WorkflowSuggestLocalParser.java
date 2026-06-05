package com.workflow.politicas.service;

import com.workflow.politicas.dto.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser local en español cuando Gemini/ai-service no está disponible.
 */
public final class WorkflowSuggestLocalParser {

    private static final String FALLBACK_MSG =
            "Sugerencia generada con parser local (IA externa no disponible). Revise antes de aplicar.";

    private WorkflowSuggestLocalParser() {}

    public static AiWorkflowSuggestResponse parse(AiWorkflowSuggestRequest request) {
        String prompt = request.getPrompt() != null ? request.getPrompt().trim() : "";
        AiWorkflowSuggestResponse response = new AiWorkflowSuggestResponse();
        response.setAiAvailable(false);
        response.setFallbackUsed(true);
        response.setError(FALLBACK_MSG);
        response.setRequiresConfirmation(true);
        response.setSuggestions(new ArrayList<>());
        response.setWarnings(new ArrayList<>());

        if (prompt.isBlank()) {
            response.setExplanation("Indique qué desea hacer en el diagrama (crear actividad, conectar, decisión, etc.).");
            response.setIntent("UNKNOWN");
            response.setFlowType("UNKNOWN");
            return response;
        }

        if (WorkflowFullPromptParser.canParse(prompt)) {
            return WorkflowFullPromptParser.parse(request);
        }

        String lower = prompt.toLowerCase(Locale.ROOT);
        Map<String, String> existingByNorm = indexActivities(request.getActivities());

        if (containsValidateIntent(lower)) {
            buildValidateOnly(response, prompt);
            return response;
        }

        List<AiSuggestActivityItem> activities = new ArrayList<>();
        List<AiSuggestTransitionItem> transitions = new ArrayList<>();
        List<AiSuggestResponsibleItem> responsibles = new ArrayList<>();
        Set<String> intents = new LinkedHashSet<>();

        parseCreateActivity(prompt, lower, activities, responsibles, intents);
        parseCreateDecision(prompt, lower, activities, responsibles, intents);
        parseConnect(prompt, lower, transitions, existingByNorm, activities, intents);
        parseParallel(prompt, lower, transitions, intents, response.getWarnings());
        parseConditional(prompt, lower, transitions, intents);
        parseChangeTransitionType(prompt, lower, transitions, intents);

        if (activities.isEmpty() && transitions.isEmpty()) {
            parseCreateActivityFallback(prompt, activities, responsibles, intents);
        }

        dedupeResponsibles(responsibles);
        response.setSuggestedActivities(activities);
        response.setSuggestedTransitions(transitions);
        response.setSuggestedResponsibles(responsibles);
        response.setIntent(intents.isEmpty() ? "UNKNOWN" : String.join("_", intents));
        response.setFlowType(resolveFlowType(transitions));
        response.setExplanation(buildExplanation(prompt, activities, transitions, response.getFlowType()));
        response.getSuggestions().add("Confirme con «Aplicar sugerencia» para persistir en WorkflowActivity y WorkflowTransition.");
        if (!transitions.isEmpty()) {
            response.getSuggestions().add("Tras aplicar, use «Validar flujo» para comprobar inicio, fin y conexiones.");
        }
        return response;
    }

    private static boolean containsValidateIntent(String lower) {
        return (lower.contains("validar") || lower.contains("validación"))
                && (lower.contains("diagrama") || lower.contains("flujo") || lower.contains("workflow"));
    }

    private static void buildValidateOnly(AiWorkflowSuggestResponse response, String prompt) {
        response.setIntent("VALIDATE_DIAGRAM");
        response.setFlowType("VALIDATION");
        response.setExplanation(
                "Solicitud de validación detectada. Use el botón «Validar flujo» del diseñador; "
                        + "no se modificará el diagrama automáticamente.");
        response.getSuggestions().add("Pulse «Validar flujo» en la barra de herramientas del lienzo.");
        response.getWarnings().add("El parser local no ejecuta validación por sí solo: " + prompt.substring(0, Math.min(80, prompt.length())));
    }

    private static Map<String, String> indexActivities(List<AiWorkflowContextActivityDto> list) {
        Map<String, String> map = new HashMap<>();
        if (list == null) return map;
        for (AiWorkflowContextActivityDto a : list) {
            if (a.getName() != null) {
                map.put(normalizeName(a.getName()), a.getName());
            }
        }
        return map;
    }

    private static void parseCreateActivity(
            String prompt,
            String lower,
            List<AiSuggestActivityItem> activities,
            List<AiSuggestResponsibleItem> responsibles,
            Set<String> intents
    ) {
        Pattern[] patterns = {
                Pattern.compile(
                        "crear\\s+(?:una\\s+)?actividad\\s+(.+?)(?:\\s+en\\s+(?:el\\s+)?departamento\\s+([^,\\.y]+))?(?:\\s+y\\s+|$)",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
                Pattern.compile(
                        "agregar\\s+(?:una\\s+)?actividad\\s+(.+?)(?:\\s+en\\s+(?:el\\s+)?departamento\\s+([^,\\.y]+))?",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
                Pattern.compile(
                        "nueva\\s+actividad\\s+(.+?)(?:\\s+en\\s+(?:el\\s+)?departamento\\s+([^,\\.y]+))?",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(prompt);
            if (m.find()) {
                String name = cleanName(m.group(1));
                String dept = m.groupCount() >= 2 && m.group(2) != null ? cleanName(m.group(2)) : null;
                if (!name.isBlank() && !isGenericWord(name)) {
                    AiSuggestActivityItem item = newActivity(name, "TASK", dept);
                    item.setConnectAfterActivityName(extractConnectAfter(prompt));
                    activities.add(item);
                    addResponsible(responsibles, dept);
                    intents.add("CREATE_ACTIVITY");
                }
                break;
            }
        }
    }

    private static void parseCreateActivityFallback(
            String prompt,
            List<AiSuggestActivityItem> activities,
            List<AiSuggestResponsibleItem> responsibles,
            Set<String> intents
    ) {
        if (WorkflowFullPromptParser.canParse(prompt)) {
            return;
        }
        if (!prompt.toLowerCase(Locale.ROOT).contains("crear") && !prompt.toLowerCase(Locale.ROOT).contains("agregar")) {
            return;
        }
        Matcher m = Pattern.compile("(?:crear|agregar)\\s+(.+)", Pattern.CASE_INSENSITIVE).matcher(prompt);
        if (m.find()) {
            String tail = cleanName(m.group(1));
            if (tail.length() > 3 && tail.length() < 120) {
                activities.add(newActivity(tail, "TASK", null));
                intents.add("CREATE_ACTIVITY");
            }
        }
    }

    private static void parseCreateDecision(
            String prompt,
            String lower,
            List<AiSuggestActivityItem> activities,
            List<AiSuggestResponsibleItem> responsibles,
            Set<String> intents
    ) {
        if (!lower.contains("decisión") && !lower.contains("decision") && !lower.contains("gateway")) {
            return;
        }
        Pattern p = Pattern.compile(
                "(?:crear|agregar)\\s+(?:un\\s+)?(?:punto\\s+de\\s+)?decisi[oó]n\\s+(.+?)(?:\\s+en\\s+(.+?))?(?:\\.|,|$)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m = p.matcher(prompt);
        String name = "¿Decisión?";
        String resp = null;
        if (m.find()) {
            name = cleanName(m.group(1));
            if (m.groupCount() >= 2 && m.group(2) != null) {
                resp = cleanName(m.group(2));
            }
        }
        if (name.isBlank() || isGenericWord(name)) {
            name = "¿Decisión?";
        }
        activities.add(newActivity(name, "DECISION", resp));
        addResponsible(responsibles, resp);
        intents.add("CREATE_DECISION");
    }

    private static void parseConnect(
            String prompt,
            String lower,
            List<AiSuggestTransitionItem> transitions,
            Map<String, String> existingByNorm,
            List<AiSuggestActivityItem> newActivities,
            Set<String> intents
    ) {
        String transitionType = lower.contains("condicional") || lower.contains("condición")
                ? "CONDITIONAL"
                : "SEQUENTIAL";
        String condition = null;
        if ("CONDITIONAL".equals(transitionType)) {
            condition = extractConditionLabel(prompt);
        }

        Pattern afterPattern = Pattern.compile(
                "(?:conectar(?:la)?|enlazar(?:la)?)\\s+(?:despu[eé]s\\s+de|tras)\\s+(.+?)(?:\\s+(?:con|a|hacia)\\s+(.+?))?(?:\\s+de\\s+forma|$)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher after = afterPattern.matcher(prompt);
        if (after.find()) {
            String from = resolveActivityName(cleanName(after.group(1)), existingByNorm);
            String to = after.groupCount() >= 2 && after.group(2) != null
                    ? resolveActivityName(cleanName(after.group(2)), existingByNorm)
                    : findNewActivityName(newActivities);
            if (from != null && to != null) {
                transitions.add(newTransition(from, to, transitionType, condition));
                intents.add("CONNECT");
            }
            return;
        }

        Pattern connectPattern = Pattern.compile(
                "conectar(?:la)?\\s+(.+?)\\s+(?:con|a|hacia)\\s+(.+?)(?:\\s+de\\s+forma|$)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher conn = connectPattern.matcher(prompt);
        if (conn.find()) {
            String from = resolveActivityName(cleanName(conn.group(1)), existingByNorm);
            String to = resolveActivityName(cleanName(conn.group(2)), existingByNorm);
            if (from != null && to != null) {
                transitions.add(newTransition(from, to, transitionType, condition));
                intents.add("CONNECT");
            }
        }

        String connectAfter = extractConnectAfter(prompt);
        if (connectAfter != null && !newActivities.isEmpty()) {
            AiSuggestActivityItem last = newActivities.get(newActivities.size() - 1);
            if (last.getConnectAfterActivityName() == null) {
                last.setConnectAfterActivityName(connectAfter);
            }
            String from = resolveActivityName(connectAfter, existingByNorm);
            String to = last.getName();
            if (from != null && to != null && transitions.stream().noneMatch(t -> to.equals(t.getToActivityName()))) {
                transitions.add(newTransition(from, to, transitionType, condition));
                intents.add("CONNECT");
            }
        }
    }

    private static void parseParallel(
            String prompt,
            String lower,
            List<AiSuggestTransitionItem> transitions,
            Set<String> intents,
            List<String> warnings
    ) {
        if (!lower.contains("paralel") && !lower.contains("bifurc") && !lower.contains("fork")) {
            return;
        }
        intents.add("PARALLEL");
        warnings.add(
                "División/unión paralela (PARALLEL_SPLIT/JOIN) está en el modelo pero el lienzo guiado aún la marca como próxima. "
                        + "Revise el tipo de conexión tras aplicar.");
        Pattern p = Pattern.compile(
                "paralelo\\s+entre\\s+(.+?)\\s+y\\s+(.+?)(?:\\.|,|$)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m = p.matcher(prompt);
        if (m.find()) {
            String a = cleanName(m.group(1));
            String b = cleanName(m.group(2));
            transitions.add(newTransition(a, b, "PARALLEL_SPLIT", null));
        }
    }

    private static void parseConditional(
            String prompt,
            String lower,
            List<AiSuggestTransitionItem> transitions,
            Set<String> intents
    ) {
        if (!lower.contains("condicional") && !lower.contains("si ") && !lower.contains("aprobado")) {
            return;
        }
        intents.add("CONDITIONAL");
        if (!transitions.isEmpty()) {
            AiSuggestTransitionItem last = transitions.get(transitions.size() - 1);
            last.setTransitionType("CONDITIONAL");
            if (last.getConditionLabel() == null || last.getConditionLabel().isBlank()) {
                last.setConditionLabel(extractConditionLabel(prompt));
            }
        }
    }

    private static void parseChangeTransitionType(
            String prompt,
            String lower,
            List<AiSuggestTransitionItem> transitions,
            Set<String> intents
    ) {
        if (!lower.contains("cambiar") || !lower.contains("transición") && !lower.contains("transicion") && !lower.contains("conexión") && !lower.contains("conexion")) {
            return;
        }
        intents.add("CHANGE_TRANSITION");
        Pattern p = Pattern.compile(
                "entre\\s+(.+?)\\s+y\\s+(.+?)(?:\\s+a\\s+)?(secuencial|condicional)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m = p.matcher(prompt);
        if (m.find()) {
            String type = m.group(3).toLowerCase(Locale.ROOT).contains("cond") ? "CONDITIONAL" : "SEQUENTIAL";
            transitions.add(newTransition(cleanName(m.group(1)), cleanName(m.group(2)), type,
                    "CONDITIONAL".equals(type) ? extractConditionLabel(prompt) : null));
        }
    }

    private static String extractConnectAfter(String prompt) {
        Pattern p = Pattern.compile(
                "despu[eé]s\\s+de\\s+(.+?)(?:\\s+de\\s+forma|\\s+y\\s+conectar|,|\\.|$)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m = p.matcher(prompt);
        if (m.find()) {
            return cleanName(m.group(1));
        }
        return null;
    }

    private static String extractConditionLabel(String prompt) {
        Pattern p = Pattern.compile(
                "(?:condici[oó]n|si|cuando)\\s+(.+?)(?:\\.|,|$)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m = p.matcher(prompt);
        if (m.find()) {
            String label = cleanName(m.group(1));
            if (!label.isBlank() && label.length() < 60) {
                return label;
            }
        }
        if (prompt.toLowerCase(Locale.ROOT).contains("aprobado")) {
            return "Aprobado";
        }
        if (prompt.toLowerCase(Locale.ROOT).contains("rechazado")) {
            return "Rechazado";
        }
        return "Condición";
    }

    private static String findNewActivityName(List<AiSuggestActivityItem> activities) {
        if (activities.isEmpty()) return null;
        return activities.get(activities.size() - 1).getName();
    }

    private static String resolveActivityName(String name, Map<String, String> existingByNorm) {
        if (name == null || name.isBlank()) return null;
        String norm = normalizeName(name);
        if (existingByNorm.containsKey(norm)) {
            return existingByNorm.get(norm);
        }
        for (Map.Entry<String, String> e : existingByNorm.entrySet()) {
            if (e.getKey().contains(norm) || norm.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return name;
    }

    private static AiSuggestActivityItem newActivity(String name, String type, String responsible) {
        AiSuggestActivityItem item = new AiSuggestActivityItem();
        item.setOperation("CREATE");
        item.setName(name);
        item.setActivityType(type);
        if (responsible != null && !responsible.isBlank()) {
            item.setResponsibleName(responsible);
            item.setResponsibleType("DEPARTMENT");
        } else {
            item.setResponsibleType("ROLE");
        }
        return item;
    }

    private static AiSuggestTransitionItem newTransition(
            String from,
            String to,
            String type,
            String condition
    ) {
        AiSuggestTransitionItem t = new AiSuggestTransitionItem();
        t.setOperation("CREATE");
        t.setFromActivityName(from);
        t.setToActivityName(to);
        t.setTransitionType(type != null ? type.toUpperCase(Locale.ROOT) : "SEQUENTIAL");
        t.setConditionLabel(condition);
        return t;
    }

    private static void addResponsible(List<AiSuggestResponsibleItem> list, String name) {
        if (name == null || name.isBlank()) return;
        AiSuggestResponsibleItem r = new AiSuggestResponsibleItem();
        r.setName(name);
        r.setType("DEPARTMENT");
        list.add(r);
    }

    private static void dedupeResponsibles(List<AiSuggestResponsibleItem> list) {
        Map<String, AiSuggestResponsibleItem> byNorm = new LinkedHashMap<>();
        for (AiSuggestResponsibleItem r : list) {
            byNorm.putIfAbsent(normalizeName(r.getName()), r);
        }
        list.clear();
        list.addAll(byNorm.values());
    }

    private static String resolveFlowType(List<AiSuggestTransitionItem> transitions) {
        boolean parallel = false;
        boolean conditional = false;
        for (AiSuggestTransitionItem t : transitions) {
            String type = t.getTransitionType() != null ? t.getTransitionType().toUpperCase(Locale.ROOT) : "";
            if (type.contains("PARALLEL")) parallel = true;
            if ("CONDITIONAL".equals(type)) conditional = true;
        }
        if (parallel) return "PARALLEL";
        if (conditional) return "CONDITIONAL";
        if (!transitions.isEmpty()) return "SEQUENTIAL";
        return "MIXED";
    }

    private static String buildExplanation(
            String prompt,
            List<AiSuggestActivityItem> activities,
            List<AiSuggestTransitionItem> transitions,
            String flowType
    ) {
        StringBuilder sb = new StringBuilder(FALLBACK_MSG);
        sb.append(" ");
        if (!activities.isEmpty()) {
            sb.append("Actividades sugeridas: ");
            sb.append(activities.stream().map(AiSuggestActivityItem::getName).reduce((a, b) -> a + ", " + b).orElse(""));
            sb.append(". ");
        }
        if (!transitions.isEmpty()) {
            sb.append("Conexiones sugeridas: ");
            for (AiSuggestTransitionItem t : transitions) {
                sb.append(t.getFromActivityName()).append(" → ").append(t.getToActivityName());
                sb.append(" (").append(t.getTransitionType()).append("); ");
            }
        }
        sb.append("Tipo de flujo: ").append(flowType).append(".");
        return sb.toString().trim();
    }

    private static String cleanName(String raw) {
        if (raw == null) return "";
        String s = raw.trim()
                .replaceAll("^(?:la|el|una|un)\\s+", "")
                .replaceAll("\\s+de\\s+forma\\s+secuencial.*$", "")
                .replaceAll("\\s+y\\s+conectar.*$", "")
                .trim();
        if (s.endsWith(".") || s.endsWith(",")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static boolean isGenericWord(String name) {
        String n = normalizeName(name);
        return n.equals("actividad") || n.equals("una actividad") || n.equals("decisión");
    }
}
