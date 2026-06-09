from typing import Any


def fallback_task_assistant(data: dict[str, Any]) -> dict[str, Any]:
    """Genera una respuesta básica basada en los datos proporcionados."""
    tramite_name = data.get("tramiteName") or "Trámite desconocido"
    activity_name = data.get("activityName") or "Actividad desconocida"
    status = data.get("taskStatus") or "PENDIENTE"
    assigned_to = data.get("assignedTo") or "Sin asignar"

    important_data = [
        f"Trámite: {tramite_name}",
        f"Actividad actual: {activity_name}",
        f"Estado de la tarea: {status}",
        f"Responsable asignado: {assigned_to}",
    ]

    form_data = data.get("formData") or {}
    documents = data.get("documents") or []
    missing_data = []
    if not form_data:
        missing_data.append("Verificar si el formulario fue completado")
    if not documents:
        missing_data.append("Verificar si existen documentos requeridos")
    if not missing_data:
        missing_data.append("Confirmar que los datos del formulario son correctos antes de avanzar")

    return {
        "summary": "El funcionario debe revisar la actividad actual del trámite y continuar con el flujo correspondiente.",
        "importantData": important_data,
        "missingData": missing_data,
        "recommendedAction": "Revisar la información disponible y completar la tarea si los datos son correctos. Si falta información, registrar una observación antes de continuar.",
        "source": "LOCAL_FALLBACK"
    }
