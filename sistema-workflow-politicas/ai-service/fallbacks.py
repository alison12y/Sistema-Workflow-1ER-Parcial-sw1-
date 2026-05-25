"""Respuestas locales de respaldo cuando Gemini no está disponible (p. ej. cuota 429)."""

from typing import Any

QUOTA_MESSAGE = "Cuota de Gemini excedida o no disponible"


def quota_meta() -> dict[str, Any]:
    return {
        "aiAvailable": False,
        "fallbackUsed": True,
        "error": QUOTA_MESSAGE,
    }


def success_meta() -> dict[str, Any]:
    return {
        "aiAvailable": True,
        "fallbackUsed": False,
        "error": None,
    }


def fallback_generate_workflow(prompt: str) -> dict[str, Any]:
    topic = prompt.strip()[:80] if prompt else "proceso"
    activities = [
        {"id": "start-1", "name": "Inicio", "type": "START", "swimlaneId": "lane-solicitante"},
        {"id": "task-1", "name": "Registrar solicitud", "type": "TASK", "swimlaneId": "lane-solicitante"},
        {"id": "decision-1", "name": "¿Aprobado?", "type": "DECISION", "swimlaneId": "lane-aprobador"},
        {"id": "task-2", "name": "Notificar resultado", "type": "TASK", "swimlaneId": "lane-aprobador"},
        {"id": "end-1", "name": "Fin", "type": "END", "swimlaneId": "lane-solicitante"},
    ]
    transitions = [
        {"sourceActivityId": "start-1", "targetActivityId": "task-1", "condition": None},
        {"sourceActivityId": "task-1", "targetActivityId": "decision-1", "condition": None},
        {"sourceActivityId": "decision-1", "targetActivityId": "task-2", "condition": "aprobado"},
        {"sourceActivityId": "task-2", "targetActivityId": "end-1", "condition": None},
    ]
    swimlanes = [
        {"id": "lane-solicitante", "name": "Solicitante"},
        {"id": "lane-aprobador", "name": "Aprobador"},
    ]
    suggestions = [
        f"Flujo de respaldo generado localmente para: {topic}",
        "Revise swimlanes y condiciones antes de activar la política",
        "Cuando la cuota de Gemini esté disponible, vuelva a generar con IA real",
    ]
    return {
        **quota_meta(),
        "activities": activities,
        "transitions": transitions,
        "swimlanes": swimlanes,
        "suggestions": suggestions,
    }


def fallback_assist_form(prompt: str, field_name: str) -> dict[str, Any]:
    text = (
        f"Texto sugerido básico para el campo '{field_name}': "
        f"Por medio de la presente se solicita registrar lo indicado en el trámite. "
        f"{prompt.strip()[:200] if prompt else ''}"
    ).strip()
    return {
        **quota_meta(),
        "suggestedText": text,
        "confidence": 0.5,
    }


def fallback_validate_diagram() -> dict[str, Any]:
    return {
        **quota_meta(),
        "valid": False,
        "errors": [],
        "suggestions": [],
    }
