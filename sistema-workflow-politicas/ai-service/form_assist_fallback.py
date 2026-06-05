"""Parser local para asistencia de formularios (F7 Ciclo 1)."""

from __future__ import annotations

import re
from datetime import date
from typing import Any

from fallbacks import QUOTA_MESSAGE, quota_meta


def _norm(text: str) -> str:
    return re.sub(r"\s+", " ", (text or "").strip().lower())


def _match_option(report: str, options: str | None) -> str | None:
    if not options:
        return None
    low = report.lower()
    for opt in options.split(","):
        o = opt.strip()
        if o and o.lower() in low:
            return o
    return None


def _extract_labeled(report: str, label: str, name: str) -> str | None:
    for key in (label, name):
        if not key:
            continue
        m = re.search(
            rf"{re.escape(key)}\s*[:=]\s*([^\n.;]+)",
            report,
            re.I,
        )
        if m:
            return m.group(1).strip()
    return None


def fallback_assist_form(
    report: str,
    fields: list[dict[str, Any]],
    current_values: dict[str, Any] | None = None,
) -> dict[str, Any]:
    current_values = current_values or {}
    suggestions: list[dict[str, Any]] = []
    suggested_values: dict[str, str] = {}
    warnings: list[str] = []
    unmatched: list[str] = []
    matched = 0

    for field in fields:
        name = (field.get("name") or "").strip() or _norm(field.get("label") or "campo").replace(" ", "_")
        label = field.get("label") or name
        ftype = (field.get("type") or "TEXT").upper()

        if ftype == "FILE":
            suggestions.append(
                {
                    "fieldName": name,
                    "fieldLabel": label,
                    "fieldType": ftype,
                    "suggestedValue": None,
                    "applicable": False,
                    "confidence": 0.0,
                    "message": "Los campos FILE no se autocompletan.",
                }
            )
            continue

        extracted = _extract_labeled(report, label, name)
        low_label = _norm(label)
        low_report = report.lower()

        if not extracted:
            if ftype == "DATE" and "fecha" in low_label:
                extracted = date.today().isoformat()
            elif ftype in ("TEXTAREA", "TEXT") and any(
                k in low_label for k in ("observ", "motivo", "coment", "informe", "descripcion")
            ):
                extracted = report[:500] if len(report) > 500 else report
            elif ftype in ("SELECT", "RADIO"):
                extracted = _match_option(report, field.get("options"))
            elif ftype == "CHECKBOX" and any(w in low_report for w in ("acepto", "confirmo", "sí", "si ")):
                extracted = "true"
            elif ftype == "NUMBER":
                nm = re.search(
                    r"(?:monto|cantidad|total)\s*[:=]?\s*([\d.,]+)",
                    report,
                    re.I,
                )
                if nm:
                    extracted = nm.group(1).replace(",", ".")

        if not extracted:
            if field.get("required"):
                unmatched.append(label)
            suggestions.append(
                {
                    "fieldName": name,
                    "fieldLabel": label,
                    "fieldType": ftype,
                    "suggestedValue": None,
                    "applicable": False,
                    "confidence": 0.0,
                    "message": "No se detectó valor en el informe.",
                }
            )
            continue

        if ftype == "CHECKBOX":
            val = "true" if str(extracted).lower() in ("true", "si", "sí", "1", "acepto", "confirmo") else "false"
        elif ftype == "FILE":
            val = None
        else:
            val = str(extracted).strip()

        if val is None:
            continue

        matched += 1
        suggested_values[name] = val
        suggestions.append(
            {
                "fieldName": name,
                "fieldLabel": label,
                "fieldType": ftype,
                "suggestedValue": val,
                "applicable": True,
                "confidence": 0.6,
                "message": None,
            }
        )

    if current_values:
        warnings.append("Los valores ya cargados no se sobrescriben sin confirmación en el cliente.")

    return {
        **quota_meta(),
        "explanation": (
            f"{QUOTA_MESSAGE} Se interpretó el informe y se mapearon {matched} de {len(fields)} campos."
        ),
        "confidence": 0.55 if matched else 0.3,
        "suggestedText": report[:200] + ("…" if len(report) > 200 else ""),
        "fieldSuggestions": suggestions,
        "suggestedValues": suggested_values,
        "warnings": warnings,
        "unmatchedFields": unmatched,
    }
