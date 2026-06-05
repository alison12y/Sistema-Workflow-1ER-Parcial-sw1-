# Reglas de negocio — Ciclo 1

Reglas que gobiernan el diseño, la ejecución y la auditoría del sistema en el primer parcial.

---

## 1. Política y flujo UML

| ID | Regla |
|----|--------|
| RN-01 | Toda política de negocio que se active para trámites debe tener un flujo UML con **al menos un nodo de inicio (START)** y **un nodo de fin (END)**. |
| RN-02 | El flujo oficial se modela con `WorkflowActivity` y `WorkflowTransition` vinculados por `policyId` (no con el modelo legacy BPM). |
| RN-03 | La validación del diseñador (`Validar flujo`) reporta errores (sin inicio/fin, nodos huérfanos, paralelo incompleto) y advertencias antes de operación masiva. |

---

## 2. Actividades y responsables

| ID | Regla |
|----|--------|
| RN-04 | Toda actividad de tipo **tarea (TASK)** que participe en la ejecución debe tener **responsable** configurado (`responsibleType`: `USER`, `ROLE` o `DEPARTMENT` y nombre/identificador asociado). |
| RN-05 | Los **swimlanes** del diagrama se derivan del nombre del responsable (`responsibleName`); agrupan visualmente las actividades por actor organizacional. |
| RN-06 | Una actividad desactivada (`INACTIVA`) no participa en nuevos avances; las conexiones incidentes pueden desactivarse en cascada. |

---

## 3. Estructuras de flujo (transiciones)

| ID | Regla |
|----|--------|
| RN-07 | El flujo soporta tipos de transición: **secuencial**, **condicional**, **iterativa**, **paralela** (`PARALLEL_SPLIT` / `PARALLEL_JOIN`). |
| RN-08 | Transición **condicional**: se evalúa `conditionExpression` contra respuestas del formulario; si no hay expresión, puede usarse `conditionLabel` como respaldo. |
| RN-09 | Transición **iterativa**: permite volver a una actividad anterior tras completar la actual (revisión). |
| RN-10 | **Paralelo**: al activar un split se crean varias tareas `EN_CURSO`; el join exige completar las ramas requeridas antes de continuar (traza `ESPERA_PARALELO` si aplica). |
| RN-11 | No se permiten dos conexiones activas idénticas (mismo origen, destino y política). |

---

## 4. Motor de enrutamiento (ejecución)

| ID | Regla |
|----|--------|
| RN-12 | El **funcionario no decide** la siguiente ruta del trámite; solo completa su tarea y formulario. |
| RN-13 | El **motor enruta automáticamente** (`WorkflowRoutingService`) según el diagrama UML y los datos capturados. |
| RN-14 | El avance manual (`PUT /api/tramites/{id}/advance`) está restringido a **administradores** (depuración); no es el flujo del funcionario. |
| RN-15 | La primera tarea única tras crear o avanzar puede quedar **auto-tomada** (`EN_CURSO`); las siguientes suelen iniciar en `PENDIENTE` hasta **Tomar**. |

---

## 5. Formularios

| ID | Regla |
|----|--------|
| RN-16 | Cada actividad de trabajo puede tener un **formulario dinámico** (`DynamicForm` + `FormField`) para la ejecución. |
| RN-17 | Al completar actividad, los campos **obligatorios** deben validarse antes de `FormSubmission` y avance. |
| RN-18 | Las respuestas del formulario alimentan condiciones del motor y quedan asociadas a `workflowActivityId` y tarea. |

---

## 6. Auditoría y trazabilidad

| ID | Regla |
|----|--------|
| RN-19 | Todo evento relevante del trámite genera **traza** (`TraceItem`): creación, tareas, formulario, avance, paralelo, errores, finalización. |
| RN-20 | Acciones administrativas y de diseño registran **bitácora** (`bitacora`): usuarios, políticas, formularios, diseñador UML, colaboración, IA (resumen). |
| RN-21 | La asistencia IA en formularios **no almacena el informe completo** en traza; solo metadatos (campos sugeridos/aplicados). |

---

## 7. Colaboración en diseño (F8)

| ID | Regla |
|----|--------|
| RN-22 | Varios diseñadores pueden **ver presencia** en el mismo diagrama (polling ~11 s; sesión expira ~25 s sin heartbeat). |
| RN-23 | Cada mutación del workflow incrementa una **revisión** por política; el cliente guarda la revisión al abrir/recargar. |
| RN-24 | Si otro usuario modificó el diagrama, el sistema **bloquea guardados** en el diseñador hasta **recargar** (sin merge automático / sin CRDT). |
| RN-25 | Eventos de colaboración en bitácora: `WORKFLOW_ABIERTO`, `WORKFLOW_MODIFICADO`, `CONFLICTO_EDICION`. |

---

## 8. Inteligencia artificial

| ID | Regla |
|----|--------|
| RN-26 | La IA es **asistencia**, no decisión autónoma: toda sugerencia de diagrama o formulario requiere **confirmación humana** antes de persistir. |
| RN-27 | Si `ai-service` (Gemini) no está disponible, operan **fallbacks** locales (Java/Python) con capacidad limitada. |
| RN-28 | La IA de diseño no reemplaza el diseñador UML manual; complementa CU5/CU6. |
| RN-29 | La IA de formularios no sustituye la validación de negocio ni el envío oficial del funcionario. |

---

## 9. Seguridad y permisos

| ID | Regla |
|----|--------|
| RN-30 | Autenticación **JWT**; cada endpoint valida rol y permisos granulares (`WORKFLOW_DESIGN`, `TASKS_EXECUTE`, `MONITORING_VIEW`, etc.). |
| RN-31 | Diseño de workflow exige permisos de mutación; lectura del diseñador permite presencia colaborativa sin editar. |
| RN-32 | La bandeja filtra tareas según responsable de la actividad vs. usuario autenticado (salvo administrador). |

---

## Referencias

- Modelo de datos: [ciclo1-modelo-workflow.md](./ciclo1-modelo-workflow.md)
- Motor F1: [f1-motor-workflow-pruebas.md](./f1-motor-workflow-pruebas.md)
- Colaboración F8: [f8-colaboracion-pruebas.md](./f8-colaboracion-pruebas.md)
