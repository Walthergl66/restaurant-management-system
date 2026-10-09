#!/usr/bin/env bash
# ============================================================
#  Estación Burger — Arranque de la app móvil (Expo)
#  Uso: ./scripts/start-mobile.sh   (ejecutar desde la raíz)
# ============================================================
set -euo pipefail

cd "$(dirname "$0")/../mobile"

# URL base de la API (prioridad: EXPO_PUBLIC_API_URL > app.json > default):
#   - iOS Simulator:    localhost funciona por defecto
#   - Android Emulator: descomenta la siguiente línea
#   - Teléfono físico:  usa la IP de tu PC en la misma red
# export EXPO_PUBLIC_API_URL=http://10.0.2.2:8080/api/v1

exec npx expo start
