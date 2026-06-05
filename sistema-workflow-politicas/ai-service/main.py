from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse

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
from schemas import (
    AssistantRequest,
    AssistFormRequest,
    GenerateWorkflowRequest,
    ValidateDiagramRequest,
    WorkflowSuggestRequest,
)
from workflow_suggest_fallback import fallback_workflow_suggest
from form_assist_fallback import fallback_assist_form as fallback_assist_form_structured

app = FastAPI(title="AI Service for Workflow Policies")

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


@app.post("/assistant")
async def assistant(request: AssistantRequest):
    return generate_assistant(request.prompt, request.module, request.context)


@app.post("/workflow/suggest")
async def workflow_suggest(request: WorkflowSuggestRequest):
    import json

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
    except GeminiError as exc:
        return fallback_workflow_suggest(
            request.prompt,
            request.policyId,
            request.activities,
            request.transitions,
            request.lanes,
        )
    except InvalidJsonResponseError:
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
    import json

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
        import json

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


@app.exception_handler(Exception)
async def unhandled_exception_handler(_, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={"error": "Internal server error", "detail": str(exc)},
    )
