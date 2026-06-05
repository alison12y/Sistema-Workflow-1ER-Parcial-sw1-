# F7 — IA para asistencia en formularios (pruebas)

## API

`POST /api/ai/assist-form` — permiso `TASKS_EXECUTE` (y roles de ejecución).

`POST /api/my-activities/{tramiteId}/ai-form-assisted` — traza resumen (sin texto del informe).

## Flujo

1. Funcionario abre **Mis actividades** → **Completar** (tarea `EN_CURSO`).
2. Panel **Asistencia IA** (derecha): informe libre o dictado por voz.
3. **Asistir formulario** → vista previa por campo.
4. **Aplicar sugerencias** → solo campos vacíos (confirmación si hay conflicto).
5. Edición manual y **Completar actividad** → `FormSubmission` + motor F1.

## Cómo probar

| Paso | Acción |
|------|--------|
| Texto | Informe: *Motivo: permiso familiar. Fecha: 2026-06-15. Observación: documentación adjunta en mesa.* |
| Voz | Chrome/Edge → **Dictar por voz** → **Asistir formulario** |
| Aplicar | **Aplicar sugerencias** → ver campos llenos; campos ya completados no se sobrescriben |
| Validación | Dejar obligatorio vacío → **Completar** debe pedir completar |
| Envío | Completar → ver traza `IA_FORMULARIO_ASISTIDO` y `FORMULARIO_ENVIADO` en monitor |
| Fallback | Detener ai-service → parser local (Java/Python) |
| FILE | Campo archivo → mensaje “no autocompletar” |

## Trazabilidad

Evento `IA_FORMULARIO_ASISTIDO` con resumen: campos sugeridos/aplicados, actividad y tarea. No se guarda el informe completo en traza.

## Listo para F8

Bandeja, formularios, trazas y permisos de ejecución listos para colaboración básica entre funcionarios.
