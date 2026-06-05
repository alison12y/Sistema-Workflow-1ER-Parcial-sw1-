package com.workflow.politicas.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Logs del motor de avance Ciclo 1.
 * {@link #advance} siempre escribe en INFO; el resto requiere {@code logging.level.CU7_DEBUG=DEBUG}.
 */
public final class Cu7WorkflowDebugLog {

    public static final Logger LOG = LoggerFactory.getLogger("CU7_DEBUG");
    public static final Logger ADVANCE = LoggerFactory.getLogger("WORKFLOW_ADVANCE");

    private Cu7WorkflowDebugLog() {
    }

    /** Log obligatorio en cada avance de trámite (visible sin activar DEBUG). */
    public static void advance(String message, Object... args) {
        ADVANCE.info(message, args);
    }

    public static void log(String message, Object... args) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(message, args);
        }
    }

    public static String stepDataSummary(Map<String, Object> stepData) {
        if (stepData == null || stepData.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : stepData.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            Object v = e.getValue();
            sb.append(e.getKey())
                    .append("=")
                    .append(v == null ? "null" : v)
                    .append(v == null ? "" : "(" + v.getClass().getSimpleName() + ")");
        }
        sb.append("}");
        return sb.toString();
    }
}
