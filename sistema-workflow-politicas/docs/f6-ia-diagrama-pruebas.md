# F6 — IA para diseño de workflow (pruebas)

## Requisitos cubiertos

- CU14 Diseñar workflow con IA por texto/voz
- CU5 Diseñar workflow UML 2.5 (sin reemplazar el diseñador manual)
- CU6 Configurar estructuras de flujo (sugerencias de actividades y conexiones)

## API

`POST /api/ai/workflow/suggest`

**Permiso:** `WORKFLOW_DESIGN`, `WORKFLOW_MANAGE`, `POLICIES_MANAGE` o `ROLE_ADMIN` / `ROLE_PROCESS_OWNER`.

**Body ejemplo:**

```json
{
  "policyId": "<id>",
  "prompt": "Crear actividad Validar documentación en departamento Legal y conectarla después de Recepción de solicitud de forma secuencial.",
  "activities": [],
  "transitions": [],
  "lanes": []
}
```

**Respuesta:** `explanation`, `flowType`, `intent`, `suggestedActivities`, `suggestedTransitions`, `suggestedResponsibles`, `aiAvailable`, `fallbackUsed`, `requiresConfirmation`.

## Cómo probar

1. Iniciar sesión con usuario diseño (`sistema.admin` o rol con `WORKFLOW_DESIGN`).
2. Abrir **Políticas** → política → **Diagrama UML**.
3. Activar **Modo edición**.
4. En el panel **Asistente IA** (columna izquierda):
   - Pegar el prompt de ejemplo → **Generar sugerencia**.
   - Revisar vista previa (actividades, conexiones, responsables).
   - **Aplicar sugerencia** → confirmar.
5. Verificar en MongoDB / listas **Actividades** y **Conexiones** que existen `WorkflowActivity` y `WorkflowTransition` nuevos.
6. Pulsar **Validar flujo** (se ejecuta también tras aplicar).
7. **Conectar:** prompt `Conectar Recepción de solicitud con Validar documentación de forma secuencial` (con actividades existentes).
8. **Decisión:** `Crear decisión ¿Documentación válida?` → aplicar → nodo rombo en lienzo.
9. **Condicional:** incluir «condicional» y «si aprobado» en el prompt.
10. **Voz:** Chrome/Edge → **Dictar por voz** → hablar el prompt → texto en el textarea → generar.
11. **Fallback:** detener `ai-service` → generar de nuevo → badge «Parser local» y mensaje claro.

## Servicios

- Backend Java: `AiService.suggestWorkflow` → `ai-service` `/workflow/suggest`; si falla, `WorkflowSuggestLocalParser`.
- Python: Gemini + `workflow_suggest_fallback.py`.

## Riesgos

- Reconocimiento de voz depende del navegador (no offline).
- Paralelo/bifurcación: modelo soporta `PARALLEL_SPLIT` pero el toolbox guiado aún marca Fork/Join como próximos.
- Nombres similares pueden enlazar la actividad equivocada (resolución fuzzy por nombre).

## Listo para F7 (IA formularios)

- Endpoint `/api/ai/assist-form` ya existe.
- Patrón sugerir → confirmar → aplicar reutilizable en `form-execution`.
