# Arquitectura — Login del frontend embebido en Telegram Mini App (TMA)

> Estado: **propuesta para implementación**
> Branch del bot: `dev`
> Alcance de este documento: diseño end-to-end + cambios concretos a delegar por equipo (Bot / Backend / Frontend).

---

## 1. Contexto y problema

El bot de Telegram permite hoy iniciar sesión con `/login <email> <password>`
(`commands/LoginCommand.java`). Esto tiene dos problemas:

1. **Las credenciales quedan en el historial del chat en texto plano**, visibles para
   siempre para cualquiera con acceso a ese chat/dispositivo.
2. **Rompe la coherencia visual** con el frontend: es un flujo de texto crudo, no la
   pantalla de login real del producto.

La solución es **embeber la pantalla de login del frontend (`LoginPage`) dentro de una
Telegram Mini App (TMA)**. El usuario tipea sus credenciales en un formulario HTML dentro
del webview de Telegram — eso **no es un mensaje de Telegram**, por lo que nunca queda en
el historial — y se reutiliza el diseño existente del producto.

---

## 2. Objetivos y no-objetivos

**Objetivos**
- Que las credenciales **nunca** queden en el historial del chat.
- Reutilizar la `LoginPage` del frontend (coherencia de diseño y de lógica de sesión).
- Mantener el contrato de sesión actual del frontend intacto (`localStorage.token` /
  `localStorage.currentUser`).
- No exponer la contraseña al servicio del bot.

**No-objetivos (por ahora)**
- Reemplazar el sistema de JWT del backend ni introducir refresh tokens.
- Migrar el almacenamiento de sesión a cookies `httpOnly`.
- Persistir el vínculo Telegram↔cuenta del lado del backend (queda en el bot, salvo que
  se tome la mejora opcional de Backend).

---

## 3. Decisiones de arquitectura

| Decisión | Resolución | Razón |
|---|---|---|
| **¿Dónde se valida `initData`?** | En el **bot** (`:8081`) | El HMAC requiere el `TELEGRAM_BOT_TOKEN`, y el bot es el **único** servicio que lo posee. El backend no lo conoce. |
| **¿Quién recibe email/password?** | El **backend**, directo desde el webview (`POST /api/auth/login`) | El bot **no debe ver la contraseña**. Es el mismo flujo que la web actual. |
| **¿Quién emite el JWT de sesión?** | El **backend** (sin cambios) | El bot no conoce `JWT_SECRET`, así que no puede emitir JWTs válidos. |
| **¿Se confía en `initDataUnsafe`?** | **Nunca** para seguridad | Llega sin firmar; es trivialmente falsificable. Solo *hint* de UI. |
| **¿Dónde vive el vínculo Telegram↔cuenta?** | En el `SessionStore` del bot (`sessions.json`) | Ya existe y persiste. Clave = `user.id` de Telegram (== `chatId` en chats privados). |
| **¿Dónde se guarda el JWT en el cliente?** | `localStorage` (`token` / `currentUser`) | Es el contrato que ya espera el frontend. No se rompe. |

---

## 4. Flujo end-to-end

La solución se entrega en **dos niveles**. El Nivel 1 ya cumple los objetivos; el Nivel 2
agrega identidad verificada y auto-login.

### Nivel 1 — MVP (credenciales fuera del chat + diseño reutilizado)

```
Usuario toca botón web_app (/login)
        │
        ▼
Telegram abre la LoginPage en el webview (inyecta initData firmado)
        │   (el usuario tipea credenciales DENTRO del webview, no en el chat)
        ▼
POST /api/auth/login  ──▶  BACKEND  ──▶  { token, user }     (el bot NO ve la password)
        │
        ▼
localStorage.token / currentUser  ──▶  logueado, igual que la web
```

### Nivel 2 — Recomendado (identidad verificada + auto-login + sesión compartida con el bot)

```
Apertura de la TMA
   │  POST {BOT}/tma/verify { initData }      (axios SIN interceptores)
   ▼
 BOT valida HMAC + auth_date  ──▶  ¿hay Session vinculada para este user.id?
   ├─ SÍ  ─▶ { linked:true, token, userId } ─▶ FE hidrata y entra SIN credenciales
   └─ NO  ─▶ { linked:false }               ─▶ FE muestra la LoginPage normal
                                                   │ login OK contra el backend → { token, user }
                                                   ▼
                               POST {BOT}/tma/link { initData, token, userId }
                                                   ▼
                               BOT re-valida initData y guarda Session(userId, token)
                               ⇒ el bot conversacional (/catalogo, /collection, texto libre)
                                  comparte el login automáticamente
```

---

## 5. Componentes y responsabilidades

| Componente | Servicio | Rol en la solución |
|---|---|---|
| `LoginPage` (React) | Frontend | UI de login, ahora también renderizada dentro del webview |
| SDK `telegram-web-app.js` | Frontend | Provee `initData`, tema, viewport, `ready()`/`expand()` |
| `POST /api/auth/login` | Backend | Autenticación real; emite el JWT (sin cambios funcionales) |
| Botón `web_app` (`/login`) | Bot | Abre la Mini App apuntando a la URL del frontend |
| `InitDataValidator` | Bot | Valida HMAC de `initData` + frescura de `auth_date` |
| `TmaAuthController` (`/tma/*`) | Bot | `verify` (auto-login) y `link` (vincular sesión) |
| `SessionStore` | Bot | Persiste el vínculo `telegram user.id → Session(userId, token)` |

---

## 6. Cambios por equipo

### 6.1 BOT — `TP TACS 2026/` (este equipo, en scope)

Archivos nuevos (paquete `com.tacs.tp1c2026.tma`):
- `InitDataValidator.java` — valida el HMAC-SHA256 de `initData` (algoritmo oficial de
  Telegram) y la frescura de `auth_date` (TTL 15 min). Extrae el `user.id`.
- `TelegramUser.java` — record `{ long id, String rawUserJson }`.
- `InvalidInitDataException.java` — excepción → HTTP 401.
- `TmaAuthController.java` — `@RestController @RequestMapping("/tma")` con `POST /verify`
  y `POST /link`, CORS restringido al origen de la TMA.

Archivos modificados:
- `commands/LoginCommand.java` — **se reescribe**: en vez de pedir `email/password` por
  texto, responde con un teclado inline con un botón `web_app` que abre la Mini App.
  Deja de depender de `BackendApiClient` y `SessionStore`.
- `FiguritasBot.java` — actualizar los dos mensajes que dicen
  *"Usá /login &lt;email&gt; &lt;password&gt;"* (líneas ~104 y ~108) al nuevo flujo.
- `application.properties` — agregar:
  ```properties
  tma.url=${TMA_URL:https://localhost:5173/login}
  tma.allowed-origin=${TMA_ALLOWED_ORIGIN:https://localhost:5173}
  ```

Tests (patrón del proyecto, `src/test`):
- `InitDataValidator`: firma válida / inválida, `auth_date` expirado, `hash` ausente.
- `TmaAuthController`: `verify` linked / unlinked, `link` persiste la sesión, 401 ante
  `initData` inválido.

Variables de entorno nuevas: `TMA_URL`, `TMA_ALLOWED_ORIGIN`.

---

### 6.2 BACKEND — `tp1c2026/backend/` (DELEGAR al equipo de backend)

> El bot y el frontend funcionan en Nivel 1/2 **sin** estos cambios, pero los puntos 1 y 2
> son **recomendados para producción** por seguridad.

1. **[Recomendado] Reducir `JWT_EXPIRATION`.**
   Hoy el default es ~1 año (`31536000000` ms). El JWT vivirá en `localStorage` de un
   webview; un token de un año ahí es un riesgo alto. Bajarlo a horas.
   *Impacto:* al expirar, el usuario re-loguea (salvo que se implemente el punto 3).

2. **[Recomendado] Endurecer CORS.**
   `CorsConfig` permite `*` con credentials (ya hay un `TODO`). Fijar
   `CORS_ALLOWED_ORIGINS` al origen real de la Mini App (el dominio donde se sirve el
   frontend). El frontend habla directo al backend con `Authorization: Bearer`, así que
   idealmente **sin** `allowCredentials`.

3. **[Opcional] Endpoint de re-emisión sin contraseña** — `POST /api/auth/telegram`.
   Permitiría al bot (que ya verificó la identidad de Telegram por HMAC) pedir un JWT
   nuevo para un usuario **sin reenviar la contraseña**, usando un secreto compartido
   bot↔backend. Devolvería el mismo shape `{ token, user{...} }`.
   *Beneficio:* elimina el re-login con password cuando el JWT expira y cierra el problema
   del token de larga duración de forma limpia.

4. **[Opcional] Hidratación de usuario** — confirmar/exponer `GET /api/users/{id}`
   devolviendo el `UserDto` completo. Lo necesita el auto-login (Nivel 2) para reconstruir
   `currentUser` a partir del `userId`. Si no existe, el frontend caería a decodificar el
   JWT (datos mínimos: id, email, role).

5. **[Opcional] Vínculo autoritativo del lado servidor** — campo `telegramId` en `User`
   + endpoint para asociarlo. Hoy el vínculo vive **solo** en `sessions.json` del bot.

6. **[Confirmar] Contrato estable de login.**
   `POST /api/auth/login` debe seguir devolviendo `{ token, user{ id, name, email,
   rating, exchangesAmount, avatarId, creationDate } }`. El bot y el frontend dependen de
   ese shape exacto (campo del JWT = `token`).

---

### 6.3 FRONTEND — `tacs-2026-c1-FE/` (DELEGAR al equipo de frontend)

1. **Cargar el SDK de Telegram.**
   En `index.html`, antes de `/src/main.tsx`:
   ```html
   <script src="https://telegram.org/js/telegram-web-app.js"></script>
   ```
   y agregar `viewport-fit=cover` al meta viewport existente.

2. **Helper `src/app/utils/telegram.ts`** — `getTelegram()`, `isInsideTelegram()`
   (true si `initData` no está vacío), `initTelegram()` (`ready()`, `expand()`,
   `setHeaderColor('#4B2D7F')`, `disableVerticalSwipes()`). Llamar `initTelegram()` al boot.

3. **Cliente del bot `src/app/api/TelegramAuthService.ts`** — usar una instancia
   **`axios.create()` propia** (sin los interceptores globales), para que un 401 del bot
   **no** borre el token del backend ni redirija a `/login`. Expone:
   - `verifyTelegramSession(): { linked, token, userId }`
   - `linkTelegramSession(token, userId): void`

4. **Adaptar `pages/login/LoginPage.tsx`:**
   - Tras `login(user, token)`: si `isInsideTelegram()`, llamar
     `linkTelegramSession(token, user.id)` (best-effort, en try/catch).
   - **[Nivel 2]** En un `useEffect` al montar: si `isInsideTelegram()`, llamar
     `verifyTelegramSession()`; si `linked`, hidratar el usuario y navegar, salteando el
     formulario.

5. **Layout del webview.**
   En `LoginPage.styles.ts`, cambiar `min-height: 100vh` por
   `min-height: var(--tg-viewport-stable-height, 100vh)`.

6. **Env var nueva:** `VITE_BOT_BASE_URL` = URL pública HTTPS del bot (`:8081`).

> Restricción de scope del proyecto: estos cambios los implementa el equipo de frontend.
> El equipo del bot puede entregar los snippets listos para pegar, pero no edita
> `tacs-2026-c1-FE/`.

---

## 7. Contrato de los endpoints nuevos del bot

Base URL: `https://<host-del-bot>:8081`
CORS: solo el origen de la Mini App (`tma.allowed-origin`).

### `POST /tma/verify`
Verifica `initData` y, si hay sesión vinculada, devuelve el JWT.

Request:
```json
{ "initData": "<string crudo de Telegram.WebApp.initData>" }
```
Response `200`:
```json
{ "linked": true,  "token": "<jwt>", "userId": "<id mongo>" }
{ "linked": false, "token": null,    "userId": null }
```
Error `401`:
```json
{ "error": "invalid_init_data", "message": "firma inválida | initData expirado | ..." }
```

### `POST /tma/link`
Tras un login exitoso, vincula el JWT al usuario de Telegram.

Request:
```json
{ "initData": "<string crudo>", "token": "<jwt>", "userId": "<id mongo>" }
```
Response: `204 No Content`. Error: `401` (mismo shape que arriba).

### Algoritmo de validación de `initData` (referencia)
1. Parsear el query string; URL-decodificar cada valor.
2. `data_check_string` = todos los campos **excepto `hash`**, ordenados alfabéticamente
   por clave, en formato `key=value` unidos por `\n`.
3. `secret_key = HMAC_SHA256(key="WebAppData", msg=bot_token)`.
4. `hash_esperado = hex(HMAC_SHA256(key=secret_key, msg=data_check_string))`.
5. Comparar en tiempo constante con el `hash` recibido.
6. Validar `auth_date` (TTL corto, p. ej. 15 min) para evitar replays con datos viejos.

---

## 8. Seguridad

| # | Riesgo | Severidad | Mitigación | Responsable |
|---|---|---|---|---|
| 1 | Confiar en `initDataUnsafe` para auth | crítico | Solo *hint* de UI; auth solo con `initData` validado por HMAC | Bot + Front |
| 2 | Replay de `initData` | alto | TTL de `auth_date` (15 min) + HTTPS | Bot |
| 3 | JWT de ~1 año en `localStorage` (XSS, vida larga) | alto | Bajar `JWT_EXPIRATION`; CSP; limpiar en logout | **Backend** |
| 4 | CORS `*` con credentials | medio | Fijar `CORS_ALLOWED_ORIGINS` al origen de la TMA | **Backend** |
| 5 | Bot como intermediario de credenciales | evitado | El form pega directo al backend; el bot no ve la password | Diseño |
| 6 | Credenciales en historial del chat | **resuelto** | Se tipean en el webview, no como mensaje | Diseño |
| 7 | `/tma/link` confía en el token recibido | bajo | No escala privilegios; opcional: verificar el token contra el backend | Bot |

---

## 9. Configuración y despliegue

- **HTTPS obligatorio.** Telegram exige HTTPS para botones `web_app`, y el webview corre
  en el teléfono del usuario → `localhost` no resuelve. Los **tres** servicios (frontend,
  bot `:8081`, backend `:8080`) deben ser públicos por HTTPS.
- **Dev:** túneles (`cloudflared` / `ngrok`) para `5173`, `8081`, `8080`. Setear
  `TMA_URL`, `TMA_ALLOWED_ORIGIN`, `VITE_BOT_BASE_URL`, `VITE_API_BASE_URL` con esas URLs.
- **Entrypoints en Telegram:** botón inline del `/login` (incluido) y/o **Menu Button**
  persistente vía @BotFather → Bot Settings → Menu Button → URL (apuntando a `TMA_URL`).

Variables de entorno por servicio:

| Servicio | Variable | Ejemplo |
|---|---|---|
| Bot | `TMA_URL` | `https://tma.tudominio.com/login` |
| Bot | `TMA_ALLOWED_ORIGIN` | `https://tma.tudominio.com` |
| Frontend | `VITE_BOT_BASE_URL` | `https://bot.tudominio.com` |
| Frontend | `VITE_API_BASE_URL` | `https://api.tudominio.com` |
| Backend | `CORS_ALLOWED_ORIGINS` | `https://tma.tudominio.com` |
| Backend | `JWT_EXPIRATION` | `7200000` (2 h) |

---

## 10. Plan de trabajo

1. **Bot (este equipo):** `InitDataValidator` + `TmaAuthController` + reescribir
   `LoginCommand` + properties + tests. → entregable independiente y testeable.
2. **Frontend (delegado):** SDK + helper + `TelegramAuthService` + adaptación de
   `LoginPage` + layout del webview.
3. **Backend (delegado):** CORS + `JWT_EXPIRATION` (recomendados); endpoints opcionales
   según se priorice auto-login sin re-login.
4. **Integración:** túneles HTTPS, configurar `TMA_URL`/Menu Button, prueba end-to-end en
   un dispositivo real.

> Nivel 1 desbloquea los objetivos del producto con solo (1) + parte de (2). El Nivel 2
> (auto-login y sesión compartida con el bot) suma (1) completo + (2) completo y, para
> evitar re-logins al expirar, el punto 3 opcional del backend.
