# F5 — KPI y cuellos de botella — Guía

## Fuentes de datos (modelo oficial)

- `Tramite` — estado, fechas, `workflowError`
- `TramiteTask` — PENDIENTE / EN_CURSO / COMPLETADA, `takenAt`, `completedAt`, `takenBy`
- `WorkflowActivity` — `estimatedTimeHours` (SLA para demoras)
- `User` / `Department` — carga por funcionario y departamento

## Fórmulas principales

| Indicador | Cálculo |
|-----------|---------|
| Tiempo promedio trámite | `updatedAt` o ahora − `createdAt` (promedio de trámites filtrados) |
| Tiempo promedio actividad | Promedio de (`completedAt` − `takenAt` o `startedAt`) en tareas COMPLETADA |
| Espera activa | Ahora − inicio de tarea para PENDIENTE / EN_CURSO |
| Tarea demorada | Espera > `estimatedTimeHours` de WorkflowActivity, o > 48 h si no hay SLA |
| Cuello de botella | Nivel Alto/Medio/Bajo por: tareas detenidas, tiempo de espera, cantidad demoradas |
| Carga funcionario | Conteo de tareas por `takenBy` / responsable |
| Carga departamento | Departamento del usuario o `WorkflowActivity` tipo DEPARTMENT |

## API

- `GET /api/kpis/dashboard?policyId=&status=&from=&to=`
- `GET /api/kpis/summary?...` (compatibilidad)
- `GET /api/kpis/bottlenecks?...` (compatibilidad)

Estados filtro: `ACTIVO`, `EN_PROCESO`, `INICIADO`, `FINALIZADO`, `CANCELADO`, `ERROR`.

## Cómo probar

1. Crear 2–3 trámites de la misma política con varias actividades.
2. Completar algunas tareas (dejar `completedAt` / `takenBy` poblados).
3. Dejar otras en PENDIENTE o EN_CURSO sin completar.
4. Abrir **KPIs** (`/kpi`) — ver tarjetas y tablas con datos reales.
5. Esperar 12 s sin F5 — indicadores deben refrescarse.
6. Filtrar por política y verificar que los totales cambian.
7. Verificar **Carga por funcionario** (usuario que tomó/completó tareas).
8. Verificar **Carga por departamento**.
9. Verificar **Cuellos de botella** con actividad de mayor demora.

## Listo para F6 (IA diagrama)

- Métricas por `workflowActivityId` y política exportables.
- Detección de demoras y SLA por actividad.
- Sin datos simulados cuando existen trámites reales.

## Riesgos

- `findAll()` en trámites/usuarios — optimizar en producción.
- Responsable como texto libre puede no coincidir 100 % con usuarios del sistema.
- Sin PDF/Excel ni IA predictiva (fuera de Ciclo 1).
