# F1 — Motor workflow: guía de pruebas

## Requisitos previos

1. MongoDB en ejecución.
2. Política **ACTIVE** con actividades y transiciones en `workflow_activities` / `workflow_transitions` (diseñador UML o migración Fase 3/4).
3. Formularios por actividad TASK (`activity-form-designer`).
4. Usuario funcionario con rol/responsable alineado al swimlane.

## Flujo secuencial

1. Diseñar: START → T1 → T2 → END con transiciones `SEQUENTIAL`.
2. Iniciar trámite en **Trámites**.
3. Iniciar sesión como funcionario asignado a T1 → **Mis actividades** → completar formulario.
4. Verificar: trámite pasa a T2 sin botón "Avanzar" (no admin).
5. Completar T2 → trámite **Finalizado**.

## Flujo condicional

1. Desde actividad DECISION o TASK con dos salidas `CONDITIONAL`:
   - `conditionExpression`: `monto > 1000` o `aprobado == true`
   - `conditionLabel`: `Aprobado` (respaldo si no hay expresión)
2. Formulario con campo `monto` o checkbox `aprobado`.
3. Completar actividad con valor que cumple una rama → debe activar solo esa tarea siguiente.

## Flujo iterativo

1. Transición `ITERATIVE` de T2 → T1 (revisión).
2. Completar T2 con datos que disparen la iterativa → vuelve a T1 (nueva tarea EN_CURSO).

## Flujo paralelo

1. Desde una actividad, varias transiciones `PARALLEL_SPLIT` hacia T-A y T-B.
2. Transiciones `PARALLEL_JOIN` desde T-A y T-B hacia actividad "Unión" (o destino común).
3. Al iniciar/avanzar: dos tareas **EN_CURSO** en Mis actividades (distintos responsables).
4. Completar una → trámite espera (traza `ESPERA_PARALELO`).
5. Completar la otra → activa actividad tras el JOIN.

## Avance manual

- Usuario no admin: `PUT /api/tramites/{id}/advance` → error 400.
- Admin: permitido (depuración).

## API clave

- `POST /api/tramites` — inicia en primera TASK tras START.
- `POST /api/my-activities/{tramiteId}/complete` — enrutamiento automático con `stepData` del formulario.
