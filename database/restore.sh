#!/bin/bash
set -euo pipefail

# Restore de PostgreSQL — Fase 8 RNF-05
# Uso: ./database/restore.sh <archivo.dump>
# CUIDADO: sobrescribe datos. Usar solo en restore de desastre o test.

if [ $# -lt 1 ]; then
  echo "Uso: $0 <archivo.dump>"
  echo "Ejemplo: $0 ./database/backups/restaurante_20260922_120000.dump"
  exit 1
fi

BACKUP_FILE="$1"
DB_CONTAINER="${DB_CONTAINER:-restaurante-postgres}"
DB_NAME="${DB_NAME:-restaurante}"
DB_USER="${DB_USERNAME:-restaurante}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "[restore] archivo no encontrado: $BACKUP_FILE" >&2
  exit 1
fi

echo "[restore] restaurando $BACKUP_FILE -> $DB_NAME (contenedor $DB_CONTAINER)"

if docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
  echo "[restore] copiando dump al contenedor"
  docker cp "$BACKUP_FILE" "$DB_CONTAINER:/tmp/restore.dump"
  echo "[restore] limpiando conexiones y recreando DB"
  docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='$DB_NAME' AND pid <> pg_backend_pid();"
  docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS \"$DB_NAME\";"
  docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE \"$DB_NAME\" OWNER \"$DB_USER\";"
  echo "[restore] pg_restore"
  docker exec "$DB_CONTAINER" pg_restore -U "$DB_USER" -d "$DB_NAME" -c --if-exists /tmp/restore.dump
  echo "[restore] Flyway validate (la app lo hará al arrancar)"
else
  echo "[restore] pg_restore directo"
  psql -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS \"$DB_NAME\";"
  psql -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "$DB_USER" -d postgres -c "CREATE DATABASE \"$DB_NAME\" OWNER \"$DB_USER\";"
  pg_restore -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "$DB_USER" -d "$DB_NAME" -c --if-exists "$BACKUP_FILE"
fi

echo "[restore] OK — verificar con: docker compose -f docker-compose.prod.yml up -d && curl http://localhost:8080/actuator/health"
