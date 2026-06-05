package com.workflow.politicas.workflow.cycle1;

/**
 * Referencia central del modelo oficial del Ciclo 1.
 * <p>
 * Ver {@code docs/ciclo1-modelo-workflow.md} para diagramas, migración y plan F1.
 * </p>
 */
public final class Cycle1WorkflowModel {

    private Cycle1WorkflowModel() {
    }

    /** Versión de consolidación arquitectónica (F0). */
    public static final String CONSOLIDATION_VERSION = "cycle1-f0";

    // --- Colecciones MongoDB oficiales (diseño + ejecución) ---

    public static final String COLLECTION_BUSINESS_POLICIES = "business_policies";
    public static final String COLLECTION_WORKFLOW_ACTIVITIES = "workflow_activities";
    public static final String COLLECTION_WORKFLOW_TRANSITIONS = "workflow_transitions";
    public static final String COLLECTION_DYNAMIC_FORMS = "dynamic_forms";
    public static final String COLLECTION_FORM_FIELDS = "form_fields";
    public static final String COLLECTION_TRAMITES = "tramites";
    public static final String COLLECTION_FORM_SUBMISSIONS = "form_submissions";

    // --- Colecciones deprecated (compatibilidad temporal) ---

    public static final String COLLECTION_ACTIVITY_DIAGRAMS = "activity_diagrams";
    public static final String COLLECTION_WORKFLOW_DIAGRAMS = "workflow_diagrams";
    public static final String COLLECTION_ACTIVITIES_LEGACY = "activities";
    public static final String COLLECTION_TRANSITIONS_LEGACY = "transitions";
    public static final String COLLECTION_PROCESS_INSTANCES = "process_instances";
    public static final String COLLECTION_TASK_INSTANCES = "task_instances";

    // --- APIs REST oficiales (prefijos) ---

    public static final String API_POLICIES = "/api/policies";
    public static final String API_WORKFLOW_ACTIVITIES = "/api/workflow-activities";
    public static final String API_WORKFLOW_TRANSITIONS = "/api/workflow-transitions";
    public static final String API_WORKFLOW_DESIGNER = "/api/workflow-designer";
    public static final String API_TRAMITES = "/api/tramites";
    public static final String API_MY_ACTIVITIES = "/api/my-activities";

    // --- APIs REST deprecated ---

    public static final String API_ACTIVITY_DIAGRAMS = "/api/activity-diagrams";
    public static final String API_PROCESS_INSTANCES = "/api/process";
    public static final String API_TASK_INSTANCES = "/api/tasks";
    public static final String API_WORKFLOWS_LEGACY = "/api/workflows";

    /**
     * Contrato previsto para F1 ({@code WorkflowRoutingService}).
     * Entrada: política, actividad actual, datos de paso (formulario).
     * Salida: siguiente {@code WorkflowActivity} o fin de trámite.
     */
    public static final String ROUTING_SERVICE_PLANNED = "com.workflow.politicas.service.WorkflowRoutingService";
}
