# Manual de instalación — Ciclo 1

## 1. Requisitos

| Componente | Versión recomendada |
|------------|---------------------|
| Java JDK | 17+ |
| Maven | 3.9+ |
| Node.js | 20 LTS |
| npm | 10+ |
| MongoDB | 6.x / 7.x (o imagen Docker) |
| Python | 3.11+ (solo para `ai-service`) |
| Docker + Docker Compose | Opcional, para despliegue integrado |
| Git | Para clonar el repositorio |

Opcional para IA con Gemini:

- Cuenta Google AI / API key en `ai-service/.env` (`AI_API_KEY`)

---

## 2. Clonar el proyecto

```bash
git clone <url-del-repositorio>
cd sistema-workflow-politicas
```

---

## 3. MongoDB

### Opción A — Docker (recomendado)

```bash
docker compose up -d mongodb
```

- Puerto: `27017`
- Base de datos: `workflow_db`
- Script init: `database/mongo-init.js` (colecciones)

### Opción B — Instalación local

1. Instalar MongoDB Community.
2. Iniciar el servicio `mongod`.
3. URI por defecto del backend: `mongodb://localhost:27017/workflow_db`

---

## 4. Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

- API: `http://localhost:8080`
- Al arrancar, `DatabaseSeeder` carga roles, departamentos, usuarios demo y política de ejemplo (*Solicitud de instalación de medidor*) si la base está vacía.
- Configuración: `backend/src/main/resources/application.properties`

Variables relevantes:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/workflow_db
server.port=8080
ai.service.url=http://localhost:8000
```

---

## 5. Frontend (Angular)

```bash
cd frontend
npm install
npm start
```

- UI: `http://localhost:4200`
- API configurada en `frontend/src/environments/environment.ts` → `http://localhost:8080`

Build producción:

```bash
npm run build
```

Salida: `frontend/dist/frontend`

---

## 6. ai-service (FastAPI + Gemini)

```bash
cd ai-service
python -m venv .venv
# Windows:
.venv\Scripts\activate
# Linux/macOS:
# source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
# Editar .env y definir AI_API_KEY
uvicorn main:app --reload --port 8000
```

- Swagger: `http://localhost:8000/docs`
- Sin `AI_API_KEY`: el sistema usa **fallbacks** locales (diseño y formularios siguen funcionando con capacidad limitada).

---

## 7. Docker Compose (stack completo)

Desde la raíz del proyecto:

```bash
docker compose up --build
```

| Servicio | Puerto |
|----------|--------|
| mongodb | 27017 |
| backend | 8080 |
| frontend | 80 |
| ai-service | 8000 |

Ajustar CORS/proxy del frontend en Docker según despliegue (desarrollo local suele usar `npm start` + backend en 8080).

---

## 8. Usuarios demo (seed)

Contraseñas definidas en `Phase1BootstrapData` (cargadas por `DatabaseSeeder`):

| Usuario | Contraseña | Rol | Uso en demo |
|---------|------------|-----|-------------|
| `sistema.admin` | `Admin.Sistema2024!` | Administrador del sistema | Diseño, políticas, monitor, KPI, admin |
| `carlos.mendoza` | `Carlos.M2024!` | Dueño de proceso | Diseño de políticas y workflow |
| `ana.rodriguez` | `Ana.R2024!` | Funcionario | Bandeja, formularios, ejecución |

Política seed típica: **Solicitud de instalación de medidor** (workflow y formularios precargados tras migraciones Phase 2–4).

---

## 9. Verificación rápida

1. `GET http://localhost:8080/api/auth/login` — no aplica; usar login desde UI.
2. Login `sistema.admin` → debe cargar menú con Políticas y Diseñador.
3. `GET http://localhost:8080/api/policies` (con JWT) — lista políticas.
4. `GET http://localhost:8000/docs` — ai-service activo.

---

## 10. Problemas frecuentes

| Síntoma | Solución |
|---------|----------|
| 401 en API | Verificar token; volver a login |
| 403 en diseñador | Usuario necesita `WORKFLOW_DESIGN` o rol Dueño/Admin |
| MongoDB connection refused | Levantar Mongo o corregir URI |
| IA siempre en fallback | Revisar `AI_API_KEY` y logs de `ai-service` |
| Bandeja vacía | Comprobar responsable de actividad vs. usuario/rol/departamento |

---

## 11. Documentación relacionada

- [guia-demo-ciclo1.md](./guia-demo-ciclo1.md) — defensa y demo
- [arquitectura.md](./arquitectura.md) — capas y modelo
- Guías por fase: `f1-motor-workflow-pruebas.md` … `f8-colaboracion-pruebas.md`
