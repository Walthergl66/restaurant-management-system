/**
 * Estación Burger — Configuración de entorno
 *
 * URL base de la API. Prioridad:
 *   1. EXPO_PUBLIC_API_URL (variable de entorno, inyectada al bundlear)
 *   2. extra.apiUrl de app.json (runtime vía expo-constants)
 *   3. Default de desarrollo local
 *
 * Ejemplo para probar desde el teléfono en la misma red:
 *   EXPO_PUBLIC_API_URL=http://192.168.2.100:8080/api/v1 npx expo start
 */

import Constants from 'expo-constants';

const DEFAULT_API_URL = 'http://localhost:8080/api/v1';

function resolveApiUrl(): string {
  // Metro inlining de EXPO_PUBLIC_* al momento de bundlear
  const env = (
    globalThis as { process?: { env?: Record<string, string | undefined> } }
  ).process;
  const fromEnv = env?.env?.EXPO_PUBLIC_API_URL;
  if (fromEnv) return fromEnv;

  const extra = Constants.expoConfig?.extra as
    | { apiUrl?: string }
    | undefined;
  if (extra?.apiUrl) return extra.apiUrl;

  return DEFAULT_API_URL;
}

export const API_BASE_URL = resolveApiUrl();
