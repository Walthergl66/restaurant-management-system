# ODDD — Definición operativa

En este repositorio se usa ODDD como documento de diseño/definición operativa; no se encontró una expansión oficial de la sigla.

## Componentes en ejecución

Docker Compose levanta backend Spring y web Next.js. PostgreSQL es externo y se configura con `DB_*`. La aplicación móvil se distribuye/ejecuta aparte mediante Expo. Ver [guía de producción](../produccion.md).

## Inicio

1. Crear una base PostgreSQL accesible y un usuario con permisos para migraciones.
2. Configurar `backend/.env` según `backend/.env.example`, incluyendo credenciales, `DB_HOST` y `DB_SSLMODE`.
3. Ejecutar `docker compose up --build -d` desde la raíz. La web se publica por defecto en 3000 y API en 8080; puertos configurables mediante `FRONTEND_PORT` y `BACKEND_PORT`.
4. Next.js proxea `/api/*` al servicio backend en la red Compose. Flyway aplica migraciones al iniciar backend.

## Observabilidad y tareas

Compose define healthchecks HTTP para backend y web. El backend expone Actuator según perfiles; producción documenta health/probes. Los scripts `database/backup.sh` y `restore.sh` requieren cliente PostgreSQL instalado y usan la configuración de entorno.

## Recuperación y operación pendiente

No se encontró procedimiento automatizado de respaldo remoto, RPO/RTO, rotación de secretos, alertas ni política de actualización/rollback. Acordarlos con operaciones antes de considerar el runbook completo.
