# TACS 2026 C1 — Telegram Bot (figuritas)

Bot de Telegram para el TP de **TACS 2026 — UTN FRBA** (intercambio de figuritas). Hace de fachada conversacional sobre el backend del proyecto: permite al usuario consultar el catálogo, gestionar su colección y marcar figuritas faltantes desde Telegram.

Forma parte del workspace TACS 2026 C1 junto al backend (`tp1c2026/`) y el frontend (`tacs-2026-c1-FE/`); este repositorio solo contiene el bot.

## Stack

- Java 21
- Spring Boot 3.5.14
- [`telegrambots-springboot-longpolling-starter`](https://github.com/rubenlagus/TelegramBots) 9.2.1
- `RestClient` de Spring para hablar con el backend
- Maven Wrapper (`./mvnw`)

## Prerequisitos

- JDK 21 (o usar el Dockerfile, que lo trae)
- El backend de TACS 2026 corriendo y accesible (por defecto en `http://localhost:8080`)
- Un token de bot de Telegram. Para obtenerlo: hablar con [`@BotFather`](https://t.me/BotFather) → `/newbot` → seguir el prompt → guardar el token y el username.

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | _(obligatoria)_ | Token devuelto por `@BotFather` |
| `TELEGRAM_BOT_USERNAME` | _(obligatoria)_ | Username del bot (sin `@`) |
| `BACKEND_URL` | `http://localhost:8080` | URL base del backend; el bot le agrega `/api` automáticamente |
| `CHATLINKS_FILE` | `./chat-links.json` | Ruta del archivo donde se persiste el mapeo `chatId → userId` |

Crear un archivo `.env` en la raíz del proyecto (ya está en `.gitignore`):

```
TELEGRAM_BOT_TOKEN=tu_token_aca
TELEGRAM_BOT_USERNAME=tu_bot_username
```

## Correr local

```bash
./mvnw spring-boot:run
```

El bot levanta en el puerto **8081** (no 8080 — ese lo usa el backend). El long-polling se conecta solo a la API de Telegram; no hay que exponer puertos para que reciba mensajes.

> Telegram solo permite **un** consumer por bot. Si ya tenés el bot corriendo en Docker, no lo levantes simultáneamente con IntelliJ — chocan.

## Correr con Docker

El `docker-compose.yml` del bot se conecta a la red externa del backend (`backend_tacs-network`), así habla con `http://backend:8080` y comparte la misma Mongo.

1. Primero levantar el backend (desde `tp1c2026/backend/`):
   ```bash
   docker compose up -d
   ```
2. Después el bot (desde este directorio):
   ```bash
   docker compose up --build -d
   ```

`./bot-data/chat-links.json` se monta como volumen para que el mapeo `chatId → userId` sobreviva a reinicios.

## Comandos disponibles

| Comando | Descripción |
|---|---|
| `/start` | Mensaje de bienvenida |
| `/help` | Lista de comandos |
| `/yosoy <userId>` | Asocia este chat a un usuario del backend (stub de identidad — ver más abajo) |
| `/olvidame` | Desasocia el chat |
| `/catalogo [pág]` | Catálogo paginado (10 por página) |
| `/figurita <id>` | Detalle de una figurita |
| `/coleccion` | Mi colección |
| `/agregar <cardId>` | Agrega/incrementa una figurita en mi colección |
| `/quitar <cardId>` | Decrementa una figurita en mi colección |
| `/faltantes` | Mis figuritas faltantes |
| `/agregarFaltante <cardId>` | Marca una figurita como faltante |

## Estructura del proyecto

```
src/main/java/com/tacs/tp1c2026/
├── chatlink/        ChatLinkStore — persistencia chatId → userId (JSON local)
├── client/          BackendApiClient + BackendDataMapper + BackendApiException
│   └── wire/        DTOs que matchean el JSON del backend
├── commands/        CommandHandler interface + CommandDispatcher + 11 handlers
├── dtos/            DTOs de dominio del bot (Card, User, CollectionCard, MissingCard)
├── BackendConfig    Bean RestClient con statusHandler global de errores
└── FiguritasBot     Long-polling consumer
```

## Manejo de errores

El backend devuelve un objeto `ApiError { status, error, message }` en respuestas 4xx/5xx. El bot lo parsea en `BackendConfig` (vía `defaultStatusHandler` del `RestClient`) y lo envuelve en `BackendApiException`.

Política de respuestas al usuario (alineada con el patrón del frontend):

- **400 / 404 / 409** (errores esperados): se muestra el `message` del backend tal cual, salvo que el handler tenga un mensaje UX más útil para ese código (por ej. "No existe una figurita con id X" para 404).
- **Otros 4xx y todos los 5xx**: se muestra un mensaje genérico ("No pude... Probá más tarde."). Los detalles van al log con nivel `error`.

## Identidad

El comando `/yosoy <userId>` es un **stub temporal**: el bot escribe el mapeo `chatId → userId` en `chat-links.json` localmente y lo usa para los comandos que requieren usuario. Cuando se integre el flujo de autenticación real del backend (JWT — ya disponible), se reemplazará por `/login email password` con manejo de tokens.

## Tests

```bash
./mvnw test
```
