# Configuración de producción

Este documento describe las variables de entorno que el backend exige en el
perfil `prod` y cómo suministrar los secretos de forma segura. La aplicación
se niega a arrancar si faltan las obligatorias (placeholders sin default en
`application-prod.yml` + `ProdSecretsValidator`).

## Variables obligatorias

| Variable | Uso |
|---|---|
| `JWT_SECRET` | Secreto de firma HS256. Mínimo 32 caracteres (64+ recomendado). |
| `ADMIN_INITIAL_PASSWORD` | Contraseña del primer usuario `admin`. Solo se consume si no existe ningún ADMIN; si falta, no se crea. |
| `DB_USER` | Usuario de PostgreSQL. |
| `DB_PASSWORD` | Contraseña de PostgreSQL. |
| `CORS_ALLOWED_ORIGINS` | Lista separada por comas de orígenes permitidos para navegador, p.ej. `https://app.midominio.com,https://admin.midominio.com`. No usar `*` ni localhost en producción. |
| `BREVO_API_KEY` | Clave de la API transaccional de Brevo (envío de códigos de verificación y recuperación de contraseña). |
| `BREVO_REMITENTE_EMAIL` | Remitente verificado en la cuenta de Brevo; si no está verificado la API responde 400 y el correo no sale. |

## Opcionales (tienen default razonable)

| Variable | Default | Nota |
|---|---|---|
| `DB_HOST` | `localhost` | Suele apuntar al nombre del servicio del proxy/compose. |
| `DB_PORT` | `5432` | |
| `DB_NAME` | — | Sin default: defínala igual que el POSTGRES_DB creado. |
| `JWT_EXPIRATION_MS` | `3600000` | Access token: 1 hora. |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | Refresh token: 7 días. |
| `BREVO_REMITENTE_NOMBRE` | `Estación Burger` | Nombre visible del remitente y de la marca en el correo. |
| `BREVO_TIMEOUT` | `5s` | Timeout de conexión y de la petición a Brevo. |

## Cómo suministrar los secretos

Nunca se commitean valores de `JWT_SECRET`, `ADMIN_INITIAL_PASSWORD` ni
`DB_PASSWORD` al repositorio. Opciones según el despliegue:

1. **Archivo `.env` fuera del repo** (no commiteado, `chmod 600`), cargado por
   el proceso (systemd `EnvironmentFile=`, compose `env_file:`).
   ```ini
   JWT_SECRET=...    # generar con: openssl rand -base64 48
   ADMIN_INITIAL_PASSWORD=...
   DB_USER=...
   DB_PASSWORD=...
   CORS_ALLOWED_ORIGINS=https://app.midominio.com
   BREVO_API_KEY=...
   BREVO_REMITENTE_EMAIL=no-reply@midominio.com
   ```
2. **Secretos de orquestación**: Docker Secrets o el secret manager del
   proveedor (AWS Secrets Manager, Vault, etc.); la app lee variables de entorno,
   así que el encargado de orquestar es quien inyecta el valor.
3. **Rotación del `JWT_SECRET`**: generar uno nuevo (`openssl rand -base64 48`),
   desplegar la instancia con el nuevo valor para emitir/validar y revocar las
   sesiones activas (consulte el flujo de `sesion_version`): al cambiar la clave
   los tokens firmados con la anterior quedan inválidos, así que la rotación debe
   programarse en una ventana en la que se pueda forzar re-login.

## Correo transaccional (Brevo)

El envío de códigos de verificación y de recuperación de contraseña usa la API
transaccional de Brevo. En el perfil `prod` el correo está **siempre encendido**:
sin `BREVO_API_KEY` o `BREVO_REMITENTE_EMAIL` la aplicación no arranca.

- El remitente debe estar **verificado** en la cuenta de Brevo; si no, la API
  responde `400`, el fallo se registra sin el código y la cuenta se crea igual
  (el usuario puede pedir el reenvío).
- Las plantillas viven en el repositorio (`backend/src/main/resources/correo/`),
  así que un cambio de texto se revisa en git y no depende del panel de Brevo.
- El correo se envía **después de confirmar la transacción**: una operación
  revertida no genera correo, y un proveedor caído no revierte una operación ya
  confirmada.

## TLS, proxy y WebSocket

- La app se despliega **siempre detrás de un proxy TLS** (Caddy, nginx,
  Traefik, ALB + ACM, etc.). El proxy termina HTTPS y reenvía hacia el backend
  con `X-Forwarded-*`; el backend activa `server.forward-headers-strategy` para
  construir URLs/`wss` correctos y emite HSTS. Nunca publiques el backend
  directamente en un puerto abierto.
- Los orígenes del WebSocket `/ws` (handshake STOMP) usan la **misma lista**
  `CORS_ALLOWED_ORIGINS` que el REST; en producción es obligatoria y no se
  acepta `*`. El proxy debe reenviar las cabeceras de upgrade
  (`Upgrade`/`Connection`) y soportar `wss`.
- La duración del access token la define `JWT_EXPIRATION_MS` en el despliegue
  (default 1 hora). `docker-compose.yml` solo levanta PostgreSQL; el backend no
  se ejecuta en Compose, de modo que los valores los inyecta el orquestador del
  proxy (systemd/`EnvironmentFile`, K8s `Secrets`, etc.).

## Arranque

```bash
SPRING_PROFILES_ACTIVE=prod java -jar restaurante-backend.jar
```

Si falta una variable obligatoria, el arranque falla indicando cuál falta.
La documentación OpenAPI/Swagger está desactivada en este perfil.