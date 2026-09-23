#!/bin/bash
set -euo pipefail

# Backup de PostgreSQL para restaurante — Fase 8 RNF-05
# Uso: ./database/backup.sh [nombre_archivo]
# Requiere: docker compose up -d postgres o acceso directo a postgres

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="./database/backups"
BACKUP_FILE="${1:-$BACKUP_DIR/restaurante_${DATE}.dump}"
DB_CONTAINER="${DB_CONTAINER:-restaurante-postgres}"
DB_NAME="${DB_NAME:-restaurante}"
DB_USER="${DB_USERNAME:-restaurante}"

mkdir -p "$BACKUP_DIR"

echo "[backup] $DATE — iniciando backup de $DB_NAME -> $BACKUP_FILE"

if docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
  echo "[backup] usando contenedor $DB_CONTAINER"
  if docker exec "$DB_CONTAINER" sh -c "test -d /backups" 2>/dev/null; then
    docker exec "$DB_CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" -F c -f "/backups/$(basename "$BACKUP_FILE")"
    if [ -f "$BACKUP_DIR/$(basename "$BACKUP_FILE")" ]; then
      echo "[backup] completado: $BACKUP_DIR/$(basename "$BACKUP_FILE")"
    else
      docker cp "$DB_CONTAINER:/backups/$(basename "$BACKUP_FILE")" "$BACKUP_FILE"
      echo "[backup] completado (copiado): $BACKUP_FILE"
    fi
    ls -lh "$BACKUP_DIR"/restaurante_*.dump 2>/dev/null | tail -n 5
    echo "[backup] validar con pg_restore --list (sin restaurar)"
    docker exec "$DB_CONTAINER" pg_restore --list "/backups/$(basename "$BACKUP_FILE")" | head -n 20
  else
    echo "[backup] /backups no montado en dev, usando stdout"
    docker exec "$DB_CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" -F c > "$BACKUP_FILE"
    echo "[backup] completado (stdout): $BACKUP_FILE"
    ls -lh "$BACKUP_FILE" 2>/dev/null | tail -n 5
    echo "[backup] validar con pg_restore --list (sin restaurar)"
    pg_restore --list "$BACKUP_FILE" 2>&1 | head -n 20 || docker exec "$DB_CONTAINER" pg_restore --list "/tmp/$(basename "$BACKUP_FILE")" 2>&1 | head -n 20
  fi
else
  echo "[backup] contenedor no encontrado, intentando pg_dump directo"
  pg_dump -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "$DB_USER" -d "$DB_NAME" -F c -f "$BACKUP_FILE"
  echo "[backup] completado: $BACKUP_FILE"
  ls -lh "$BACKUP_DIR"/restaurante_*.dump 2>/dev/null | tail -n 5
  echo "[backup] validar con pg_restore --list (sin restaurar)"
  pg_restore --list "$BACKUP_FILE" | head -n 20
fi

echo "[backup] OK"
