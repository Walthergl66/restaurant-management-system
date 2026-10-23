import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { WebView } from 'react-native-webview';
import { colors } from '@/theme/colors';
import { decodePolyline, type LatLng } from '../polyline';

/**
 * Mapa de la ruta de entrega renderizado con Leaflet dentro de un WebView.
 *
 * Se usa este enfoque para que funcione en Expo Go sin depender de una
 * Google Maps API key ni de un development build. Las teselas son de
 * OpenStreetMap/CARTO y la geometría la calcula el backend con ORS.
 */
export interface ParadaMapa {
  orden: number;
  latitude: number;
  longitude: number;
  codigo?: string | null;
  resuelta?: boolean;
}

interface MapaRutaProps {
  geometria: string;
  ubicacionRepartidor: LatLng | null;
  paradas?: ParadaMapa[];
  origen?: LatLng | null;
}

export default function MapaRuta({
  geometria,
  ubicacionRepartidor,
  paradas,
  origen,
}: MapaRutaProps) {
  const route = useMemo(() => decodePolyline(geometria), [geometria]);
  const restaurante = origen ?? (route.length > 0 ? route[0] : null);
  const esMultiparada = (paradas?.length ?? 0) > 0;
  const destino =
    !esMultiparada && route.length > 1 ? route[route.length - 1] : null;

  const markers = useMemo(() => {
    const result: Array<{ lat: number; lng: number; label: string; color: string }> = [];
    if (restaurante) {
      result.push({ lat: restaurante.latitude, lng: restaurante.longitude, label: 'Restaurante', color: colors.neonOrange });
    }
    if (destino) {
      result.push({ lat: destino.latitude, lng: destino.longitude, label: 'Destino', color: colors.neonPink });
    }
    paradas?.forEach((parada) => {
      result.push({
        lat: parada.latitude,
        lng: parada.longitude,
        label: `Parada ${parada.orden + 1}${parada.codigo ? ` · ${parada.codigo}` : ''}`,
        color: parada.resuelta ? colors.success : colors.neonPink,
      });
    });
    if (ubicacionRepartidor) {
      result.push({
        lat: ubicacionRepartidor.latitude,
        lng: ubicacionRepartidor.longitude,
        label: 'Tu ubicación',
        color: colors.success,
      });
    }
    return result;
  }, [restaurante, destino, paradas, ubicacionRepartidor]);

  const routeCoords = useMemo(
    () => route.map((point) => [point.latitude, point.longitude]),
    [route]
  );

  if (routeCoords.length === 0 && markers.length === 0) {
    return (
      <View style={styles.vacio} accessibilityLabel="Ruta no disponible">
        <Text style={styles.vacioText}>Ruta no disponible.</Text>
      </View>
    );
  }

  const html = `<!DOCTYPE html>
<html>
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
      html, body, #map { height: 100%; margin: 0; padding: 0; background: #0a0a0a; }
      .leaflet-control-attribution { font-size: 10px; background: rgba(0,0,0,.65); color: #888; }
      .marker { width: 14px; height: 14px; border-radius: 7px; border: 2px solid #fff; box-shadow: 0 0 4px rgba(0,0,0,.7); }
    </style>
  </head>
  <body>
    <div id="map"></div>
    <script>
      const routeCoords = ${JSON.stringify(routeCoords)};
      const markers = ${JSON.stringify(markers)};
      const map = L.map('map', { zoomControl: true, attributionControl: true });
      const tiles = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
        subdomains: 'abcd',
        maxZoom: 20
      });
      tiles.addTo(map);
      tiles.on('tileerror', function () {
        const note = document.createElement('div');
        note.textContent = 'No se pudieron cargar las teselas del mapa.';
        note.style.cssText = 'position:fixed;left:8px;bottom:8px;z-index:999;color:#ef4444;font-size:12px;background:rgba(0,0,0,.7);padding:4px 8px;border-radius:8px;';
        document.body.appendChild(note);
      });
      const latLngs = routeCoords.map(function (coord) { return L.latLng(coord[0], coord[1]); });
      if (latLngs.length > 1) {
        L.polyline(latLngs, { color: '#F5A623', weight: 5, opacity: 0.95 }).addTo(map);
      }
      markers.forEach(function (marker) {
        const icon = L.divIcon({
          className: '',
          html: '<div class="marker" style="background:' + marker.color + ';"></div>',
          iconSize: [14, 14],
          iconAnchor: [7, 7]
        });
        L.marker([marker.lat, marker.lng], { icon: icon }).addTo(map).bindPopup(marker.label);
      });
      const bounds = L.latLngBounds([]);
      latLngs.forEach(function (coord) { bounds.extend(coord); });
      markers.forEach(function (marker) { bounds.extend([marker.lat, marker.lng]); });
      if (bounds.isValid()) {
        map.fitBounds(bounds, { padding: [40, 40] });
      } else if (markers.length > 0) {
        map.setView([markers[0].lat, markers[0].lng], 14);
      } else {
        map.setView([0, 0], 2);
      }
    </script>
  </body>
</html>`;

  return (
    <View style={styles.contenedor}>
      <WebView
        source={{ html }}
        style={styles.mapa}
        javaScriptEnabled
        domStorageEnabled
        originWhitelist={['*']}
        mixedContentMode="always"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  contenedor: {
    height: 260,
    borderRadius: 16,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    backgroundColor: colors.surface,
  },
  mapa: { flex: 1, backgroundColor: colors.surface },
  vacio: {
    height: 160,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  vacioText: { color: colors.textMuted, fontWeight: '700' },
});