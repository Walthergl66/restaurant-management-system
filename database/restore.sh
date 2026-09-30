#!/usr/bin/env bash
set -euo pipefail

# Restauración de PostgreSQL para restaurante
# Uso: ./database/restore.sh <archivo.dump> [nombre_bd_destino]
# Lee automáticamente las credenciales desde backend/.env si existe.
# CUIDADO: La base de datos destino se recrea y se descartan los datos actuales.

if [ $# -lt 1 ]; then
  echo "Uso: $0 <archivo.dump> [nombre_bd_destino]"
  echo "Ejemplo: $0 ./database/backups/restaurante_20260929_120000.dump"
  exit 1
fi

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

BACKUP_FILE="$1"
DB_NAME="${2:-${DB_NAME:-restaurante}}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-${DB_USERNAME:-restaurante}}"
export PGPASSWORD="${PGPASSWORD:-${DB_PASSWORD:-}}"
DB_CONTAINER="${DB_CONTAINER:-restaurante-backend}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "[restore] ERROR: archivo no encontrado: $BACKUP_FILE" >&2
  exit 1
fi

echo "[restore] restaurando $BACKUP_FILE en la base '$DB_NAME' ($DB_HOST:$DB_PORT)"

echo "[restore] ejecutando restore directamente contra PostgreSQL"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS \"$DB_NAME\";"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"$DB_NAME\" OWNER \"$DB_USER\";"
pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" --no-owner --no-privileges --single-transaction "$BACKUP_FILE"

echo "[restore] OK — Restauración completada exitosamente en '$DB_NAME'."
