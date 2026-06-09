# Sistema de Gestión de Políticas de Negocio basado en Workflow

Sistema integral para **diseñar**, **ejecutar** y **supervisar** procesos organizacionales con diagramas **UML 2.5** (swimlanes), formularios dinámicos, motor de enrutamiento automático, bandeja del funcionario, monitoreo, KPI, asistencia IA y colaboración básica en el diseñador.

**Ciclo 1 (primer parcial):** implementado en fases **F0–F8**.  
**Ciclo 2 (en progreso):** FASE 2.1 gestión documental (CU17–CU20), FASE 2.2 agente inteligente (CU21–CU23), FASE 2.3 analítica inteligente (CU24–CU26), **FASE 2.4 movilidad PWA + offline (CU27–CU28)**.  
**Ciclo 2 pendiente:** cliente Flutter nativo, TensorFlow/ML profundo, exportación real PDF/Excel, motor predictivo avanzado.

---

## Alcance Ciclo 1 — 16 casos de uso

| CU | Caso de uso | 
|----|-------------|
| CU1 | Gestionar usuarios 
| CU2 | Gestionar roles y permisos 
| CU3 | Gestionar departamentos 
| CU4 | Gestionar políticas de negocio
| CU5 | Diseñar workflow UML 2.5 con swimlanes 
| CU6 | Configurar estructuras de flujo 
| CU7 | Configurar responsables y enrutamiento automático
| CU8 | Diseñar formularios dinámicos por actividad
| CU9 | Iniciar trámite 
| CU10 | Ejecutar actividad
| CU11 | Monitorear y rastrear trámite
| CU12 | Bandeja realtime del funcionario 
| CU13 | KPI y cuellos de botella
| CU14 | Diseñar workflow con IA por texto/voz 
| CU15 | Asistencia IA para formularios 
| CU16 | Diseño colaborativo básico realtime 

Detalle: [docs/casos-uso.md](docs/casos-uso.md) · Demo Ciclo 1: [docs/guia-demo-ciclo1.md](docs/guia-demo-ciclo1.md)

### Ciclo 2 — Casos de uso implementados (FASE 2.1–2.4)

| CU | Caso de uso |
|----|-------------|
| CU17 | Gestionar repositorio documental por trámite |
| CU18 | Gestionar documentos digitales |
| CU19 | Gestionar permisos, auditoría y colaboración documental |
| CU20 | Integrar almacenamiento AWS S3 
| CU21 | Gestionar agente inteligente de atención  |
| CU22 | Asignar automáticamente políticas de negocio  |
| CU23 | Procesar consultas mediante voz, texto y documentos  |
| CU24 | Generar reportes dinámicos por voz o texto |
| CU25 | Analizar riesgos, anomalías y cuellos de botella |
| CU26 | Recomendar rutas y prioridades |
| CU27 | Atender solicitudes desde aplicación móvil/PWA |
| CU28 | Operar en modo offline y sincronizar información  |

Rutas clave: `/intelligent-analytics` · PWA instalable (build producción)  
Demo: [docs/guia-demo-ciclo2.md](docs/guia-demo-ciclo2.md) · PWA: [docs/guia-instalacion-pwa.md](docs/guia-instalacion-pwa.md)

---

## Tecnologías

| Capa | Stack |
|------|--------|
| Frontend | Angular 19, TypeScript, SCSS, **PWA** (`@angular/pwa`, service worker) |
| Backend | Java 17, Spring Boot 3, Spring Security JWT |
| Base de datos | MongoDB |
| IA | Python 3, FastAPI, Google Gemini (+ fallbacks Java/Python) |
| Infraestructura | Docker, Docker Compose |

---

## Modelo oficial (F0)

Fuente de verdad del diseño y ejecución:

```
BusinessPolicy → WorkflowActivity → WorkflowTransition
              → DynamicForm → FormField

Tramite → TramiteTask → FormSubmission
       → TraceItem (trazabilidad)
```

Documentación: [docs/ciclo1-modelo-workflow.md](docs/ciclo1-modelo-workflow.md)  
Reglas: [docs/reglas-negocio.md](docs/reglas-negocio.md)  
Arquitectura: [docs/arquitectura.md](docs/arquitectura.md)

Modelos **legacy** (`ActivityDiagram`, `ProcessInstance`, BPM antiguo) están **deprecated** y no se usan en el frontend Angular del Ciclo 1.

---

## Cómo ejecutar

### Desarrollo local (recomendado para demo)

1. **MongoDB:** `docker compose up -d mongodb`
2. **Backend:** `cd backend && mvn spring-boot:run` → http://localhost:8080
3. **Frontend:** `cd frontend && npm install && npm start` → http://localhost:4200
4. **IA (opcional):** `cd ai-service`, configurar `.env` con `AI_API_KEY`, `uvicorn main:app --port 8000`

### Docker Compose (stack completo)

```bash
docker compose up --build
```

Instrucciones detalladas: [docs/manual-instalacion.md](docs/manual-instalacion.md)

---

## Usuarios demo (seed)

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `sistema.admin` | `Admin.Sistema2024!` | Administrador |
| `carlos.mendoza` | `Carlos.M2024!` | Dueño de proceso |
| `luis.supervisor` | `Luis.S2024!` | Supervisor |
| `ana.rodriguez` | `Ana.R2024!` | Funcionario |

Política de ejemplo: **Solicitud de instalación de medidor**.

> **Analítica inteligente (CU24–CU26):** solo Admin, Dueño de proceso y Supervisor. Atención al cliente **no** tiene `INTELLIGENT_ANALYTICS_VIEW`.

---

## Módulos implementados (F0–F8)

| Módulo | Descripción |
|--------|-------------|
| Administración | Usuarios, roles, permisos, departamentos |
| Políticas | CRUD y activación de políticas de negocio |
| Diseñador UML | Lienzo, swimlanes, validación, colaboración |
| Actividades / Conexiones | CRUD alineado al modelo oficial |
| Formularios | Diseñador por actividad y ejecución dinámica |
| Motor workflow | Enrutamiento secuencial, condicional, iterativo, paralelo |
| Trámites | Inicio y ciclo de vida de instancias |
| Mis tareas | Bandeja con tomar/completar y polling |
| Monitoreo | Timeline, tareas, responsables (polling) |
| KPI | Métricas y cuellos de botella |
| IA diseño | Sugerencias UML por texto/voz con confirmación |
| IA formularios | Autocompletado asistido en ejecución |
| Bitácora | Auditoría de acciones administrativas y diseño |
| Analítica inteligente | Reportes por lenguaje natural, riesgos y recomendaciones (CU24–CU26) |
| PWA / Móvil | App instalable Android, Windows, tablet (CU27) |
| Modo offline | IndexedDB + cola `pending_sync_queue` + sincronización (CU28) |

Guías de prueba por fase: `docs/f1-motor-workflow-pruebas.md` … `docs/f8-colaboracion-pruebas.md`  
Demo Ciclo 2: [docs/guia-demo-ciclo2.md](docs/guia-demo-ciclo2.md) · Instalación PWA: [docs/guia-instalacion-pwa.md](docs/guia-instalacion-pwa.md)

---

## FASE 2.3 — Analítica inteligente (CU24–CU26)

Módulo **Analítica Inteligente** en `/intelligent-analytics`. El usuario describe en texto o voz qué reporte necesita; el sistema devuelve tablas, tarjetas, gráfico simple, riesgos y recomendaciones.

| Capa | Endpoints |
|------|-----------|
| Spring Boot | `GET /api/intelligent-analytics/ping` · `POST .../report` · `POST .../risks` · `POST .../recommendations` |
| FastAPI | `GET /analytics/health` · `POST /analytics/report` · `POST /analytics/risks` · `POST /analytics/recommendations` |

**Fallback:** si `ai-service` no responde, Spring Boot usa `IntelligentAnalyticsFallbackMatcher` (heurísticas Java sobre `Tramite`, `TramiteTask`, `TraceItem`, `KpiService`).

**Permiso:** `INTELLIGENT_ANALYTICS_VIEW` — roles: Administrador, Dueño de proceso, Supervisor.

**Limitaciones actuales:** sin TensorFlow/entrenamiento profundo; formato PDF/Excel/Word solo sugerido (sin export real).

### Probar desde UI

1. Iniciar sesión como `luis.supervisor` / `Luis.S2024!` (o admin / dueño de proceso).
2. Menú → **Analítica Inteligente**.
3. Escribir o dictar: *"Cuáles trámites están demorados"* → **Generar análisis**.
4. Revisar tarjetas, tabla, gráfico, riesgos y recomendaciones.

### Probar con curl

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"luis.supervisor","password":"Luis.S2024!"}' | jq -r .token)

# 2. Reporte dinámico (CU24)
curl -s -X POST http://localhost:8080/api/intelligent-analytics/report \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"Cuál es la política más usada"}' | jq .

# 3. Riesgos (CU25)
curl -s -X POST http://localhost:8080/api/intelligent-analytics/risks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"Analizar riesgos"}' | jq .

# 4. Recomendaciones (CU26)
curl -s -X POST http://localhost:8080/api/intelligent-analytics/recommendations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .

# 5. Ping (público)
curl -s http://localhost:8080/api/intelligent-analytics/ping
```

Si la BD existía antes del despliegue de FASE 2.3, ejecutar `POST /api/dev/migrate-phase1` y volver a iniciar sesión para obtener el permiso en el token.

---

## FASE 2.4 — PWA y modo offline (CU27–CU28)

Angular funciona como **Progressive Web App** instalable en Android, Windows y tablets.

| CU | Descripción |
|----|-------------|
| CU27 | PWA instalable: manifest, service worker, menú móvil, vistas optimizadas (Dashboard, Tareas, Trámites, Documentos, Agente) |
| CU28 | Offline: IndexedDB, cola `pending_sync_queue`, sincronización automática al reconectar |

### Build producción (requerido para PWA)

El **service worker solo se activa con build de producción**, no con `ng serve`:

```bash
cd frontend
npm install --legacy-peer-deps
npm run build
npx http-server dist/frontend/browser -p 4200
```

Abrir `http://localhost:4200` → login → **Instalar app** (Chrome/Edge).

### IndexedDB y cola de sincronización

| Store | Contenido |
|-------|-----------|
| `pending_sync_queue` | Operaciones pendientes (tomar tarea, borrador, completar, subir documento) |
| `pending_forms` | Formularios guardados sin conexión |
| `pending_documents` | Archivos pendientes de subir |
| `cached_activities` | Última bandeja de tareas consultada |

Al volver en línea, `OfflineSyncService` sincroniza y audita `OFFLINE_DATA_STORED` / `OFFLINE_SYNC_COMPLETED`.

**API:** `POST /api/offline/notify-stored` · `POST /api/offline/notify-sync-completed`

### Limitaciones CU27/CU28

- Service worker **solo en build producción** (`npm run build`).
- API REST no se cachea por el SW (origen distinto); offline usa IndexedDB.
- Agente inteligente y analítica requieren conexión.
- JWT no se guarda en IndexedDB; se valida expiración antes de sincronizar.
- Sin resolución automática de conflictos en la cola.

Guía completa: [docs/guia-instalacion-pwa.md](docs/guia-instalacion-pwa.md)

---

## Estructura del repositorio

```
/backend          API REST Spring Boot + motor
/frontend         SPA Angular
/ai-service       Microservicio FastAPI (Gemini)
/database         mongo-init.js
/docs             Documentación académica y guías F1–F9
/diagrams         PlantUML (casos de uso, arquitectura, secuencias)
/docker           Dockerfiles
docker-compose.yml
```

---

## Diagramas PlantUML

Generar imágenes con [PlantUML](https://plantuml.com/) o extensión del IDE:

- `diagrams/casos-uso-ciclo1.puml` — 16 CU
- `diagrams/tramite-actividad-general.puml`
- `diagrams/secuencia-motor-workflow.puml`
- `diagrams/arquitectura-capas.puml`
- `diagrams/arquitectura-ciclo1-workflow.puml`
- `diagrams/colaboracion-basica.puml`
- `diagrams/ia-workflow.puml`
- `diagrams/ia-formularios.puml`

---

## Próximo hito — F10 Estabilización

- Pruebas integradas automatizadas (API + flujos críticos)
- Índices MongoDB y optimización de consultas de bandeja/monitor
- Endurecimiento de validación en mutaciones con revisión colaborativa
- Limpieza de APIs legacy no usadas
- Preparación de entrega formal (empaquetado, variables de entorno producción)

---

## Licencia / curso

Proyecto académico — Primer parcial Ingeniería de Software / Workflow.
