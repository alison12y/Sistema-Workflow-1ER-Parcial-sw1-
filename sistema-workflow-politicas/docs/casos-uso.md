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

## Fuera de alcance Ciclo 1 (Ciclo 2)

No documentar como implementado: S3, documental avanzado, Flutter, offline, reportes dinámicos IA, motor predictivo, agente inteligente cliente.
