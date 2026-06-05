package com.workflow.politicas.service;

import com.workflow.politicas.dto.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser local para prompts que describen un workflow completo (carriles, flujo, decisiones).
 * CU14 — Asistente IA del Diseñador UML.
 */
public final class WorkflowFullPromptParser {

    private static final String FALLBACK_MSG =
            "Sugerencia generada con parser local (IA externa no disponible). Revise antes de aplicar.";

    private WorkflowFullPromptParser() {}

    public static boolean canParse(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return false;
        }
        String lower = prompt.toLowerCase(Locale.ROOT);
        boolean workflowIntent = lower.contains("workflow") || lower.contains("flujo de trabajo");
        boolean structured = lower.contains("carriles") || lower.contains("carril:")
                || lower.contains("flujo:") || lower.contains("flujo\n");
        boolean processSteps = lower.contains("inicio")
                && (lower.contains("finaliza") || lower.contains("termina") || lower.contains("fin"));
        return (workflowIntent && structured) || (workflowIntent && processSteps) || matchesPermisoLaboral(lower);
    }

    public static AiWorkflowSuggestResponse parse(AiWorkflowSuggestRequest request) {
        String prompt = request.getPrompt() != null ? request.getPrompt().trim() : "";
        AiWorkflowSuggestResponse response = baseResponse();

        if (matchesPermisoLaboral(prompt.toLowerCase(Locale.ROOT))) {
            buildPermisoLaboral(response, prompt);
            return response;
        }

        List<String> lanes = parseLanes(prompt);
        List<String> sentences = parseFlowSentences(prompt);
        if (sentences.isEmpty()) {
            response.setExplanation("No se pudo interpretar el flujo del prompt. Indique sección «Flujo:» con pasos.");
            response.setIntent("UNKNOWN");
            return response;
        }

        buildGenericWorkflow(response, lanes, sentences, prompt);
        return response;
    }

    private static boolean matchesPermisoLaboral(String lower) {
        return lower.contains("permiso laboral")
                || (lower.contains("funcionario")
                && lower.contains("recursos humanos")
                && lower.contains("supervisor")
                && (lower.contains("documentación") || lower.contains("documentacion")));
    }

    private static void buildPermisoLaboral(AiWorkflowSuggestResponse response, String prompt) {
        List<String> lanes = parseLanes(prompt);
        if (lanes.isEmpty()) {
            lanes = List.of("Funcionario", "Recursos Humanos", "Supervisor");
        }

        String laneFuncionario = matchLane(lanes, "funcionario", "Funcionario");
        String laneRh = matchLane(lanes, "recursos humanos", "Recursos Humanos");
        String laneSupervisor = matchLane(lanes, "supervisor", "Supervisor");

        List<AiSuggestActivityItem> activities = new ArrayList<>();
        List<AiSuggestTransitionItem> transitions = new ArrayList<>();
        List<AiSuggestResponsibleItem> responsibles = new ArrayList<>();

        addLaneResponsibles(responsibles, laneFuncionario, laneRh, laneSupervisor);

        activities.add(activity("Inicio", "START", null));
        activities.add(activity("Registrar solicitud", "TASK", laneFuncionario));
        activities.add(activity("Revisar documentación", "TASK", laneRh));
        activities.add(activity("Documentación completa?", "DECISION", laneRh));
        activities.add(activity("Solicitar corrección", "TASK", laneFuncionario));
        activities.add(activity("Revisión supervisor", "TASK", laneSupervisor));
        activities.add(activity("Aprueba permiso?", "DECISION", laneSupervisor));
        activities.add(activity("Aprobar permiso", "TASK", laneSupervisor));
        activities.add(activity("Notificar rechazo", "TASK", laneRh));
        activities.add(activity("Fin", "END", null));

        transitions.add(transition("Inicio", "Registrar solicitud", "SEQUENTIAL", null));
        transitions.add(transition("Registrar solicitud", "Revisar documentación", "SEQUENTIAL", null));
        transitions.add(transition("Revisar documentación", "Documentación completa?", "SEQUENTIAL", null));
        transitions.add(transition("Documentación completa?", "Solicitar corrección", "CONDITIONAL", "completa == false"));
        transitions.add(transition("Solicitar corrección", "Revisar documentación", "SEQUENTIAL", null));
        transitions.add(transition("Documentación completa?", "Revisión supervisor", "CONDITIONAL", "completa == true"));
        transitions.add(transition("Revisión supervisor", "Aprueba permiso?", "SEQUENTIAL", null));
        transitions.add(transition("Aprueba permiso?", "Aprobar permiso", "CONDITIONAL", "aprobado == true"));
        transitions.add(transition("Aprueba permiso?", "Notificar rechazo", "CONDITIONAL", "aprobado == false"));
        transitions.add(transition("Aprobar permiso", "Fin", "SEQUENTIAL", null));
        transitions.add(transition("Notificar rechazo", "Fin", "SEQUENTIAL", null));

        finalizeResponse(response, activities, transitions, responsibles, prompt,
                "GENERATE_FULL_WORKFLOW", "CONDITIONAL",
                "Workflow de solicitud de permiso laboral con carriles, revisiones, decisiones y cierre.");
    }

    private static void buildGenericWorkflow(
            AiWorkflowSuggestResponse response,
            List<String> lanes,
            List<String> sentences,
            String prompt
    ) {
        List<AiSuggestActivityItem> activities = new ArrayList<>();
        List<AiSuggestTransitionItem> transitions = new ArrayList<>();
        List<AiSuggestResponsibleItem> responsibles = new ArrayList<>();
        for (String lane : lanes) {
            addResponsible(responsibles, lane);
        }

        String lastSequential = null;
        String pendingDecision = null;
        boolean hasStart = false;
        boolean hasEnd = false;

        for (String sentence : sentences) {
            String lower = sentence.toLowerCase(Locale.ROOT).trim();
            if (lower.isBlank()) {
                continue;
            }

            if (isStartSentence(lower)) {
                if (!hasStart) {
                    activities.add(activity("Inicio", "START", null));
                    hasStart = true;
                    lastSequential = "Inicio";
                }
                continue;
            }

            if (isEndSentence(lower)) {
                hasEnd = true;
                continue;
            }

            if (isConditionalSentence(lower)) {
                String decisionName = inferDecisionName(lower, pendingDecision);
                if (activityIndex(activities, decisionName) < 0) {
                    String lane = inferLane(sentence, lanes);
                    activities.add(activity(decisionName, "DECISION", lane));
                    if (lastSequential != null) {
                        transitions.add(transition(lastSequential, decisionName, "SEQUENTIAL", null));
                    }
                    pendingDecision = decisionName;
                    lastSequential = decisionName;
                }

                String falseTarget = inferFalseBranchTask(lower);
                if (falseTarget != null && activityIndex(activities, falseTarget) < 0) {
                    String lane = inferLane(sentence, lanes);
                    activities.add(activity(falseTarget, "TASK", lane));
                    transitions.add(transition(pendingDecision, falseTarget, "CONDITIONAL", inferConditionLabel(lower, false)));
                }

                String trueTarget = inferTrueBranchTask(lower, sentences, lanes);
                if (trueTarget != null && activityIndex(activities, trueTarget) < 0) {
                    String lane = inferLaneForTask(trueTarget, lanes);
                    activities.add(activity(trueTarget, "TASK", lane));
                    transitions.add(transition(pendingDecision, trueTarget, "CONDITIONAL", inferConditionLabel(lower, true)));
                    lastSequential = trueTarget;
                } else if (trueTarget != null) {
                    transitions.add(transition(pendingDecision, trueTarget, "CONDITIONAL", inferConditionLabel(lower, true)));
                    lastSequential = trueTarget;
                }
                continue;
            }

            String taskName = inferTaskName(sentence);
            String lane = inferLane(sentence, lanes);
            if (taskName == null || taskName.isBlank()) {
                continue;
            }
            if (activityIndex(activities, taskName) < 0) {
                activities.add(activity(taskName, "TASK", lane));
            }
            if (lastSequential != null && !lastSequential.equals(taskName)) {
                transitions.add(transition(lastSequential, taskName, "SEQUENTIAL", null));
            }
            lastSequential = taskName;
        }

        if (!hasStart) {
            activities.add(0, activity("Inicio", "START", null));
            if (activities.size() > 1) {
                transitions.add(0, transition("Inicio", activities.get(1).getName(), "SEQUENTIAL", null));
            }
        }
        if (!hasEnd || activityIndex(activities, "Fin") < 0) {
            activities.add(activity("Fin", "END", null));
            if (lastSequential != null && !"Fin".equals(lastSequential)) {
                transitions.add(transition(lastSequential, "Fin", "SEQUENTIAL", null));
            }
        }

        dedupeTransitions(transitions);
        finalizeResponse(response, activities, transitions, responsibles, prompt,
                "GENERATE_FULL_WORKFLOW", resolveFlowType(transitions),
                "Workflow generado a partir del texto (carriles, pasos y condiciones detectados).");
    }

    private static AiWorkflowSuggestResponse baseResponse() {
        AiWorkflowSuggestResponse response = new AiWorkflowSuggestResponse();
        response.setAiAvailable(false);
        response.setFallbackUsed(true);
        response.setError(FALLBACK_MSG);
        response.setRequiresConfirmation(true);
        response.setSuggestions(new ArrayList<>());
        response.setWarnings(new ArrayList<>());
        return response;
    }

    private static void finalizeResponse(
            AiWorkflowSuggestResponse response,
            List<AiSuggestActivityItem> activities,
            List<AiSuggestTransitionItem> transitions,
            List<AiSuggestResponsibleItem> responsibles,
            String prompt,
            String intent,
            String flowType,
            String summary
    ) {
        dedupeResponsibles(responsibles);
        response.setSuggestedActivities(activities);
        response.setSuggestedTransitions(transitions);
        response.setSuggestedResponsibles(responsibles);
        response.setIntent(intent);
        response.setFlowType(flowType);
        response.setExplanation(FALLBACK_MSG + " " + summary
                + " Actividades: " + activities.size() + "; conexiones: " + transitions.size() + ".");
        response.getSuggestions().add("Revise la sugerencia y pulse «Aplicar sugerencia» en el diseñador.");
        response.getSuggestions().add("Tras aplicar, use «Validar flujo» para comprobar inicio, fin y conexiones.");
        if (activities.stream().noneMatch(a -> "START".equals(a.getActivityType()))) {
            response.getWarnings().add("No se detectó actividad START; valide el diagrama tras aplicar.");
        }
        if (activities.stream().noneMatch(a -> "END".equals(a.getActivityType()))) {
            response.getWarnings().add("No se detectó actividad END; valide el diagrama tras aplicar.");
        }
        if (prompt.length() > 200) {
            response.getWarnings().add("Prompt extenso interpretado de forma estructurada; ajuste nombres si es necesario.");
        }
    }

    private static List<String> parseLanes(String prompt) {
        String section = extractSection(prompt, "carriles", "flujo");
        if (section.isBlank()) {
            return List.of();
        }
        List<String> lanes = new ArrayList<>();
        for (String line : section.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            Matcher bullet = Pattern.compile("^[-*•]\\s*(.+)$").matcher(trimmed);
            if (bullet.matches()) {
                lanes.add(cleanListItem(bullet.group(1)));
                continue;
            }
            if (!trimmed.endsWith(":") && trimmed.length() < 80) {
                lanes.add(cleanListItem(trimmed));
            }
        }
        return lanes;
    }

    private static List<String> parseFlowSentences(String prompt) {
        String section = extractSection(prompt, "flujo", null);
        if (section.isBlank()) {
            section = prompt;
        }
        List<String> sentences = new ArrayList<>();
        for (String part : section.split("(?<=[.;])\\s+|\\R")) {
            String s = part.trim();
            if (s.isBlank() || s.equalsIgnoreCase("flujo:") || s.equalsIgnoreCase("carriles:")) {
                continue;
            }
            if (s.toLowerCase(Locale.ROOT).startsWith("crear un workflow")
                    || s.toLowerCase(Locale.ROOT).startsWith("crear workflow")) {
                continue;
            }
            sentences.add(s);
        }
        return sentences;
    }

    private static String extractSection(String prompt, String startMarker, String endMarker) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        int startIdx = lower.indexOf(startMarker);
        if (startIdx < 0) {
            return "";
        }
        int contentStart = startIdx + startMarker.length();
        while (contentStart < prompt.length()) {
            char c = prompt.charAt(contentStart);
            if (c == ':' || c == '\n' || c == '\r') {
                contentStart++;
            } else if (Character.isWhitespace(c)) {
                contentStart++;
            } else {
                break;
            }
        }
        int endIdx = prompt.length();
        if (endMarker != null) {
            int markerPos = lower.indexOf(endMarker, contentStart);
            if (markerPos > contentStart) {
                endIdx = markerPos;
            }
        }
        return prompt.substring(contentStart, endIdx).trim();
    }

    private static String matchLane(List<String> lanes, String keyword, String fallback) {
        for (String lane : lanes) {
            if (lane.toLowerCase(Locale.ROOT).contains(keyword)) {
                return lane;
            }
        }
        return fallback;
    }

    private static void addLaneResponsibles(
            List<AiSuggestResponsibleItem> responsibles,
            String... lanes
    ) {
        for (String lane : lanes) {
            addResponsible(responsibles, lane);
        }
    }

    private static boolean isStartSentence(String lower) {
        return lower.equals("inicio") || lower.startsWith("inicio.") || lower.startsWith("inicio ");
    }

    private static boolean isEndSentence(String lower) {
        return lower.equals("fin")
                || lower.contains("finaliza")
                || lower.contains("termina el proceso")
                || (lower.contains("termina") && !lower.startsWith("si "));
    }

    private static boolean isConditionalSentence(String lower) {
        return lower.startsWith("si ") || lower.contains(" si la ") || lower.contains(" si está")
                || lower.contains(" si esta") || lower.contains(" si aprueba") || lower.contains(" si rechaza");
    }

    private static String inferDecisionName(String lower, String pending) {
        if (lower.contains("documentación") || lower.contains("documentacion") || lower.contains("incompleta")
                || lower.contains("completa")) {
            return "Documentación completa?";
        }
        if (lower.contains("aprueba") || lower.contains("rechaza")) {
            return "Aprueba permiso?";
        }
        if (pending != null) {
            return pending;
        }
        Matcher m = Pattern.compile("si\\s+(.+?)(?:\\s+se\\s+|\\s+pasa|\\s+termina|,|$)", Pattern.CASE_INSENSITIVE)
                .matcher(lower);
        if (m.find()) {
            String topic = capitalizeWords(m.group(1));
            return topic.endsWith("?") ? topic : topic + "?";
        }
        return "¿Decisión?";
    }

    private static String inferFalseBranchTask(String lower) {
        if (lower.contains("incompleta") || lower.contains("corrección") || lower.contains("correccion")) {
            return "Solicitar corrección";
        }
        if (lower.contains("rechaza") || lower.contains("notifica")) {
            return "Notificar rechazo";
        }
        return null;
    }

    private static String inferTrueBranchTask(String lower, List<String> allSentences, List<String> lanes) {
        if (lower.contains("completa") && lower.contains("supervisor")) {
            return "Revisión supervisor";
        }
        if (lower.contains("aprueba")) {
            return "Aprobar permiso";
        }
        if (lower.contains("supervisor")) {
            return "Revisión supervisor";
        }
        for (String s : allSentences) {
            String sl = s.toLowerCase(Locale.ROOT);
            if (sl.contains("supervisor") && !isConditionalSentence(sl)) {
                return inferTaskName(s);
            }
        }
        return null;
    }

    private static String inferConditionLabel(String lower, boolean trueBranch) {
        if (lower.contains("documentación") || lower.contains("documentacion") || lower.contains("incompleta")
                || lower.contains("completa")) {
            return trueBranch ? "completa == true" : "completa == false";
        }
        if (lower.contains("aprueba") || lower.contains("rechaza")) {
            return trueBranch ? "aprobado == true" : "aprobado == false";
        }
        return trueBranch ? "Sí" : "No";
    }

    private static String inferTaskName(String sentence) {
        String s = sentence.trim();
        String lower = s.toLowerCase(Locale.ROOT);

        if (lower.contains("registra") && lower.contains("solicitud")) {
            return "Registrar solicitud";
        }
        if (lower.contains("revisa") && (lower.contains("documentación") || lower.contains("documentacion"))) {
            return "Revisar documentación";
        }
        if (lower.contains("corrección") || lower.contains("correccion")) {
            return "Solicitar corrección";
        }
        if (lower.contains("supervisor") && (lower.contains("aprueba") || lower.contains("revisión")
                || lower.contains("revision") || lower.contains("revisa"))) {
            return "Revisión supervisor";
        }
        if (lower.contains("aprueba") && lower.contains("permiso")) {
            return "Aprobar permiso";
        }
        if (lower.contains("notifica") || lower.contains("rechaza")) {
            return "Notificar rechazo";
        }

        s = s.replaceAll("^(?:el|la|los|las)\\s+[^,\\.]+\\s+", "");
        s = s.replaceAll("^(?:entonces|luego|después|despues)\\s+", "");
        Matcher verb = Pattern.compile(
                "([a-záéíóúñ]+(?:\\s+[a-záéíóúñ]+){0,4})",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        ).matcher(s);
        if (verb.find()) {
            return capitalizeWords(verb.group(1));
        }
        return capitalizeWords(s.length() > 60 ? s.substring(0, 60) : s);
    }

    private static String inferLane(String sentence, List<String> lanes) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        for (String lane : lanes) {
            if (lower.contains(lane.toLowerCase(Locale.ROOT))) {
                return lane;
            }
        }
        if (lower.contains("funcionario")) {
            return matchLane(lanes, "funcionario", null);
        }
        if (lower.contains("recursos humanos")) {
            return matchLane(lanes, "recursos humanos", null);
        }
        if (lower.contains("supervisor")) {
            return matchLane(lanes, "supervisor", null);
        }
        return lanes.isEmpty() ? null : lanes.get(0);
    }

    private static String inferLaneForTask(String taskName, List<String> lanes) {
        String lower = taskName.toLowerCase(Locale.ROOT);
        if (lower.contains("supervisor") || lower.contains("aprob")) {
            return matchLane(lanes, "supervisor", null);
        }
        if (lower.contains("document") || lower.contains("corrección") || lower.contains("correccion")
                || lower.contains("notificar")) {
            return matchLane(lanes, "recursos humanos", null);
        }
        if (lower.contains("registrar") || lower.contains("solicitud")) {
            return matchLane(lanes, "funcionario", null);
        }
        return lanes.isEmpty() ? null : lanes.get(0);
    }

    private static int activityIndex(List<AiSuggestActivityItem> activities, String name) {
        String norm = normalize(name);
        for (int i = 0; i < activities.size(); i++) {
            if (normalize(activities.get(i).getName()).equals(norm)) {
                return i;
            }
        }
        return -1;
    }

    private static AiSuggestActivityItem activity(String name, String type, String lane) {
        AiSuggestActivityItem item = new AiSuggestActivityItem();
        item.setOperation("CREATE");
        item.setName(name);
        item.setActivityType(type);
        if (lane != null && !lane.isBlank()) {
            item.setResponsibleName(lane);
            item.setResponsibleType("DEPARTMENT");
        } else {
            item.setResponsibleType("ROLE");
        }
        return item;
    }

    private static AiSuggestTransitionItem transition(String from, String to, String type, String condition) {
        AiSuggestTransitionItem t = new AiSuggestTransitionItem();
        t.setOperation("CREATE");
        t.setFromActivityName(from);
        t.setToActivityName(to);
        t.setTransitionType(type);
        t.setConditionLabel(condition);
        return t;
    }

    private static void addResponsible(List<AiSuggestResponsibleItem> list, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        AiSuggestResponsibleItem r = new AiSuggestResponsibleItem();
        r.setName(name);
        r.setType("DEPARTMENT");
        list.add(r);
    }

    private static void dedupeResponsibles(List<AiSuggestResponsibleItem> list) {
        Map<String, AiSuggestResponsibleItem> byNorm = new LinkedHashMap<>();
        for (AiSuggestResponsibleItem r : list) {
            byNorm.putIfAbsent(normalize(r.getName()), r);
        }
        list.clear();
        list.addAll(byNorm.values());
    }

    private static void dedupeTransitions(List<AiSuggestTransitionItem> transitions) {
        Set<String> seen = new LinkedHashSet<>();
        transitions.removeIf(t -> {
            String key = normalize(t.getFromActivityName()) + "->" + normalize(t.getToActivityName())
                    + ":" + (t.getConditionLabel() != null ? t.getConditionLabel() : "");
            return !seen.add(key);
        });
    }

    private static String resolveFlowType(List<AiSuggestTransitionItem> transitions) {
        boolean conditional = transitions.stream()
                .anyMatch(t -> "CONDITIONAL".equalsIgnoreCase(t.getTransitionType()));
        return conditional ? "CONDITIONAL" : "SEQUENTIAL";
    }

    private static String cleanListItem(String raw) {
        return raw.trim().replaceAll("^[-*•]\\s*", "");
    }

    private static String capitalizeWords(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] parts = raw.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (p.length() == 1) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            } else {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return sb.toString();
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
