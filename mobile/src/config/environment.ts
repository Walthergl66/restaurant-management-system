/**
 * Estación Burger — Configuración de entorno
 *
 * URL base de la API. Prioridad:
 *   1. EXPO_PUBLIC_API_URL (variable de entorno, inyectada al bundlear)
 *   2. Host del dev server de Expo (hostUri) en desarrollo → la IP del PC
 *   3. extra.apiUrl de app.json (builds sin dev server)
 *   4. Default de desarrollo local
 *
 * ¿Por qué no basta `localhost`? En un teléfono físico `localhost` es el
 * propio teléfono, no la máquina de desarrollo: eso produce el clásico
 * "Sin conexión / no se pudo conectar al servidor". En desarrollo tomamos la
 * IP del PC del mismo `hostUri` con el que el teléfono cargó el bundle.
 *
 * Override manual (útil si el backend vive en otra máquina/puerto):
 *   EXPO_PUBLIC_API_URL=http://192.168.2.100:8080/api/v1 npx expo start
 */

import Constants from 'expo-constants';

const DEFAULT_API_URL = 'http://localhost:8080/api/v1';
const API_PORT = 8080;

/**
 * Deriva `http://<IP-del-PC>:8080/api/v1` desde el host del dev server de Expo
 * (solo presente en desarrollo con @expo/cli). Devuelve null si no aplica.
 */
function resolveDevServerApiUrl(): string | null {
  const hostUri = Constants.expoConfig?.hostUri;
  if (!hostUri) return null;

  const host = hostUri
    .replace(/^https?:\/\//, '')
    .split('/')[0]
    ?.split(':')[0];
  if (!host || host === 'localhost' || host === '127.0.0.1') return null;

  return `http://${host}:${API_PORT}/api/v1`;
}

function resolveApiUrl(): string {
  // 1) Override explícito al bundlear (Metro inlining de EXPO_PUBLIC_*)
  const env = (
    globalThis as { process?: { env?: Record<string, string | undefined> } }
  ).process;
  const fromEnv = env?.env?.EXPO_PUBLIC_API_URL;
  if (fromEnv) return fromEnv;

  // 2) En desarrollo, la IP del PC desde el dev server de Expo
  const fromDevServer = resolveDevServerApiUrl();
  if (fromDevServer) return fromDevServer;

  // 3) Configurado en app.json (builds sin dev server)
  const extra = Constants.expoConfig?.extra as
    | { apiUrl?: string }
    | undefined;
  if (extra?.apiUrl) return extra.apiUrl;

  // 4) Default
  return DEFAULT_API_URL;
}

export const API_BASE_URL = resolveApiUrl();
