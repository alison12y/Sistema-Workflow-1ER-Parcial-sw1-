from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
import asyncio
import json

from fallbacks import (
    fallback_assist_form,
    fallback_generate_workflow,
    fallback_validate_diagram,
    success_meta,
)
from gemini_client import (
    GeminiError,
    GeminiQuotaExceededError,
    InvalidJsonResponseError,
    generate_structured,
)
from assistant_service import generate_assistant
from agent_fallback import fallback_agent_analyze
from analytics_fallback import (
    fallback_analytics_recommendations,
    fallback_analytics_report,
    fallback_analytics_risks,
)
from schemas import (
    AgentAnalyzeRequest,
    AnalyticsRequest,
    AssistantRequest,
    AssistFormRequest,
    GenerateWorkflowRequest,
    TaskAssistantRequest,
    TaskAssistantResponse,
    ValidateDiagramRequest,
    WorkflowSuggestRequest,
)
from workflow_suggest_fallback import fallback_workflow_suggest
from form_assist_fallback import fallback_assist_form as fallback_assist_form_structured
from task_assistant_fallback import fallback_task_assistant

app = FastAPI(title="AI Service for Workflow Policies")

TASK_ASSISTANT_TIMEOUT_SECONDS = 25.0

TASK_ASSISTANT_SYSTEM = """You are an intelligent task assistant for business workflow officials.
Your goal is to provide a clear orientation based on the task, process, and current activity data.
Return ONLY valid JSON with this exact structure:
{
  "summary": "Clear summary of the current task in Spanish",
  "importantData": ["List of most relevant data points to check"],
  "missingData": ["List of missing or to-be-verified data points"],
  "recommendedAction": "Clear and brief recommended action for the official",
  "source": "AI"
}
Rules:
- Be formal, brief, and clear.
- Do not invent information.
- If data is missing, list it in missingData.
- Do not make final decisions or approve/reject automatically.
- Language: Spanish."""


def build_task_assistant_prompt(data: dict) -> str:
    docs = ", ".join(
        d.get("name") or d.get("fileName") or "Documento"
        for d in (data.get("documents") or [])
    )
    return (
        "Datos de la Tarea:\n"
        f"- Trámite: {data.get('tramiteName')} (Actividad: {data.get('activityName')})\n"
        f"- Descripción: {data.get('activityDescription')}\n"
        f"- Estado: {data.get('taskStatus')} | Asignado a: {data.get('assignedTo')}\n"
        f"- Datos Formulario: {json.dumps(data.get('formData', {}), ensure_ascii=False)}\n"
        f"- Documentos: {docs if docs else 'Ninguno'}\n"
        f"- Observaciones: {data.get('observations') or 'Ninguna'}\n"
        f"- Creado: {data.get('createdAt')}\n\n"
        "Analiza la situación y genera la respuesta JSON."
    )


WORKFLOW_SYSTEM = """You are a BPM workflow designer assistant.
Return ONLY valid JSON with this exact structure:
{
  "activities": [{"id": "string", "name": "string", "type": "START|TASK|DECISION|END", "swimlaneId": "string"}],
  "transitions": [{"sourceActivityId": "string", "targetActivityId": "string", "condition": "string or null"}],
  "swimlanes": [{"id": "string", "name": "string"}],
  "suggestions": ["string"]
}
Use Spanish labels when appropriate. Include START and END activities."""

FORM_SYSTEM = """You are a form filling assistant for business workflow execution.
Read the user's natural language report and map values to form fields.
Return ONLY valid JSON:
{
  "explanation": "string in Spanish",
  "confidence": 0.0,
  "suggestedText": "short summary without sensitive extra data",
  "fieldSuggestions": [{
    "fieldName": "string",
    "fieldLabel": "string",
    "fieldType": "TEXT|TEXTAREA|NUMBER|DATE|SELECT|RADIO|CHECKBOX|FILE",
    "suggestedValue": "string or null",
    "applicable": true,
    "confidence": 0.0,
    "message": "string or null"
  }],
  "suggestedValues": {"fieldName": "value"},
  "warnings": ["string"],
  "unmatchedFields": ["field labels not mapped"]
}
Rules: never autofill FILE type (applicable=false). Respect SELECT/RADIO options. Use ISO dates YYYY-MM-DD."""

WORKFLOW_SUGGEST_SYSTEM = """You are a BPM workflow designer assistant for incremental diagram edits.
Return ONLY valid JSON:
{
  "explanation": "string in Spanish",
  "flowType": "SEQUENTIAL|CONDITIONAL|PARALLEL|MIXED",
  "intent": "CREATE_ACTIVITY|CONNECT|CREATE_DECISION|VALIDATE_DIAGRAM|MIXED",
  "requiresConfirmation": true,
  "suggestedActivities": [{
    "operation": "CREATE",
    "name": "string",
    "activityType": "START|TASK|DECISION|END",
    "responsibleName": "string or null",
    "responsibleType": "ROLE|DEPARTMENT|USER",
    "connectAfterActivityName": "string or null"
  }],
  "suggestedTransitions": [{
    "operation": "CREATE",
    "fromActivityName": "string",
    "toActivityName": "string",
    "transitionType": "SEQUENTIAL|CONDITIONAL|PARALLEL_SPLIT|PARALLEL_JOIN",
    "conditionLabel": "string or null"
  }],
  "suggestedResponsibles": [{"name": "string", "type": "ROLE|DEPARTMENT"}],
  "suggestions": ["string"],
  "warnings": ["string"]
}
Use existing activity names from context when connecting. Do not invent duplicate activities."""

AGENT_ANALYZE_SYSTEM = """You are a business policy routing assistant for citizen service requests.
Return ONLY valid JSON:
{
  "detectedIntent": "RECLAMO_SERVICIO|SOLICITUD_VACACIONES|PERMISO_LABORAL|INSTALACION_MEDIDOR|REVISION_DOCUMENTAL|APROBACION_INTERNA|GESTION_BIENES|SOLICITUD_GENERAL",
  "recommendedPolicyId": "string",
  "recommendedPolicyName": "string",
  "confidenceScore": 0.0,
  "explanation": "string in Spanish explaining why this policy fits",
  "requiredDocuments": ["string"],
  "suggestedFields": [{
    "name": "string",
    "label": "string",
    "type": "TEXT|TEXTAREA|SELECT",
    "required": true,
    "suggestedValue": "string or null"
  }],
  "warnings": ["string"]
}

Reglas de clasificación:
1. Especificidad: Elige siempre la política más específica de la lista 'policies'.
2. Vacaciones: Si el mensaje menciona vacaciones, licencia anual, descanso, días libres o ausencia planificada, prioriza "Solicitud de vacaciones" sobre políticas generales de permiso laboral.
3. Reclamos: Si menciona reclamo, falla, queja, demora o insatisfacción, prioriza "Reclamo de servicio".
4. Técnico: Si menciona medidor, instalación, energía, contador o suministro, prioriza "Solicitud de instalación de medidor".
5. Intenciones: No uses SOLICITUD_GENERAL si existe una intención específica que encaje.
6. Idioma: Las explicaciones deben ser en español. Selecciona exactamente una política ACTIVA de la lista proporcionada."""

ANALYTICS_REPORT_SYSTEM = """You are a workflow analytics assistant for business process reports.
Return ONLY valid JSON:
{
  "title": "string",
  "explanation": "string in Spanish",
  "reportType": "TRAMITES_MES|POLITICA_MAS_USADA|FUNCIONARIO_CARGA|TRAMITES_DEMORADOS|RESUMEN_FINALIZADOS|RESUMEN_GENERAL",
  "columns": ["string"],
  "rows": [{"columnName": "value"}],
  "appliedFilters": {"key": "value"},
  "suggestedFormat": "PANTALLA|PDF|EXCEL|WORD",
  "cards": [{"label": "string", "value": "string", "hint": "string", "severity": "info|warning|danger|success"}],
  "chart": {"type": "bar|pie", "title": "string", "labels": ["string"], "values": [0.0]},
  "warnings": ["string"]
}
Reglas:
1. Si la consulta es sobre carga de trabajo, funcionarios, responsables o tareas asignadas, usa EXCLUSIVAMENTE el dataset 'employeeLoad'.
2. El 'reportType' debe ser 'FUNCIONARIO_CARGA' para estas consultas.
3. Las explicaciones deben ser en español y mencionar el nombre del funcionario con mayor carga si se detecta.
4. Usa tramiteSample y KPI context para otros tipos de reportes."""

ANALYTICS_RISKS_SYSTEM = """You are a workflow risk analyst.
Return ONLY valid JSON:
{
  "summary": "string in Spanish",
  "risks": [{
    "type": "DEMORA|VENCIDA|CARGA|ANOMALIA|CUELLO",
    "severity": "ALTO|MEDIO|BAJO",
    "title": "string",
    "description": "string",
    "entityType": "string",
    "entityId": "string or null",
    "entityLabel": "string"
  }],
  "cards": [{"label": "string", "value": "string", "hint": "string", "severity": "info|warning|danger|success"}],
  "warnings": ["string"]
}"""

ANALYTICS_RECOMMENDATIONS_SYSTEM = """You are a workflow operations advisor.
Return ONLY valid JSON:
{
  "summary": "string in Spanish",
  "recommendations": [{
    "priority": "ALTA|MEDIA|BAJA",
    "type": "PRIORIZAR_TRAMITE|REVISAR_CUELLO|REASIGNAR_TAREA|POLITICA_RIESGO|RUTA_SUGERIDA",
    "title": "string",
    "action": "string",
    "rationale": "string",
    "tramiteCode": "string or null",
    "activityName": "string or null"
  }],
  "cards": [{"label": "string", "value": "string", "hint": "string", "severity": "info|warning|danger|success"}],
  "warnings": ["string"]
}"""

VALIDATE_SYSTEM = """You are a workflow diagram validator.
Return ONLY valid JSON:
{
  "valid": true,
  "errors": ["string"],
  "suggestions": ["string"]
}
Check START/END presence, TASK swimlanes, DECISION conditions, and connectivity."""


@app.get("/")
async def root():
    return {"message": "AI Service is running", "provider": "gemini"}


@app.get("/agent/health")
async def agent_health():
    return {"status": "ok", "service": "smart-agent"}


@app.get("/analytics/health")
async def analytics_health():
    return {"status": "ok", "service": "intelligent-analytics"}


@app.post("/assistant")
async def assistant(request: AssistantRequest):
    return generate_assistant(request.prompt, request.module, request.context)


@app.post("/agent/analyze")
async def agent_analyze(request: AgentAnalyzeRequest):
    combined = request.message
    if request.audioText:
        combined = f"{combined} {request.audioText}".strip()
    if request.attachmentFileName:
        combined = f"{combined} Documento adjunto: {request.attachmentFileName}".strip()

    payload = {
        "message": combined,
        "audioText": request.audioText,
        "requesterName": request.requesterName,
        "attachmentFileName": request.attachmentFileName,
        "documentContext": request.documentContext,
        "policies": request.policies,
    }

    try:
        result = generate_structured(
            AGENT_ANALYZE_SYSTEM,
            "User request and available policies:\n"
            + json.dumps(payload, ensure_ascii=False),
        )
        confidence = result.get("confidenceScore", 0.75)
        try:
            confidence = float(confidence)
        except (TypeError, ValueError):
            confidence = 0.75
        return {
            **success_meta(),
            "detectedIntent": result.get("detectedIntent", "SOLICITUD_GENERAL"),
            "recommendedPolicyId": result.get("recommendedPolicyId"),
            "recommendedPolicyName": result.get("recommendedPolicyName"),
            "confidenceScore": max(0.0, min(1.0, confidence)),
            "explanation": result.get("explanation", ""),
            "requiredDocuments": result.get("requiredDocuments", []),
            "suggestedFields": result.get("suggestedFields", []),
            "warnings": result.get("warnings", []),
            "source": "AI_SERVICE",
        }
    except (GeminiQuotaExceededError, GeminiError, InvalidJsonResponseError, ValueError):
        return fallback_agent_analyze(payload)


@app.post("/analytics/report")
async def analytics_report(request: AnalyticsRequest):
    payload = request.model_dump()
    combined = request.effective_message() or "resumen general"
    try:
        result = generate_structured(
            ANALYTICS_REPORT_SYSTEM,
            "Analytics request and context:\n" + json.dumps(payload, ensure_ascii=False),
        )
        return {
            **success_meta(),
            **result,
            "source": "AI_SERVICE",
        }
    except (GeminiQuotaExceededError, GeminiError, InvalidJsonResponseError, ValueError):
        payload["message"] = combined
        return fallback_analytics_report(payload)


@app.post("/analytics/risks")
async def analytics_risks(request: AnalyticsRequest):
    payload = request.model_dump()
    try:
        result = generate_structured(
            ANALYTICS_RISKS_SYSTEM,
            "Risk analysis context:\n" + json.dumps(payload, ensure_ascii=False),
        )
        return {
            **success_meta(),
            **result,
            "source": "AI_SERVICE",
        }
    except (GeminiQuotaExceededError, GeminiError, InvalidJsonResponseError, ValueError):
        return fallback_analytics_risks(payload)


@app.post("/analytics/recommendations")
async def analytics_recommendations(request: AnalyticsRequest):
    payload = request.model_dump()
    try:
        result = generate_structured(
            ANALYTICS_RECOMMENDATIONS_SYSTEM,
            "Recommendation context:\n" + json.dumps(payload, ensure_ascii=False),
        )
        return {
            **success_meta(),
            **result,
            "source": "AI_SERVICE",
        }
    except (GeminiQuotaExceededError, GeminiError, InvalidJsonResponseError, ValueError):
        return fallback_analytics_recommendations(payload)


@app.post("/workflow/suggest")
async def workflow_suggest(request: WorkflowSuggestRequest):
    context = json.dumps(
        {
            "policyId": request.policyId,
            "activities": request.activities,
            "transitions": request.transitions,
            "lanes": request.lanes,
        },
        ensure_ascii=False,
    )
    try:
        result = generate_structured(
            WORKFLOW_SUGGEST_SYSTEM,
            f"User prompt:\n{request.prompt}\n\nCurrent diagram context:\n{context}",
        )
        return {
            **success_meta(),
            "explanation": result.get("explanation", ""),
            "flowType": result.get("flowType", "MIXED"),
            "intent": result.get("intent", "MIXED"),
            "requiresConfirmation": bool(result.get("requiresConfirmation", True)),
            "suggestedActivities": result.get("suggestedActivities", []),
            "suggestedTransitions": result.get("suggestedTransitions", []),
            "suggestedResponsibles": result.get("suggestedResponsibles", []),
            "suggestions": result.get("suggestions", []),
            "warnings": result.get("warnings", []),
        }
    except GeminiQuotaExceededError:
        return fallback_workflow_suggest(
            request.prompt,
            request.policyId,
            request.activities,
            request.transitions,
            request.lanes,
        )
    except ValueError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except (GeminiError, InvalidJsonResponseError):
        return fallback_workflow_suggest(
            request.prompt,
            request.policyId,
            request.activities,
            request.transitions,
            request.lanes,
        )


@app.post("/generate-workflow")
async def generate_workflow(request: GenerateWorkflowRequest):
    try:
        result = generate_structured(
            WORKFLOW_SYSTEM,
            f"Design a workflow for: {request.prompt}",
        )
        return {
            **success_meta(),
            "activities": result.get("activities", []),
            "transitions": result.get("transitions", []),
            "swimlanes": result.get("swimlanes", []),
            "suggestions": result.get("suggestions", []),
        }
    except GeminiQuotaExceededError:
        return fallback_generate_workflow(request.prompt)
    except ValueError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except GeminiError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    except InvalidJsonResponseError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@app.post("/assist-form")
async def assist_form(request: AssistFormRequest):
    try:
        report = request.effective_report()
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    payload = json.dumps(
        {
            "policyId": request.policyId,
            "tramiteId": request.tramiteId,
            "workflowActivityId": request.workflowActivityId,
            "formId": request.formId,
            "activityName": request.activityName,
            "fields": request.fields,
            "currentValues": request.currentValues,
            "context": request.context,
        },
        ensure_ascii=False,
    )
    try:
        result = generate_structured(
            FORM_SYSTEM,
            f"User report:\n{report}\n\nForm definition:\n{payload}",
        )
        confidence = result.get("confidence", 0.8)
        try:
            confidence = float(confidence)
        except (TypeError, ValueError):
            confidence = 0.8
        return {
            **success_meta(),
            "explanation": result.get("explanation", ""),
            "suggestedText": result.get("suggestedText", ""),
            "confidence": max(0.0, min(1.0, confidence)),
            "fieldSuggestions": result.get("fieldSuggestions", []),
            "suggestedValues": result.get("suggestedValues", {}),
            "warnings": result.get("warnings", []),
            "unmatchedFields": result.get("unmatchedFields", []),
        }
    except GeminiQuotaExceededError:
        return fallback_assist_form_structured(report, request.fields, request.currentValues)
    except ValueError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except (GeminiError, InvalidJsonResponseError):
        return fallback_assist_form_structured(report, request.fields, request.currentValues)


@app.post("/validate-diagram")
async def validate_diagram(request: ValidateDiagramRequest):
    try:
        payload = json.dumps(
            {
                "activities": request.activities,
                "transitions": request.transitions,
            },
            ensure_ascii=False,
        )
        result = generate_structured(
            VALIDATE_SYSTEM,
            f"Validate this workflow diagram:\n{payload}",
        )
        return {
            **success_meta(),
            "valid": bool(result.get("valid", False)),
            "errors": result.get("errors", []),
            "suggestions": result.get("suggestions", []),
        }
    except GeminiQuotaExceededError:
        return fallback_validate_diagram()
    except ValueError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except GeminiError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    except InvalidJsonResponseError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@app.post("/ai/task-assistant", response_model=TaskAssistantResponse)
async def task_assistant(request: TaskAssistantRequest):
    payload = request.model_dump()

    try:
        prompt = build_task_assistant_prompt(payload)
        result = await asyncio.wait_for(
            asyncio.to_thread(generate_structured, TASK_ASSISTANT_SYSTEM, prompt),
            timeout=TASK_ASSISTANT_TIMEOUT_SECONDS,
        )
        return TaskAssistantResponse(
            summary=result.get("summary") or "",
            importantData=result.get("importantData") or [],
            missingData=result.get("missingData") or [],
            recommendedAction=result.get("recommendedAction") or "",
            source="AI",
        )
    except (
        asyncio.TimeoutError,
        GeminiQuotaExceededError,
        GeminiError,
        InvalidJsonResponseError,
        ValueError,
    ):
        return TaskAssistantResponse(**fallback_task_assistant(payload))


@app.exception_handler(Exception)
async def unhandled_exception_handler(_, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={"error": "Internal server error", "detail": str(exc)},
    )
