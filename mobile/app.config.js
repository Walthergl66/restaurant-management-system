// Expo carga mobile/.env; no se leen archivos del directorio padre.
module.exports = ({ config }) => ({
  ...config,
  extra: {
    ...(config.extra || {}),
    mapboxToken: process.env.EXPO_PUBLIC_MAPBOX_TOKEN,
  },
});
