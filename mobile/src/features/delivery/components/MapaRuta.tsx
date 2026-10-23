import React, { useMemo, useRef } from 'react';
import { Platform, StyleSheet, Text, View } from 'react-native';
import MapView, {
  Marker,
  Polyline,
  PROVIDER_GOOGLE,
  type MapViewProps,
} from 'react-native-maps';
import { colors } from '@/theme/colors';
import { decodePolyline, type LatLng } from '../polyline';

/**
 * Mapa nativo de la ruta individual (restaurante -> destino).
 *
 * IMPORTA react-native-maps AQUÍ. Este archivo solo se consume desde pantallas
 * del grupo (delivery); nunca desde flujos de cliente/mesero (Expo Go).
 *
 * Origen/destino: el backend ya conoce el origen (restaurante) y traza la ruta
 * desde ahí, así que NO pedimos coordenadas extra: el PRIMER punto de la
 * polyline decodificada es el restaurante y el ÚLTIMO es el destino del cliente.
 *
 * Atribución obligatoria: Google muestra su logo automáticamente con
 * PROVIDER_GOOGLE; añadimos además el crédito de datos de ruta de ORS/OSM.
 */

interface MapaRutaProps {
  /** Polyline codificado (precisión 5) de RutaCalculada.geometria. */
  geometria: string;
  /** Ubicación actual del repartidor, o null si no hay permiso/lectura. */
  ubicacionRepartidor: LatLng | null;
}

const PROVIDER = Platform.OS === 'android' ? PROVIDER_GOOGLE : undefined;

export default function MapaRuta({ geometria, ubicacionRepartidor }: MapaRutaProps) {
  const mapRef = useRef<MapView>(null);

  const ruta = useMemo(() => decodePolyline(geometria), [geometria]);
  const restaurante = ruta.length > 0 ? ruta[0] : null;
  const destino = ruta.length > 1 ? ruta[ruta.length - 1] : null;

  const regionInicial = useMemo<MapViewProps['initialRegion']>(() => {
    const centro = restaurante ?? ubicacionRepartidor;
    if (!centro) return undefined;
    return {
      latitude: centro.latitude,
      longitude: centro.longitude,
      latitudeDelta: 0.05,
      longitudeDelta: 0.05,
    };
  }, [restaurante, ubicacionRepartidor]);

  const encuadrar = () => {
    const puntos = [...ruta];
    if (ubicacionRepartidor) puntos.push(ubicacionRepartidor);
    if (puntos.length < 2 || !mapRef.current) return;
    mapRef.current.fitToCoordinates(puntos, {
      edgePadding: { top: 60, right: 60, bottom: 60, left: 60 },
      animated: true,
    });
  };

  if (ruta.length === 0) {
    return (
      <View style={styles.vacio} accessibilityLabel="Ruta no disponible">
        <Text style={styles.vacioText}>Ruta no disponible.</Text>
      </View>
    );
  }

  return (
    <View style={styles.contenedor}>
      <MapView
        ref={mapRef}
        style={styles.mapa}
        provider={PROVIDER}
        initialRegion={regionInicial}
        onMapReady={encuadrar}
        showsUserLocation={false}
        showsMyLocationButton={false}
        accessibilityLabel="Mapa de la ruta de entrega"
      >
        <Polyline
          coordinates={ruta}
          strokeColor={colors.neonOrange}
          strokeWidth={5}
        />

        {restaurante && (
          <Marker
            coordinate={restaurante}
            title="Restaurante"
            description="Punto de partida"
            pinColor={colors.neonOrange}
            accessibilityLabel="Marcador del restaurante"
          />
        )}

        {destino && (
          <Marker
            coordinate={destino}
            title="Destino"
            description="Entrega del cliente"
            pinColor={colors.neonPink}
            accessibilityLabel="Marcador del destino"
          />
        )}

        {ubicacionRepartidor && (
          <Marker
            coordinate={ubicacionRepartidor}
            title="Tu ubicación"
            pinColor={colors.success}
            accessibilityLabel="Marcador de tu ubicación"
          />
        )}
      </MapView>

      {/* Atribución de datos de ruta (ORS/OSM). Google añade su logo aparte. */}
      <View style={styles.atribucion} pointerEvents="none">
        <Text style={styles.atribucionText}>
          Rutas por OpenRouteService / OpenStreetMap contributors
        </Text>
      </View>
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
  mapa: { flex: 1 },
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
  atribucion: {
    position: 'absolute',
    left: 8,
    bottom: 8,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    backgroundColor: colors.overlay,
  },
  atribucionText: { color: colors.textSecondary, fontSize: 10 },
});
