/**
 * Estación Burger — Selector de ubicación del checkout (A1)
 *
 * Mapa con MARCADOR ARRASTRABLE para fijar el punto exacto de entrega, más:
 *   - Botón "Usar mi ubicación actual": pide permiso (expo-location) y, si se
 *     concede, centra el mapa y coloca el marcador en el GPS. Si se deniega,
 *     mensaje claro y se puede seguir ajustando a mano (NO se inventa ubicación).
 *   - Buscador: el texto se consulta contra el endpoint de geocoding del backend
 *     (que reutiliza ORS sin exponer la api-key); al elegir un candidato se mueve
 *     el marcador y se rellena la descripción textual.
 *   - Marcador arrastrable / toque en el mapa: fija lat/lng manualmente.
 *
 * react-native-maps se importa AQUÍ. Este componente SOLO se monta en el
 * checkout cuando la entrega es DOMICILIO, para no cargar el nativo de más en
 * otras pantallas del cliente. Reutiliza el patrón de delivery/MapaRuta
 * (MapView + Marker, PROVIDER_GOOGLE en Android, atribución OSM/ORS).
 */

import React, { useCallback, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Platform,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import MapView, {
  Marker,
  PROVIDER_GOOGLE,
  type MapPressEvent,
  type MarkerDragStartEndEvent,
  type Region,
} from 'react-native-maps';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';
import { Input } from '@/components/ui/Input';
import { pedidosService } from '@/features/pedidos';
import type { CandidatoDireccion } from '@/features/pedidos';
import { useUbicacionActual, type Coordenada } from './useUbicacionActual';

const PROVIDER = Platform.OS === 'android' ? PROVIDER_GOOGLE : undefined;

/** Centro por defecto (San Salvador) hasta que el usuario fije un punto. */
const REGION_DEFECTO: Region = {
  latitude: 13.6929,
  longitude: -89.2182,
  latitudeDelta: 0.03,
  longitudeDelta: 0.03,
};

interface SelectorUbicacionProps {
  /** Punto confirmado (marcador), o null si aún no se fijó. */
  valor: Coordenada | null;
  /** Se invoca cada vez que cambia el punto (GPS, búsqueda o arrastre). */
  onCambio: (coord: Coordenada) => void;
  /** Se invoca al elegir un candidato de búsqueda (rellena la descripción). */
  onDescripcion?: (descripcion: string) => void;
}

export function SelectorUbicacion({
  valor,
  onCambio,
  onDescripcion,
}: SelectorUbicacionProps) {
  const mapRef = useRef<MapView>(null);
  const { estado, error: errorGps, solicitar } = useUbicacionActual();

  const [texto, setTexto] = useState('');
  const [candidatos, setCandidatos] = useState<CandidatoDireccion[]>([]);
  const [buscando, setBuscando] = useState(false);
  const [errorBusqueda, setErrorBusqueda] = useState<string | null>(null);

  const centrar = useCallback((coord: Coordenada) => {
    mapRef.current?.animateToRegion(
      {
        latitude: coord.latitude,
        longitude: coord.longitude,
        latitudeDelta: 0.01,
        longitudeDelta: 0.01,
      },
      350
    );
  }, []);

  const usarMiUbicacion = useCallback(async () => {
    const coord = await solicitar();
    if (coord) {
      onCambio(coord);
      centrar(coord);
    }
  }, [solicitar, onCambio, centrar]);

  const buscar = useCallback(async () => {
    const t = texto.trim();
    if (!t) return;
    setBuscando(true);
    setErrorBusqueda(null);
    try {
      const resultados = await pedidosService.geocodificar(t);
      setCandidatos(resultados);
      if (resultados.length === 0) {
        setErrorBusqueda(
          'Sin resultados. Ajusta el marcador en el mapa manualmente.'
        );
      }
    } catch {
      setErrorBusqueda(
        'No se pudo buscar ahora. Ajusta el marcador en el mapa manualmente.'
      );
    } finally {
      setBuscando(false);
    }
  }, [texto]);

  const elegirCandidato = useCallback(
    (c: CandidatoDireccion) => {
      const coord = { latitude: c.lat, longitude: c.lng };
      onCambio(coord);
      onDescripcion?.(c.descripcion);
      centrar(coord);
      setCandidatos([]);
      setTexto(c.descripcion);
    },
    [onCambio, onDescripcion, centrar]
  );

  const fijarEnMapa = useCallback(
    (e: MapPressEvent) => {
      onCambio(e.nativeEvent.coordinate);
    },
    [onCambio]
  );

  const arrastrarMarcador = useCallback(
    (e: MarkerDragStartEndEvent) => {
      onCambio(e.nativeEvent.coordinate);
    },
    [onCambio]
  );

  const regionInicial: Region = valor
    ? {
        latitude: valor.latitude,
        longitude: valor.longitude,
        latitudeDelta: 0.01,
        longitudeDelta: 0.01,
      }
    : REGION_DEFECTO;

  return (
    <View>
      {/* Buscador de dirección */}
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
              <Ionicons
                name="location-outline"
                size={16}
                color={colors.neonOrange}
              />
              <Text style={styles.candidatoText} numberOfLines={2}>
                {c.descripcion}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      )}

      {errorBusqueda && <Text style={styles.aviso}>{errorBusqueda}</Text>}

      {/* Mapa con marcador arrastrable */}
      <View style={styles.mapaContenedor}>
        <MapView
          ref={mapRef}
          style={styles.mapa}
          provider={PROVIDER}
          initialRegion={regionInicial}
          onPress={fijarEnMapa}
          accessibilityLabel="Mapa para seleccionar la ubicación de entrega"
        >
          {valor && (
            <Marker
              coordinate={valor}
              draggable
              onDragEnd={arrastrarMarcador}
              pinColor={colors.neonOrange}
              title="Ubicación de entrega"
              description="Arrástrame o toca el mapa para ajustar"
              accessibilityLabel="Marcador de la ubicación de entrega"
            />
          )}
        </MapView>

        {!valor && (
          <View style={styles.hint} pointerEvents="none">
            <Text style={styles.hintText}>
              Toca el mapa o usa tu ubicación para fijar el punto de entrega
            </Text>
          </View>
        )}

        <View style={styles.atribucion} pointerEvents="none">
          <Text style={styles.atribucionText}>
            Datos por OpenRouteService / OpenStreetMap contributors
          </Text>
        </View>
      </View>

      {/* Botón GPS */}
      <TouchableOpacity
        style={styles.gpsBtn}
        onPress={usarMiUbicacion}
        disabled={estado === 'cargando'}
        accessibilityLabel="Usar mi ubicación actual"
      >
        {estado === 'cargando' ? (
          <ActivityIndicator size="small" color={colors.neonOrange} />
        ) : (
          <Ionicons name="navigate" size={18} color={colors.neonOrange} />
        )}
        <Text style={styles.gpsText}>Usar mi ubicación actual</Text>
      </TouchableOpacity>

      {estado === 'denegado' && (
        <Text style={styles.aviso}>
          {errorGps ??
            'Permiso de ubicación denegado. Puedes ajustar el marcador en el mapa manualmente.'}
        </Text>
      )}

      {valor && (
        <Text style={styles.coords}>
          Punto fijado: {valor.latitude.toFixed(5)}, {valor.longitude.toFixed(5)}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  buscadorRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
  },
  buscadorInput: {
    flex: 1,
  },
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
  candidatoText: {
    flex: 1,
    color: colors.textPrimary,
    fontSize: 13,
  },
  mapaContenedor: {
    height: 240,
    borderRadius: 16,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    backgroundColor: colors.surface,
    marginBottom: 10,
  },
  mapa: { flex: 1 },
  hint: {
    position: 'absolute',
    top: 8,
    left: 8,
    right: 8,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
    backgroundColor: colors.overlay,
  },
  hintText: {
    color: colors.textSecondary,
    fontSize: 11,
    textAlign: 'center',
  },
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
  },
  gpsText: {
    color: colors.neonOrange,
    fontSize: 14,
    fontWeight: '700',
  },
  aviso: {
    color: colors.textSecondary,
    fontSize: 12,
    marginTop: 8,
  },
  coords: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 8,
  },
});
