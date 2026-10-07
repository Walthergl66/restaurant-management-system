#!/usr/bin/env bash
set -euo pipefail

# Restauración de PostgreSQL para restaurante
# Uso: ./database/restore.sh <archivo.dump> [nombre_bd_destino]
# Credenciales: backend/.env (o ENV_FILE).
# Cliente pg: el contenedor `restaurante-postgres` si está corriendo; si no,
# el psql/pg_restore del host.
# CUIDADO: la base de datos destino se recrea y se descartan los datos actuales.

if [ $# -lt 1 ]; then
  echo "Uso: $0 <archivo.dump> [nombre_bd_destino]"
  echo "Ejemplo: $0 ./database/backups/restaurante_restaurante_20260929_120000.dump"
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
DB_CONTAINER="${DB_CONTAINER:-restaurante-postgres}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "[restore] ERROR: archivo no encontrado: $BACKUP_FILE" >&2
  exit 1
fi

# Target local va por socket unix dentro del contenedor; cualquier otro host se
# pasa como TCP con las credenciales de .env.
REMOTO=()
case "$DB_HOST" in
  ""|localhost|127.0.0.1|postgres) ;;
  *) REMOTO=(-h "$DB_HOST" -p "$DB_PORT") ;;
esac

echo "[restore] restaurando $BACKUP_FILE en la base '$DB_NAME' ($DB_HOST:$DB_PORT)"

run_psql() {
  if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${DB_CONTAINER}$"; then
    docker exec -i -e PGPASSWORD="$PGPASSWORD" "$DB_CONTAINER" \
      psql "${REMOTO[@]}" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 -c "$1"
  else
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -v ON_ERROR_STOP=1 -c "$1"
  fi
}

run_pg_restore() {
  if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${DB_CONTAINER}$"; then
    docker exec -i -e PGPASSWORD="$PGPASSWORD" "$DB_CONTAINER" \
      pg_restore "${REMOTO[@]}" -U "$DB_USER" -d "$DB_NAME" --no-owner --no-privileges --single-transaction < "$BACKUP_FILE"
  else
    pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" --no-owner --no-privileges --single-transaction "$BACKUP_FILE"
  fi
}

echo "[restore] recreando la base de datos (WITH (FORCE) cierra conexiones activas, p. ej. el backend corriendo)"
run_psql "DROP DATABASE IF EXISTS \"$DB_NAME\" WITH (FORCE);"
run_psql "CREATE DATABASE \"$DB_NAME\" OWNER \"$DB_USER\";"
echo "[restore] volcando el dump"
run_pg_restore

echo "[restore] OK — Restauración completada exitosamente en '$DB_NAME'."