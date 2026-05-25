import json
import re
from typing import Any

import google.generativeai as genai

from config import ensure_api_key, get_ai_config

try:
    from google.api_core.exceptions import ResourceExhausted
except ImportError:
    ResourceExhausted = None


class GeminiError(Exception):
    pass


class GeminiQuotaExceededError(GeminiError):
    """Cuota de Gemini excedida (HTTP 429 / ResourceExhausted)."""


class InvalidJsonResponseError(Exception):
    pass


def _is_quota_error(exc: Exception) -> bool:
    if ResourceExhausted is not None and isinstance(exc, ResourceExhausted):
        return True
    message = str(exc).lower()
    return (
        "429" in message
        or "quota" in message
        or "resource exhausted" in message
        or "rate limit" in message
        or "too many requests" in message
    )


def call_gemini(system_instruction: str, user_prompt: str) -> str:
    ensure_api_key()
    _, api_key, model_name = get_ai_config()

    try:
        genai.configure(api_key=api_key)
        model = genai.GenerativeModel(
            model_name=model_name,
            system_instruction=system_instruction,
        )
        response = model.generate_content(user_prompt)
        if not response or not response.text:
            raise GeminiError("Gemini returned an empty response")
        return response.text.strip()
    except GeminiError:
        raise
    except InvalidJsonResponseError:
        raise
    except Exception as exc:
        if _is_quota_error(exc):
            raise GeminiQuotaExceededError(
                "Gemini quota exceeded or temporarily unavailable"
            ) from exc
        raise GeminiError(f"Gemini request failed: {exc}") from exc


def parse_json_response(text: str) -> dict[str, Any]:
    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
        cleaned = re.sub(r"\s*```$", "", cleaned)

    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", cleaned, re.DOTALL)
        if match:
            try:
                return json.loads(match.group())
            except json.JSONDecodeError:
                pass
    raise InvalidJsonResponseError("AI response is not valid JSON")


def generate_structured(system_instruction: str, user_prompt: str) -> dict[str, Any]:
    raw = call_gemini(system_instruction, user_prompt)
    try:
        return parse_json_response(raw)
    except InvalidJsonResponseError as exc:
        raise InvalidJsonResponseError(
            "AI response is not valid JSON"
        ) from exc
