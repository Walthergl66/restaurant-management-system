/**
 * Estación Burger — Selector de ubicación del checkout
 *
 * Mapa Leaflet + Mapbox dentro de WebView, compatible con Expo Go.
 * El punto solo se confirma cuando el usuario pulsa "Confirmar ubicación".
 */
import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { WebView } from 'react-native-webview';
import Constants from 'expo-constants';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';
import { Input } from '@/components/ui/Input';
import { pedidosService } from '@/features/pedidos';
import type { CandidatoDireccion } from '@/features/pedidos';
import { useUbicacionActual, type Coordenada } from './useUbicacionActual';

interface SelectorUbicacionProps {
  valor: Coordenada | null;
  onCambio: (coord: Coordenada) => void;
  onDescripcion?: (descripcion: string) => void;
}

const CENTRO_INICIAL: Coordenada = { latitude: -0.9580285, longitude: -80.6384797 };

export function SelectorUbicacion({ valor, onCambio, onDescripcion }: SelectorUbicacionProps) {
  const webViewRef = useRef<WebView>(null);
  const { estado: estadoGps, error: errorGps, solicitar } = useUbicacionActual();
  const [texto, setTexto] = useState('');
  const [candidatos, setCandidatos] = useState<CandidatoDireccion[]>([]);
  const [buscando, setBuscando] = useState(false);
  const [errorBusqueda, setErrorBusqueda] = useState<string | null>(null);
  const [pendiente, setPendiente] = useState<Coordenada | null>(valor);
  const [cargado, setCargado] = useState(false);

  useEffect(() => {
    setPendiente(valor);
  }, [valor]);

  const centrar = useCallback((coord: Coordenada) => {
    webViewRef.current?.injectJavaScript(
      `if (window.mapa) { window.mapa.setView([${coord.latitude}, ${coord.longitude}], 16); } true;`
    );
  }, []);

  const fijarPendiente = useCallback((coord: Coordenada) => {
    setPendiente(coord);
    webViewRef.current?.injectJavaScript(
      `if (window.fijarMarcador) { window.fijarMarcador(${coord.latitude}, ${coord.longitude}); } true;`
    );
  }, []);

  const usarMiUbicacion = useCallback(async () => {
    const coord = await solicitar();
    if (coord) {
      fijarPendiente(coord);
      centrar(coord);
    }
  }, [solicitar, fijarPendiente, centrar]);

  const buscar = useCallback(async () => {
    const t = texto.trim();
    if (!t) return;
    setBuscando(true);
    setErrorBusqueda(null);
    try {
      const resultados = await pedidosService.geocodificar(t);
      setCandidatos(resultados);
      if (resultados.length === 0) {
        setErrorBusqueda('Sin resultados. Ajusta el marcador en el mapa manualmente.');
      }
    } catch {
      setErrorBusqueda('No se pudo buscar ahora. Ajusta el marcador en el mapa manualmente.');
    } finally {
      setBuscando(false);
    }
  }, [texto]);

  const elegirCandidato = useCallback(
    (c: CandidatoDireccion) => {
      const coord = { latitude: c.lat, longitude: c.lng };
      fijarPendiente(coord);
      centrar(coord);
      setCandidatos([]);
      setTexto(c.descripcion);
      onDescripcion?.(c.descripcion);
    },
    [fijarPendiente, centrar, onDescripcion]
  );

  const confirmar = useCallback(() => {
    if (pendiente) onCambio(pendiente);
  }, [pendiente, onCambio]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (texto.trim().length >= 3) void buscar();
    }, 450);
    return () => clearTimeout(timer);
  }, [texto, buscar]);

  const token =
    Constants.expoConfig?.extra?.mapboxToken ??
    process.env.EXPO_PUBLIC_MAPBOX_TOKEN ??
    '';

  const html = `<!DOCTYPE html>
<html>
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
      html, body, #map { height: 100%; margin: 0; padding: 0; background: #0a0a0a; touch-action: none; }
      .leaflet-control-attribution { font-size: 10px; background: rgba(0,0,0,.65); color: #888; }
      .pin { width: 18px; height: 18px; border-radius: 9px; background: #F5A623; border: 3px solid #fff; box-shadow: 0 0 6px rgba(0,0,0,.8); }
    </style>
  </head>
  <body>
    <div id="map"></div>
    <script>
      const token = ${JSON.stringify(token)};
      const initial = ${JSON.stringify(valor ?? CENTRO_INICIAL)};
      const map = L.map('map', { dragging: true, touchZoom: true, scrollWheelZoom: true, doubleClickZoom: true, zoomControl: true });
      if (!token) {
        const note = document.createElement('div');
        note.textContent = 'Configura EXPO_PUBLIC_MAPBOX_TOKEN para cargar el mapa.';
        note.style.cssText = 'position:fixed;left:8px;bottom:8px;z-index:999;color:#ef4444;font-size:12px;background:rgba(0,0,0,.7);padding:4px 8px;border-radius:8px;';
        document.body.appendChild(note);
      } else {
        L.tileLayer('https://api.mapbox.com/styles/v1/mapbox/dark-v11/tiles/{z}/{x}/{y}?access_token=' + encodeURIComponent(token), {
          attribution: '&copy; Mapbox &copy; OpenStreetMap contributors',
          tileSize: 512,
          zoomOffset: -1,
          maxZoom: 20
        }).addTo(map);
      }
      const marker = L.marker([initial.latitude, initial.longitude], {
        draggable: true,
        icon: L.divIcon({ className: '', html: '<div class="pin"></div>', iconSize: [18, 18], iconAnchor: [9, 9] })
      }).addTo(map);
      window.mapa = map;
      window.fijarMarcador = function (lat, lng) {
        marker.setLatLng([lat, lng]);
        map.setView([lat, lng], 16);
      };
      map.on('click', function (e) {
        marker.setLatLng(e.latlng);
        if (window.ReactNativeWebView) {
          window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'select', lat: e.latlng.lat, lng: e.latlng.lng }));
        }
      });
      marker.on('dragend', function (e) {
        const pos = e.target.getLatLng();
        if (window.ReactNativeWebView) {
          window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'select', lat: pos.lat, lng: pos.lng }));
        }
      });
      map.setView([initial.latitude, initial.longitude], 15);
    </script>
  </body>
</html>`;

  const onMessage = (event: { nativeEvent: { data: string } }) => {
    try {
      const data = JSON.parse(event.nativeEvent.data) as { type: string; lat: number; lng: number };
      if (data.type === 'select') {
        setPendiente({ latitude: data.lat, longitude: data.lng });
      }
    } catch {
      // Ignorar mensajes malformados.
    }
  };

  return (
    <View>
      <View style={styles.buscadorRow}>
        <View style={styles.buscadorInput}>
          <Input
            placeholder="Buscar dirección o lugar..."
            value={texto}
            onChangeText={setTexto}
            icon="search-outline"
            onSubmitEditing={buscar}
            returnKeyType="search"
            compact
            autoCorrect={false}
          />
        </View>
        <TouchableOpacity
          style={styles.buscarBtn}
          onPress={buscar}
          disabled={buscando || texto.trim().length === 0}
          accessibilityLabel="Buscar dirección"
        >
          {buscando ? (
            <ActivityIndicator size="small" color={colors.background} />
          ) : (
            <Ionicons name="search" size={20} color={colors.background} />
          )}
        </TouchableOpacity>
      </View>

      {candidatos.length > 0 && (
        <View style={styles.candidatos}>
          {candidatos.map((c, i) => (
            <TouchableOpacity
              key={`${c.lat},${c.lng},${i}`}
              style={styles.candidato}
              onPress={() => elegirCandidato(c)}
              accessibilityLabel={`Elegir ${c.descripcion}`}
            >
              <Ionicons name="location-outline" size={16} color={colors.neonOrange} />
              <Text style={styles.candidatoText} numberOfLines={2}>
                {c.descripcion}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      )}

      {errorBusqueda && <Text style={styles.aviso}>{errorBusqueda}</Text>}

      <View style={styles.mapaContenedor}>
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
          onMessage={onMessage}
        />
      </View>

      <TouchableOpacity
        style={styles.gpsBtn}
        onPress={usarMiUbicacion}
        disabled={estadoGps === 'cargando'}
        accessibilityLabel="Usar mi ubicación actual"
      >
        {estadoGps === 'cargando' ? (
          <ActivityIndicator size="small" color={colors.neonOrange} />
        ) : (
          <Ionicons name="navigate" size={18} color={colors.neonOrange} />
        )}
        <Text style={styles.gpsText}>Usar mi ubicación actual</Text>
      </TouchableOpacity>

      {estadoGps === 'denegado' && (
        <Text style={styles.aviso}>
          {errorGps ?? 'Permiso de ubicación denegado. Puedes ajustar el marcador en el mapa manualmente.'}
        </Text>
      )}

      {pendiente && (
        <Text style={styles.coords}>
          Punto fijado: {pendiente.latitude.toFixed(5)}, {pendiente.longitude.toFixed(5)}
        </Text>
      )}

      <TouchableOpacity
        style={[styles.confirmarBtn, !pendiente && styles.confirmarBtnDisabled]}
        onPress={confirmar}
        disabled={!pendiente}
        accessibilityLabel="Confirmar ubicación"
      >
        <Text style={styles.confirmarText}>Confirmar ubicación</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  buscadorRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 8 },
  buscadorInput: { flex: 1 },
  buscarBtn: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: colors.neonOrange,
    alignItems: 'center',
    justifyContent: 'center',
  },
  candidatos: {
    backgroundColor: colors.surface,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    marginBottom: 10,
    overflow: 'hidden',
  },
  candidato: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 12,
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.surfaceBorder,
  },
  candidatoText: { flex: 1, color: colors.textPrimary, fontSize: 13 },
  mapaContenedor: {
    height: 240,
    borderRadius: 16,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    backgroundColor: colors.surface,
    marginBottom: 10,
  },
  mapa: { flex: 1, backgroundColor: colors.surface },
  gpsBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 12,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.neonOrange,
    backgroundColor: colors.surface,
    marginBottom: 8,
  },
  gpsText: { color: colors.neonOrange, fontSize: 14, fontWeight: '700' },
  aviso: { color: colors.textSecondary, fontSize: 12, marginTop: 8 },
  coords: { color: colors.textMuted, fontSize: 12, marginTop: 8 },
  confirmarBtn: {
    marginTop: 8,
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
    backgroundColor: colors.neonOrange,
  },
  confirmarBtnDisabled: { opacity: 0.5 },
  confirmarText: { color: colors.background, fontWeight: '800', fontSize: 14 },
});