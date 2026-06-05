# F4 — Formularios dinámicos unificados — Guía de pruebas

## Modelo oficial (Ciclo 1)

```
BusinessPolicy → WorkflowActivity → DynamicForm (activityId) → FormField
                                                      ↓
Tramite → TramiteTask (workflowActivityId) → FormSubmission → stepData → F1
```

- **Clave de diseño:** `policyId` + `workflowActivityId` (`DynamicForm.activityId`)
- **Clave de ejecución:** `tramiteId` + `workflowActivityId` + `taskOrder`
- `activityName` es informativo (etiqueta en UI y compatibilidad legado)

## Flujo

1. **Diseño** — Desde el diseñador de workflow, enlace `/activities/{workflowActivityId}/form` (`activity-form-designer`).
2. **Ejecución** — Bandeja → Completar → `/mis-actividades/{tramiteId}/form?workflowActivityId=…&taskOrder=…`
3. **Carga** — `GET /api/forms/activity/{workflowActivityId}`
4. **Borrador** — `POST /api/form-submissions` con `workflowActivityId`
5. **Completar** — Valida campos → guarda `FormSubmission` (`submittedAt`) → traza `FORMULARIO_ENVIADO` → F1 (`stepData`)

## Sin formulario configurado

Si la actividad no tiene `DynamicForm` activo, la UI muestra el campo **Observación de cierre** (`observacion_cierre`) obligatorio para completar.

## API relevante

| Método | Ruta |
|--------|------|
| GET | `/api/forms/activity/{workflowActivityId}` |
| GET | `/api/forms/activity/{workflowActivityId}/detail` |
| POST | `/api/form-submissions` |
| GET | `/api/form-submissions/tramite/{id}/activity?workflowActivityId=&taskOrder=` |
| PUT | `/api/my-activities/{id}/complete` |

## Cómo probar

### 1. Crear formulario para una actividad

1. Login admin → Diseñador de workflow de la política.
2. Seleccionar actividad TASK → **Formulario** (o `/activities/{id}/form`).
3. Crear formulario, agregar campos (TEXT, SELECT con opciones, FILE).
4. Guardar y activar.

### 2. Iniciar trámite y tomar tarea

1. Crear trámite de la política.
2. Funcionario responsable → **Tomar** tarea si está `PENDIENTE`.

### 3. Abrir formulario correcto

1. **Completar** en bandeja.
2. Verificar URL con `workflowActivityId` (no solo nombre).
3. Deben aparecer los campos del diseñador oficial.

### 4. Completar y verificar FormSubmission

1. Enviar formulario.
2. MongoDB / API: `GET /api/form-submissions/tramite/{tramiteId}` debe incluir:
   - `workflowActivityId`
   - `submittedBy`, `submittedAt`
   - `taskOrder`, `responses`

### 5. Avance condicional (F1)

1. En una transición condicional usar expresión con nombre de campo del formulario (ej. `monto > 1000`).
2. Completar con valor que cumpla/no cumpla la condición.
3. Verificar que el trámite enruta a la actividad esperada.

### 6. Detalle del trámite

1. Abrir detalle del trámite.
2. Sección **Respuestas del formulario**: actividad, tarea #, funcionario, fecha envío, campos principales.

### 7. Trazabilidad

En traza del trámite: `FORMULARIO_ENVIADO` y luego `ACTIVIDAD_COMPLETADA` / `TAREA_COMPLETADA`.

### 8. Diseñador legado

`/form-designer/{policyId}` redirige a `/workflow-designer/{policyId}`.

## Validaciones (F4)

- Campos obligatorios según `FormField.required`
- Tipos: NUMBER, SELECT (opciones), CHECKBOX, FILE (existencia + máx. 10 MB en servidor)
- Nombres de campo únicos por formulario (diseño y respuestas)
- `stepData` construido desde `fieldName` de respuestas

## Riesgos pendientes

- Envíos antiguos sin `workflowActivityId` siguen consultables por `activityName`.
- `GET /api/forms/policy/{id}?activity=` permanece por compatibilidad; no usar en flujos nuevos.
- IA de formularios (F7) aún usa heurísticas por nombre de actividad.

## Listo para F7 (IA formularios)

- Formularios y envíos keyed por `workflowActivityId`.
- `FormField` con `name`, `options`, tipos normalizados.
- Endpoint de detalle por actividad para contexto IA.
- `stepData` estable para sugerencias y validación asistida.
