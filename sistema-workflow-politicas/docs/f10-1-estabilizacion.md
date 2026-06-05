# F10.1 — Estabilización y corrección de bugs críticos (Ciclo 1)

Solo correcciones y endurecimiento; **sin nuevas funcionalidades** ni Ciclo 2.

---

## 1. Bugs encontrados (auditoría)

| # | Área | Severidad | Descripción |
|---|------|-----------|-------------|
| B1 | F1 Motor | Crítica | Tras completar tarea, `TASK → DECISION → PARALLEL_SPLIT` activaba una sola rama |
| B2 | F1 Motor | Crítica | JOIN con votación parcial elegía destino incorrecto sin `PARALLEL_JOIN` completo |
| B3 | F1 Motor | Alta | Tras paralelo sin `joinId` en trámite, se enrutaba desde última rama saltando unión |
| B4 | F1 Motor | Alta | `ACTIVATE_TASKS` sin tareas TASK creadas dejaba trámite colgado |
| B5 | F1 Motor | Alta | `ITERATIVE` sin tope generaba tareas duplicadas indefinidamente |
| B6 | F1 Motor | Media | Múltiples nodos START no detectados |
| B7 | F1 Motor | Media | Actividades TASK sin responsable aceptadas en runtime |
| B8 | F1 Motor | Media | Trámite `INICIADO` con tarea ya `EN_CURSO` (inconsistencia cabecera) |
| B9 | F8 Colaboración | Media | Conflicto falso si la revisión subía por el mismo usuario |
| B10 | F8 Colaboración | Media | Cambio de política en ruta no reiniciaba sesión/polling |
| B11 | F8 Colaboración | Baja | Sesiones zombie (~25 s TTL) |
| B12 | API | Media | `IllegalStateException` de workflow devolvía 500 genérico |
| B13 | F2/F10 | Verificado OK | Permisos tomar/completar en `MyActivitiesService` |
| B14 | F4 | Verificado OK | Validación obligatorios y claves duplicadas en `FormSubmissionService` |
| B15 | F3/F2 polling | Verificado OK | `ngOnDestroy` limpia `setInterval` en bandeja/monitor/KPI/detalle |

---

## 2. Bugs corregidos

| ID | Corrección |
|----|------------|
| B1 | `routeThroughTransitionTarget` + expansión anidada en `buildParallelActivation` |
| B2 | `resolveJoinActivityAfterParallelSplit` solo retorna JOIN con votos de **todas** las ramas |
| B3 | Al cerrar grupo paralelo, reintenta resolver `joinId` desde IDs de ramas del grupo |
| B4 | Tras `applyRoutingActivation`, valida que exista al menos una tarea abierta nueva |
| B5 | Límite `MAX_TASK_VISITS_PER_ACTIVITY = 20` por actividad en trámite |
| B6 | `requireStartActivity` rechaza más de un START |
| B7 | `hasResponsibleConfigured` antes de crear `TramiteTask` |
| B8 | Estado `EN_PROCESO` si hay tarea activa al crear trámite |
| B9 | Frontend ignora `staleForClient` si `lastModifiedByUsername === currentUsername` |
| B10 | `resetCollaborationSession` al cambiar `policyId` y en `ngOnDestroy` |
| B11 | TTL presencia 20 s (antes 25) |
| B12 | `GlobalExceptionHandler` para `IllegalStateException` workflow → HTTP 400 mensaje claro |

---

## 3. Archivos modificados

**Backend**

- `service/WorkflowRoutingService.java`
- `service/TramiteService.java`
- `service/WorkflowCollaborationService.java`
- `exception/GlobalExceptionHandler.java`

**Frontend**

- `pages/workflow-designer/workflow-designer.component.ts`

**Documentación**

- `docs/f10-1-estabilizacion.md` (este archivo)

---

## 4. Riesgos pendientes (no bloquean demo)

| Riesgo | Nota |
|--------|------|
| Paralelo muy anidado | Decisiones con splits encadenados pueden requerir diagrama simple en demo |
| Último guardado en CRUD paralelo | Editar actividades fuera del diseñador sin revisión colaborativa |
| Bandeja O(n) trámites | Rendimiento con muchos trámites (F10.2 índices) |
| `tryCompleteOrError` sin END | Políticas sin nodo END aún pueden finalizar por agotamiento (diseño legacy) |
| Admin `advance` manual | Sigue tomando primera tarea `EN_CURSO` arbitraria |
| WAIT_PARALLEL en routing | Rama de código sin uso; espera real vía `ESPERA_PARALELO` en trámite |

---

## 5. Checklist demo final

| # | Verificación | OK |
|---|--------------|-----|
| 1 | Login `sistema.admin` / `ana.rodriguez` | ☐ |
| 2 | Política medidor ACTIVE, un START | ☐ |
| 3 | Diseñador: validar flujo sin error crítico | ☐ |
| 4 | Colaboración 2 navegadores: conflicto real, no falso tras guardar propio | ☐ |
| 5 | Crear trámite → estado coherente | ☐ |
| 6 | Funcionario toma y completa → avance automático | ☐ |
| 7 | Monitor actualiza ~12 s sin F5 | ☐ |
| 8 | KPI muestra datos | ☐ |
| 9 | IA diseño/formulario con confirmación | ☐ |
| 10 | Error workflow legible (sin stacktrace en UI) | ☐ |

Guía completa: [guia-demo-ciclo1.md](./guia-demo-ciclo1.md)

---

## 6. Recomendaciones antes de presentar

1. Reiniciar backend tras desplegar F10.1 para cargar clases Java.
2. Usar política seed **Solicitud de instalación de medidor** o flujo probado en F1.
3. En demo de paralelo, usar diagrama con `PARALLEL_JOIN` explícito hacia actividad de unión.
4. Evitar dos diseñadores ignorando aviso de conflicto (último guardado gana en CRUD).
5. Tener `ai-service` arriba o explicar fallback local.
6. Probar una vez: crear trámite → completar 1ª tarea → ver 2ª en bandeja del responsable correcto.
7. No prometer: S3, Flutter, offline, predictivo, reportes IA dinámicos (Ciclo 2).

---

## 7. Próximo F10.2 (sugerido, fuera de F10.1)

- Índices MongoDB en `tramites.tasks`, `workflow_activities.policyId`
- Tests automatizados motor secuencial + condicional + paralelo
- Validación diseño: paralelo incompleto, múltiples START en `validateFlow`
