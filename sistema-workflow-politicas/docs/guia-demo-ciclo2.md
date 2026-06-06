# Guía de demo — Ciclo 2 (FASE 2.3 y 2.4)

Demostración de:

- **FASE 2.3 — CU24, CU25, CU26:** analítica inteligente (reportes, riesgos, recomendaciones).
- **FASE 2.4 — CU27, CU28:** PWA instalable y modo offline con sincronización.

Duración sugerida: **15–20 minutos** (analítica ~10 min + PWA/offline ~10 min).

---

## Preparación

1. Servicios en ejecución — ver [manual-instalacion.md](./manual-instalacion.md):
   - MongoDB (`docker compose up -d mongodb`)
   - Backend (`cd backend && mvn spring-boot:run`) → http://localhost:8080
   - Frontend desarrollo (`cd frontend && npm start`) → http://localhost:4200
   - **Para PWA (CU27/CU28):** `cd frontend && npm run build && npx http-server dist/frontend/browser -p 4200` → http://localhost:4200
   - **Opcional:** `ai-service` en puerto 8000 (con `AI_API_KEY` en `.env`)

2. **Datos:** al menos 2–3 trámites en distintos estados (activos y finalizados) para que reportes y riesgos tengan contenido. Política **Solicitud de instalación de medidor** en `ACTIVE`.

3. **Permisos:** si la base de datos existía antes de FASE 2.3, sincronizar roles:
   ```bash
   curl -X POST http://localhost:8080/api/dev/migrate-phase1
   ```
   Luego **cerrar sesión y volver a entrar** para refrescar el JWT.

4. **Usuarios con acceso** (tienen `INTELLIGENT_ANALYTICS_VIEW`):

| Perfil | Usuario | Contraseña |
|--------|---------|------------|
| Administrador | `sistema.admin` | `Admin.Sistema2024!` |
| Dueño de proceso | `carlos.mendoza` | `Carlos.M2024!` |
| Supervisor | `luis.supervisor` | `Luis.S2024!` |

> **No usar** `patricia.cliente` (Atención al cliente) ni `ana.rodriguez` (Funcionario) para esta demo: no ven el menú **Analítica Inteligente**.

---

## Ruta de demo (UI)

### 1. Acceso y permiso

1. Login como `luis.supervisor` / `Luis.S2024!`.
2. Verificar que en el menú lateral aparece **Analítica Inteligente** (icono analytics).
3. **Mencionar:** requiere permiso `INTELLIGENT_ANALYTICS_VIEW`; Atención al cliente no lo tiene por diseño actual.

**Frase clave:** *“Solo roles de supervisión y diseño de proceso acceden a la analítica inteligente.”*

---

### 2. CU24 — Reporte dinámico por texto

1. Ir a **Analítica Inteligente** (`/intelligent-analytics`).
2. En la caja de consulta escribir, por ejemplo:
   - *Muéstrame los trámites de mayo*
   - *Cuál es la política más usada*
   - *Qué funcionario tiene más carga*
   - *Cuáles trámites están demorados*
   - *Genera resumen de trámites finalizados*
3. Opcional: aplicar filtros (fecha desde/hasta, política, estado).
4. Clic en **Generar análisis**.

**Mostrar en pantalla:**

- Título y explicación del reporte
- Tarjetas resumen (totales, política más usada, etc.)
- Tabla de datos con columnas sugeridas
- Gráfico de barras simple (si aplica)
- Formato sugerido: `PANTALLA`, `PDF` o `EXCEL` (solo sugerencia, sin descarga real)

**Frase clave:** *“El usuario pide el reporte en lenguaje natural; el sistema interpreta la intención y arma columnas y datos.”*

---

### 3. CU24 — Dictado por voz (opcional)

1. Clic en **Dictar por voz** (Chrome/Edge recomendado).
2. Decir: *“Cuáles trámites están demorados”*.
3. **Generar análisis** de nuevo.

**Mencionar:** usa Web Speech API del navegador; no requiere micrófono en el servidor.

---

### 4. CU25 — Riesgos y anomalías

Tras **Generar análisis**, bajar a la sección **Riesgos y anomalías**.

**Explicar qué detecta el motor:**

- Trámites demorados (SLA ~48 h sin avance)
- Tareas vencidas
- Funcionarios con sobrecarga (≥ 4 tareas activas)
- Cuellos de botella (desde `KpiService`)
- Políticas con más retrasos
- Anomalías por duración excesiva

**Frase clave:** *“Combina runtime de trámites, tareas, trazas y los mismos KPI que ya usa el supervisor.”*

---

### 5. CU26 — Recomendaciones

Sección **Recomendaciones** en la misma pantalla.

**Ejemplos de salida:**

- Priorizar trámite `TRM-xxx`
- Revisar actividad con cuello de botella
- Reasignar carga de un funcionario
- Política con riesgo alto de demora
- Siguiente acción sugerida en la ruta

**Frase clave:** *“Son acciones concretas ordenadas por prioridad, no solo gráficos.”*

---

### 6. Auditoría (opcional, 1 min)

1. Login como `sistema.admin`.
2. **Bitácora** → filtrar módulo **Analítica inteligente**.
3. Mostrar acciones:
   - `ANALYTICS_REPORT_REQUESTED`
   - `ANALYTICS_RISK_ANALYZED`
   - `ANALYTICS_RECOMMENDATION_GENERATED`

---

### 7. Fallback Java (opcional, impacto alto)

1. Detener `ai-service` (o no levantarlo).
2. Repetir consulta en UI.
3. En resultado, **Fuente** debe indicar **Motor local (Java)** y `source: LOCAL_FALLBACK` en la API.
4. Advertencia en pantalla: servicio IA no disponible.

**Frase clave:** *“Si FastAPI cae, Spring Boot sigue respondiendo con heurísticas locales.”*

---

## Probar con curl

### Obtener token

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"luis.supervisor\",\"password\":\"Luis.S2024!\"}"
```

Copiar el campo `token` de la respuesta.

### Ping (sin autenticación)

```bash
curl -s http://localhost:8080/api/intelligent-analytics/ping
```

Respuesta esperada: `intelligent-analytics-ok`

### CU24 — Reporte

```bash
curl -s -X POST http://localhost:8080/api/intelligent-analytics/report \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"Cuál es la política más usada\",\"fromDate\":\"2025-01-01\"}"
```

Campos útiles en la respuesta: `title`, `explanation`, `columns`, `rows`, `appliedFilters`, `suggestedFormat`, `source`, `cards`, `chart`.

### CU25 — Riesgos

```bash
curl -s -X POST http://localhost:8080/api/intelligent-analytics/risks \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"Analizar riesgos y cuellos de botella\"}"
```

Campos: `summary`, `risks[]` (`type`, `severity`, `title`, `description`), `cards`.

### CU26 — Recomendaciones

```bash
curl -s -X POST http://localhost:8080/api/intelligent-analytics/recommendations \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{}"
```

Campos: `summary`, `recommendations[]` (`priority`, `type`, `title`, `action`, `rationale`).

### FastAPI directo (si está levantado)

```bash
curl -s http://localhost:8000/analytics/health

curl -s -X POST http://localhost:8000/analytics/report \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"resumen de trámites\",\"tramiteSample\":[]}"
```

---

## Endpoints de referencia

| Método | Spring Boot | FastAPI |
|--------|-------------|---------|
| GET | `/api/intelligent-analytics/ping` | `/analytics/health` |
| POST | `/api/intelligent-analytics/report` | `/analytics/report` |
| POST | `/api/intelligent-analytics/risks` | `/analytics/risks` |
| POST | `/api/intelligent-analytics/recommendations` | `/analytics/recommendations` |

Configuración IA en backend: `ai.service.url=http://localhost:8000` (`application.properties`).

---

---

## FASE 2.4 — PWA y modo offline (CU27 / CU28)

> **Importante:** el service worker **solo funciona con build de producción** (`npm run build`), no con `ng serve`.

### Preparación PWA

```bash
cd frontend
npm run build
npx http-server dist/frontend/browser -p 4200 -c-1
```

Backend en `http://localhost:8080`. Login como `ana.rodriguez` / `Ana.R2024!` (funcionario de campo).

### 3. CU27 — PWA instalable

1. Abrir `http://localhost:4200` en Chrome o Edge.
2. Iniciar sesión.
3. Clic en **Instalar app** (barra superior) o icono ⊕ en la barra de direcciones.
4. Verificar ventana `standalone` y **barra inferior móvil**: Dashboard, Tareas, Trámites, Agente.
5. Redimensionar a &lt; 768px o usar DevTools móvil → menú hamburguesa lateral.

**Frase clave:** *“Es una PWA Angular: instalable en Android, Windows y tablet sin app nativa.”*

### 4. CU28 — Offline y sincronización

1. Con sesión activa, ir a **Mis actividades** (carga bandeja en `cached_activities`).
2. DevTools → **Network** → marcar **Offline**.
3. Verificar banner **Sin conexión** y contador de pendientes (0 inicialmente).
4. **Tomar una tarea** o **guardar borrador** de formulario → mensaje de guardado local.
5. Verificar contador: *"N elemento(s) pendiente(s) de sincronizar"*.
6. Quitar **Offline** → sincronización automática; banner **En línea**; contador vuelve a 0.
7. Opcional: Bitácora admin → eventos `OFFLINE_DATA_STORED` / `OFFLINE_SYNC_COMPLETED`.

**Frase clave:** *“Las operaciones van a IndexedDB en `pending_sync_queue` y se replican al backend cuando vuelve la red.”*

### IndexedDB (mencionar en defensa)

| Store | Uso |
|-------|-----|
| `pending_sync_queue` | Cola principal (`TAKE_TASK`, `FORM_DRAFT`, `COMPLETE_ACTIVITY`, `DOCUMENT_UPLOAD`) |
| `pending_forms` | Borradores pendientes |
| `pending_documents` | Archivos pendientes |
| `cached_activities` | Última bandeja consultada |

### Limitaciones CU27/CU28

| Tema | Estado actual |
|------|----------------|
| Service worker | Solo build producción, no `ng serve` |
| API REST | No cacheada por SW (origen distinto); offline vía IndexedDB |
| Agente / Analítica | Requieren conexión |
| JWT | No en IndexedDB; se valida expiración antes de sincronizar |
| Conflictos | Sin resolución automática en cola |
| Flutter nativo | No implementado (solo PWA) |

Guía detallada: [guia-instalacion-pwa.md](./guia-instalacion-pwa.md)

---

## Limitaciones a mencionar en la defensa (FASE 2.3)

| Tema | Estado actual |
|------|----------------|
| TensorFlow / ML profundo | No implementado; reglas y KPI |
| Export PDF / Excel / Word | Solo formato **sugerido** en UI |
| Dictado por voz | Depende del navegador (Web Speech API) |
| Atención al cliente | Sin permiso `INTELLIGENT_ANALYTICS_VIEW` |
| Predicción a largo plazo | Heurísticas + SLA, no modelo entrenado |

---

## Qué NO prometer (Ciclo 2 pendiente)

- Cliente Flutter nativo (la movilidad actual es PWA Angular)
- Exportación real de reportes a archivos
- Motor predictivo con TensorFlow
- Agente móvil dedicado para ciudadanos
- Resolución automática de conflictos offline

---

## Documentación relacionada

- Casos de uso CU24–CU28: [casos-uso.md](./casos-uso.md)
- Arquitectura y flujo fallback: [arquitectura.md](./arquitectura.md)
- Instalación PWA: [guia-instalacion-pwa.md](./guia-instalacion-pwa.md)
- README del repositorio: [../README.md](../README.md)
