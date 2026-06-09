"""Fallback local para analítica inteligente (CU24–CU26)."""

from __future__ import annotations

import re
import unicodedata
from collections import Counter
from typing import Any


def _normalize(text: str) -> str:
    lower = (text or "").strip().lower()
    normalized = unicodedata.normalize("NFD", lower)
    return "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")


MONTHS = {
    "enero": 1,
    "febrero": 2,
    "marzo": 3,
    "abril": 4,
    "mayo": 5,
    "junio": 6,
    "julio": 7,
    "agosto": 8,
    "septiembre": 9,
    "octubre": 10,
    "noviembre": 11,
    "diciembre": 12,
}


def _detect_report_type(message: str) -> str:
    text = _normalize(message)
    if any(m in text for m in MONTHS) or "mes" in text or "periodo" in text:
        return "TRAMITES_MES"
    if "politica" in text and ("mas usada" in text or "mas utilizada" in text):
        return "POLITICA_MAS_USADA"
    if any(k in text for k in ("funcionario", "empleado", "carga", "responsable")):
        return "FUNCIONARIO_CARGA"
    if any(k in text for k in ("demorad", "retrasad", "atrasad", "lentos")):
        return "TRAMITES_DEMORADOS"
    if any(k in text for k in ("finalizad", "cerrad", "completad", "terminad")):
        return "RESUMEN_FINALIZADOS"
    return "RESUMEN_GENERAL"


def _tramite_rows(tramites: list[dict[str, Any]], limit: int = 50) -> list[dict[str, Any]]:
    rows = []
    for t in tramites[:limit]:
        rows.append(
            {
                "Código": t.get("code", "—"),
                "Política": t.get("policyName", "—"),
                "Estado": t.get("status", "—"),
                "Prioridad": t.get("priority", "—"),
                "Actividad": t.get("currentActivity", "—"),
                "Actualizado": t.get("updatedAt", "—"),
            }
        )
    return rows


def _status_chart(tramites: list[dict[str, Any]], title: str) -> dict[str, Any]:
    counts = Counter(t.get("status", "—") for t in tramites)
    labels, values = zip(*counts.most_common(8)) if counts else ([], [])
    return {"type": "bar", "title": title, "labels": list(labels), "values": [float(v) for v in values]}


def fallback_analytics_report(payload: dict[str, Any]) -> dict[str, Any]:
    message = payload.get("message") or ""
    tramites = payload.get("tramiteSample") or []
    employee_load = payload.get("employeeLoad") or []
    report_type = _detect_report_type(message)

    if report_type == "FUNCIONARIO_CARGA":
        rows = [
            {
                "Funcionario": load.get("displayName", "—"),
                "Usuario": load.get("key", "—"),
                "Tareas activas": int(load.get("totalActive") or 0),
                "Completadas": int(load.get("completedCount") or 0),
                "Departamento": load.get("departmentName", "—"),
            }
            for load in sorted(
                employee_load,
                key=lambda x: int(x.get("totalActive") or 0),
                reverse=True
            )
        ]
        title = "Reporte de carga por funcionario"
        top = rows[0]["Funcionario"] if rows else "Sin datos"
        load_val = rows[0]["Tareas activas"] if rows else 0
        explanation = f"El funcionario con mayor carga es {top}, con {load_val} tareas activas."
        cards = [
            {"label": "Mayor carga", "value": top, "hint": "Funcionario", "severity": "warning"},
            {"label": "Total tareas", "value": str(sum(r["Tareas activas"] for r in rows)), "hint": "Activas", "severity": "info"}
        ]
        chart = {
            "type": "bar",
            "title": "Tareas activas por funcionario",
            "labels": [r["Funcionario"] for r in rows[:8]],
            "values": [float(r["Tareas activas"]) for r in rows[:8]]
        }
        suggested_format = "PANTALLA"
    elif report_type == "POLITICA_MAS_USADA":
        counts = Counter(t.get("policyName", "Sin política") for t in tramites)
        total = max(len(tramites), 1)
        rows = [
            {
                "Política": name,
                "Trámites": count,
                "Porcentaje": f"{round(count * 100 / total)}%",
            }
            for name, count in counts.most_common()
        ]
        top = counts.most_common(1)[0][0] if counts else "—"
        title = "Políticas más utilizadas"
        explanation = "Ranking de políticas según los trámites del periodo."
        cards = [{"label": "Política más usada", "value": top, "hint": "Muestra analizada", "severity": "success"}]
        chart = _status_chart(tramites, "Uso por política")
        suggested_format = "EXCEL" if len(rows) > 10 else "PANTALLA"
    elif report_type == "TRAMITES_DEMORADOS":
        rows = [
            {
                "Código": t.get("code"),
                "Política": t.get("policyName", "—"),
                "Estado": t.get("status", "—"),
                "Actividad actual": t.get("currentActivity", "—"),
            }
            for t in tramites
            if str(t.get("status", "")).upper() in {"INICIADO", "EN_PROCESO", "ACTIVO", "IN_PROGRESS"}
        ]
        title = "Trámites demorados"
        explanation = "Trámites activos potencialmente demorados según el contexto enviado."
        cards = [{"label": "Demorados", "value": str(len(rows)), "hint": "Activos en muestra", "severity": "danger"}]
        chart = _status_chart(rows, "Demoras por política")
        suggested_format = "PDF"
    elif report_type == "RESUMEN_FINALIZADOS":
        finished = [t for t in tramites if str(t.get("status", "")).upper() in {"FINALIZADO", "COMPLETADO", "DONE"}]
        rows = [
            {
                "Código": t.get("code"),
                "Política": t.get("policyName", "—"),
                "Prioridad": t.get("priority", "—"),
                "Finalizado": t.get("updatedAt", "—"),
            }
            for t in finished
        ]
        title = "Resumen de trámites finalizados"
        explanation = "Consolidado de trámites completados."
        cards = [{"label": "Finalizados", "value": str(len(rows)), "hint": "En muestra", "severity": "success"}]
        chart = _status_chart(finished, "Finalizados por política")
        suggested_format = "PDF"
    else:
        rows = _tramite_rows(tramites)
        title = "Resumen operativo de trámites"
        explanation = "Reporte dinámico generado a partir de la consulta en lenguaje natural."
        cards = [{"label": "Trámites", "value": str(len(tramites)), "hint": "En muestra", "severity": "info"}]
        chart = _status_chart(tramites, "Distribución por estado")
        suggested_format = "PANTALLA"

    return {
        "title": title,
        "explanation": explanation,
        "reportType": report_type,
        "columns": list(rows[0].keys()) if rows else ["Código", "Política", "Estado"],
        "rows": rows,
        "appliedFilters": {
            "reportType": report_type,
            "policyId": payload.get("policyId"),
            "status": payload.get("status"),
            "fromDate": payload.get("fromDate"),
            "toDate": payload.get("toDate"),
        },
        "suggestedFormat": suggested_format,
        "cards": cards,
        "chart": chart,
        "warnings": ["Análisis generado con fallback local de analítica."],
        "source": "LOCAL_FALLBACK",
    }


def fallback_analytics_risks(payload: dict[str, Any]) -> dict[str, Any]:
    tramites = payload.get("tramiteSample") or []
    bottlenecks = payload.get("bottlenecks") or []
    employee_load = payload.get("employeeLoad") or []

    risks: list[dict[str, Any]] = []
    for t in tramites:
        if str(t.get("status", "")).upper() in {"INICIADO", "EN_PROCESO", "ACTIVO", "IN_PROGRESS"}:
            risks.append(
                {
                    "type": "DEMORA",
                    "severity": "MEDIO",
                    "title": f"Trámite demorado: {t.get('code')}",
                    "description": f"El trámite {t.get('code')} requiere seguimiento en {t.get('currentActivity', 'actividad actual')}.",
                    "entityType": "Tramite",
                    "entityId": t.get("id"),
                    "entityLabel": t.get("code"),
                }
            )

    for b in bottlenecks[:5]:
        risks.append(
            {
                "type": "CUELLO",
                "severity": "ALTO" if str(b.get("level", "")).lower() == "alto" else "MEDIO",
                "title": f"Cuello de botella: {b.get('activityName')}",
                "description": b.get("observation") or "Acumulación detectada en la actividad.",
                "entityType": "WorkflowActivity",
                "entityId": b.get("workflowActivityId"),
                "entityLabel": b.get("activityName"),
            }
        )

    for load in employee_load:
        active = int(load.get("totalActive") or 0)
        if active >= 4:
            risks.append(
                {
                    "type": "CARGA",
                    "severity": "ALTO" if active >= 6 else "MEDIO",
                    "title": f"Sobrecarga: {load.get('displayName')}",
                    "description": f"{active} tareas activas.",
                    "entityType": "User",
                    "entityId": load.get("key"),
                    "entityLabel": load.get("displayName"),
                }
            )

    return {
        "summary": f"Se detectaron {len(risks)} riesgo(s) en la muestra analizada.",
        "risks": risks[:25],
        "cards": [
            {"label": "Riesgos", "value": str(len(risks)), "hint": "Detectados", "severity": "warning"},
            {"label": "Trámites", "value": str(len(tramites)), "hint": "Muestra", "severity": "info"},
        ],
        "warnings": ["Análisis de riesgos con fallback local."],
        "source": "LOCAL_FALLBACK",
    }


def fallback_analytics_recommendations(payload: dict[str, Any]) -> dict[str, Any]:
    tramites = payload.get("tramiteSample") or []
    bottlenecks = payload.get("bottlenecks") or []
    employee_load = payload.get("employeeLoad") or []

    recommendations: list[dict[str, Any]] = []
    for t in tramites[:3]:
        if str(t.get("status", "")).upper() in {"INICIADO", "EN_PROCESO", "ACTIVO", "IN_PROGRESS"}:
            recommendations.append(
                {
                    "priority": "ALTA" if str(t.get("priority", "")).upper() in {"URGENTE", "ALTA"} else "MEDIA",
                    "type": "PRIORIZAR_TRAMITE",
                    "title": f"Priorizar trámite {t.get('code')}",
                    "action": f"Revisar y avanzar el trámite {t.get('code')}.",
                    "rationale": f"Actividad actual: {t.get('currentActivity', '—')}.",
                    "tramiteCode": t.get("code"),
                    "activityName": t.get("currentActivity"),
                }
            )

    for b in bottlenecks[:2]:
        recommendations.append(
            {
                "priority": "ALTA",
                "type": "REVISAR_CUELLO",
                "title": f"Revisar cuello de botella: {b.get('activityName')}",
                "action": f"Analizar tiempos en {b.get('activityName')}.",
                "rationale": b.get("observation") or "Cuello detectado por KPI.",
                "activityName": b.get("activityName"),
            }
        )

    for load in employee_load[:2]:
        if int(load.get("totalActive") or 0) >= 4:
            recommendations.append(
                {
                    "priority": "MEDIA",
                    "type": "REASIGNAR_TAREA",
                    "title": f"Reasignar carga de {load.get('displayName')}",
                    "action": f"Redistribuir tareas de {load.get('displayName')}.",
                    "rationale": f"{load.get('totalActive')} tareas activas.",
                }
            )

    return {
        "summary": f"Se generaron {len(recommendations)} recomendación(es).",
        "recommendations": recommendations[:15],
        "cards": [
            {"label": "Recomendaciones", "value": str(len(recommendations)), "hint": "Sugeridas", "severity": "info"}
        ],
        "warnings": ["Recomendaciones con fallback local."],
        "source": "LOCAL_FALLBACK",
    }
