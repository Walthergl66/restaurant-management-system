#!/usr/bin/env bash
# Backup de la base PostgreSQL (plan mejoras 4).
# Uso: ./scripts/backup-restaurante.sh [directorio-salida]
# Variables: DB_HOST (localhost) DB_PORT (5432) DB_NAME (restaurante)
#            DB_USER (restaurante) y PGPASSWORD en el entorno.
set -euo pipefail

OUT_DIR="${1:-backups}"
DB_USER="${DB_USER:-restaurante}"
DB_NAME="${DB_NAME:-restaurante}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

if [[ -z "${PGPASSWORD:-}" ]]; then
  echo "ERROR: define PGPASSWORD antes de ejecutar el backup." >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
FILE="$OUT_DIR/restaurante-$DB_NAME-$STAMP.dump"

pg_dump --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" \
  --format=custom --no-owner --no-privileges "$DB_NAME" > "$FILE"

echo "Backup creado: $FILE ($(du -h "$FILE" | cut -f1))"
echo "Para restaurarlo: PGPASSWORD=... DB_NAME=... ./scripts/restore-restaurante.sh $FILE"