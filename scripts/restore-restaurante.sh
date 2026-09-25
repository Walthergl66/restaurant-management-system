#!/usr/bin/env bash
# Restauración de un backup custom de PostgreSQL (plan mejoras 4).
# Uso: ./scripts/restore-restaurante.sh <archivo.dump> [nombre-bd]
# PELIGRO: la base destino se recrea (DROP + CREATE) y se pierde lo que tenga.
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Uso: $0 <archivo.dump> [nombre-bd=DB_NAME]" >&2
  exit 1
fi
DUMP="$1"
DB_NAME="${2:-${DB_NAME:-restaurante}}"
DB_USER="${DB_USER:-restaurante}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

if [[ ! -f "$DUMP" ]]; then
  echo "ERROR: no existe $DUMP" >&2
  exit 1
fi
if [[ -z "${PGPASSWORD:-}" ]]; then
  echo "ERROR: define PGPASSWORD antes de ejecutar la restauración." >&2
  exit 1
fi

echo "Recreando la base '$DB_NAME' (se descartan los datos actuales)..."
psql --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" -d postgres \
  -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS \"$DB_NAME\";"
psql --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" -d postgres \
  -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"$DB_NAME\";"

pg_restore --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" \
  --dbname="$DB_NAME" --no-owner --no-privileges --single-transaction "$DUMP"

echo "Restauración completada en '$DB_NAME'."
echo "Si el backup no trae esquema de migraciones: la app re-aplica Flyway
  (flyway.baseline-on-migrate=true) para reconciliar versiones."