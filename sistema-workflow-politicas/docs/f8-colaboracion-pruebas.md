# F8 — Diseño colaborativo básico realtime (pruebas)

## API

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/workflow-designer/policy/{policyId}/collaboration?sessionId=&baseRevision=` | Heartbeat + estado (polling) |
| POST | `/api/workflow-designer/policy/{policyId}/collaboration/open` | Registrar apertura (`WORKFLOW_ABIERTO`) |
| POST | `/api/workflow-designer/policy/{policyId}/collaboration/heartbeat` | Mantener presencia |
| POST | `/api/workflow-designer/policy/{policyId}/collaboration/close` | Salir del diseñador |
| POST | `/api/workflow-designer/policy/{policyId}/collaboration/conflict` | Bitácora `CONFLICTO_EDICION` |
| POST | `/api/workflow-designer/policy/{policyId}/collaboration/editing` | Registrar edición activa (nodo/conexión) |
| DELETE | `/api/workflow-designer/policy/{policyId}/collaboration/editing/{elementId}?sessionId=` | Liberar edición activa |

Permisos: lectura del diseñador (`WORKFLOW_VIEW` / `WORKFLOW_DESIGN` / `WORKFLOW_MANAGE`). Las mutaciones del diagrama siguen exigiendo permiso de diseño.

## Flujo

1. Al abrir el diseñador se registra sesión y evento `WORKFLOW_ABIERTO` en bitácora.
2. Cada guardado de actividad/conexión/posición incrementa `revision` y registra `WORKFLOW_MODIFICADO`.
3. Polling cada **11 s** (POST heartbeat) devuelve `connectedUsers`, `activeEdits` (quién edita qué nodo/conexión), `recentActions` (últimas 10 acciones), última modificación detallada (`lastModifiedSummary`, `lastModifiedActionLabel`, `lastModifiedElementName`) y si la revisión del cliente quedó obsoleta (`staleForClient`).
4. Al seleccionar nodo/conexión el cliente envía **editing** (`SELECTING` / `MOVING` / `EDITING`). Si otro usuario ya edita el mismo elemento, aparece advertencia (bloqueo suave).
4. Si otro usuario modificó el diagrama, el cliente muestra conflicto y bloquea guardados hasta **Recargar diagrama**.
5. Al detectar conflicto se registra una vez `CONFLICTO_EDICION`.

## Cómo probar (dos navegadores)

| Paso | Usuario A (ej. María) | Usuario B (ej. Alison) |
|------|----------------------|------------------------|
| 1 | Login → `/workflow-designer/{policyId}` | Mismo policyId |
| 2 | Panel: **Conectados** + **Editando ahora** | Igual |
| 3 | Seleccionar nodo “Recepción…” | Ve “María está editando/seleccionando …” |
| 4 | — | Seleccionar otro nodo → María ve actividad de Alison |
| 5 | — | Intentar editar el mismo nodo de María → advertencia |
| 6 | — | Guardar cambio → María ve **Última modificación** y **Actividad reciente** con detalle (ej. Alison editó actividad "Aprobación Legal") |
| 7 | Guardar después | Alison ve última modificación y actividad reciente de María |
| 8 | Cerrar ventana de B | Tras ~25 s desaparece de **Editando ahora** y **Conectados** |
| 9 | Conflicto global | B guarda → en ~11 s María ve mensaje con nombre y hora de Alison + **Recargar diagrama** |

## Trazabilidad

- **Bitácora:** `WORKFLOW_ABIERTO`, `WORKFLOW_MODIFICADO`, `CONFLICTO_EDICION` (módulo *Diseñador UML*).
- **Monitor trámites:** etiquetas en `monitoring-display.util.ts` para eventos de trámite; la colaboración de diseño se consulta en bitácora.

## Listo para F9

Diseñador UML con presencia, detección de conflicto por revisión, bloqueo de sobrescritura y trazas para demo/documentación del Ciclo 1.

Documentación académica y guía de defensa: [guia-demo-ciclo1.md](./guia-demo-ciclo1.md), [casos-uso.md](./casos-uso.md), [arquitectura.md](./arquitectura.md).

## Riesgos pendientes

- Sin merge automático: el último guardado gana si dos usuarios ignoran el aviso.
- Presencia expira ~25 s sin heartbeat (TTL); al cerrar ventana el usuario desaparece tras el TTL.
- Varias pestañas del mismo usuario se muestran una sola vez en el panel (agrupación por `userId`).
- Cambios solo en pestañas del diseñador Angular (no sincroniza listas CRUD de actividades/transiciones en otra ruta).
- `updatePosition` incrementa revisión (movimiento de nodos cuenta como modificación).
