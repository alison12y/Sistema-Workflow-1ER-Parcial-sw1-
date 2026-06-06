from __future__ import annotations

import re
import unicodedata
from typing import Any


INTENT_KEYWORDS: dict[str, list[str]] = {
    "INSTALACION_MEDIDOR": [
        "medidor",
        "instalacion",
        "instalar",
        "suministro",
        "electricidad",
        "gas",
        "contador",
    ],
    "GESTION_DECOMISADOS": [
        "decomisado",
        "decomisados",
        "bienes",
        "confiscado",
        "incautacion",
        "comiso",
    ],
    "RECLAMO_SERVICIO": ["reclamo", "queja", "mal servicio", "defecto", "reclamar"],
    "REVISION_DOCUMENTAL": ["documento", "revision", "aprobacion", "validar", "legal", "contrato"],
    "POLITICA_IA": ["inteligencia artificial", "ia", "chatbot", "asistente", "automatizar"],
    "SOLICITUD_GENERAL": ["solicitud", "tramite", "pedido", "requiero", "necesito"],
}


def _normalize(value: str) -> str:
    lowered = (value or "").lower().strip()
    normalized = unicodedata.normalize("NFD", lowered)
    without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", without_marks)


def _detect_intent(text: str) -> str:
    best_intent = "SOLICITUD_GENERAL"
    best_hits = 0
    for intent, keywords in INTENT_KEYWORDS.items():
        hits = sum(1 for keyword in keywords if keyword in text)
        if hits > best_hits:
            best_hits = hits
            best_intent = intent
    return best_intent


def _score_policy(text: str, intent: str, policy: dict[str, Any]) -> tuple[float, str]:
    score = 0.2
    reasons: list[str] = []
    policy_text = _normalize(
        f"{policy.get('name', '')} {policy.get('description', '')} {policy.get('type', '')}"
    )
    keywords = INTENT_KEYWORDS.get(intent, [])
    for keyword in keywords:
        nk = _normalize(keyword)
        if nk in policy_text:
            score += 0.18
            reasons.append(f"la política contiene '{keyword}'")
        if nk in text:
            score += 0.08
    for token in text.split():
        if len(token) >= 4 and token in policy_text:
            score += 0.05
            reasons.append(f"coincidencia con '{token}'")
    reason = "; ".join(reasons) if reasons else "mejor coincidencia entre políticas activas"
    return min(score, 0.95), reason


def _required_documents(intent: str, policy_name: str) -> list[str]:
    mapping = {
        "INSTALACION_MEDIDOR": [
            "Documento de identidad",
            "Comprobante de domicilio",
            "Plano o croquis del punto de instalación (opcional)",
        ],
        "GESTION_DECOMISADOS": [
            "Acta o informe de decomiso",
            "Inventario de bienes",
            "Documento de identidad del solicitante",
        ],
        "RECLAMO_SERVICIO": [
            "Descripción detallada del reclamo",
            "Evidencia fotográfica o comprobantes (opcional)",
        ],
        "REVISION_DOCUMENTAL": [
            "Documento a revisar",
            "Carta o memo de solicitud (opcional)",
        ],
        "POLITICA_IA": [
            "Descripción del caso de uso",
            "Documentación técnica de referencia (opcional)",
        ],
    }
    docs = list(mapping.get(intent, ["Documento de soporte (opcional)", "Descripción de la solicitud"]))
    docs.append(f"Política: {policy_name}")
    return docs


def fallback_agent_analyze(payload: dict[str, Any]) -> dict[str, Any]:
    message = _normalize(payload.get("message") or "")
    policies = [p for p in payload.get("policies", []) if str(p.get("status", "")).upper() == "ACTIVE"]
    attachment = payload.get("attachmentFileName")
    requester = payload.get("requesterName")

    if not policies:
        return {
            "detectedIntent": "SIN_POLITICAS",
            "recommendedPolicyId": None,
            "recommendedPolicyName": None,
            "confidenceScore": 0.0,
            "explanation": "No hay políticas activas para recomendar.",
            "requiredDocuments": [],
            "suggestedFields": [],
            "warnings": ["Configure al menos una política activa."],
            "source": "LOCAL_FALLBACK",
        }

    intent = _detect_intent(message)
    scored = []
    for policy in policies:
        score, reason = _score_policy(message, intent, policy)
        scored.append((score, reason, policy))
    scored.sort(key=lambda item: item[0], reverse=True)
    best_score, best_reason, best_policy = scored[0]

    confidence = max(0.3, min(best_score, 0.95))
    explanation = (
        f"Se recomienda «{best_policy.get('name')}» porque {best_reason}. "
        f"Intención detectada: {intent.replace('_', ' ').lower()}."
    )
    if attachment:
        explanation += f" Se consideró el archivo adjunto: {attachment}."

    warnings = []
    if confidence < 0.45:
        warnings.append("Confianza baja; confirme la política antes de iniciar el trámite.")

    return {
        "detectedIntent": intent,
        "recommendedPolicyId": best_policy.get("id"),
        "recommendedPolicyName": best_policy.get("name"),
        "confidenceScore": confidence,
        "explanation": explanation,
        "requiredDocuments": _required_documents(intent, best_policy.get("name", "")),
        "suggestedFields": [
            {
                "name": "description",
                "label": "Descripción de la solicitud",
                "type": "TEXTAREA",
                "required": True,
                "suggestedValue": payload.get("message", "")[:500],
            },
            {
                "name": "requestedBy",
                "label": "Solicitante",
                "type": "TEXT",
                "required": True,
                "suggestedValue": requester,
            },
            {
                "name": "priority",
                "label": "Prioridad",
                "type": "SELECT",
                "required": False,
                "suggestedValue": "NORMAL",
            },
        ],
        "warnings": warnings,
        "source": "LOCAL_FALLBACK",
    }
