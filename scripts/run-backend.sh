#!/usr/bin/env bash
# Levanta el backend en modo dev.
#
# Resuelve los dos tropiezos habituales al arrancar a mano:
#   1. spring-boot:run NO lee el .env de backend, asi que sin esto la app cae
#      contra localhost:5432 en vez de la base configurada.
#   2. El java del PATH puede ser un JRE sin javac (no compila release 25).
#
# Uso: ./scripts/run-backend.sh [args extra para spring-boot:run]
# Variables: JDK_HOME (por defecto ~/tools/jdk-25), ENV_FILE (por defecto backend/.env).
set -euo pipefail

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

if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME:-}/bin/javac" ]]; then
  CANDIDATO="${JDK_HOME:-$HOME/tools/jdk-25}"
  if [[ -x "$CANDIDATO/bin/javac" ]]; then
    export JAVA_HOME="$CANDIDATO"
  else
    echo "ERROR: no encuentro un JDK con javac." >&2
    echo "       Define JDK_HOME=/ruta/al/jdk-25 o exporta JAVA_HOME." >&2
    exit 1
  fi
fi
export PATH="$JAVA_HOME/bin:$PATH"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  echo "AVISO: no existe $ENV_FILE; se usan los defaults de application.yml." >&2
fi

exec "$RAIZ/backend/mvnw" -f "$RAIZ/backend/pom.xml" spring-boot:run "$@"
