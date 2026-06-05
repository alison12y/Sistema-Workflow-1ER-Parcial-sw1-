"""Parser local de sugerencias de workflow (español) — F6 Ciclo 1."""

from __future__ import annotations

import re
from typing import Any

from fallbacks import QUOTA_MESSAGE, quota_meta


def _norm(name: str) -> str:
    return re.sub(r"\s+", " ", (name or "").strip().lower())


def _clean(raw: str) -> str:
    s = (raw or "").strip()
    s = re.sub(r"^(la|el|una|un)\s+", "", s, flags=re.I)
    s = re.sub(r"\s+de\s+forma\s+secuencial.*$", "", s, flags=re.I)
    s = re.sub(r"\s+y\s+conectar.*$", "", s, flags=re.I)
    return s.strip(" .,")


def _index_activities(activities: list[dict[str, Any]]) -> dict[str, str]:
    out: dict[str, str] = {}
    for a in activities or []:
        name = a.get("name")
        if name:
            out[_norm(name)] = name
    return out


def _extract_connect_after(prompt: str) -> str | None:
    m = re.search(
        r"despu[eé]s\s+de\s+(.+?)(?:\s+de\s+forma|\s+y\s+conectar|,|\.|$)",
        prompt,
        re.I,
    )
    return _clean(m.group(1)) if m else None


def _extract_condition(prompt: str) -> str:
    m = re.search(r"(?:condici[oó]n|si|cuando)\s+(.+?)(?:\.|,|$)", prompt, re.I)
    if m:
        label = _clean(m.group(1))
        if label and len(label) < 60:
            return label
    low = prompt.lower()
    if "aprobado" in low:
        return "Aprobado"
    if "rechazado" in low:
        return "Rechazado"
    return "Condición"


def fallback_workflow_suggest(
    prompt: str,
    policy_id: str,
    activities: list[dict[str, Any]],
    transitions: list[dict[str, Any]],
    lanes: list[dict[str, Any]],
) -> dict[str, Any]:
    low = prompt.lower()
    existing = _index_activities(activities)
    suggested_activities: list[dict[str, Any]] = []
    suggested_transitions: list[dict[str, Any]] = []
    suggested_responsibles: list[dict[str, Any]] = []
    warnings: list[str] = []
    intents: list[str] = []

    if ("validar" in low or "validación" in low) and (
        "diagrama" in low or "flujo" in low or "workflow" in low
    ):
        return {
            **quota_meta(),
            "explanation": (
                "Solicitud de validación. Use «Validar flujo» en el diseñador; "
                "no se aplicarán cambios automáticos."
            ),
            "flowType": "VALIDATION",
            "intent": "VALIDATE_DIAGRAM",
            "requiresConfirmation": True,
            "suggestedActivities": [],
            "suggestedTransitions": [],
            "suggestedResponsibles": [],
            "suggestions": ["Pulse «Validar flujo» en la barra del lienzo."],
            "warnings": [QUOTA_MESSAGE],
        }

    m = re.search(
        r"crear\s+(?:una\s+)?actividad\s+(.+?)(?:\s+en\s+(?:el\s+)?departamento\s+([^,\.y]+))?",
        prompt,
        re.I,
    )
    if m:
        name = _clean(m.group(1))
        dept = _clean(m.group(2)) if m.lastindex and m.lastindex >= 2 and m.group(2) else None
        if name and name.lower() not in ("actividad", "una actividad"):
            item: dict[str, Any] = {
                "operation": "CREATE",
                "name": name,
                "activityType": "TASK",
                "responsibleType": "DEPARTMENT" if dept else "ROLE",
            }
            if dept:
                item["responsibleName"] = dept
                suggested_responsibles.append({"name": dept, "type": "DEPARTMENT"})
            after = _extract_connect_after(prompt)
            if after:
                item["connectAfterActivityName"] = after
            suggested_activities.append(item)
            intents.append("CREATE_ACTIVITY")

    if "decisión" in low or "decision" in low or "gateway" in low:
        dm = re.search(
            r"(?:crear|agregar)\s+(?:un\s+)?(?:punto\s+de\s+)?decisi[oó]n\s+(.+?)(?:\s+en\s+(.+?))?(?:\.|,|$)",
            prompt,
            re.I,
        )
        dname = _clean(dm.group(1)) if dm else "¿Decisión?"
        suggested_activities.append(
            {
                "operation": "CREATE",
                "name": dname or "¿Decisión?",
                "activityType": "DECISION",
                "responsibleType": "ROLE",
            }
        )
        intents.append("CREATE_DECISION")

    ttype = "CONDITIONAL" if "condicional" in low or "condición" in low else "SEQUENTIAL"
    condition = _extract_condition(prompt) if ttype == "CONDITIONAL" else None

    cm = re.search(
        r"conectar(?:la)?\s+(?:despu[eé]s\s+de|tras)\s+(.+?)(?:\s+(?:con|a|hacia)\s+(.+?))?(?:\s+de\s+forma|$)",
        prompt,
        re.I,
    )
    if cm:
        from_name = _clean(cm.group(1))
        to_name = _clean(cm.group(2)) if cm.lastindex and cm.lastindex >= 2 and cm.group(2) else None
        if not to_name and suggested_activities:
            to_name = suggested_activities[-1]["name"]
        if from_name and to_name:
            suggested_transitions.append(
                {
                    "operation": "CREATE",
                    "fromActivityName": from_name,
                    "toActivityName": to_name,
                    "transitionType": ttype,
                    "conditionLabel": condition,
                }
            )
            intents.append("CONNECT")
    elif suggested_activities:
        after = _extract_connect_after(prompt)
        if after:
            new_name = suggested_activities[-1]["name"]
            suggested_transitions.append(
                {
                    "operation": "CREATE",
                    "fromActivityName": after,
                    "toActivityName": new_name,
                    "transitionType": ttype,
                    "conditionLabel": condition,
                }
            )
            intents.append("CONNECT")

    if "paralel" in low or "bifurc" in low or "fork" in low:
        intents.append("PARALLEL")
        warnings.append(
            "Paralelo/bifurcación sugerida; verifique PARALLEL_SPLIT/JOIN en el diagrama."
        )

    flow = "SEQUENTIAL"
    if any(t.get("transitionType", "").startswith("PARALLEL") for t in suggested_transitions):
        flow = "PARALLEL"
    elif any(t.get("transitionType") == "CONDITIONAL" for t in suggested_transitions):
        flow = "CONDITIONAL"
    elif suggested_transitions:
        flow = "SEQUENTIAL"
    elif suggested_activities:
        flow = "MIXED"

    explanation = (
        f"{QUOTA_MESSAGE} Sugerencia local para política {policy_id or '—'}. "
        f"Actividades: {len(suggested_activities)}; conexiones: {len(suggested_transitions)}."
    )

    return {
        **quota_meta(),
        "explanation": explanation,
        "flowType": flow,
        "intent": "_".join(intents) if intents else "UNKNOWN",
        "requiresConfirmation": True,
        "suggestedActivities": suggested_activities,
        "suggestedTransitions": suggested_transitions,
        "suggestedResponsibles": suggested_responsibles,
        "suggestions": [
            "Revise la sugerencia y pulse «Aplicar sugerencia» en el diseñador.",
            "Luego valide el flujo antes de activar la política.",
        ],
        "warnings": warnings,
    }
