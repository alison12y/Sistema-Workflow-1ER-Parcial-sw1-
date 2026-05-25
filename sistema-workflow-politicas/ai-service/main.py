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
from schemas import AssistantRequest, AssistFormRequest, GenerateWorkflowRequest, ValidateDiagramRequest

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

FORM_SYSTEM = """You are a form writing assistant for business workflows.
Return ONLY valid JSON:
{
  "suggestedText": "string",
  "confidence": 0.0
}
confidence must be a number between 0 and 1."""

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
        result = generate_structured(
            FORM_SYSTEM,
            (
                f"Field: {request.fieldName}\n"
                f"Prompt: {request.prompt}\n"
                f"Context: {request.context}"
            ),
        )
        confidence = result.get("confidence", 0.8)
        try:
            confidence = float(confidence)
        except (TypeError, ValueError):
            confidence = 0.8
        return {
            **success_meta(),
            "suggestedText": result.get("suggestedText", ""),
            "confidence": max(0.0, min(1.0, confidence)),
        }
    except GeminiQuotaExceededError:
        return fallback_assist_form(request.prompt, request.fieldName)
    except ValueError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except GeminiError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    except InvalidJsonResponseError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


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
