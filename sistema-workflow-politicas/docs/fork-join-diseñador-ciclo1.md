# Fork / Join en el diseñador UML (Ciclo 1)

## Representación interna

- Nodos **Fork** y **Join** son `WorkflowActivity` con `activityType`: `FORK` o `JOIN`.
- No son tareas humanas: el motor (`TramiteService`) solo crea `TramiteTask` para actividades `TASK`.
- El paralelismo en ejecución sigue modelado con transiciones:
  - `PARALLEL_SPLIT` (salidas desde Fork hacia ramas)
  - `PARALLEL_JOIN` (entradas hacia Join desde ramas)

## Recorrido en runtime (cambio mínimo)

`WorkflowRoutingService` trata `FORK`/`JOIN` como pasarelas:

- **Fork**: si tiene salidas `PARALLEL_SPLIT`, activa ramas en paralelo (igual que decisión con split).
- **Join**: al completar todas las ramas, el trámite activa el Join y continúa por su salida (típicamente `SEQUENTIAL`).

## Patrón recomendado en el diagrama

```
[Tarea previa] --SECUENCIAL--> [Fork] --PARALLEL_SPLIT--> [Rama A]
                              `--PARALLEL_SPLIT--> [Rama B]
[Rama A] --PARALLEL_JOIN--> [Join] --SECUENCIAL--> [Siguiente tarea]
[Rama B] --PARALLEL_JOIN--/
```

## Cómo probar

1. Diseñador → modo edición → toolbox **Fork** y **Join**.
2. Conectar: tarea → Fork → dos tareas (tipo división paralela en cada salida del Fork).
3. Conectar cada rama → Join (tipo unión paralela).
4. Join → siguiente tarea (secuencial).
5. **Validar flujo** (debe pasar o solo advertencias menores).
6. Iniciar trámite → deben crearse dos tareas paralelas.
7. Completar una rama → trámite en espera paralela.
8. Completar la otra → continúa tras el Join.

## Limitaciones conocidas

- No hay símbolo UML de pasarela en el estándar BPMN completo; se usa barra negra + etiqueta.
- Split directo desde una `TASK` sin nodo Fork sigue siendo válido (comportamiento anterior).
- `my-activities` / KPI no filtran Fork/Join explícitamente (no generan tareas).
