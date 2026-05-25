"""Orquestación del asistente general (Gemini + normalización de respuesta)."""

import json
from typing import Any

from assistant_fallbacks import fallback_assistant, normalize_module
from fallbacks import success_meta
from gemini_client import GeminiQuotaExceededError, generate_structured

ASSISTANT_SYSTEM = """You are a workflow policy system assistant.
You NEVER save data. You only suggest structured data for the user to confirm in the frontend.

Return ONLY valid JSON with this structure:
{
  "module": "users|roles|departments|policies|workflow|forms|process|tasks|monitoring|kpi|audit|reports",
  "intent": "string_snake_case",
  "answer": "explanatory text in Spanish for the user",
  "suggestedData": {},
  "suggestedEndpoint": {"method": "POST|GET", "url": "/api/..."} or null,
  "requiresConfirmation": true,
  "suggestions": ["string"],
  "warnings": ["string"]
}

Rules:
- requiresConfirmation MUST be true when suggesting create/update/delete actions.
- For monitoring, kpi, audit, reports: suggestedEndpoint must be null and requiresConfirmation false.
- users: never generate real passwords; password null; warn about institutional email.
- Do not invent database IDs; use names and let frontend resolve IDs.
- Match the requested module and user prompt intent."""


def _normalize_response(raw: dict[str, Any], module: str, meta: dict[str, Any]) -> dict[str, Any]:
    mod = normalize_module(raw.get("module") or module)
    endpoint = raw.get("suggestedEndpoint")
    if endpoint is not None and not isinstance(endpoint, dict):
        endpoint = None

    requires = bool(raw.get("requiresConfirmation", endpoint is not None))
    if mod in ("monitoring", "kpi", "audit", "reports"):
        requires = False
        endpoint = None

    return {
        **meta,
        "module": mod,
        "intent": raw.get("intent") or f"assist_{mod}",
        "answer": raw.get("answer") or "Sugerencia generada por el asistente.",
        "suggestedData": raw.get("suggestedData") if isinstance(raw.get("suggestedData"), dict) else {},
        "suggestedEndpoint": endpoint,
        "requiresConfirmation": requires,
        "suggestions": raw.get("suggestions") if isinstance(raw.get("suggestions"), list) else [],
        "warnings": raw.get("warnings") if isinstance(raw.get("warnings"), list) else [],
    }


def generate_assistant(prompt: str, module: str, context: dict[str, Any]) -> dict[str, Any]:
    mod = normalize_module(module)
    ctx = context or {}
    user_message = (
        f"Module: {mod}\n"
        f"User prompt: {prompt}\n"
        f"Context JSON: {json.dumps(ctx, ensure_ascii=False)}"
    )
    try:
        raw = generate_structured(ASSISTANT_SYSTEM, user_message)
        return _normalize_response(raw, mod, success_meta())
    except GeminiQuotaExceededError:
        return fallback_assistant(prompt, mod, ctx)
