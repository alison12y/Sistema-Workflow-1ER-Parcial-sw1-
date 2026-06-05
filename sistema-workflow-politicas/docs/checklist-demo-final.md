# Checklist demo final — Ciclo 1 (F10.3)

Freeze de presentación. Marque cada ítem antes y durante la defensa.

---

## A. Build y servicios (pre-demo, ~15 min)

| Verificación | Comando / acción | Resultado F10.3 |
|--------------|------------------|-----------------|
| Backend tests | `cd backend && mvn clean test` | ✅ 12 tests OK |
| Backend JAR | `cd backend && mvn clean package -DskipTests` | ✅ `target/politicas-0.0.1-SNAPSHOT.jar` (~31 MB) |
| Frontend build | `cd frontend && npm install` (si falta) && `npm run build` | ✅ `frontend/dist/frontend` (warning Sass budget diseñador) |
| MongoDB | `mongod` o contenedor puerto 27017 | Manual |
| Backend API | `mvn spring-boot:run` o JAR → `:8080` | Manual |
| Frontend | `npm start` → `http://localhost:4200` | Manual |
| ai-service | `cd ai-service && pip install -r requirements.txt && uvicorn main:app --port 8000` | ⚠️ Requiere Python + deps; sin FastAPI instalado falla import |
| Docker Compose | `docker compose config` en raíz | ⚠️ Docker no disponible en entorno de verificación; archivo `docker-compose.yml` válido (mongodb, backend, frontend, ai-service) |

---

## B. Rutas críticas (Angular)

| Ruta solicitada | Ruta real | Componente | Guard |
|-----------------|-----------|------------|-------|
| `/login` | `/login` | Login | loginGuard |
| `/dashboard` | `/dashboard` | Dashboard | auth |
| `/policies` | `/policies` | Políticas | POLICIES_MANAGE |
| `/workflow-designer/:id` | `/workflow-designer/:id` | Diseñador UML | WORKFLOW_* |
| `/tramites` | `/tramites` | Trámites | TASKS_EXECUTE / monitor |
| `/my-activities` | **`/mis-actividades`** (alias `/my-activities` → redirect) | Mis tareas | TASKS_EXECUTE |
| `/monitoring` | `/monitoring` | Monitoreo | MONITORING_VIEW |
| `/kpi` | **`/kpis`** (alias `/kpi` → redirect) | KPI | KPI_VIEW |
| `/bitacora` | `/bitacora` | Bitácora | AUDIT_VIEW |

**No usar en demo principal:** `/policies/:id/actividades` y `/policies/:id/transiciones` (vistas legacy de listas). Usar **Diseñar workflow**.

---

## C. Usuarios demo

Ver tabla completa en [guia-demo-ciclo1.md](./guia-demo-ciclo1.md#preparación-5-min-antes).

| Perfil | Usuario | Contraseña |
|--------|---------|------------|
| Admin | `sistema.admin` | `Admin.Sistema2024!` |
| Diseñador | `carlos.mendoza` | `Carlos.M2024!` |
| Funcionario | `ana.rodriguez` | `Ana.R2024!` |
| Supervisor | `luis.supervisor` | `Luis.S2024!` |
| Atención al cliente | `patricia.cliente` | `Patricia.C2024!` |

**Departamentos seed:** Tecnología e Información, Recursos Humanos, Operaciones, Dirección General.

---

## D. Datos demo (seed automático)

| Dato | Estado seed | Nota para presentación |
|------|-------------|------------------------|
| Política *Solicitud de instalación de medidor* | ✅ Phase 2 | Activar si está en DRAFT |
| Actividades workflow | ✅ Phase 3 | Incluye START, TASK, END; transición **CONDITIONAL** en Phase 4 |
| DECISION / PARALLEL / JOIN en seed medidor | ❌ No precargados | Mostrar en **diseñador en vivo** o política de prueba ya editada |
| Formulario en actividad | ⚠️ Manual | Desde diseñador → actividad TASK → **Formulario** |
| Al menos 1 trámite | ⚠️ Manual | **Trámites** → iniciar con política ACTIVE antes de monitor/KPI |
| Workflow válido (validar flujo) | Manual | Botón **Validar flujo** → 0 errores |

### Script rápido pre-monitor (5 min)

1. Login `sistema.admin` → Políticas → *Medidor* → **Activar** si aplica.
2. **Diseñar workflow** → **Validar flujo** (sin errores).
3. Actividad *Revisión de requisitos* → formulario con 1–2 campos obligatorios → guardar.
4. **Trámites** → **Iniciar trámite** (política medidor).
5. Login `ana.rodriguez` → **Mis tareas** → tomar → completar (prueba formulario).

---

## E. Checklist funcional (durante la demo)

| # | Ítem | Usuario sugerido | ☐ |
|---|------|------------------|---|
| 1 | Login correcto y menú según rol | Todos | ☐ |
| 2 | Diseñador UML (swimlanes, tareas, conexiones) | `carlos.mendoza` / admin | ☐ |
| 3 | IA workflow (sugerir / aplicar / validar) | Diseñador | ☐ |
| 4 | Colaboración básica (2 navegadores, conflicto) | admin + diseñador | ☐ |
| 5 | Activar política ACTIVE | admin | ☐ |
| 6 | Iniciar trámite | admin / atención | ☐ |
| 7 | Bandeja funcionario (tomar tarea) | `ana.rodriguez` | ☐ |
| 8 | IA formulario (informe libre → campos) | funcionario | ☐ |
| 9 | Completar actividad (obligatorios) | funcionario | ☐ |
| 10 | Avance automático motor | funcionario → monitor | ☐ |
| 11 | Monitor sin F5 (polling ~12 s) | supervisor / admin | ☐ |
| 12 | KPI / cuellos de botella | supervisor | ☐ |
| 13 | Bitácora (eventos recientes) | admin | ☐ |

---

## F. Mensajes y menú (F10.3)

| Tema | Corrección aplicada |
|------|---------------------|
| Menú «Asistente IA» | Ya no dice «en desarrollo»; etiqueta **Guía asistente IA** (IA real en Diseñador y Mis tareas) |
| Seguimiento | Etiqueta **Seguimiento (alternativa)**; demo principal en **Monitoreo** |
| Rutas alias | `/my-activities` y `/kpi` redirigen a rutas canónicas |

---

## G. Pasos exactos para presentación (25 min)

1. **Entorno visible:** terminal con backend + frontend; opcional ai-service.
2. **Login admin** → Dashboard → Políticas → medidor ACTIVE.
3. **Diseñador** → validar flujo → (opcional) nodo DECISION o paralelo si preguntan CU6.
4. **IA diseñador** → prompt → sugerencia → aplicar → validar.
5. **Colaboración** (si hay tiempo): segundo navegador, mismo policyId.
6. **Trámite** → iniciar → cambiar a **ana.rodriguez**.
7. **Mis tareas** → tomar → IA formulario → completar → mensaje éxito.
8. **Monitor** → ver trámite y trazas (esperar refresh o pulsar Actualizar).
9. **KPIs** → indicadores con el trámite creado.
10. **Bitácora** → filtrar módulo Workflow / Trámites.
11. Cierre: *Ciclo 2 no incluido* (S3, Flutter, predictivo, etc.) — ver guía demo.

---

## H. Errores conocidos / riesgos

| Hallazgo | Severidad | Acción |
|----------|-----------|--------|
| Docker CLI no instalado en PC de verificación | Info | Levantar servicios manualmente |
| ai-service sin `pip install` | Media | Usar fallback local (F6/F7) en demo |
| Seed medidor sin PARALLEL en BD nueva | Info | Mostrar en diseñador o omitir |
| BD antigua sin usuarios supervisor/cliente | Media | Crear en Admin o Mongo vacío |
| Sin trámite → KPI/monitor vacíos | Alta | Ejecutar script sección D antes |

---

## I. Referencias

- Guía narrativa: [guia-demo-ciclo1.md](./guia-demo-ciclo1.md)
- Optimización/pruebas: [f10-2-optimizacion-pruebas.md](./f10-2-optimizacion-pruebas.md)
- Instalación: [manual-instalacion.md](./manual-instalacion.md)
