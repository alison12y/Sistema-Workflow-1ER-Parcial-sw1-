# Guía de instalación PWA — CU27 / CU28

Progressive Web App del sistema Workflow para **Android**, **Windows** y **tablets**.

---

## Requisitos

- Backend en `http://localhost:8080` (o URL de producción con HTTPS)
- Frontend compilado en **modo producción** (el service worker solo se activa fuera de `ng serve` dev)
- Navegador compatible: Chrome/Edge (recomendado), Firefox, Safari iOS 16.4+

---

## Compilar e instalar (desarrollo local)

```bash
cd frontend
npm install --legacy-peer-deps
npm run build
npx http-server dist/frontend/browser -p 4200 -c-1
```

> En desarrollo (`ng serve`) la PWA y el service worker **no** están habilitados (`isDevMode()`).

Abrir `http://localhost:4200`, iniciar sesión y usar el botón **Instalar app** en la barra superior (si el navegador lo ofrece).

### Android (Chrome)

1. Abrir la URL en Chrome.
2. Menú ⋮ → **Instalar aplicación** / **Añadir a pantalla de inicio**.
3. Confirmar. El icono aparece como app independiente.

### Windows (Edge / Chrome)

1. Abrir la URL en Edge o Chrome.
2. Icono de instalación en la barra de direcciones (⊕) o botón **Instalar app**.
3. La app se abre en ventana propia (modo `standalone`).

### Tablet (iPad / Android tablet)

- Mismo flujo que móvil.
- En Safari iOS: **Compartir** → **Añadir a pantalla de inicio**.

---

## Funciones móviles (CU27)

| Vista | Ruta | Acceso móvil |
|-------|------|----------------|
| Dashboard | `/dashboard` | Barra inferior + menú |
| Mis tareas | `/mis-actividades` | Barra inferior |
| Trámites | `/tramites` | Barra inferior |
| Documentos | `/tramites/:id` (pestaña) | Desde trámite |
| Agente Inteligente | `/smart-agent` | Barra inferior (con permiso) |

- Menú lateral deslizable (hamburguesa) en pantallas &lt; 768px
- Indicador **En línea** / **Sin conexión** en la parte superior
- Contador: *"N elementos pendientes de sincronizar"*

---

## Modo offline (CU28)

### Qué se guarda en IndexedDB

| Store | Contenido |
|-------|-----------|
| `pending_sync_queue` | Cola de operaciones a sincronizar |
| `pending_forms` | Borradores y completados pendientes |
| `pending_documents` | Archivos pendientes de subir |
| `cached_activities` | Última bandeja de tareas consultada |

### Operaciones en cola

- Tomar tarea (`TAKE_TASK`)
- Guardar borrador de formulario (`FORM_DRAFT`)
- Completar actividad (`COMPLETE_ACTIVITY`)
- Subir documento (`DOCUMENT_UPLOAD`)

Al reconectar, `OfflineSyncService` sincroniza automáticamente y registra en bitácora:
- `OFFLINE_DATA_STORED`
- `OFFLINE_SYNC_COMPLETED`

### Seguridad JWT

- El token **no** se almacena en IndexedDB.
- Se valida expiración del JWT en `localStorage` antes de sincronizar.
- Si el token expiró, se redirige a login (no se sincroniza con sesión inválida).

---

## Probar offline

1. Instalar o abrir la app en producción build.
2. Iniciar sesión como `ana.rodriguez` / `Ana.R2024!`.
3. Ir a **Mis tareas** → tomar una tarea (con conexión).
4. En DevTools → **Network** → marcar **Offline**.
5. Completar formulario o guardar borrador → mensaje de guardado local.
6. Ver banner: *"N elemento(s) pendiente(s) de sincronizar"*.
7. Quitar **Offline** → sincronización automática.

### API de auditoría offline

```bash
curl -X POST http://localhost:8080/api/offline/notify-stored \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"pendingCount":2,"types":["FORM_DRAFT","TAKE_TASK"]}'
```

---

## Archivos clave

| Archivo | Rol |
|---------|-----|
| `frontend/ngsw-config.json` | Configuración service worker |
| `frontend/public/manifest.webmanifest` | Manifest PWA |
| `frontend/src/app/core/offline/offline-sync.service.ts` | Cola y sincronización |
| `frontend/src/app/core/offline/offline-db.service.ts` | IndexedDB |
| `backend/.../OfflineController.java` | Auditoría offline |

---

## Limitaciones conocidas

- **Service worker** solo en build de producción.
- **API REST** no se cachea por el SW (origen distinto al frontend); el offline usa IndexedDB + cola.
- **Agente inteligente** y **analítica** requieren conexión (IA en servidor).
- **Dictado por voz** depende del navegador y suele requerir red.
- Iconos PWA en SVG; para Android antiguo conviene agregar PNG 192/512 en `public/icons/`.
- Sincronización **sin resolución automática de conflictos** (última operación en cola gana).
- Trámites nuevos o diseño workflow no están en alcance offline.
