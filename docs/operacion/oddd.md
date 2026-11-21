# ODDD — Definición operativa

En este repositorio se usa ODDD como documento de diseño/definición operativa; no se encontró una expansión oficial de la sigla.

## Componentes en ejecución

Docker Compose levanta backend Spring y web Next.js. PostgreSQL es externo y se configura con `DB_*`. La aplicación móvil se distribuye/ejecuta aparte mediante Expo. Ver [guía de producción](../produccion.md).

## Inicio

1. Crear una base PostgreSQL accesible y un usuario con permisos para migraciones.
2. Configurar `backend/.env` según `backend/.env.example`, incluyendo credenciales, `DB_HOST` y `DB_SSLMODE`.
3. Copiar las plantillas de cada componente a `backend/.env`, `frontend/.env` y `mobile/.env` según corresponda. Iniciar el entorno web y API con `docker compose --env-file backend/.env --env-file frontend/.env up --build -d` desde la raíz. La web se publica en `http://localhost:7070` (puerto interno 3000) y la API en 8080. Cambiar `FRONTEND_PORT` en `frontend/.env` o `BACKEND_PORT` en `backend/.env` según se necesite. Para producción, cargar `backend/.env` con `--env-file` y usar `docker-compose.prod.yml`.
4. En Compose de desarrollo, Next.js corre en modo `dev` con el código de `frontend/` montado como volumen; los cambios de archivos se recargan automáticamente. Las dependencias y la caché `.next` usan volúmenes Docker. Tras cambiar dependencias, ejecuta `docker compose exec frontend npm ci` y recrea el frontend. Next.js proxea `/api/*` al servicio backend; Flyway aplica migraciones al iniciar backend.

## Migraciones y archivos multimedia

Las migraciones Flyway que ya se aplicaron son inmutables: los cambios nuevos
deben usar una versión posterior y no reutilizar números existentes. Si el
checksum de una migración aplicada cambia, Flyway detiene el backend hasta
restaurar la migración original y ordenar las nuevas versiones.
No renumeres migraciones para insertar una funcionalidad entre versiones
existentes: asígnale el siguiente número libre y conserva los nombres y el
contenido de las migraciones ya publicadas.

Para que el catálogo permita subir imágenes o videos, configura en
`backend/.env` `APP_MEDIA_UPLOAD_URL`, `APP_MEDIA_PUBLIC_URL`,
`APP_MEDIA_USUARIO` y `APP_MEDIA_CLAVE`. El backend realiza la carga WebDAV; las
credenciales no se configuran en el frontend ni en la app móvil. Configura
`ARCHIVOS_LECTURA_URL` para conservar la lectura de archivos vinculados en el
almacenamiento SFTP anterior. El destino WebDAV debe ser alcanzable desde el
contenedor backend; la URL pública también debe ser alcanzable por el navegador.

### Migraciones recientes

Flyway las aplica solas al arrancar el backend. Antes de aplicarlas en una base con datos, haz un
respaldo (`database/backup.sh`) y, si puedes, pruébalas primero en una copia
(`database/restore.sh <dump> restaurante_prueba` y arranca el backend con `DB_NAME=restaurante_prueba`).

| Versión | Qué hace |
|---|---|
| `V21__zonas_de_mesas.sql` | Crea `catalogo.zonas`, convierte cada `mesas.ubicacion` en una zona y **elimina esa columna**. |
| `V22__rol_cocina_y_permisos_operacion.sql` | Crea el rol `COCINA` y concede permisos de operación a `COCINA` y `MESERO`. |
| `V23__mesas_ocupadas_backfill.sql` | Marca `OCUPADA` las mesas que ya tenían una cuenta abierta. |

Una migración ya aplicada no se edita: si hay que corregirla, se restaura la base desde el respaldo o se
crea una versión nueva.

## Observabilidad y tareas

Compose define healthchecks HTTP para backend y web. El backend expone Actuator según perfiles; producción documenta health/probes. Los scripts `database/backup.sh` y `restore.sh` requieren cliente PostgreSQL instalado y usan la configuración de entorno.

## Recuperación y operación pendiente

No se encontró procedimiento automatizado de respaldo remoto, RPO/RTO, rotación de secretos, alertas ni política de actualización/rollback. Acordarlos con operaciones antes de considerar el runbook completo.
