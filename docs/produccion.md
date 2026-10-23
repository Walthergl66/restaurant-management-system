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

## Opcionales (tienen default razonable)

| Variable | Default | Nota |
|---|---|---|
| `DB_HOST` | `localhost` | Suele apuntar al nombre del servicio del proxy/compose. |
| `DB_PORT` | `5432` | |
| `DB_NAME` | — | Sin default: defínala igual que el POSTGRES_DB creado. |
| `JWT_EXPIRATION_MS` | `3600000` | Access token: 1 hora. |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | Refresh token: 7 días. |

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
   ```
2. **Secretos de orquestación**: Docker Secrets o el secret manager del
   proveedor (AWS Secrets Manager, Vault, etc.); la app lee variables de entorno,
   así que el encargado de orquestar es quien inyecta el valor.
3. **Rotación del `JWT_SECRET`**: generar uno nuevo (`openssl rand -base64 48`),
   desplegar la instancia con el nuevo valor para emitir/validar y revocar las
   sesiones activas (consulte el flujo de `sesion_version`): al cambiar la clave
   los tokens firmados con la anterior quedan inválidos, así que la rotación debe
   programarse en una ventana en la que se pueda forzar re-login.

## Correo transaccional — RF-45 (API de Resend)

El envío del código de verificación está apagado por defecto y se activa por
variable de entorno. La capa de aplicación habla con el puerto `EmisorCorreo`;
el adaptador de API solo se monta si `MAIL_ENABLED=true` (el otro adaptador,
el de log, es el defecto). **Sin configurar nada, nada cambia.**

| Variable | Default | Uso |
|---|---|---|
| `MAIL_ENABLED` | `false` | Interruptor maestro. `true` = envío real; `false` = el código sale por el log del servidor. |
| `RESEND_API_KEY` | — | Clave API de Resend (`re_...`), generada en el panel (API Keys) y visible solo al crearla. Secreta. |
| `MAIL_FROM` | — | Remitente. Sin dominio verificado en Resend: `onboarding@resend.dev` (solo entrega al correo de la propia cuenta). |
| `MAIL_FROM_NAME` | — | Nombre visible del remitente. |
| `MAIL_CONNECT_TIMEOUT_MS` | `5000` | Timeout de conexión a la API. |
| `MAIL_READ_TIMEOUT_MS` | `10000` | Timeout de lectura de la API. |
| `MAIL_API_URL` | `https://api.resend.com` | Base URL de la API; solo se toca en pruebas o con proxy corporativo. |

Reglas:

- **Si `MAIL_ENABLED=true` en `prod`, `RESEND_API_KEY`, `MAIL_FROM` y
  `MAIL_FROM_NAME` son obligatorias**: `ProdSecretsValidator` detiene el
  arranque si falta alguna (no existe envío encendido a medias).
- **Apagado de emergencia:** `MAIL_ENABLED=false` + reinicio ⇒ el sistema
  vuelve al modo log sin tocar código.
- **Rotación de la clave:** crear una clave nueva en el panel de Resend,
  sustituir `RESEND_API_KEY` en el entorno del servidor y reiniciar. No toca
  el código. Recomendado cada 90–180 días o ante cualquier baja/sospecha.
- **Deliverability:** Resend exige dominio propio verificado (DKIM/SPF) para
  enviar a terceros; mientras tanto solo se entrega al correo de la cuenta. Al
  verificar un dominio basta con cambiar `MAIL_FROM`.
- El código de 6 dígitos **nunca se registra** en logs ni en la auditoría
  (RNF-10); solo el evento de envío o de fallo.

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