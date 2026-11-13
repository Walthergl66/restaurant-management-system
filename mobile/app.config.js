const fs = require('fs');
const path = require('path');

function loadEnv(file) {
  const values = {};
  if (!fs.existsSync(file)) return values;
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$/);
    if (match) {
      values[match[1]] = match[2].trim().replace(/^['"]|['"]$/g, '');
    }
  }
  return values;
}

module.exports = ({ config }) => {
  const rootEnv = loadEnv(path.resolve(__dirname, '..', '.env'));
  return {
    ...config,
    extra: {
      ...(config.extra || {}),
      mapboxToken: rootEnv.EXPO_PUBLIC_MAPBOX_TOKEN || process.env.EXPO_PUBLIC_MAPBOX_TOKEN,
    },
  };
};