# Casos de uso — Ciclo 1 (primer parcial)

Sistema de gestión de políticas de negocio basado en workflow UML 2.5.  
**Actores principales:** Administrador del sistema, Dueño de proceso / Diseñador, Funcionario, Supervisor.

| ID | Caso de uso | Fase | Actor principal |
|----|-------------|------|-----------------|
| CU1 | Gestionar usuarios | F0 | Administrador |
| CU2 | Gestionar roles y permisos | F0 | Administrador |
| CU3 | Gestionar departamentos | F0 | Administrador |
| CU4 | Gestionar políticas de negocio | F0 | Dueño de proceso |
| CU5 | Diseñar workflow UML 2.5 con swimlanes | F0–F1 | Diseñador |
| CU6 | Configurar estructuras de flujo | F1 | Diseñador |
| CU7 | Configurar responsables y enrutamiento automático | F1–F2 | Diseñador |
| CU8 | Diseñar formularios dinámicos por actividad | F4 | Diseñador |
| CU9 | Iniciar trámite | F1 | Solicitante / Admin |
| CU10 | Ejecutar actividad | F1–F2 | Funcionario |
| CU11 | Monitorear y rastrear trámite | F3 | Supervisor |
| CU12 | Bandeja realtime del funcionario | F2 | Funcionario |
| CU13 | KPI y cuellos de botella | F5 | Supervisor |
| CU14 | Diseñar workflow con IA por texto/voz | F6 | Diseñador |
| CU15 | Asistencia IA para formularios | F7 | Funcionario |
| CU16 | Diseño colaborativo básico realtime | F8 | Diseñador |

Diagrama: `diagrams/casos-uso-ciclo1.puml`

---

## CU1 — Gestionar usuarios

| Campo | Descripción |
|-------|-------------|
| **Actor** | Administrador del sistema |
| **Propósito** | Crear, consultar, actualizar y desactivar cuentas de usuario vinculadas a departamento y roles |
| **Precondición** | Usuario autenticado con permiso `USERS_MANAGE` o rol Administrador |
| **Flujo principal** | 1. Acceder a **Usuarios**. 2. Listar usuarios. 3. Crear/editar datos (nombre, email, departamento, roles, contraseña). 4. Guardar. 5. Sistema persiste en MongoDB y registra bitácora |
| **Postcondición** | Usuario disponible para autenticación JWT con permisos del rol asignado |
| **Excepciones** | E1: Usuario duplicado → mensaje de validación. E2: Sin permiso → HTTP 403. E3: Usuario inactivo → login rechazado |

---

## CU2 — Gestionar roles y permisos

| Campo | Descripción |
|-------|-------------|
| **Actor** | Administrador del sistema |
| **Propósito** | Definir roles y el conjunto de permisos granulares del sistema |
| **Precondición** | Permiso `ROLES_MANAGE` |
| **Flujo principal** | 1. **Roles** → listar. 2. Crear/editar rol con nombre y permisos (`WORKFLOW_DESIGN`, `TASKS_EXECUTE`, etc.). 3. Guardar. 4. Los usuarios con ese rol heredan permisos en el token |
| **Postcondición** | Rol actualizado; nuevas sesiones reflejan permisos |
| **Excepciones** | E1: Rol en uso → no eliminar sin reasignar usuarios. E2: Sin permiso → 403 |

---

## CU3 — Gestionar departamentos

| Campo | Descripción |
|-------|-------------|
| **Actor** | Administrador del sistema |
| **Propósito** | Mantener la estructura organizacional usada en swimlanes y asignación `DEPARTMENT` |
| **Precondición** | Permiso `DEPARTMENTS_MANAGE` |
| **Flujo principal** | 1. **Departamentos** → CRUD. 2. Asociar nombre y descripción. 3. Usar departamento al crear usuarios y actividades (`responsibleType=DEPARTMENT`) |
| **Postcondición** | Departamento disponible en diseño y bandeja |
| **Excepciones** | E1: Departamento referenciado → restricción al eliminar. E2: Sin permiso → 403 |

---

## CU4 — Gestionar políticas de negocio

| Campo | Descripción |
|-------|-------------|
| **Actor** | Dueño de proceso / Administrador |
| **Propósito** | Definir políticas (procesos de negocio) que agrupan el workflow, formularios y trámites |
| **Precondición** | Permiso `POLICIES_MANAGE`; usuario autenticado |
| **Flujo principal** | 1. **Políticas de negocio** → crear política (nombre, descripción, tipo). 2. Estado `DRAFT` → diseñar flujo y formularios. 3. Activar política (`ACTIVE`). 4. Bitácora registra cambios |
| **Postcondición** | Política `ACTIVE` habilita iniciar trámites |
| **Excepciones** | E1: Activar sin flujo válido → validación en diseñador. E2: Política archivada → no nuevos trámites |

---

## CU5 — Diseñar workflow UML 2.5 con swimlanes

| Campo | Descripción |
|-------|-------------|
| **Actor** | Diseñador (Dueño de proceso) |
| **Propósito** | Modelar el diagrama de actividades con nodos UML (inicio, tarea, decisión, fin) y carriles por responsable |
| **Precondición** | Permiso `WORKFLOW_VIEW` / `WORKFLOW_DESIGN`; política existente |
| **Flujo principal** | 1. Abrir **Diseñador workflow** (`/workflow-designer/{policyId}`). 2. Agregar actividades en swimlanes. 3. Posicionar nodos en lienzo. 4. Validar flujo. 5. Persistir en `workflow_activities` |
| **Postcondición** | Actividades UML almacenadas y visibles en el diseñador |
| **Excepciones** | E1: Solo lectura sin `WORKFLOW_DESIGN`. E2: Conflicto colaborativo (CU16) → recargar antes de guardar |

---

## CU6 — Configurar estructuras de flujo

| Campo | Descripción |
|-------|-------------|
| **Actor** | Diseñador |
| **Propósito** | Definir transiciones entre actividades: secuencial, condicional, iterativa, paralela (split/join) |
| **Precondición** | Al menos dos actividades; permiso de diseño |
| **Flujo principal** | 1. En diseñador o **Conexiones** → crear `WorkflowTransition`. 2. Elegir tipo (`SEQUENTIAL`, `CONDITIONAL`, `ITERATIVE`, `PARALLEL_SPLIT`, `PARALLEL_JOIN`). 3. Opcional: expresión/etiqueta condicional. 4. Guardar. 5. **Validar flujo** |
| **Postcondición** | Transiciones activas listas para el motor F1 |
| **Excepciones** | E1: Conexión duplicada → error de negocio. E2: Paralelo incompleto → advertencia en validación |

---

## CU7 — Configurar responsables y enrutamiento automático

| Campo | Descripción |
|-------|-------------|
| **Actor** | Diseñador |
| **Propósito** | Asignar responsable por actividad (`USER`, `ROLE`, `DEPARTMENT`) para que el motor cree tareas sin intervención del funcionario en la ruta |
| **Precondición** | Actividades TASK con responsable; transiciones configuradas |
| **Flujo principal** | 1. Editar actividad → `responsibleType` y `responsibleName`/`responsibleId`. 2. Al ejecutar trámite, `WorkflowRoutingService` resuelve siguiente actividad según transiciones y datos del formulario. 3. Crea `TramiteTask` para el responsable correcto |
| **Postcondición** | Tareas asignadas automáticamente; funcionario no elige siguiente paso |
| **Excepciones** | E1: Sin transición válida → `workflowError` en trámite. E2: Responsable no resoluble → tarea sin asignación clara |

---

## CU8 — Diseñar formularios dinámicos por actividad

| Campo | Descripción |
|-------|-------------|
| **Actor** | Diseñador / Técnico |
| **Propósito** | Vincular formulario dinámico a cada actividad de trabajo para capturar datos en la ejecución |
| **Precondición** | Permiso `FORMS_MANAGE`; actividad TASK existente |
| **Flujo principal** | 1. Desde diseñador → **Formulario** de la actividad (`activity-form-designer`). 2. Agregar campos (texto, número, fecha, lista, archivo, etc.). 3. Marcar obligatorios. 4. Guardar `DynamicForm` + `FormField` |
| **Postcondición** | Formulario disponible al completar tarea (CU10) |
| **Excepciones** | E1: Actividad sin formulario → completar con validación mínima según configuración. E2: Campo FILE → IA no autocompleta (CU15) |

---

## CU9 — Iniciar trámite

| Campo | Descripción |
|-------|-------------|
| **Actor** | Solicitante, Administrador o Atención al cliente |
| **Propósito** | Crear instancia de proceso (`Tramite`) bajo una política activa |
| **Precondición** | Política `ACTIVE`; flujo con inicio y fin válidos |
| **Flujo principal** | 1. **Trámites** → **Nuevo trámite**. 2. Seleccionar política y datos iniciales. 3. Sistema crea `Tramite`, posiciona en actividad inicial, genera primera(s) `TramiteTask`, traza `TRAMITE_CREADO` / `TAREA_ASIGNADA` |
| **Postcondición** | Trámite en curso; bandeja del responsable actualizada |
| **Excepciones** | E1: Política inactiva. E2: Error de motor → estado con `workflowError` |

---

## CU10 — Ejecutar actividad

| Campo | Descripción |
|-------|-------------|
| **Actor** | Funcionario |
| **Propósito** | Tomar tarea, llenar formulario y completar actividad para disparar el avance automático |
| **Precondición** | Tarea asignada al usuario/rol/departamento; permiso `TASKS_EXECUTE` |
| **Flujo principal** | 1. **Mis tareas** → **Tomar** (si `PENDIENTE`). 2. **Completar** → formulario dinámico. 3. Enviar respuestas → `FormSubmission`. 4. Motor enruta → nuevas tareas o finalización |
| **Postcondición** | Tarea `COMPLETADA`; trámite avanzado o finalizado |
| **Excepciones** | E1: Campos obligatorios vacíos. E2: Completar sin tomar tarea. E3: Paralelo → espera otras ramas (`ESPERA_PARALELO`) |

---

## CU11 — Monitorear y rastrear trámite

| Campo | Descripción |
|-------|-------------|
| **Actor** | Supervisor / Administrador |
| **Propósito** | Supervisar estado operativo, timeline de trazas y tareas sin recargar la página manualmente |
| **Precondición** | Permiso `MONITORING_VIEW` |
| **Flujo principal** | 1. **Monitoreo** o **Seguimiento de trámites**. 2. Filtrar por estado/política. 3. Abrir detalle → timeline, responsables, errores. 4. Polling ~12 s actualiza vista |
| **Postcondición** | Visibilidad operativa del trámite |
| **Excepciones** | E1: Trámite con `workflowError` → destacado en UI. E2: Sin permiso → 403 |

---

## CU12 — Bandeja realtime del funcionario

| Campo | Descripción |
|-------|-------------|
| **Actor** | Funcionario |
| **Propósito** | Ver tareas pendientes, en proceso y finalizadas propias o de su rol/departamento |
| **Precondición** | `TASKS_EXECUTE`; trámites con tareas asignadas |
| **Flujo principal** | 1. **Mis tareas**. 2. Filtrar por estado/política. 3. Tomar / completar. 4. Polling 12 s refresca bandeja |
| **Postcondición** | Funcionario gestiona su trabajo sin elegir ruta |
| **Excepciones** | E1: Tarea de otro responsable → no visible (salvo admin). E2: Polling deshabilitado si pestaña inactiva prolongada |

---

## CU13 — KPI y cuellos de botella

| Campo | Descripción |
|-------|-------------|
| **Actor** | Supervisor / Dueño de proceso |
| **Propósito** | Analizar tiempos, carga y actividades con mayor demora (SLA / cuellos de botella) |
| **Precondición** | Permiso `KPI_VIEW`; trámites en ejecución o histórico |
| **Flujo principal** | 1. **KPIs / Cuellos de botella**. 2. Aplicar filtros (política, fechas). 3. Revisar métricas por actividad, carga y ranking de demoras |
| **Postcondición** | Indicadores para mejora de proceso |
| **Excepciones** | E1: Sin datos → tablas vacías. E2: SLA no definido → umbral por defecto 48 h |

---

## CU14 — Diseñar workflow con IA por texto/voz

| Campo | Descripción |
|-------|-------------|
| **Actor** | Diseñador |
| **Propósito** | Obtener sugerencias de actividades, conexiones y responsables a partir de lenguaje natural (o dictado) |
| **Precondición** | `WORKFLOW_DESIGN`; `ai-service` disponible (o fallback local) |
| **Flujo principal** | 1. Panel **Asistente IA** en diseñador. 2. Ingresar prompt o **Dictar por voz**. 3. **Generar sugerencia** → revisar vista previa. 4. **Aplicar sugerencia** (confirmación). 5. Validar flujo |
| **Postcondición** | Cambios persistidos como actividades/transiciones; IA no decide sin confirmación humana |
| **Excepciones** | E1: `ai-service` caído → parser fallback. E2: Sugerencia inválida → mensaje; no aplicar |

---

## CU15 — Asistencia IA para formularios

| Campo | Descripción |
|-------|-------------|
| **Actor** | Funcionario |
| **Propósito** | Autocompletar campos del formulario de ejecución a partir de informe libre o voz |
| **Precondición** | Tarea `EN_CURSO`; formulario configurado; `TASKS_EXECUTE` |
| **Flujo principal** | 1. **Completar** actividad → panel **Asistencia IA**. 2. Escribir/dictar informe. 3. **Asistir formulario** → vista previa por campo. 4. **Aplicar sugerencias** (solo vacíos). 5. Editar manualmente y **Completar actividad** |
| **Postcondición** | `FormSubmission` + traza `IA_FORMULARIO_ASISTIDO` (sin guardar informe completo en auditoría) |
| **Excepciones** | E1: Campo ya lleno → no sobrescribir sin confirmar. E2: Tipo FILE → no autocompletar |

---

## CU16 — Diseño colaborativo básico realtime

| Campo | Descripción |
|-------|-------------|
| **Actor** | Diseñador (dos o más concurrentes) |
| **Propósito** | Ver quién edita la misma política, última modificación y evitar sobrescribir cambios ajenos |
| **Precondición** | Acceso al mismo `policyId` en diseñador; permiso de vista mínimo |
| **Flujo principal** | 1. Abrir diseñador → registro `WORKFLOW_ABIERTO`. 2. Panel **Colaboración** lista conectados. 3. Polling 11 s. 4. Si otro guarda → aviso de conflicto; bloqueo de guardado. 5. **Recargar diagrama** → sincronizar |
| **Postcondición** | Usuario consciente del estado remoto; bitácora `WORKFLOW_MODIFICADO`, `CONFLICTO_EDICION` |
| **Excepciones** | E1: Ignorar aviso y editar en otra pantalla CRUD → riesgo último guardado gana. E2: Sin CRDT → no edición simultánea en mismo nodo |

---

---

# Ciclo 2 — Casos de uso (FASE 2.3)

| ID | Caso de uso | Fase | Actor principal |
|----|-------------|------|-----------------|
| CU24 | Generar reportes dinámicos por voz o texto | 2.3 | Supervisor / Dueño de proceso |
| CU25 | Analizar riesgos, anomalías y cuellos de botella | 2.3 | Supervisor / Dueño de proceso |
| CU26 | Recomendar rutas y prioridades | 2.3 | Supervisor / Dueño de proceso |

Ruta UI: `/intelligent-analytics` · Permiso: `INTELLIGENT_ANALYTICS_VIEW`  
Demo: [guia-demo-ciclo2.md](./guia-demo-ciclo2.md)

---

## CU24 — Generar reportes dinámicos por voz o texto

| Campo | Descripción |
|-------|-------------|
| **Actor** | Supervisor, Dueño de proceso, Administrador |
| **Propósito** | Obtener reportes operativos describiendo la necesidad en lenguaje natural (texto o dictado por voz) |
| **Precondición** | Permiso `INTELLIGENT_ANALYTICS_VIEW`; trámites en MongoDB |
| **Flujo principal** | 1. **Analítica Inteligente** → escribir o dictar consulta. 2. Opcional: filtros (fecha, política, estado). 3. **Generar análisis**. 4. Sistema interpreta intención, consulta `Tramite`/`TramiteTask`/`KpiService` y devuelve título, explicación, columnas, filas, filtros aplicados y formato sugerido |
| **Consultas soportadas (ejemplos)** | Trámites de un mes; política más usada; funcionario con más carga; trámites demorados; resumen de finalizados |
| **Postcondición** | Reporte visible en pantalla (tabla + tarjetas + gráfico); bitácora `ANALYTICS_REPORT_REQUESTED` |
| **Excepciones** | E1: Sin texto ni voz → validación. E2: `ai-service` caído → fallback Java (`LOCAL_FALLBACK`). E3: Sin permiso → 403 |

**API Spring Boot:** `POST /api/intelligent-analytics/report`  
**API FastAPI:** `POST /analytics/report`

---

## CU25 — Analizar riesgos, anomalías y cuellos de botella

| Campo | Descripción |
|-------|-------------|
| **Actor** | Supervisor, Dueño de proceso, Administrador |
| **Propósito** | Detectar demoras, tareas vencidas, sobrecarga de funcionarios, políticas con retrasos y cuellos de botella |
| **Precondición** | Permiso `INTELLIGENT_ANALYTICS_VIEW` |
| **Flujo principal** | 1. Desde **Generar análisis** (o `POST .../risks`). 2. Motor analiza trámites activos, tareas (`TramiteTask`), trazas (`TraceItem`) y KPI (`KpiService`). 3. Lista riesgos por severidad (ALTO/MEDIO/BAJO) y tipo (DEMORA, VENCIDA, CARGA, ANOMALIA, CUELLO) |
| **Postcondición** | Tarjetas y listado de riesgos; bitácora `ANALYTICS_RISK_ANALYZED` |
| **Excepciones** | E1: Sin trámites en periodo → resumen vacío. E2: FastAPI no disponible → heurísticas Java (SLA 48 h, carga ≥ 4 tareas) |

**API Spring Boot:** `POST /api/intelligent-analytics/risks`  
**API FastAPI:** `POST /analytics/risks`

---

## CU26 — Recomendar rutas y prioridades

| Campo | Descripción |
|-------|-------------|
| **Actor** | Supervisor, Dueño de proceso, Administrador |
| **Propósito** | Sugerir acciones: priorizar trámites, revisar cuellos, reasignar carga, alertar políticas en riesgo, siguiente paso en ruta |
| **Precondición** | Permiso `INTELLIGENT_ANALYTICS_VIEW` |
| **Flujo principal** | 1. **Generar análisis** invoca también recomendaciones (o `POST .../recommendations`). 2. Sistema ordena por prioridad (ALTA/MEDIA/BAJA) según demora, prioridad del trámite y KPI. 3. Muestra acción concreta y justificación |
| **Ejemplos de salida** | *Priorizar trámite TRM-xxx*; *Revisar actividad con cuello de botella*; *Reasignar tarea si funcionario tiene demasiada carga* |
| **Postcondición** | Listado de recomendaciones; bitácora `ANALYTICS_RECOMMENDATION_GENERATED` |
| **Excepciones** | E1: Flujo normal sin alertas → mensaje informativo. E2: Sin ML profundo → reglas heurísticas, no predicción TensorFlow |

**API Spring Boot:** `POST /api/intelligent-analytics/recommendations`  
**API FastAPI:** `POST /analytics/recommendations`

---

## Acceso y seguridad (FASE 2.3)

| Rol | Permiso `INTELLIGENT_ANALYTICS_VIEW` |
|-----|--------------------------------------|
| Administrador del sistema | Sí (vía `SystemPermissions.ALL`) |
| Dueño de proceso | Sí |
| Supervisor | Sí |
| Funcionario | No |
| Atención al cliente | **No** (por diseño actual) |

Auditoría (módulo **Analítica inteligente**): `ANALYTICS_REPORT_REQUESTED`, `ANALYTICS_RISK_ANALYZED`, `ANALYTICS_RECOMMENDATION_GENERATED`.

---

## Limitaciones FASE 2.3

- **Sin TensorFlow / entrenamiento profundo** — análisis por reglas, SLA y agregaciones KPI.
- **Sin exportación real** PDF/Excel/Word — solo se sugiere formato (`PANTALLA`, `PDF`, `EXCEL`).
- **Dictado por voz** depende del navegador (Web Speech API).
- **Fallback Java** activo cuando `ai-service` (puerto 8000) está apagado o sin API key Gemini.

---

---

# Ciclo 2 — Casos de uso (FASE 2.4)

| ID | Caso de uso | Fase | Actor principal |
|----|-------------|------|-----------------|
| CU27 | Aplicación móvil inteligente (PWA) | 2.4 | Funcionario / Supervisor |
| CU28 | Operación offline y sincronización | 2.4 | Funcionario |

Guía PWA: [guia-instalacion-pwa.md](./guia-instalacion-pwa.md) · Demo: [guia-demo-ciclo2.md](./guia-demo-ciclo2.md)

---

## CU27 — Aplicación móvil inteligente (PWA)

| Campo | Descripción |
|-------|-------------|
| **Actor** | Funcionario, Supervisor, cualquier usuario autenticado |
| **Propósito** | Usar el sistema desde celular o tablet como app instalable (PWA) |
| **Precondición** | Frontend compilado en **modo producción** (`npm run build`); navegador compatible (Chrome/Edge recomendado) |
| **Flujo principal** | 1. Abrir URL servida desde `dist/frontend/browser`. 2. Iniciar sesión. 3. **Instalar app** (botón o menú del navegador). 4. Usar menú inferior móvil: Dashboard, Tareas, Trámites, Agente. 5. Consultar trazabilidad en detalle de trámite |
| **Vistas optimizadas** | Dashboard, Mis actividades, Trámites, Documentos (pestaña en trámite), Agente Inteligente |
| **Indicadores** | Banner **En línea** / **Sin conexión**; contador de elementos pendientes de sincronizar |
| **Postcondición** | App en pantalla de inicio del dispositivo (modo `standalone`) |
| **Excepciones** | E1: `ng serve` en desarrollo → service worker deshabilitado. E2: Safari iOS requiere “Añadir a pantalla de inicio” manual |

**Artefactos:** `manifest.webmanifest`, `ngsw-config.json`, `ngsw-worker.js` (generado en build).

---

## CU28 — Operación offline y sincronización

| Campo | Descripción |
|-------|-------------|
| **Actor** | Funcionario (campo, conectividad intermitente) |
| **Propósito** | Consultar tareas y guardar operaciones sin red; sincronizar al reconectar |
| **Precondición** | Sesión JWT válida (no expirada); al menos una consulta previa online para cachear bandeja |
| **Flujo principal** | 1. Sin conexión → banner offline. 2. Usuario toma tarea, guarda borrador, completa actividad o sube documento. 3. Sistema encola en IndexedDB (`pending_sync_queue`). 4. Muestra *"N elemento(s) pendiente(s) de sincronizar"*. 5. Al reconectar → `OfflineSyncService` sincroniza automáticamente |
| **Operaciones en cola** | `TAKE_TASK`, `FORM_DRAFT`, `COMPLETE_ACTIVITY`, `DOCUMENT_UPLOAD` |
| **Postcondición** | Datos replicados al backend; bitácora `OFFLINE_SYNC_COMPLETED` |
| **Excepciones** | E1: JWT expirado → redirige a login sin sincronizar. E2: Agente/analítica no disponibles offline |

**Stores IndexedDB:**

| Store | Uso |
|-------|-----|
| `pending_sync_queue` | Cola principal de sincronización |
| `pending_forms` | Borradores y completados pendientes |
| `pending_documents` | Archivos pendientes |
| `cached_activities` | Última bandeja consultada |

**Auditoría:** `OFFLINE_DATA_STORED`, `OFFLINE_SYNC_COMPLETED` (módulo **Modo offline**).

**API:** `POST /api/offline/notify-stored` · `POST /api/offline/notify-sync-completed`

---

## Limitaciones FASE 2.4

- **Service worker solo con build producción** — no en `ng serve`.
- API REST en otro origen (puerto 8080); no cacheada por SW; offline vía IndexedDB.
- Sin cliente Flutter nativo (solo PWA Angular).
- Sin resolución automática de conflictos en cola.
- Agente inteligente, analítica y dictado por voz requieren red.

---

## Fuera de alcance Ciclo 2 (pendiente)

No documentar como implementado: cliente Flutter nativo, motor predictivo con TensorFlow, exportación documental real masiva, agente cliente móvil dedicado.
