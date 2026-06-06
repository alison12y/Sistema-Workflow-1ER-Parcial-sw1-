# Arquitectura del sistema — Ciclo 1

Sistema de gestión de políticas de negocio basado en workflow UML 2.5 para el primer parcial.

Diagramas PlantUML en `/diagrams` (ver índice al final).

---

## 1. Visión general

Arquitectura en **capas** con frontend SPA, API REST, persistencia documental y microservicio de IA opcional.

```
┌─────────────────────────────────────────────────────────────┐
│  Presentación — Angular 19 (TypeScript, PWA)                │
│  Diseñador UML, bandeja, monitor, KPI, analítica IA, admin    │
│  Service worker + IndexedDB (CU27/CU28)                       │
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
| Analítica inteligente | `/intelligent-analytics` | CU24–CU26 |
| PWA / Móvil | layout responsive, barra inferior | CU27 |
| Offline | IndexedDB + `OfflineSyncService` | CU28 |

- Autenticación: token JWT en `localStorage`, interceptor HTTP (no se persiste en IndexedDB).
- Actualización “realtime”: **polling** 11–12 s (bandeja, monitor, colaboración); sin WebSocket en Ciclo 1.
- **PWA:** `manifest.webmanifest`, `ngsw-config.json`; service worker generado en `npm run build` (deshabilitado en `ng serve` / `isDevMode()`).
- **Offline:** stores IndexedDB `pending_sync_queue`, `pending_forms`, `pending_documents`, `cached_activities`; sincronización al reconectar vía `ConnectivityService`.

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
- `IntelligentAnalyticsService` — reportes, riesgos y recomendaciones (FASE 2.3); usa `KpiService`, `TramiteRepository`, FastAPI y `IntelligentAnalyticsFallbackMatcher`.
- `WorkflowCollaborationService` — presencia y revisión (F8).
- `BitacoraService` — auditoría administrativa.
- `OfflineAuditService` — auditoría modo offline (FASE 2.4): `OFFLINE_DATA_STORED`, `OFFLINE_SYNC_COMPLETED`.

### 2.3 Persistencia (MongoDB)

Base de datos: `workflow_db` (ver `database/mongo-init.js`).

**Colecciones oficiales Ciclo 1:**

`business_policies`, `workflow_activities`, `workflow_transitions`, `workflow_collaboration`, `dynamic_forms`, `form_fields`, `tramites`, `form_submissions`, `bitacora`, `users`, `roles`, `departments`

**Deprecated (compatibilidad, no usar en nuevas features):**

`activity_diagrams`, `workflow_diagrams`, `activities`, `transitions`, `process_instances`, `task_instances`

### 2.4 Microservicio IA (FastAPI)

- Puerto: **8000**
- Integración: `ai.service.url` en `application.properties`
- Endpoints diseño/formularios: `/workflow/suggest`, `/assist-form`, `/generate-workflow`, `/validate-diagram`, `/agent/analyze`
- Endpoints analítica (FASE 2.3): `/analytics/report`, `/analytics/risks`, `/analytics/recommendations`, `/analytics/health`
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

### 4.5 Analítica inteligente (FASE 2.3)

```
Usuario → Angular /intelligent-analytics
       → POST /api/intelligent-analytics/report|risks|recommendations
       → IntelligentAnalyticsService
              ├─ OK  → FastAPI /analytics/* (Gemini) → source: AI_SERVICE
              └─ FAIL → IntelligentAnalyticsFallbackMatcher (Java) → LOCAL_FALLBACK
       → BitacoraService (ANALYTICS_*)
```

Fuentes de datos: `Tramite`, `TramiteTask`, `TraceItem`, `KpiService` (cuellos, carga, SLA).

Permiso: `INTELLIGENT_ANALYTICS_VIEW` — Admin, Dueño de proceso, Supervisor (no Atención al cliente).

### 4.6 PWA y modo offline (FASE 2.4)

```
Usuario → Angular (build producción)
       → Service Worker (ngsw) cachea shell estático (HTML, JS, CSS, manifest, iconos)
       → Sin red: ConnectivityService → banner offline
       → Operaciones locales → IndexedDB (pending_sync_queue, …)
       → Al reconectar: OfflineSyncService → API REST + JWT válido
       → POST /api/offline/notify-stored | notify-sync-completed → OfflineAuditService
```

| Componente frontend | Rol |
|---------------------|-----|
| `OfflineDbService` | IndexedDB (4 stores) |
| `OfflineSyncService` | Cola y replay de operaciones |
| `ConnectivityService` | Estado online/offline + contador pendientes |
| `ConnectivityBannerComponent` | Banner En línea / Sin conexión |
| `PwaInstallComponent` | Prompt de instalación CU27 |

**Service worker:** solo activo tras `npm run build` (no en `ng serve`). La API REST (puerto 8080) no se cachea por el SW; el modo offline depende de IndexedDB.

**Integración offline:** `my-activities`, `form-execution`, `tramite-documents`.

Guía: [guia-instalacion-pwa.md](./guia-instalacion-pwa.md)

---

## 5. Seguridad

- **Stateless JWT** tras `POST /api/auth/login`
- Autorización por rol (`ROLE_ADMIN`, …) y permisos (`SystemPermissions`)
- Rutas de workflow, formularios, monitor, KPI y analítica (`/api/intelligent-analytics/**`) segmentadas en `SecurityConfig`
- Analítica: permiso `INTELLIGENT_ANALYTICS_VIEW` o roles Admin / Dueño de proceso / Supervisor

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
| FASE 2.3 | Analítica inteligente CU24–CU26 |
| FASE 2.4 | PWA instalable + offline CU27–CU28 |

**Próximo hito documentado:** F10 — estabilización (pruebas integradas, rendimiento, índices MongoDB).

### Endpoints analítica (referencia)

| Método | Spring Boot | FastAPI |
|--------|-------------|---------|
| GET | `/api/intelligent-analytics/ping` | `/analytics/health` |
| POST | `/api/intelligent-analytics/report` | `/analytics/report` |
| POST | `/api/intelligent-analytics/risks` | `/analytics/risks` |
| POST | `/api/intelligent-analytics/recommendations` | `/analytics/recommendations` |

### Endpoints offline (referencia)

| Método | Spring Boot | Descripción |
|--------|-------------|-------------|
| GET | `/api/offline/ping` | Salud del módulo offline |
| POST | `/api/offline/notify-stored` | Auditoría `OFFLINE_DATA_STORED` |
| POST | `/api/offline/notify-sync-completed` | Auditoría `OFFLINE_SYNC_COMPLETED` |

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

## 9. Fuera de alcance / limitaciones

**Implementado en Ciclo 2:** FASE 2.3 analítica inteligente (CU24–CU26); FASE 2.4 PWA instalable y offline con IndexedDB (CU27–CU28).

**Aún pendiente:** TensorFlow/ML profundo, exportación real PDF/Excel/Word, cliente Flutter nativo, motor predictivo avanzado.

**Limitaciones FASE 2.3:** heurísticas y KPI (SLA 48 h); formato de export solo sugerido en UI; dictado depende del navegador.

**Limitaciones FASE 2.4:** service worker solo en build producción; API REST no cacheada por SW; agente y analítica requieren red; sin resolución automática de conflictos en cola; JWT validado antes de sincronizar (no almacenado en IndexedDB).
