#!/usr/bin/env bash
# ============================================================
#  Estación Burger — Arranque del backend en desarrollo
#  Uso: ./scripts/start-backend.sh   (ejecutar desde la raíz)
# ============================================================
set -euo pipefail

# --- 1) JDK 25 (obligatorio: Maven + Spring Boot 4 no corren en JRE 8) ---
JDK_DIR="${JAVA_HOME:-$HOME/tools/jdk-25.0.4.1+1}"
if [ ! -x "$JDK_DIR/bin/java" ]; then
  echo "ERROR: no se encontró un JDK 25 en: $JDK_DIR" >&2
  echo "       Instálalo (Temurin 25) y ajusta JDK_DIR en este script." >&2
  exit 1
fi
export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
echo ">> JDK: $("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"

# --- 2) Base de datos local (PostgreSQL 16) ---
docker compose up -d postgres

# --- 3) Backend ---
cd "$(dirname "$0")/../backend"
export DB_PASSWORD="${DB_PASSWORD:-restaurante}"
echo ">> Backend en http://localhost:8080  (health: /actuator/health)"
exec ./mvnw spring-boot:run
