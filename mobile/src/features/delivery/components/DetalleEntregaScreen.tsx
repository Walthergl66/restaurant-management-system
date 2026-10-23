import React from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors } from '@/theme/colors';
import { useMisEntregas } from '../useMisEntregas';
import { useUbicacion } from '../useUbicacion';
import { useEntregaIndividual } from '../useEntregaIndividual';
import { estiloEstadoAsignacion } from '../estados';
import EstadoBadge from './EstadoBadge';
import MapaRuta from './MapaRuta';

/**
 * Detalle de una entrega individual (rol DELIVERY). Muestra el mapa nativo con
 * la ruta (restaurante -> destino), distancia/tiempo ESTIMADOS y el flujo real
 * cableado a la API: iniciar reparto -> marcar entregado -> finalizar recorrido.
 *
 * react-native-maps y expo-location se importan solo desde esta pantalla del
 * grupo (delivery) a través de MapaRuta/useUbicacion, para no romper Expo Go.
 */
export default function DetalleEntregaScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { id } = useLocalSearchParams<{ id: string }>();
  const asignacionId = Number(id);
  const { datos, loading, error, noHabilitado, refrescar } = useMisEntregas();

  const entrega = datos?.asignaciones.find((e) => e.asignacionId === asignacionId);

  const ubicacion = useUbicacion();
  const flujo = useEntregaIndividual(asignacionId, entrega, datos, ubicacion.ubicacion);

  const recorrido = datos?.recorridos.find((r) =>
    r.paradas.some((p) => p.asignacionId === asignacionId)
  );
  const parada = recorrido?.paradas.find((p) => p.asignacionId === asignacionId);
  const origenCoord =
    recorrido?.origenLat != null && recorrido?.origenLng != null
      ? { latitude: Number(recorrido.origenLat), longitude: Number(recorrido.origenLng) }
      : null;
  const destinoCoord =
    parada?.lat != null && parada?.lng != null
      ? { latitude: parada.lat, longitude: parada.lng }
      : null;
  const hayCoordenadas = Boolean(origenCoord || destinoCoord || ubicacion.ubicacion);

  const header = (
    <View style={[styles.header, { paddingTop: insets.top + 12 }]}>
      <TouchableOpacity
        onPress={() => router.back()}
        accessibilityRole="button"
        accessibilityLabel="Volver"
        hitSlop={10}
      >
        <Ionicons name="chevron-back" size={26} color={colors.textPrimary} />
      </TouchableOpacity>
      <Text style={styles.title}>Detalle de entrega</Text>
      <View style={{ width: 26 }} />
    </View>
  );

  if (loading) {
    return (
      <View style={styles.screen}>
        {header}
        <View style={styles.center}>
          <ActivityIndicator color={colors.neonOrange} />
        </View>
      </View>
    );
  }

  if (!entrega) {
    return (
      <View style={styles.screen}>
        {header}
        <View style={styles.center}>
          <Text style={styles.muted}>
            {noHabilitado
              ? 'Aún no estás habilitado para repartir.'
              : error ?? 'No se encontró esta entrega.'}
          </Text>
        </View>
      </View>
    );
  }

  const estado = estiloEstadoAsignacion(entrega.estadoAsignacion);

  const distanciaKm =
    flujo.ruta != null ? (flujo.ruta.distanciaM / 1000).toFixed(1) : null;
  const duracionMin =
    flujo.ruta != null ? Math.round(flujo.ruta.duracionS / 60) : null;

  const finalizar = async () => {
    try {
      await flujo.finalizarRecorrido();
      await refrescar();
      router.back();
    } catch {
      // El error ya quedó reflejado en flujo.accionError; no navegamos.
    }
  };

  const puedeIniciar =
    !flujo.recorridoEnCurso && !flujo.paradaEntregada && !flujo.recorridoFinalizado;
  const puedeEntregar = flujo.recorridoEnCurso && flujo.paradaId != null && !flujo.paradaEntregada;
  const puedeFinalizar = flujo.recorridoId != null && flujo.paradaEntregada;

  return (
    <View style={styles.screen}>
      {header}
      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 40 }]}
      >
        <View style={styles.topRow}>
          <Text style={styles.codigo}>#{entrega.codigo}</Text>
          <EstadoBadge etiqueta={estado.etiqueta} color={estado.color} />
        </View>

        {/* Mapa de la ruta individual. Se muestra aunque ORS falle, si hay coords. */}
        {flujo.rutaCargando ? (
          <View style={styles.mapLoading}>
            <ActivityIndicator color={colors.neonOrange} />
            <Text style={styles.mapText}>Calculando ruta…</Text>
          </View>
        ) : flujo.rutaNoDisponible ? (
          <View style={styles.mapInfo}>
            <Ionicons name="cloud-offline-outline" size={30} color={colors.warning} />
            <Text style={styles.mapInfoText}>
              Rutas no disponibles ahora mismo, intenta más tarde.
            </Text>
          </View>
        ) : hayCoordenadas || flujo.ruta ? (
          <>
            <MapaRuta
              geometria={flujo.ruta?.geometria ?? ''}
              ubicacionRepartidor={ubicacion.ubicacion}
              paradas={
                destinoCoord
                  ? [{ orden: 0, latitude: destinoCoord.latitude, longitude: destinoCoord.longitude }]
                  : []
              }
              origen={origenCoord}
            />
            {flujo.rutaError ? (
              <View style={styles.mapInfo}>
                <Ionicons name="alert-circle-outline" size={30} color={colors.error} />
                <Text style={styles.mapInfoText}>{flujo.rutaError}</Text>
              </View>
            ) : null}
          </>
        ) : (
          <View style={styles.mapInfo}>
            <Ionicons name="location-outline" size={30} color={colors.warning} />
            <Text style={styles.mapInfoText}>
              {flujo.rutaError ??
                'No se pudo ubicar la dirección del pedido. Corrígela desde el checkout o contacta al cliente.'}
            </Text>
          </View>
        )}

        {/* Banner de permiso de ubicación denegado (no bloquea la ruta). */}
        {ubicacion.permiso === 'denegado' && (
          <View style={styles.permisoBanner}>
            <Ionicons name="location-outline" size={18} color={colors.warning} />
            <Text style={styles.permisoText}>
              Sin permiso de ubicación. Puedes ver la ruta, pero no tu posición en el mapa.
            </Text>
          </View>
        )}

        {/* Distancia y tiempo estimados */}
        {flujo.ruta && (
          <View style={styles.metrics}>
            <View style={styles.metric}>
              <Ionicons name="navigate-outline" size={16} color={colors.neonOrange} />
              <Text style={styles.metricValue}>{distanciaKm} km</Text>
              <Text style={styles.metricLabel}>Distancia estimada</Text>
            </View>
            <View style={styles.metricDivider} />
            <View style={styles.metric}>
              <Ionicons name="time-outline" size={16} color={colors.neonOrange} />
              <Text style={styles.metricValue}>{duracionMin} min</Text>
              <Text style={styles.metricLabel}>Tiempo estimado</Text>
            </View>
          </View>
        )}

        <View style={styles.card}>
          <Dato icono="location-outline" etiqueta="Dirección" valor={entrega.direccion} />
          <Dato icono="person-outline" etiqueta="Cliente" valor={entrega.clienteNombre} />
          <Dato
            icono="call-outline"
            etiqueta="Teléfono"
            valor={entrega.telefono ?? 'No disponible'}
          />
        </View>

        {flujo.accionError && (
          <View style={styles.errorBox}>
            <Text style={styles.error}>{flujo.accionError}</Text>
          </View>
        )}

        <View style={styles.actions}>
          <AccionBtn
            icono="play"
            texto="Iniciar reparto"
            onPress={() => void flujo.iniciarReparto()}
            cargando={flujo.accion === 'iniciar'}
            habilitado={puedeIniciar && flujo.accion == null}
            variante="primaria"
          />
          <AccionBtn
            icono="checkmark-done"
            texto={flujo.paradaEntregada ? 'Entregado' : 'Marcar como entregado'}
            onPress={() => void flujo.marcarEntregado()}
            cargando={flujo.accion === 'entregar'}
            habilitado={puedeEntregar && flujo.accion == null}
            variante="primaria"
          />
          <AccionBtn
            icono="flag"
            texto="Finalizar recorrido"
            onPress={() => void finalizar()}
            cargando={flujo.accion === 'finalizar'}
            habilitado={puedeFinalizar && flujo.accion == null}
            variante="secundaria"
          />
        </View>
      </ScrollView>
    </View>
  );
}

function AccionBtn({
  icono,
  texto,
  onPress,
  cargando,
  habilitado,
  variante,
}: {
  icono: React.ComponentProps<typeof Ionicons>['name'];
  texto: string;
  onPress: () => void;
  cargando: boolean;
  habilitado: boolean;
  variante: 'primaria' | 'secundaria';
}) {
  const activo = habilitado && !cargando;
  const color = variante === 'primaria' ? colors.neonOrange : colors.neonPink;
  return (
    <TouchableOpacity
      style={[styles.action, activo ? { borderColor: color } : styles.actionDisabled]}
      disabled={!activo}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={texto}
      accessibilityState={{ disabled: !activo, busy: cargando }}
    >
      {cargando ? (
        <ActivityIndicator color={color} />
      ) : (
        <Ionicons name={icono} size={18} color={activo ? color : colors.textMuted} />
      )}
      <Text style={[styles.actionText, activo && { color }]}>{texto}</Text>
    </TouchableOpacity>
  );
}

function Dato({
  icono,
  etiqueta,
  valor,
}: {
  icono: React.ComponentProps<typeof Ionicons>['name'];
  etiqueta: string;
  valor: string;
}) {
  return (
    <View style={styles.dato}>
      <Ionicons name={icono} size={18} color={colors.neonOrange} />
      <View style={{ flex: 1 }}>
        <Text style={styles.datoLabel}>{etiqueta}</Text>
        <Text style={styles.datoValor}>{valor}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 14,
  },
  title: { fontSize: 18, fontWeight: '800', color: colors.textPrimary },
  content: { paddingHorizontal: 20, gap: 16 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  muted: { color: colors.textMuted, textAlign: 'center' },
  topRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  codigo: { color: colors.textPrimary, fontSize: 22, fontWeight: '900' },
  mapLoading: {
    height: 160,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  mapInfo: {
    minHeight: 120,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    padding: 20,
  },
  mapInfoText: { color: colors.textSecondary, textAlign: 'center' },
  mapText: { color: colors.textMuted, fontWeight: '700' },
  permisoBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 12,
    borderRadius: 12,
    backgroundColor: 'rgba(255,170,0,.12)',
  },
  permisoText: { color: colors.textSecondary, fontSize: 12, flex: 1 },
  metrics: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  metric: { flex: 1, alignItems: 'center', gap: 4 },
  metricDivider: { width: 1, height: 40, backgroundColor: colors.surfaceBorder },
  metricValue: { color: colors.textPrimary, fontSize: 18, fontWeight: '900' },
  metricLabel: { color: colors.textMuted, fontSize: 11 },
  card: {
    padding: 16,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    gap: 16,
  },
  dato: { flexDirection: 'row', alignItems: 'flex-start', gap: 12 },
  datoLabel: { color: colors.textMuted, fontSize: 11, fontWeight: '800', letterSpacing: 0.5 },
  datoValor: { color: colors.textPrimary, fontSize: 15, marginTop: 2 },
  errorBox: {
    padding: 14,
    borderRadius: 12,
    backgroundColor: 'rgba(255,0,80,.12)',
  },
  error: { color: colors.neonPink },
  actions: { gap: 10 },
  action: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 15,
    borderRadius: 12,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  actionDisabled: { opacity: 0.5, borderColor: colors.surfaceBorder },
  actionText: { color: colors.textSecondary, fontWeight: '800' },
});
