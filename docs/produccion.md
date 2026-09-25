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

## Arranque

```bash
SPRING_PROFILES_ACTIVE=prod java -jar restaurante-backend.jar
```

Si falta una variable obligatoria, el arranque falla indicando cuál falta.
La documentación OpenAPI/Swagger está desactivada en este perfil.