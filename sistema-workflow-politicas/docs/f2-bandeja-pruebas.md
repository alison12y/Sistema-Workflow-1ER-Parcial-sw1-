# F2 — Bandeja del funcionario — Guía de pruebas

## Flujo de bandeja

1. **Asignación** — Al crear un trámite o al enrutar (F1), `WorkflowRoutingService` activa actividades y crea `TramiteTask`:
   - La **primera** actividad única del trámite arranca en `EN_CURSO` (auto-tomada).
   - Las siguientes quedan en `PENDIENTE` hasta que el responsable las **tome**.
2. **Resolución de responsable** — `MyActivitiesService` filtra por `WorkflowActivity.responsibleType`:
   - `USER` → username / id del usuario
   - `ROLE` → roles del usuario autenticado
   - `DEPARTMENT` → departamento del usuario
3. **Tomar tarea** — `PUT /api/my-activities/{tramiteId}/tasks/{taskOrder}/take` → `PENDIENTE` → `EN_CURSO`, registra `takenAt` / `takenBy` y traza `TAREA_TOMADA`.
4. **Completar** — Formulario dinámico → `PUT /api/my-activities/{id}/complete` → F1 enruta → tarea `COMPLETADA`, nuevas tareas `PENDIENTE` para el siguiente responsable.
5. **Polling** — La UI recarga la bandeja cada **12 s** sin F5.

## API

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/my-activities?status=&policyId=&tramiteId=&tramiteCode=&priority=` | Bandeja filtrada |
| GET | `/api/my-activities/{id}?taskOrder=` | Detalle de una tarea |
| PUT | `/api/my-activities/{id}/tasks/{taskOrder}/take` | Tomar tarea |
| PUT | `/api/my-activities/{id}/complete` | Completar + avance F1 |

## Cómo probar

### Preparación

```bash
# Backend
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm start
```

Usuarios demo: `ana.rodriguez` (funcionario), `sistema.admin` (admin). Política seed: *Solicitud de instalación de medidor*.

### 1. Tarea pendiente

1. Iniciar sesión como funcionario del rol/departamento de la **segunda** actividad del flujo (o crear trámite y completar la primera como otro usuario).
2. Ir a **Mis tareas**.
3. Verificar sección **Pendientes** con estado `Pendiente` y botón **Tomar**.

### 2. Tomar tarea

1. Clic en **Tomar**.
2. La tarea pasa a **En proceso** (`EN_CURSO`).
3. En detalle del trámite → pestaña trazabilidad: evento `TAREA_TOMADA`.
4. Bitácora: acción `TOMAR_TAREA`.

### 3. Tarea en proceso

1. Tarea visible en **En proceso**.
2. Botón **Completar** (no **Tomar**).
3. Si se abre el formulario sin tomar (URL manual con tarea `PENDIENTE`), debe mostrar error.

### 4. Completar tarea

1. **Completar** → formulario de la actividad.
2. Enviar campos obligatorios.
3. El trámite avanza automáticamente (F1); la tarea aparece en **Finalizadas**.
4. Trazas: `TAREA_COMPLETADA`, `ACTIVIDAD_COMPLETADA`, `TAREA_ASIGNADA` en nuevas tareas.

### 5. Siguiente responsable

1. Cerrar sesión e iniciar como el usuario/rol del siguiente paso (o otro funcionario del departamento).
2. En su bandeja debe aparecer la nueva tarea en **Pendientes** (polling ~12 s o **Actualizar**).

### 6. Filtros y polling

1. Filtrar por política, código de trámite, prioridad o estado.
2. Esperar 12 s sin F5: la hora “Última actualización” debe cambiar.

## Trazabilidad registrada

| Evento | Cuándo |
|--------|--------|
| `TAREA_ASIGNADA` | Nueva tarea creada por el motor |
| `TAREA_TOMADA` | Funcionario toma tarea pendiente |
| `TAREA_COMPLETADA` | Tarea marcada completada |
| `ACTIVIDAD_COMPLETADA` | Completitud de actividad (formulario) |

## Riesgos pendientes

- **Rendimiento**: `listInbox` recorre todos los trámites en MongoDB; conviene índice/consulta por responsable en F3+.
- **Paralelo**: varias tareas `PENDIENTE` del mismo grupo pueden requerir coordinación manual.
- **Realtime**: polling 12 s, no WebSocket (F3 monitor).
- **Observadas**: categoría por nombre que contiene “observ” o `workflowError` en trámite activo.

## Listo para F3 (Monitor realtime)

- Misma API de bandeja con filtros reutilizable para vista supervisor.
- Polling ya implementado en cliente; F3 puede exponer endpoint agregado por departamento/estado.
- Trazas y bitácora alimentan timeline del monitor.
