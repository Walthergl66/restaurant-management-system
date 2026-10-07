#!/usr/bin/env bash
set -euo pipefail

# Backup de PostgreSQL para restaurante
# Uso: ./database/backup.sh [ruta_archivo_salida]
# Credenciales: backend/.env (o ENV_FILE).
# Cliente pg: el contenedor `restaurante-postgres` si está corriendo; si no,
# el pg_dump del host. No requiere herramientas instaladas en el host.

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -z "${ENV_FILE:-}" ]]; then
  if [[ -f "$RAIZ/backend/.env" ]]; then
    ENV_FILE="$RAIZ/backend/.env"
  elif [[ -f "$RAIZ/.env" ]]; then
    ENV_FILE="$RAIZ/.env"
  else
    ENV_FILE="$RAIZ/backend/.env"
  fi
fi

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${BACKUP_DIR:-$RAIZ/database/backups}"
DB_NAME="${DB_NAME:-restaurante}"
BACKUP_FILE="${1:-$BACKUP_DIR/restaurante_${DB_NAME}_${DATE}.dump}"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-${DB_USERNAME:-restaurante}}"
export PGPASSWORD="${PGPASSWORD:-${DB_PASSWORD:-}}"
DB_CONTAINER="${DB_CONTAINER:-restaurante-postgres}"

mkdir -p "$BACKUP_DIR"

# Target local (el contenedor del compose) va por socket unix dentro del
# contenedor; cualquier otro host se pasa como TCP con las credenciales de .env.
REMOTO=()
case "$DB_HOST" in
  ""|localhost|127.0.0.1|postgres) ;;
  *) REMOTO=(-h "$DB_HOST" -p "$DB_PORT") ;;
esac

echo "[backup] $DATE — iniciando respaldo de la base '$DB_NAME'"

DESTINO="${DB_DESTINO:-local (contenedor ${DB_NAME}@${DB_HOST}:${DB_PORT})}"
echo "[backup] destino: $DESTINO"

if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${DB_CONTAINER}$"; then
  echo "[backup] usando cliente pg del contenedor: $DB_CONTAINER"
  docker exec -e PGPASSWORD="$PGPASSWORD" "$DB_CONTAINER" \
    pg_dump "${REMOTO[@]}" -U "$DB_USER" -d "$DB_NAME" -F c > "$BACKUP_FILE"
elif command -v pg_dump >/dev/null 2>&1; then
  echo "[backup] usando pg_dump del host contra $DB_HOST:$DB_PORT ($DB_NAME)"
  pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -F c -f "$BACKUP_FILE"
else
  echo "[backup] ERROR: no hay cliente pg." >&2
  echo "[backup]        Levantá el stack (docker compose up -d) o instalá postgresql-client." >&2
  exit 1
fi

echo "[backup] respaldo completado exitosamente: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"

# La validación también corre dentro del contenedor: el host no tiene por qué
# tener cliente pg. pg_restore no lee de stdin, así que el dump se copia al
# contenedor, se lista y se borra.
if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${DB_CONTAINER}$"; then
  echo "[backup] validando cabecera del dump con pg_restore --list (contenedor):"
  docker cp "$BACKUP_FILE" "$DB_CONTAINER":/tmp/restaurante_backup.dump
  docker exec "$DB_CONTAINER" pg_restore --list /tmp/restaurante_backup.dump | head -n 15
  docker exec "$DB_CONTAINER" rm -f /tmp/restaurante_backup.dump
else
  echo "[backup] validando cabecera del dump con pg_restore --list:"
  pg_restore --list "$BACKUP_FILE" | head -n 15
fi

echo "[backup] OK"