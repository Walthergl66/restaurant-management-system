const fs = require('fs');
const path = require('path');

/**
 * Expo solo lee `.env` desde el directorio del proyecto (mobile/). En este
 * monorepo el token público de Mapbox vive en el `.env` de la raíz, así que
 * lo leemos aquí y lo exponemos mediante `extra` (sin imprimirlo en logs).
 */
function loadEnvFile(filePath) {
  const result = {};
  if (!fs.existsSync(filePath)) return result;
  const content = fs.readFileSync(filePath, 'utf8');
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) continue;
    const idx = line.indexOf('=');
    if (idx === -1) continue;
    const key = line.slice(0, idx).trim();
    let value = line.slice(idx + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (key) result[key] = value;
  }
  return result;
}

module.exports = ({ config }) => {
  const rootEnv = loadEnvFile(path.resolve(__dirname, '..', '.env'));
  const mobileEnv = loadEnvFile(path.resolve(__dirname, '.env'));
  // mobile/.env tiene prioridad sobre la raíz si ambos existen.
  const merged = { ...rootEnv, ...mobileEnv };
  const mapboxToken =
    merged.EXPO_PUBLIC_MAPBOX_TOKEN || process.env.EXPO_PUBLIC_MAPBOX_TOKEN || '';

  if (mapboxToken && !process.env.EXPO_PUBLIC_MAPBOX_TOKEN) {
    process.env.EXPO_PUBLIC_MAPBOX_TOKEN = mapboxToken;
  }

  return {
    ...config,
    extra: {
      ...(config.extra || {}),
      mapboxToken,
    },
  };
};
