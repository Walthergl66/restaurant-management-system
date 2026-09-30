#!/usr/bin/env bash
set -euo pipefail

# Backup de PostgreSQL para restaurante
# Uso: ./database/backup.sh [ruta_archivo_salida]
# Lee automáticamente las credenciales desde .env si existe en la raíz del proyecto.

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$RAIZ/.env}"

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

echo "[backup] $DATE — iniciando respaldo de la base '$DB_NAME'"

if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${DB_CONTAINER}$"; then
  echo "[backup] usando contenedor Docker activo: $DB_CONTAINER"
  docker exec -e PGPASSWORD="$PGPASSWORD" "$DB_CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" -F c > "$BACKUP_FILE"
else
  echo "[backup] realizando pg_dump directo a $DB_HOST:$DB_PORT ($DB_NAME)"
  pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -F c -f "$BACKUP_FILE"
fi

echo "[backup] respaldo completado exitosamente: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"
echo "[backup] validando cabecera del dump con pg_restore --list:"
pg_restore --list "$BACKUP_FILE" | head -n 15

echo "[backup] OK"
