# Arquitectura del sistema — Ciclo 1

Sistema de gestión de políticas de negocio basado en workflow UML 2.5 para el primer parcial.

Diagramas PlantUML en `/diagrams` (ver índice al final).

---

## 1. Visión general

Arquitectura en **capas** con frontend SPA, API REST, persistencia documental y microservicio de IA opcional.

```
┌─────────────────────────────────────────────────────────────┐
│  Presentación — Angular 19 (TypeScript)                     │
│  Diseñador UML, bandeja, monitor, KPI, admin                │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTPS / REST + JWT
┌───────────────────────────▼─────────────────────────────────┐
│  Aplicación — Spring Boot 3 (Java 17)                       │
│  Controllers → Services → Repositories                       │
│  WorkflowRoutingService, MyActivities, Monitoring, KPI, AI  │
└───────────────┬─────────────────────────┬───────────────────┘
                │                         │ HTTP
┌───────────────▼─────────────┐  ┌────────▼──────────────────┐
│  Datos — MongoDB            │  │  IA — FastAPI (ai-service) │
│  Colecciones Ciclo 1        │  │  Gemini + fallbacks         │
└─────────────────────────────┘  └─────────────────────────────┘
```

Diagrama: `diagrams/arquitectura-capas.puml`

---

## 2. Componentes por capa

### 2.1 Frontend (Angular)

| Módulo | Ruta / área | Responsabilidad |
|--------|-------------|-----------------|
| Admin | `/users`, `/roles`, `/departments` | CU1–CU3 |
| Políticas | `/policies`, `/policy-detail` | CU4 |
| Diseño UML | `/workflow-designer/:id` | CU5, CU6, CU14, CU16 |
| Formularios | `/activities/:id/form` | CU8 |
| Trámites | `/tramites` | CU9 |
| Bandeja | `/mis-actividades` | CU10, CU12, CU15 |
| Monitor | `/monitoring`, `/seguimiento` | CU11 |
| KPI | `/kpis` | CU13 |

- Autenticación: token JWT en `localStorage`, interceptor HTTP.
- Actualización “realtime”: **polling** 11–12 s (bandeja, monitor, colaboración); sin WebSocket en Ciclo 1.

### 2.2 Backend (Spring Boot)

| Paquete | Rol |
|---------|-----|
| `controller` | REST `/api/*` |
| `service` | Lógica de negocio y orquestación |
| `repository` | Spring Data MongoDB |
| `security` | JWT, `SecurityConfig`, permisos |
| `workflow.cycle1` | Constantes y tipos oficiales del modelo A |
| `config` | Seed (`DatabaseSeeder`), migraciones por fase |

Servicios clave:

- `WorkflowDesignerService` — agregado del diagrama para UI.
- `WorkflowActivityService` / `WorkflowTransitionService` — CRUD UML.
- `WorkflowRoutingService` — motor de enrutamiento (F1).
- `TramiteService` — ciclo de vida del trámite.
- `MyActivitiesService` — bandeja (F2).
- `MonitoringService` — supervisión (F3).
- `KpiService` — indicadores (F5).
- `AiService` — puente a `ai-service` + fallback Java.
- `WorkflowCollaborationService` — presencia y revisión (F8).
- `BitacoraService` — auditoría administrativa.

### 2.3 Persistencia (MongoDB)

Base de datos: `workflow_db` (ver `database/mongo-init.js`).

**Colecciones oficiales Ciclo 1:**

`business_policies`, `workflow_activities`, `workflow_transitions`, `workflow_collaboration`, `dynamic_forms`, `form_fields`, `tramites`, `form_submissions`, `bitacora`, `users`, `roles`, `departments`

**Deprecated (compatibilidad, no usar en nuevas features):**

`activity_diagrams`, `workflow_diagrams`, `activities`, `transitions`, `process_instances`, `task_instances`

### 2.4 Microservicio IA (FastAPI)

- Puerto: **8000**
- Integración: `ai.service.url` en `application.properties`
- Endpoints: `/workflow/suggest`, `/assist-form`, `/generate-workflow`, `/validate-diagram`
- Proveedor: **Google Gemini** vía `AI_API_KEY` en `ai-service/.env`
- Sin clave o con error de cuota → **fallback** determinístico (Python/Java)

---

## 3. Modelo oficial Ciclo 1

Flujo de entidades (fuente de verdad):

```
BusinessPolicy
    ├── WorkflowActivity  (START, TASK, DECISION, END + swimlane/responsable)
    ├── WorkflowTransition (tipos de flujo)
    └── DynamicForm → FormField (por actividad)

Tramite (instancia)
    ├── TramiteTask (bandeja)
    ├── TraceItem (trazabilidad runtime)
    └── FormSubmission → ResponseItem (al completar)
```

Diagrama detallado: `diagrams/arquitectura-ciclo1-workflow.puml`  
Documento: [ciclo1-modelo-workflow.md](./ciclo1-modelo-workflow.md)

### Legacy deprecated

| Modelo | Estado |
|--------|--------|
| B — `ActivityDiagram`, nodos/edges JSON | API `@Deprecated`; no es fuente de verdad |
| C — `ProcessInstance`, `TaskInstance`, `WorkflowDiagram` BPM | Sin uso en Angular; APIs legacy |

El motor F1 opera sobre **WorkflowActivity + WorkflowTransition**.

---

## 4. Flujos arquitectónicos principales

### 4.1 Diseño → ejecución

1. Diseñador crea política y diagrama UML.
2. Configura formularios por actividad.
3. Activa política.
4. Usuario inicia `Tramite`.
5. `WorkflowRoutingService` crea tareas y avanza según transiciones.

Diagrama: `diagrams/secuencia-motor-workflow.puml`

### 4.2 Trámite (actividad general)

Diagrama: `diagrams/tramite-actividad-general.puml`

### 4.3 Colaboración en diseño

Polling + revisión por política; sin CRDT.

Diagrama: `diagrams/colaboracion-basica.puml`

### 4.4 IA

Diagramas: `diagrams/ia-workflow.puml`, `diagrams/ia-formularios.puml`

---

## 5. Seguridad

- **Stateless JWT** tras `POST /api/auth/login`
- Autorización por rol (`ROLE_ADMIN`, …) y permisos (`SystemPermissions`)
- Rutas de workflow, formularios, monitor y KPI segmentadas en `SecurityConfig`

---

## 6. Infraestructura

| Herramienta | Uso |
|-------------|-----|
| Docker Compose | MongoDB, backend, frontend, ai-service |
| Maven | Build backend |
| npm / Angular CLI | Build frontend |

Ver [manual-instalacion.md](./manual-instalacion.md).

---

## 7. Fases implementadas (F0–F8)

| Fase | Entregable |
|------|------------|
| F0 | Modelo oficial A, consolidación legacy |
| F1 | Motor `WorkflowRoutingService` |
| F2 | Bandeja `MyActivities` + polling |
| F3 | Monitor y seguimiento |
| F4 | Formularios dinámicos |
| F5 | KPI y cuellos de botella |
| F6 | IA diseño workflow |
| F7 | IA formularios |
| F8 | Colaboración básica diseñador |

**Próximo hito documentado:** F10 — estabilización (pruebas integradas, rendimiento, índices MongoDB).

---

## 8. Índice de diagramas PlantUML

| Archivo | Contenido |
|---------|-----------|
| `casos-uso-ciclo1.puml` | 16 casos de uso |
| `tramite-actividad-general.puml` | Flujo de vida del trámite |
| `secuencia-motor-workflow.puml` | Secuencia motor + completar tarea |
| `arquitectura-capas.puml` | Capas Angular / Spring / Mongo / IA |
| `arquitectura-ciclo1-workflow.puml` | Modelo de datos oficial vs legacy |
| `colaboracion-basica.puml` | Presencia y conflicto F8 |
| `ia-workflow.puml` | CU14 sugerencia de diagrama |
| `ia-formularios.puml` | CU15 asistencia de formulario |
| `casos-uso.puml` | *(histórico — ver casos-uso-ciclo1)* |

---

## 9. Fuera de alcance Ciclo 1

No forman parte de esta arquitectura entregada: almacenamiento S3, gestión documental avanzada, cliente Flutter, modo offline, reportes dinámicos con IA, motor predictivo, agente inteligente en cliente (Ciclo 2).
