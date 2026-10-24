import React, { useEffect, useMemo, useRef, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { WebView } from 'react-native-webview';
import Constants from 'expo-constants';
import { colors } from '@/theme/colors';
import { decodePolyline, type LatLng } from '../polyline';

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
  colorRuta?: string;
}

export default function MapaRuta({
  geometria,
  ubicacionRepartidor,
  paradas,
  origen,
  colorRuta = '#F5A623',
}: MapaRutaProps) {
  const webViewRef = useRef<WebView>(null);
  const [loaded, setLoaded] = useState(false);

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
        color: '#3B82F6',
      });
    }
    return result;
  }, [restaurante, destino, paradas, ubicacionRepartidor]);

  const routeCoords = useMemo(
    () => route.map((point) => [point.latitude, point.longitude]),
    [route]
  );

  const mapboxToken = useMemo(
    () =>
      Constants.expoConfig?.extra?.mapboxToken ??
      process.env.EXPO_PUBLIC_MAPBOX_TOKEN ??
      '',
    []
  );

  const html = useMemo(
    () => `<!DOCTYPE html>
<html>
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
      html, body, #map { height: 100%; margin: 0; padding: 0; background: #0a0a0a; touch-action: none; }
      .leaflet-control-attribution { font-size: 10px; background: rgba(0,0,0,.65); color: #888; }
      .marker { width: 14px; height: 14px; border-radius: 7px; border: 2px solid #fff; box-shadow: 0 0 4px rgba(0,0,0,.7); }
      .map-button { position: absolute; right: 10px; z-index: 1000; border: 0; border-radius: 10px; padding: 8px 10px; background: rgba(17,17,17,.92); color: #fff; font-size: 12px; font-weight: 700; }
      .map-button.primary { bottom: 52px; }
      .map-button.secondary { bottom: 10px; }
    </style>
  </head>
  <body>
    <div id="map"></div>
    <button class="map-button primary" onclick="centerOnUser()">Mi ubicación</button>
    <button class="map-button secondary" onclick="fitRoute()">Ver ruta</button>
    <script>
      const map = L.map('map', { zoomControl: true, attributionControl: true, dragging: true, touchZoom: true, scrollWheelZoom: true, doubleClickZoom: true });
      let routeLayer = null;
      let markerLayer = L.layerGroup().addTo(map);
      let userLocation = null;

      function renderRoute(coords) {
        if (routeLayer) { map.removeLayer(routeLayer); routeLayer = null; }
        const latLngs = coords.map(function (coord) { return L.latLng(coord[0], coord[1]); });
        if (latLngs.length > 1) {
          routeLayer = L.polyline(latLngs, { color: ${JSON.stringify(colorRuta)}, weight: 5, opacity: 0.95 }).addTo(map);
        }
        return latLngs;
      }

      function renderMarkers(items) {
        markerLayer.clearLayers();
        items.forEach(function (marker) {
          const icon = L.divIcon({
            className: '',
            html: '<div class="marker" style="background:' + marker.color + ';"></div>',
            iconSize: [14, 14],
            iconAnchor: [7, 7]
          });
          L.marker([marker.lat, marker.lng], { icon: icon }).addTo(markerLayer).bindPopup(marker.label);
        });
      }

      function fitBoundsFor(coords, items) {
        const bounds = L.latLngBounds([]);
        coords.forEach(function (coord) { bounds.extend(L.latLng(coord[0], coord[1])); });
        items.forEach(function (marker) { bounds.extend([marker.lat, marker.lng]); });
        if (bounds.isValid()) map.fitBounds(bounds, { padding: [40, 40] });
      }

      function centerOnUser() {
        if (userLocation) map.setView([userLocation.lat, userLocation.lng], 16);
      }

      function fitRoute() {
        const coords = window.currentRouteCoords || [];
        const items = window.currentMarkers || [];
        const bounds = L.latLngBounds([]);
        coords.forEach(function (coord) { bounds.extend(L.latLng(coord[0], coord[1])); });
        items.forEach(function (marker) { bounds.extend([marker.lat, marker.lng]); });
        if (bounds.isValid()) map.fitBounds(bounds, { padding: [40, 40] });
      }

      window.currentRouteCoords = [];
      window.currentMarkers = [];
      window.renderMap = function (coords, items, fit) {
        window.currentRouteCoords = coords || [];
        window.currentMarkers = items || [];
        renderRoute(window.currentRouteCoords);
        renderMarkers(window.currentMarkers);
        if (fit) fitBoundsFor(window.currentRouteCoords, window.currentMarkers);
      };
      window.setUserLocation = function (lat, lng) {
        userLocation = { lat: lat, lng: lng };
      };

      const mapboxToken = ${JSON.stringify(
        Constants.expoConfig?.extra?.mapboxToken ??
          process.env.EXPO_PUBLIC_MAPBOX_TOKEN ??
          ''
      )};
      if (!mapboxToken) {
        const note = document.createElement('div');
        note.textContent = 'Configura EXPO_PUBLIC_MAPBOX_TOKEN para cargar el mapa.';
        note.style.cssText = 'position:fixed;left:8px;bottom:8px;z-index:999;color:#ef4444;font-size:12px;background:rgba(0,0,0,.7);padding:4px 8px;border-radius:8px;';
        document.body.appendChild(note);
      } else {
        const tiles = L.tileLayer('https://api.mapbox.com/styles/v1/mapbox/dark-v11/tiles/{z}/{x}/{y}?access_token=' + encodeURIComponent(mapboxToken), {
          attribution: '&copy; Mapbox &copy; OpenStreetMap contributors',
          tileSize: 512,
          zoomOffset: -1,
          maxZoom: 20
        });
        tiles.addTo(map);
        tiles.on('tileerror', function () {
          const note = document.createElement('div');
          note.textContent = 'No se pudieron cargar las teselas del mapa.';
          note.style.cssText = 'position:fixed;left:8px;bottom:8px;z-index:999;color:#ef4444;font-size:12px;background:rgba(0,0,0,.7);padding:4px 8px;border-radius:8px;';
          document.body.appendChild(note);
        });
      }
      window.renderMap([], [], false);
    </script>
  </body>
</html>`,
    [mapboxToken]
  );

  useEffect(() => {
    if (!loaded) return;
    const script = `window.renderMap(${JSON.stringify(routeCoords)}, ${JSON.stringify(markers)}, true); true;`;
    webViewRef.current?.injectJavaScript(script);
  }, [loaded, routeCoords, markers]);

  useEffect(() => {
    if (!loaded || !ubicacionRepartidor) return;
    const script = `window.setUserLocation(${ubicacionRepartidor.latitude}, ${ubicacionRepartidor.longitude}); true;`;
    webViewRef.current?.injectJavaScript(script);
  }, [loaded, ubicacionRepartidor]);

  if (routeCoords.length === 0 && markers.length === 0) {
    return (
      <View style={styles.vacio} accessibilityLabel="Ruta no disponible">
        <Text style={styles.vacioText}>Ruta no disponible.</Text>
      </View>
    );
  }

  return (
    <View style={styles.contenedor}>
      <WebView
        ref={webViewRef}
        source={{ html }}
        style={styles.mapa}
        javaScriptEnabled
        domStorageEnabled
        originWhitelist={['*']}
        mixedContentMode="always"
        scrollEnabled={false}
        nestedScrollEnabled={false}
        overScrollMode="never"
        onLoadEnd={() => setLoaded(true)}
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