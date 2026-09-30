#!/usr/bin/env bash
set -euo pipefail

# Restauración de PostgreSQL para restaurante
# Uso: ./database/restore.sh <archivo.dump> [nombre_bd_destino]
# Lee automáticamente las credenciales desde .env si existe en la raíz del proyecto.
# CUIDADO: La base de datos destino se recrea y se descartan los datos actuales.

if [ $# -lt 1 ]; then
  echo "Uso: $0 <archivo.dump> [nombre_bd_destino]"
  echo "Ejemplo: $0 ./database/backups/restaurante_20260929_120000.dump"
  exit 1
fi

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$RAIZ/.env}"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

BACKUP_FILE="$1"
DB_NAME="${2:-${DB_NAME:-restaurante}}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-${DB_USERNAME:-restaurante}}"
export PGPASSWORD="${PGPASSWORD:-${DB_PASSWORD:-}}"
DB_CONTAINER="${DB_CONTAINER:-restaurante-postgres}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "[restore] ERROR: archivo no encontrado: $BACKUP_FILE" >&2
  exit 1
fi

echo "[restore] restaurando $BACKUP_FILE en la base '$DB_NAME' ($DB_HOST:$DB_PORT)"

if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${DB_CONTAINER}$"; then
  echo "[restore] restaurando mediante contenedor $DB_CONTAINER"
  docker cp "$BACKUP_FILE" "$DB_CONTAINER:/tmp/restore.dump"
  docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='$DB_NAME' AND pid <> pg_backend_pid();" || true
  docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS \"$DB_NAME\";"
  docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE \"$DB_NAME\" OWNER \"$DB_USER\";"
  docker exec "$DB_CONTAINER" pg_restore -U "$DB_USER" -d "$DB_NAME" -c --if-exists /tmp/restore.dump
else
  echo "[restore] restaurando directamente contra PostgreSQL"
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS \"$DB_NAME\";"
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"$DB_NAME\" OWNER \"$DB_USER\";"
  pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" --no-owner --no-privileges --single-transaction "$BACKUP_FILE"
fi

echo "[restore] OK — Restauración completada exitosamente en '$DB_NAME'."
