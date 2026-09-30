#!/usr/bin/env bash
# Levanta la aplicación web (frontend) en modo desarrollo.
#
# Uso: ./scripts/run-frontend.sh
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$RAIZ/frontend"

if ! command -v npm >/dev/null 2>&1; then
  echo "ERROR: npm no está instalado en el sistema o no se encuentra en PATH." >&2
  exit 1
fi

cd "$FRONTEND_DIR"

if [[ ! -d "node_modules" ]]; then
  echo "Instalando dependencias de frontend (npm install)..."
  npm install
fi

echo "Iniciando frontend en http://localhost:3000..."
exec npm run dev
