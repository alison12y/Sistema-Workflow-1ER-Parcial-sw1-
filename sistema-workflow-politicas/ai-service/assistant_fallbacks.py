"""Fallbacks inteligentes del asistente general por módulo (sin persistir datos)."""

import re
from typing import Any

from fallbacks import QUOTA_MESSAGE, quota_meta

SUPPORTED_MODULES = {
    "users", "roles", "departments", "policies", "workflow", "workflows",
    "forms", "process", "tasks", "monitoring", "kpi", "audit", "reports",
}


def normalize_module(module: str | None) -> str:
    if not module:
        return "policies"
    m = module.strip().lower()
    if m == "workflows":
        return "workflow"
    return m if m in SUPPORTED_MODULES else "policies"


def _write_endpoint(method: str, url: str) -> dict[str, str]:
    return {"method": method, "url": url}


def _extract_person_name(prompt: str) -> str:
    match = re.search(
        r"(?:usuario\s+para|crear\s+usuario\s+(?:para\s+)?)([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+(?:\s+[A-ZÁÉÍÓÚÑ][a-záéíóúñ]+)+)",
        prompt,
        re.IGNORECASE,
    )
    if match:
        return match.group(1).strip()
    match = re.search(r"([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+\s+[A-ZÁÉÍÓÚÑ][a-záéíóúñ]+)", prompt)
    return match.group(1).strip() if match else "Usuario Nuevo"


def _extract_department(prompt: str) -> str:
    match = re.search(r"(?:de|del?\s+)(Recursos Humanos|TI|Finanzas|Compras|Logística|[A-ZÁÉÍÓÚÑ][a-záéíóúñ]+)", prompt, re.I)
    return match.group(1) if match else ""


def _extract_role(prompt: str) -> str:
    match = re.search(r"como\s+([A-Za-zÁÉÍÓÚÑáéíóúñ\s]+)", prompt, re.I)
    return match.group(1).strip() if match else "Funcionario"


def _username_from_name(full_name: str) -> str:
    parts = [p.lower() for p in full_name.split() if p]
    if len(parts) >= 2:
        return f"{parts[0]}.{parts[-1]}"
    return parts[0] if parts else "usuario.nuevo"


def fallback_users(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    full_name = _extract_person_name(prompt)
    department = _extract_department(prompt) or context.get("departmentName", "")
    role = _extract_role(prompt)
    username = _username_from_name(full_name)
    return {
        **quota_meta(),
        "module": "users",
        "intent": "create_user",
        "answer": (
            f"Sugerencia para crear el usuario {full_name}. "
            "Revise los datos y confirme con Aplicar; el sistema no guardará nada hasta entonces."
        ),
        "suggestedData": {
            "username": username,
            "email": context.get("email") or "",
            "fullName": full_name,
            "departmentName": department,
            "roleSuggestion": role,
            "active": True,
            "password": None,
        },
        "suggestedEndpoint": _write_endpoint("POST", "/api/users"),
        "requiresConfirmation": True,
        "suggestions": [
            "Asigne el departmentId real tras confirmar el departamento en el sistema",
            "Vincule roleIds después de crear o seleccionar el rol",
        ],
        "warnings": [
            "No se genera contraseña automáticamente; defínala al aplicar",
            "Verifique que el correo sea institucional antes de guardar",
        ],
    }


def fallback_roles(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    name = context.get("name") or "Rol sugerido"
    if "admin" in prompt.lower():
        name = "Administrador"
    elif "funcionario" in prompt.lower():
        name = "Funcionario"
    return {
        **quota_meta(),
        "module": "roles",
        "intent": "create_role",
        "answer": "Sugerencia de rol. Confirme para llamar a POST /api/roles.",
        "suggestedData": {
            "name": name,
            "description": f"Rol derivado del prompt: {prompt[:120]}",
            "permissionIds": context.get("permissionIds", []),
        },
        "suggestedEndpoint": _write_endpoint("POST", "/api/roles"),
        "requiresConfirmation": True,
        "suggestions": ["Seleccione permisos existentes en el catálogo del sistema"],
        "warnings": [],
    }


def fallback_departments(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    name = context.get("name") or _extract_department(prompt) or "Nuevo Departamento"
    return {
        **quota_meta(),
        "module": "departments",
        "intent": "create_department",
        "answer": "Sugerencia de departamento lista para revisión.",
        "suggestedData": {
            "name": name,
            "description": f"Departamento sugerido según: {prompt[:100]}",
            "managerSuggestion": context.get("managerId") or context.get("managerSuggestion"),
        },
        "suggestedEndpoint": _write_endpoint("POST", "/api/departments"),
        "requiresConfirmation": True,
        "suggestions": ["Asigne managerId si ya conoce el usuario responsable"],
        "warnings": [],
    }


def fallback_policies(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    topic = prompt.strip()[:80]
    policy_type = "PURCHASE_REQUEST" if "compra" in prompt.lower() else "GENERAL_REQUEST"
    if "permiso" in prompt.lower() or "vacacion" in prompt.lower():
        policy_type = "LEAVE_REQUEST"
    return {
        **quota_meta(),
        "module": "policies",
        "intent": "create_policy",
        "answer": (
            "Sugerencia de política en borrador. Active el workflow después de crear el diagrama."
        ),
        "suggestedData": {
            "name": context.get("name") or f"Política - {topic[:50]}",
            "description": context.get("description") or f"Política sugerida: {topic}",
            "type": policy_type,
            "status": "DRAFT",
            "createdBy": context.get("createdBy") or context.get("userId"),
        },
        "suggestedEndpoint": _write_endpoint("POST", "/api/policies"),
        "requiresConfirmation": True,
        "suggestions": [
            "Tras crear la política, diseñe el workflow en /api/workflows",
            "Valide con POST /api/workflows/{id}/validate antes de activar",
        ],
        "warnings": ["La política quedará en DRAFT hasta activación manual"],
    }


def fallback_workflow(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    from fallbacks import fallback_generate_workflow

    wf = fallback_generate_workflow(prompt)
    return {
        **quota_meta(),
        "module": "workflow",
        "intent": "create_workflow",
        "answer": (
            "Diagrama sugerido. Aplique creando el workflow y luego las actividades/transiciones."
        ),
        "suggestedData": {
            "policyId": context.get("policyId"),
            "swimlanes": wf.get("swimlanes", []),
            "activities": wf.get("activities", []),
            "transitions": wf.get("transitions", []),
        },
        "suggestedEndpoint": _write_endpoint("POST", "/api/workflows"),
        "requiresConfirmation": True,
        "suggestions": wf.get("suggestions", []),
        "warnings": ["Cree actividades y transiciones en endpoints dedicados tras el diagrama"],
    }


def fallback_forms(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    return {
        **quota_meta(),
        "module": "forms",
        "intent": "create_form",
        "answer": "Formulario dinámico sugerido con campos típicos del trámite.",
        "suggestedData": {
            "formName": context.get("formName") or "Formulario sugerido",
            "description": context.get("description") or prompt[:200],
            "fields": [
                {"label": "Título", "name": "titulo", "type": "LABEL", "required": False, "order": 0},
                {"label": "Motivo", "name": "motivo", "type": "TEXTAREA", "required": True, "order": 1},
                {"label": "Fecha", "name": "fecha", "type": "DATE", "required": True, "order": 2},
                {"label": "Tipo", "name": "tipo", "type": "SELECT", "required": False, "order": 3},
                {"label": "Acepto términos", "name": "acepto", "type": "CHECKBOX", "required": True, "order": 4},
                {"label": "Adjunto", "name": "adjunto", "type": "FILE", "required": False, "order": 5},
            ],
        },
        "suggestedEndpoint": _write_endpoint("POST", "/api/forms"),
        "requiresConfirmation": True,
        "suggestions": [
            "Tras crear el formulario use POST /api/form-fields por cada campo",
            "Vincule dynamicFormId en la actividad TASK correspondiente",
        ],
        "warnings": [],
    }


def fallback_process(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    return {
        **quota_meta(),
        "module": "process",
        "intent": "start_process",
        "answer": (
            "Revise que la política esté ACTIVE y tenga workflow válido antes de iniciar el trámite."
        ),
        "suggestedData": {
            "policyId": context.get("policyId"),
            "initiatorId": context.get("initiatorId") or context.get("userId"),
            "formData": context.get("formData") or context.get("initialData") or {},
        },
        "suggestedEndpoint": _write_endpoint("POST", "/api/process/start"),
        "requiresConfirmation": True,
        "suggestions": [
            "Confirme policyId activa",
            "Complete formData según el formulario de la primera actividad",
        ],
        "warnings": ["No se iniciará el trámite hasta que confirme en el frontend"],
    }


def fallback_tasks(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    task_id = context.get("taskId") or "{id}"
    action = "completar"
    if "devolv" in prompt.lower():
        action = "devolver"
    elif "revis" in prompt.lower():
        action = "revisar"
    return {
        **quota_meta(),
        "module": "tasks",
        "intent": "complete_task",
        "answer": f"Sugerencia para {action} la tarea. Confirme para enviar stepData al backend.",
        "suggestedData": {
            "taskId": context.get("taskId"),
            "observacion": f"Observación sugerida: {prompt[:300]}",
            "stepData": context.get("stepData") or {"comentario": prompt[:200]},
            "recomendacion": action,
        },
        "suggestedEndpoint": _write_endpoint("POST", f"/api/tasks/{task_id}/complete"),
        "requiresConfirmation": True,
        "suggestions": ["Verifique que la tarea esté en estado PENDING"],
        "warnings": ["La acción real solo ocurre al confirmar"],
    }


def fallback_monitoring(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    return {
        **quota_meta(),
        "module": "monitoring",
        "intent": "interpret_monitoring",
        "answer": (
            "Consulte trámites en ejecución con GET /api/monitoring/processes. "
            "Use traceability para ver historial de un proceso."
        ),
        "suggestedData": {
            "interpretacion": "Revise procesos IN_PROGRESS y tareas PENDING asociadas",
            "endpointsConsulta": [
                "GET /api/monitoring/processes",
                "GET /api/monitoring/processes/{processId}/tasks",
                "GET /api/monitoring/traceability/{processId}",
            ],
            "context": context,
        },
        "suggestedEndpoint": None,
        "requiresConfirmation": False,
        "suggestions": ["Identifique procesos con mucho tiempo en ejecución"],
        "warnings": [],
    }


def fallback_kpi(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    return {
        **quota_meta(),
        "module": "kpi",
        "intent": "interpret_kpi",
        "answer": (
            "Métricas en GET /api/kpi/dashboard y cuellos de botella en GET /api/kpi/bottlenecks."
        ),
        "suggestedData": {
            "interpretacion": "Compare pendingTasks vs completedTasks y averageProcessDurationHours",
            "endpointsConsulta": ["GET /api/kpi/dashboard", "GET /api/kpi/bottlenecks"],
            "context": context,
        },
        "suggestedEndpoint": None,
        "requiresConfirmation": False,
        "suggestions": [
            "Priorice roles con más tareas pendientes",
            "Revise actividades con mayor demora media",
        ],
        "warnings": [],
    }


def fallback_audit(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    return {
        **quota_meta(),
        "module": "audit",
        "intent": "interpret_audit",
        "answer": "Auditoría disponible por entidad, usuario o listado general.",
        "suggestedData": {
            "interpretacion": "Busque cambios de estado en políticas, procesos y tareas",
            "endpointsConsulta": [
                "GET /api/audit",
                "GET /api/audit/entity/{entityName}/{entityId}",
                "GET /api/audit/user/{userId}",
            ],
            "context": context,
        },
        "suggestedEndpoint": None,
        "requiresConfirmation": False,
        "suggestions": ["Filtre por userId del responsable de la acción"],
        "warnings": [],
    }


def fallback_reports(prompt: str, context: dict[str, Any]) -> dict[str, Any]:
    return {
        **quota_meta(),
        "module": "reports",
        "intent": "suggest_report_filters",
        "answer": "Estructura de reporte sugerida; no se genera archivo automáticamente.",
        "suggestedData": {
            "filtrosSugeridos": {
                "status": context.get("status", "IN_PROGRESS"),
                "policyId": context.get("policyId"),
                "dateFrom": context.get("dateFrom"),
                "dateTo": context.get("dateTo"),
            },
            "columnas": ["policyId", "status", "initiatorId", "startedAt", "endedAt"],
            "formato": context.get("format", "JSON"),
        },
        "suggestedEndpoint": None,
        "requiresConfirmation": False,
        "suggestions": ["Combine datos de /api/process y /api/kpi/dashboard"],
        "warnings": ["No se crean reportes persistidos desde el asistente"],
    }


def fallback_assistant(prompt: str, module: str, context: dict[str, Any]) -> dict[str, Any]:
    module = normalize_module(module)
    handlers = {
        "users": fallback_users,
        "roles": fallback_roles,
        "departments": fallback_departments,
        "policies": fallback_policies,
        "workflow": fallback_workflow,
        "forms": fallback_forms,
        "process": fallback_process,
        "tasks": fallback_tasks,
        "monitoring": fallback_monitoring,
        "kpi": fallback_kpi,
        "audit": fallback_audit,
        "reports": fallback_reports,
    }
    handler = handlers.get(module, fallback_policies)
    result = handler(prompt, context or {})
    result["error"] = result.get("error") or QUOTA_MESSAGE
    return result
