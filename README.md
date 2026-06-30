# TACS 2026 C1 — Telegram Bot (figuritas)

Bot de Telegram para el TP de **TACS 2026 — UTN FRBA** (intercambio de figuritas). Hace de fachada conversacional sobre el backend del proyecto: permite al usuario consultar el catálogo, gestionar su colección y marcar figuritas faltantes desde Telegram.

Soporta dos modos de uso en simultáneo: **comandos** clásicos (`/coleccion`, `/agregar`, …) y **lenguaje natural** mediante un agente de IA (Gemini) que interpreta la intención y dispara las herramientas internas (tool-calling).

Forma parte del workspace TACS 2026 C1 junto al backend (`tp1c2026/`) y el frontend (`tacs-2026-c1-FE/`); este repositorio solo contiene el bot.

## Stack

- Java 21
- Spring Boot 3.5.14
- [`telegrambots-client`](https://github.com/rubenlagus/TelegramBots) 9.2.1 (recepción de updates por **webhook**, con un `@RestController` propio)
- `RestClient` de Spring para hablar con el backend
- [Spring AI](https://docs.spring.io/spring-ai/reference/) 1.1.7 con el modelo **Google GenAI (Gemini)** para el modo conversacional
- Maven Wrapper (`./mvnw`)

## Prerequisitos

- JDK 21 (o usar el Dockerfile, que lo trae)
- El backend de TACS 2026 corriendo y accesible (por defecto en `http://localhost:8080`)
- Un token de bot de Telegram. Para obtenerlo: hablar con [`@BotFather`](https://t.me/BotFather) → `/newbot` → seguir el prompt → guardar el token y el username.
- Una API key de Gemini (Google AI Studio): https://aistudio.google.com/apikey → guardarla en `GEMINI_API_KEY`. Sin esta key el bot arranca pero el modo conversacional (texto libre) falla; los comandos `/...` siguen funcionando.

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | _(obligatoria)_ | Token devuelto por `@BotFather` |
| `TELEGRAM_BOT_USERNAME` | _(obligatoria)_ | Username del bot (sin `@`) |
| `BACKEND_URL` | `http://localhost:8080` | URL base del backend; el bot le agrega `/api` automáticamente |
| `SESSIONS_FILE` | `./sessions.json` | Ruta del archivo donde se persisten las sesiones (`chatId → {userId, token}`) |
| `GEMINI_API_KEY` | _(obligatoria para el modo conversacional)_ | API key de Google AI Studio para Gemini |
| `WEBHOOK_URL` | _(vacío)_ | URL pública HTTPS por la que Telegram alcanza al bot. Si está vacía, el bot arranca pero **no recibe mensajes**. En Render se toma solo de `RENDER_EXTERNAL_URL`. |
| `WEBHOOK_SECRET` | _(vacío)_ | Secret token que Telegram reenvía en el header `X-Telegram-Bot-Api-Secret-Token` (solo `A-Z a-z 0-9 _ -`). Si está vacío el endpoint `/webhook` **no se autentica** (cualquiera podría postear updates falsos): en producción es **obligatorio**. |
| `PORT` | `8081` | Puerto HTTP. En la nube lo inyecta la plataforma (Render usa `PORT`). |

El modelo de Gemini se elige en `application.properties` (`spring.ai.google.genai.chat.options.model`, por defecto `gemini-2.5-flash-lite`); se puede cambiar sin recompilar.

Crear un archivo `.env` en la raíz del proyecto (ya está en `.gitignore`):

```
TELEGRAM_BOT_TOKEN=tu_token_aca
TELEGRAM_BOT_USERNAME=tu_bot_username
GEMINI_API_KEY=tu_api_key_de_gemini
```

## Correr local

```bash
./mvnw spring-boot:run
```

El bot levanta en el puerto **8081** (no 8080 — ese lo usa el backend). Como recibe los updates por **webhook**, Telegram necesita una **URL pública HTTPS** para alcanzarlo; en local eso se logra con un túnel:

```bash
# en otra terminal, exponé el 8081 (ejemplo con ngrok)
ngrok http 8081
# tomá la URL https que te da y arrancá el bot con:
#   WEBHOOK_URL=https://xxxx.ngrok-free.app  (y opcional WEBHOOK_SECRET)
```

Si arrancás **sin** `WEBHOOK_URL`, el bot levanta igual pero no registra el webhook (no recibe mensajes) — útil para correr tests o tocar código sin túnel.

> ⚠️ `mvnw spring-boot:run` **no lee el `.env`** (eso lo hace Docker). Para correr local, las 3 variables tienen que estar en el entorno de la terminal. En PowerShell se pueden cargar desde el `.env`:
> ```powershell
> Get-Content .env | Where-Object { $_ -match '=' } | ForEach-Object {
>     $k,$v = $_ -split '=',2; Set-Item -Path "Env:$($k.Trim())" -Value $v.Trim()
> }
> .\mvnw.cmd spring-boot:run
> ```

> Telegram guarda **un solo** webhook por bot: el último `setWebhook` gana. Si levantás dos instancias (p. ej. local + la de la nube), la última en arrancar se queda con los mensajes. Para devolverle el webhook a la nube, reiniciá esa instancia (o corré `deleteWebhook` desde la API de Telegram).

## Correr con Docker

El `docker-compose.yml` del bot se conecta a la red externa del backend (`backend_tacs-network`), así habla con `http://backend:8080` y comparte la misma Mongo. El contenedor recibe `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME` y `GEMINI_API_KEY` desde el `.env` del bot.

### Todo junto (recomendado)

Hay dos scripts de PowerShell que levantan/bajan el stack completo (backend + Mongo + bot), corriendo cada `docker-compose.yml` en su propia carpeta —así cada uno usa **su** `.env` y los secretos del bot no se mezclan con los del backend:

```powershell
.\start-all.ps1   # levanta backend + mongo y despues el bot (con --build)
.\stop-all.ps1    # baja el bot y despues backend + mongo (sin borrar datos)
```

> Si PowerShell bloquea el script: `powershell -ExecutionPolicy Bypass -File .\start-all.ps1`.
> El backend **no** está en este repo; los scripts lo invocan en `../tp1c2026/backend` sin modificar nada de ahí.

### Manual (paso a paso)

1. Primero el backend (desde `tp1c2026/backend/`):
   ```bash
   docker compose up -d
   ```
2. Después el bot (desde este directorio):
   ```bash
   docker compose up --build -d
   ```

`./bot-data/sessions.json` se monta como volumen para que las sesiones sobrevivan a reinicios.

## Deploy en la nube (Render)

El bot recibe los updates por webhook, así que en la nube alcanza con exponer su puerto HTTP; no hay proceso de polling.

1. En Render: **New → Blueprint** apuntando a este repo (hay un `render.yaml` listo) — o **New → Web Service** con runtime Docker.
2. Render inyecta `PORT` (el bot escucha ahí) y `RENDER_EXTERNAL_URL` (la URL pública del servicio). El bot arma el webhook como `RENDER_EXTERNAL_URL` + `/webhook` automáticamente: **no hace falta setear `WEBHOOK_URL`**.
3. Cargá en el dashboard las variables `sync:false`: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`, `GEMINI_API_KEY` y **`WEBHOOK_SECRET`** (solo `A-Z a-z 0-9 _ -`; obligatorio en prod para que el endpoint no quede abierto).
4. Al arrancar, el log debe decir `Webhook registrado en https://<servicio>.onrender.com/webhook`. Si no, revisá que el token sea válido y que la URL sea HTTPS.

> En el plan free el servicio se duerme tras un rato sin tráfico; el primer mensaje lo despierta y puede demorar unos segundos (mismo comportamiento que el backend).

## Comandos disponibles

| Comando | Descripción |
|---|---|
| `/start` | Mensaje de bienvenida |
| `/help` | Lista de comandos |
| `/login <email> <password>` | Iniciá sesión con tu usuario del backend |
| `/olvidame` | Cierra tu sesión |
| `/catalogo [pág]` | Catálogo paginado (10 por página) |
| `/figurita <id>` | Detalle de una figurita |
| `/coleccion` | Mi colección |
| `/agregar <cardId>` | Agrega/incrementa una figurita en mi colección |
| `/quitar <cardId>` | Decrementa una figurita en mi colección |
| `/faltantes` | Mis figuritas faltantes |
| `/agregarFaltante <cardId>` | Marca una figurita como faltante |
| `/quitarFaltante <cardId>` | Quita una figurita de tus faltantes |
| `/publicar <cardId> <cantidad>` | Publica una figurita repetida para intercambio |
| `/publicaciones [pág]` | Publicaciones de intercambio activas (paginado) |
| `/subastas [pág]` | Subastas activas (paginado) |

Todos los comandos excepto `/start` y `/help` requieren sesión iniciada (los endpoints del backend exigen JWT). Si no hay sesión, el bot pide hacer `/login`.

## Modo conversacional (IA)

Además de los comandos, el bot entiende **lenguaje natural**. `FiguritasBot` rutea cada mensaje:

- empieza con `/` → va al `CommandDispatcher` (comandos de arriba, comportamiento determinístico).
- texto libre → va al `ConversationalAgent`, que usa Gemini para interpretar la intención y disparar las **herramientas** correspondientes.

Ejemplos: _"¿qué figuritas me faltan?"_, _"agregá la figurita 12 a mi colección"_, _"buscá figuritas de Argentina"_, _"¿tengo la número 5?"_.

Detalles de diseño:

- **Herramientas (`tools/FiguritasTools`):** una `@Tool` por operación (buscar en catálogo, ver figurita, ver/agregar/quitar colección, ver/marcar/quitar faltantes). Son las mismas operaciones que los comandos, expuestas al modelo.
- **Sesión segura:** el token nunca es un parámetro que el modelo controle. Se inyecta fuera de banda por `ToolContext` (resuelto desde el `SessionStore` por `chatId`). `login` **no** es una herramienta: la contraseña no pasa por el modelo, se hace con `/login`.
- **Memoria:** cada chat tiene su propia ventana de conversación (últimos 20 mensajes).
- **Confirmación:** antes de una acción destructiva (quitar de colección o faltantes), el agente confirma con el usuario.
- **Catálogo:** la búsqueda filtra por equipo/país/número (máx. 50 resultados) para no inflar tokens; para listar todo, el agente sugiere `/catalogo`.
- **Errores:** un 401 durante la charla limpia la sesión y pide re-login; el resto de los errores se le devuelven al modelo para que los explique en lenguaje natural.

Para ver en detalle qué herramientas dispara el agente, subir el log:

```
logging.level.org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor=DEBUG
```

## Estructura del proyecto

```
src/main/java/com/tacs/tp1c2026/
├── session/         Session + SessionStore — persistencia chatId → {userId, token} (JSON local)
├── client/          BackendApiClient + BackendDataMapper + BackendApiException
│   └── wire/        DTOs que matchean el JSON del backend
├── commands/        CommandHandler interface + CommandDispatcher + 12 handlers
├── tools/           FiguritasTools — @Tool que el agente dispara (envuelven BackendApiClient)
├── agent/           ConversationalAgent (ChatClient + Gemini + memoria) + FiguritasToolErrorProcessor
├── dtos/            DTOs de dominio del bot (Card, CollectionCard, MissingCard)
├── webhook/         TelegramWebhookController (recibe updates) + WebhookRegistrar (setWebhook al arrancar)
├── BackendConfig    Bean RestClient con statusHandler global de errores
├── TelegramConfig   Bean TelegramClient (envío) + executor single-thread para procesar updates
└── FiguritasBot     Rutea cada update: comandos vs lenguaje natural; envía/edita mensajes
```

## Manejo de errores

El backend devuelve un objeto `ApiError { status, error, message }` en respuestas 4xx/5xx. El bot lo parsea en `BackendConfig` (vía `defaultStatusHandler` del `RestClient`) y lo envuelve en `BackendApiException`.

Política de respuestas al usuario (alineada con el patrón del frontend):

- **401** (token inválido o expirado): el bot limpia la sesión local y le pide al usuario hacer `/login` de nuevo. Para `/login` específicamente, 401 significa "credenciales inválidas".
- **400 / 404 / 409** (errores esperados): se muestra el `message` del backend tal cual, salvo que el handler tenga un mensaje UX más útil para ese código (por ej. "No existe una figurita con id X" para 404).
- **Otros 4xx y todos los 5xx**: se muestra un mensaje genérico ("No pude... Probá más tarde."). Los detalles van al log con nivel `error`.

## Identidad

`/login <email> <password>` autentica contra `POST /api/auth/login` del backend y guarda `{userId, token}` por chat en `sessions.json`. El bot adjunta `Authorization: Bearer <token>` en cada request al backend y, si una respuesta vuelve 401 (token expirado o inválido), borra la sesión del chat y le pide al usuario que vuelva a entrar.

El bot **no expone registro**: los usuarios se crean desde el frontend (`POST /api/auth/register`). Si el `email` no existe, `/login` devuelve "Credenciales inválidas" igual que con una contraseña incorrecta.

## Tests

```bash
./mvnw test
```
