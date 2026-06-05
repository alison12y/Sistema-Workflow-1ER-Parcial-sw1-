# F3 — Monitor y seguimiento realtime — Guía de pruebas

## Enfoque Ciclo 1

- **Polling cada 12 s** (sin WebSocket) en:
  - `/monitoring` — monitoreo operativo
  - `/tramite-tracking` — seguimiento (supervisor / atención)
  - `/mis-actividades` — bandeja del funcionario (F2)
  - `/tramites/:id` — detalle del trámite
- Si el modal de seguimiento está abierto, también se refresca el detalle del trámite seleccionado.

## API de monitoreo

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/monitoring` | Lista resumida con conteos P/E/C y `workflowError` |
| GET | `/api/monitoring/{id}` | Estado completo |
| GET | `/api/monitoring/{id}/trace` | Alias del detalle |
| GET | `/api/monitoring/{id}/timeline` | Solo eventos ordenados |
| GET | `/api/monitoring/{id}/tasks` | Tareas agrupadas |
| GET | `/api/monitoring/{id}/responsibles` | Resumen por responsable |

## Qué muestra el seguimiento

- Código, política, estado, actividad y responsable actuales
- Tareas **pendientes**, **en proceso** y **completadas**
- Responsables con conteo de tareas
- Línea de tiempo: `PROCESO_CREADO`, `TAREA_*`, `FORMULARIO_ENVIADO`, `ACTIVIDAD_COMPLETADA`, `PROCESO_AVANZADO`, `ESPERA_PARALELO`, `TRAMITE_FINALIZADO`, `ERROR_WORKFLOW`
- Alerta visual si hay `workflowError`

## Cómo probar

### 1. Iniciar trámite

1. Login admin o solicitante → crear trámite de política con varias actividades.
2. Abrir **Monitoreo** (`/monitoring`) en otra pestaña o usuario supervisor.

### 2. Ver en monitor sin F5

1. El trámite debe aparecer en la tabla con conteos de tareas.
2. Esperar ~12 s: la hora “Última actualización” debe cambiar sin recargar la página.

### 3. Tomar tarea (otro usuario)

1. Login funcionario → **Mis actividades** → **Tomar** tarea.
2. En monitoreo, tras el polling: sube “En curso” y baja “Pendientes”.
3. Clic **Seguimiento** → ver evento `TAREA_TOMADA` en la línea de tiempo (modal también se actualiza cada 12 s si está abierto).

### 4. Completar formulario

1. Funcionario → **Completar** → enviar formulario.
2. Monitor: nuevas tareas pendientes para el siguiente responsable, eventos `FORMULARIO_ENVIADO` y `ACTIVIDAD_COMPLETADA`.

### 5. Trazabilidad

1. En modal de seguimiento o detalle `/tramites/:id` → sección trazabilidad con colores por tipo de evento.

### 6. Seguimiento cliente

1. Ruta `/tramite-tracking` — mismo comportamiento de polling, orientada a supervisión.

## Listo para F5 (KPI)

- Conteos agregados por trámite (`taskCounts`) reutilizables en dashboard KPI.
- Endpoints granulares (`/tasks`, `/responsibles`, `/timeline`) para gráficos.
- `updatedAt` por trámite para series temporales.
- Lista `/api/monitoring` como fuente de procesos activos vs finalizados.

## Riesgos pendientes

- Polling sobre `findAll()` de trámites: escalar con índices/consultas filtradas en producción.
- Sin notificaciones push: el usuario debe tener la vista abierta.
- Evento `TRAMITE_CREADO` en backend se registra como `PROCESO_CREADO` / `TRAMITE_INICIADO` (etiquetas unificadas en UI).
