# Guía de demo — Ciclo 1 (defensa del primer parcial)

Guía paso a paso para demostrar ante el ingeniero los **16 casos de uso** implementados (F0–F8).  
Duración sugerida: **25–35 minutos** (versión completa) o **15 min** (ruta corta marcada con ⚡).

---

## Preparación (5 min antes)

1. MongoDB + backend + frontend + (opcional) ai-service en ejecución — ver [manual-instalacion.md](./manual-instalacion.md).
2. Dos navegadores o ventana normal + incógnito (colaboración CU16).
3. Credenciales demo (BD nueva o usuarios creados por seed/migración Fase 1):

| Perfil | Usuario | Contraseña | Uso en demo |
|--------|---------|------------|-------------|
| Administrador | `sistema.admin` | `Admin.Sistema2024!` | Admin, monitor, KPI, bitácora, colaboración |
| Diseñador (dueño de proceso) | `carlos.mendoza` | `Carlos.M2024!` | Diseñador UML, políticas, IA workflow |
| Funcionario | `ana.rodriguez` | `Ana.R2024!` | Bandeja, formulario, completar tarea |
| Supervisor | `luis.supervisor` | `Luis.S2024!` | Monitoreo, KPI, seguimiento |
| Atención al cliente | `patricia.cliente` | `Patricia.C2024!` | Trámites, seguimiento (rol atención) |

> Si la base ya existía antes de F10.3, cree `luis.supervisor` y `patricia.cliente` en **Usuarios** (admin) con los roles indicados, o reinicie con MongoDB vacío para que aplique el seed.

4. Tener lista la política **Solicitud de instalación de medidor** (estado **ACTIVE**) y **al menos un trámite** iniciado para monitor/KPI (ver [checklist-demo-final.md](./checklist-demo-final.md)).

---

## Ruta de demo

### 1. Login administrador (CU1–CU3 contexto)

1. Abrir `http://localhost:4200/login` (rutas canónicas: bandeja `/mis-actividades`, KPI `/kpis`).
2. Ingresar `sistema.admin`.
3. **Mencionar:** gestión de usuarios, roles y departamentos en menú Admin (CU1–CU3) — mostrar pantalla rápida si preguntan.

⚡ *Omitir CRUD admin si el tiempo es corto.*

---

### 2. Política de negocio (CU4)

1. **Políticas de negocio** → abrir *Solicitud de instalación de medidor* (o **Nueva política**).
2. Mostrar nombre, descripción, estado (`DRAFT` / `ACTIVE`).
3. Explicar: la política agrupa workflow, formularios y trámites.

---

### 3. Diseñar workflow UML con swimlanes (CU5, CU6, CU7)

1. **Diseñar workflow** → diseñador UML.
2. Mostrar:
   - Carriles / swimlanes por responsable.
   - Nodos: inicio, tareas, decisión, fin.
   - Panel **Elementos UML**.
3. Activar **Modo edición** → **+ Actividad** o **+ Conexión**.
4. Crear una transición **condicional** o **paralela** (CU6) y señalar responsable en actividad (CU7).
5. **Validar flujo** → debe quedar **sin errores** (F10.2). Las advertencias (p. ej. nodos poco conectados) no bloquean el flujo; los errores sí.

**Frase clave:** *“El funcionario no elige la ruta; el motor lee estas transiciones.”*

---

### 4. Formulario dinámico (CU8)

1. Desde una actividad TASK → enlace **Formulario** (`activity-form-designer`).
2. Agregar campos (texto, número, lista).
3. Guardar.
4. Volver al diseñador.

---

### 5. IA en diseñador (CU14)

1. Panel **Asistente IA** (columna izquierda).
2. Pegar prompt de ejemplo o **Dictar por voz** (Chrome/Edge).
3. **Generar sugerencia** → vista previa.
4. **Aplicar sugerencia** → confirmar.
5. **Validar flujo**.

**Frase clave:** *“La IA solo sugiere; el diseñador confirma.”*

⚡ Si `ai-service` está caído: mencionar fallback automático.

---

### 6. Colaboración básica — dos navegadores (CU16)

1. **Navegador A:** `sistema.admin` en diseñador de la misma política.
2. **Navegador B:** `carlos.mendoza` (o segundo admin) mismo URL.
3. Panel **Colaboración:** ambos en “Conectados ahora”.
4. En **B:** guardar actividad o conexión.
5. En **A:** en ~11 s aparece aviso de conflicto; intentar guardar → bloqueado.
6. **Recargar diagrama** en A → ver cambios de B.
7. Bitácora (mencionar): `WORKFLOW_ABIERTO`, `WORKFLOW_MODIFICADO`, `CONFLICTO_EDICION`.

---

### 7. Activar política (CU4)

1. Detalle de política → cambiar estado a **ACTIVE** (si estaba en borrador).
2. Confirmar que permite iniciar trámites.

---

### 8. Iniciar trámite (CU9)

1. **Trámites** → **Nuevo trámite**.
2. Seleccionar política activa.
3. Crear → mostrar código de trámite y estado inicial.

---

### 9. Funcionario — bandeja y ejecución (CU10, CU12)

1. Cerrar sesión → login `ana.rodriguez`.
2. **Mis tareas**:
   - Pendientes / En proceso / Finalizadas.
   - Polling ~12 s (mostrar “última actualización” si está visible).
3. **Tomar** tarea si está pendiente.
4. **Completar** → formulario dinámico.

---

### 10. IA en formulario (CU15)

1. Panel **Asistencia IA**.
2. Texto ejemplo: *Motivo: permiso. Fecha: 2026-06-15. Observación: documentación adjunta.*
3. **Asistir formulario** → **Aplicar sugerencias** (solo vacíos).
4. Completar campos obligatorios restantes.
5. **Completar actividad**.

**Frase clave:** *“El motor avanza solo; el funcionario no elige el siguiente paso.”*

---

### 11. Avance automático (CU7, F1)

1. Volver a **Mis tareas** o detalle del trámite.
2. Mostrar nueva tarea para siguiente responsable (o trámite finalizado).
3. Si hay condición/paralelo en el flujo, explicar qué rama se tomó según datos del formulario.

⚡ Completar una segunda tarea con otro usuario si el flujo lo requiere.

---

### 12. Monitor sin F5 (CU11)

1. Login `sistema.admin` (o supervisor).
2. **Monitoreo** → abrir el trámite creado.
3. Mostrar timeline: `TRAMITE_CREADO`, `TAREA_TOMADA`, `FORMULARIO_ENVIADO`, `ACTIVIDAD_COMPLETADA`, etc.
4. Esperar ~12 s — la vista se actualiza sin recargar página.

Alternativa: **Seguimiento de trámites** con el mismo polling.

---

### 13. KPI y cuello de botella (CU13)

1. **KPIs / Cuellos de botella**.
2. Filtrar por política.
3. Mostrar métricas por actividad y ranking de demoras.
4. Relacionar con `estimatedTimeHours` en actividades del diseñador.

---

### 14. Bitácora y trazabilidad (RN-19–RN-21)

1. Mencionar trazas en monitor (runtime).
2. Si hay pantalla o API de bitácora accesible para admin: eventos de diseño, colaboración, IA (resumen).
3. Enfatizar: IA de formulario **no** guarda el informe completo en auditoría.

---

## Checklist de demo (imprimir o usar en defensa)

| # | Ítem | CU | OK |
|---|------|-----|-----|
| 1 | Login admin y permisos visibles | CU1–CU3 | ☐ |
| 2 | Política creada/abierta | CU4 | ☐ |
| 3 | Diagrama UML + swimlanes | CU5 | ☐ |
| 4 | Transiciones sec/cond/iter/paralelo | CU6 | ☐ |
| 5 | Responsables en actividades | CU7 | ☐ |
| 6 | Formulario por actividad | CU8 | ☐ |
| 7 | IA diseño con confirmación | CU14 | ☐ |
| 8 | Dos usuarios en colaboración + conflicto | CU16 | ☐ |
| 9 | Política ACTIVE | CU4 | ☐ |
| 10 | Trámite iniciado | CU9 | ☐ |
| 11 | Bandeja funcionario + tomar tarea | CU12 | ☐ |
| 12 | IA formulario + completar | CU15, CU10 | ☐ |
| 13 | Avance automático del motor | CU7 | ☐ |
| 14 | Monitor actualiza sin F5 | CU11 | ☐ |
| 15 | KPI / cuellos de botella | CU13 | ☐ |
| 16 | Trazas / bitácora mencionadas | Auditoría | ☐ |

---

## Qué NO mostrar (Ciclo 2)

- Almacenamiento S3 / documental avanzado
- App Flutter / offline
- Reportes dinámicos con IA
- Motor predictivo
- Agente inteligente en cliente

Decir: *“Quedan planificados para el segundo parcial.”*

---

## Referencias rápidas por fase

| Fase | Documento |
|------|-----------|
| F1 Motor | [f1-motor-workflow-pruebas.md](./f1-motor-workflow-pruebas.md) |
| F2 Bandeja | [f2-bandeja-pruebas.md](./f2-bandeja-pruebas.md) |
| F3 Monitor | [f3-monitor-pruebas.md](./f3-monitor-pruebas.md) |
| F4 Formularios | [f4-formularios-pruebas.md](./f4-formularios-pruebas.md) |
| F5 KPI | [f5-kpi-pruebas.md](./f5-kpi-pruebas.md) |
| F6 IA diagrama | [f6-ia-diagrama-pruebas.md](./f6-ia-diagrama-pruebas.md) |
| F7 IA formulario | [f7-ia-formularios-pruebas.md](./f7-ia-formularios-pruebas.md) |
| F8 Colaboración | [f8-colaboracion-pruebas.md](./f8-colaboracion-pruebas.md) |
| F10.1 Estabilización | [f10-1-estabilizacion.md](./f10-1-estabilizacion.md) |
| F10.2 Optimización | [f10-2-optimizacion-pruebas.md](./f10-2-optimizacion-pruebas.md) |
| F10.3 Freeze demo | [checklist-demo-final.md](./checklist-demo-final.md) |

---

## Pre-demo F10.3 (obligatorio, 5 min)

Seguir [checklist-demo-final.md](./checklist-demo-final.md): builds, política ACTIVE, formulario, **un trámite** y validación de flujo.

---

## Pre-demo F10.2 (opcional, 2 min)

1. En backend: `mvn test` (pruebas unitarias del motor y validación).
2. Arrancar app y comprobar en logs: índices Mongo F10.2 verificados.
3. En diseñador: **Validar flujo** sin errores en la política de la demo.
