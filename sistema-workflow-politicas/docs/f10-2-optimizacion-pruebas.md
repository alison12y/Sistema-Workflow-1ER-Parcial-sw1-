# F10.2 — Optimización, índices y pruebas (Ciclo 1)

Entrega de rendimiento, validación de diseño, pruebas automatizadas y checklist pre-demo.  
**Sin nuevas funcionalidades**, sin Ciclo 2 y sin cambiar el comportamiento estabilizado en F10.1.

---

## 1. Índices MongoDB

### Colección `tramites`
| Índice | Uso |
|--------|-----|
| `policyId` | Bandeja, KPI y filtros por política |
| `status` | Monitor y KPI por estado |
| `createdAt` (desc) | Orden cronológico |
| `policyId + status` | Consultas compuestas |
| `tasks.status` | Tareas abiertas/cerradas |
| `tasks.workflowActivityId` | Resolución de actividad |
| `tasks.responsible` | Asignación en bandeja |
| `tasks.takenBy` | Tarea tomada por usuario |

> El modelo runtime usa `responsible` / `takenBy`, no `assignedUserId` / `assignedRoleId` / `assignedDepartmentId`. Esos nombres del requerimiento se mapean al diseño UML (`responsibleType` + `responsibleId` en actividad, `responsible` en tarea).

### Colección `workflow_activities` / `workflow_transitions`
- `policyId`
- `policyId + active` (diseño activo)

### Colección `form_submissions`
- `tramiteId`
- `workflowActivityId`
- Compuesto `tramiteId + workflowActivityId`

### Colección `bitacora`
- `createdAt` (desc)
- `module + createdAt`

### Aplicación
- **Init Docker:** `database/mongo-init.js` (`createIndex` idempotente).
- **Arranque Spring:** `MongoCycle1IndexInitializer` (no borra datos existentes).

---

## 2. Validación de diseño (`validateFlow`)

Implementación: `WorkflowFlowValidationHelper` (invocado desde `WorkflowTransitionService.validateFlow`).

| Regla | Tipo |
|-------|------|
| Sin actividades | Error |
| Sin START / múltiples START | Error |
| Sin END | Error |
| TASK sin responsable | Error |
| Transición hacia nodo inexistente | Error |
| Condicional sin `conditionLabel` | Error |
| PARALLEL_SPLIT sin JOIN que una ramas | Error |
| Split sin ramas de destino | Error |
| Actividades aisladas (sin entrada ni salida) | Advertencia |
| Ciclo iterativo sin camino a END | Advertencia |
| JOIN con origen inválido | Advertencia |

**Comportamiento F10.2:** `valid = errors.isEmpty()`. Las **advertencias no invalidan** el flujo (solo informan). El diseñador muestra mensajes traducidos vía `friendlyValidationMessage`.

---

## 3. Pruebas backend (`mvn test`)

Ubicación: `backend/src/test/java/com/workflow/politicas/`.

| Clase | Qué cubre |
|-------|-----------|
| `WorkflowRoutingServiceTest` | Flujo secuencial, avance tras completar, múltiples START, paralelo split→join |
| `WorkflowFlowValidationHelperTest` | Múltiples START, TASK sin responsable, condicional sin condición, paralelo sin JOIN |
| `TramiteServiceIterationLimitTest` | Límite 20 iteraciones por actividad |
| `MyActivitiesServiceSecurityTest` | Tomar/completar tarea ajena → 403 lógico (`IllegalArgumentException` permisos) |
| `FormSubmissionRequiredValidationTest` | Campo obligatorio vacío al completar |
| `AiServiceFallbackTest` | Parser local (`fallbackUsed=true`) cuando ai-service no responde |

**Ejecución:** desde `backend/`: `mvn test`  
En JDK 21+ el `pom.xml` incluye `-Dnet.bytebuddy.experimental=true` para Mockito.

---

## 4. Pruebas API mínimas (manual / curl)

Obtener token:

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"ana.rodriguez\",\"password\":\"Ana.R2024!\"}"
```

Usar `Authorization: Bearer <token>` en las llamadas siguientes.

| Endpoint | Método | Notas |
|----------|--------|-------|
| `/api/tramites` | POST | Crear trámite (política activa) |
| `/api/my-activities/{tramiteId}/take?taskOrder=1` | POST | Tomar tarea pendiente |
| `/api/my-activities/{tramiteId}/complete` | POST | Completar con `responses`; obligatorios validados |
| `/api/monitoring` | GET | Lista trámites no cancelados |
| `/api/kpis/dashboard` | GET | KPI con filtros opcionales |
| `/api/ai/workflow/suggest` | POST | Requiere `WORKFLOW_DESIGN`; con ai-service caído → fallback local |
| `/api/ai/assist-form` | POST | Requiere `TASKS_EXECUTE`; fallback local si no hay IA |

Ejemplo crear trámite (ajustar `policyId`):

```bash
curl -s -X POST http://localhost:8080/api/tramites \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"policyId\":\"<POLICY_ID>\",\"initiatorUsername\":\"ana.rodriguez\"}"
```

---

## 5. Rendimiento — `findAll` en trámites

| Módulo | Antes | F10.2 |
|--------|-------|-------|
| Bandeja (`MyActivitiesService`) | `findAll()` | `findById`, `findByPolicyIdAndStatusIn`, `findByStatusNot` |
| Monitor (`MonitoringService`) | `findAll()` | `findByStatusNot("CANCELADO")` |
| KPI (`KpiService`) | `findAll()` trámites/actividades | Carga acotada por filtros + actividades por `policyId` de trámites cargados |

### Riesgos pendientes
- **KPI:** sigue usando `userRepository.findAll()` y `departmentRepository.findAll()` para etiquetas (volumen bajo en demo).
- **TramiteService.generateCode:** aún consulta todos los códigos para secuencia (aceptable en parcial).
- **Dashboard admin legacy:** `DashboardService` aún usa `findAll()` (no es pantalla principal de demo Ciclo 1).

Los índices listados mitigan el crecimiento de `tramites` en bandeja/monitor/KPI.

---

## 6. Seguridad (verificado en código)

| Regla | Implementación |
|-------|----------------|
| Solo diseñador modifica workflow | `WORKFLOW_MUTATION_AUTHORITIES` en actividades, transiciones y diseñador |
| Solo asignado toma/completa tarea | `MyActivitiesService` + mensajes de permiso |
| Admin depura | Rol `ADMIN` / `isAdmin()` en bandeja |
| IA workflow | `POST /api/ai/workflow/suggest` → `WORKFLOW_DESIGN` |
| IA formulario | `POST /api/ai/assist-form` y bitácora IA → `TASKS_EXECUTE` |

Autorización fina en bandeja/monitor depende del JWT del usuario autenticado; no sustituye la validación de asignación en servicio.

---

## 7. UX mínima (sin rediseño)

- Bandeja, monitor y KPI: estados `loading`, `error` y vacío ya presentes.
- Diseñador: botón **Validar flujo** con `Validando...` y modal de errores/advertencias legibles.
- Mensajes de permiso alineados con backend (`permisos`).

---

## 8. Archivos modificados (resumen)

| Área | Archivos |
|------|----------|
| Índices | `database/mongo-init.js`, `MongoCycle1IndexInitializer.java` |
| Repositorio | `TramiteRepository.java` |
| Validación | `WorkflowFlowValidationHelper.java`, `WorkflowTransitionService.java` |
| Rendimiento | `MyActivitiesService.java`, `MonitoringService.java`, `KpiService.java` |
| Tests | `backend/src/test/.../*Test.java`, `WorkflowTestFixtures.java` |
| Build | `backend/pom.xml` (surefire Byte Buddy) |
| Docs | Este archivo, `docs/guia-demo-ciclo1.md` |

---

## 9. Checklist final antes de demo

- [ ] `mvn test` en backend — 12 tests OK
- [ ] MongoDB arriba; índices aplicados (log: *Índices Ciclo 1 F10.2 verificados*)
- [ ] Backend `:8080`, frontend `:4200`
- [ ] Validar flujo en política demo — 0 errores
- [ ] `ana.rodriguez`: bandeja con tarea; tomar → completar (formulario obligatorio)
- [ ] Monitor muestra trámite sin F5 (polling)
- [ ] KPI con datos o mensaje vacío claro
- [ ] (Opcional) Detener ai-service y probar sugerencia IA → mensaje fallback
- [ ] Dos navegadores solo si se demo CU16 colaboración

---

## Referencias

- Estabilización previa: [f10-1-estabilizacion.md](./f10-1-estabilizacion.md)
- Demo: [guia-demo-ciclo1.md](./guia-demo-ciclo1.md)
