# Sistema de Gestión de Políticas de Negocio basado en Workflow

Sistema integral para **diseñar**, **ejecutar** y **supervisar** procesos organizacionales con diagramas **UML 2.5** (swimlanes), formularios dinámicos, motor de enrutamiento automático, bandeja del funcionario, monitoreo, KPI, asistencia IA y colaboración básica en el diseñador.

**Ciclo 1 (primer parcial):** implementado en fases **F0–F8**.  
**Ciclo 2 (fuera de alcance actual):** S3, documental avanzado, Flutter, offline, reportes dinámicos IA, motor predictivo, agente cliente.

---

## Alcance Ciclo 1 — 16 casos de uso

| CU | Caso de uso | Fase |
|----|-------------|------|
| CU1 | Gestionar usuarios 
| CU2 | Gestionar roles y permisos 
| CU3 | Gestionar departamentos 
| CU4 | Gestionar políticas de negocio
| CU5 | Diseñar workflow UML 2.5 con swimlanes 
| CU6 | Configurar estructuras de flujo 
| CU7 | Configurar responsables y enrutamiento automático
| CU8 | Diseñar formularios dinámicos por actividad
| CU9 | Iniciar trámite | F1 |
| CU10 | Ejecutar actividad | F1–F2 |
| CU11 | Monitorear y rastrear trámite | F3 |
| CU12 | Bandeja realtime del funcionario | F2 |
| CU13 | KPI y cuellos de botella | F5 |
| CU14 | Diseñar workflow con IA por texto/voz | F6 |
| CU15 | Asistencia IA para formularios | F7 |
| CU16 | Diseño colaborativo básico realtime | F8 |

Detalle: [docs/casos-uso.md](docs/casos-uso.md) · Demo: [docs/guia-demo-ciclo1.md](docs/guia-demo-ciclo1.md)

---

## Tecnologías

| Capa | Stack |
|------|--------|
| Frontend | Angular 19, TypeScript, SCSS |
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
| `ana.rodriguez` | `Ana.R2024!` | Funcionario |

Política de ejemplo: **Solicitud de instalación de medidor**.

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

Guías de prueba por fase: `docs/f1-motor-workflow-pruebas.md` … `docs/f8-colaboracion-pruebas.md`

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
