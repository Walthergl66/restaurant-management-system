import React, { useMemo, useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors } from '@/theme/colors';
import { useMisEntregas } from '../useMisEntregas';
import { MIN_PARADAS_MULTIENTREGA, useMultientrega } from '../useMultientrega';
import type { CriterioOptimizacion, EntregaResumen, ParadaPropuesta } from '../types';
import MapaRuta from './MapaRuta';
import { useUbicacion } from '../useUbicacion';

/**
 * Pantalla de armado de multientrega (FASE 8).
 *
 * Flujo: seleccionar 2+ pedidos asignados -> "Optimizar ruta" (ORS/VROOM en el
 * backend) -> ver el recorrido dibujado + paradas numeradas en el ORDEN
 * propuesto + totales estimados -> "Iniciar recorrido" (crea MULTIPLE en ese
 * orden y lo inicia) -> vuelve a Mis entregas con el recorrido EN_CURSO.
 *
 * La confirmación parada-por-parada y el recálculo son FASE 9.
 *
 * LIMITACIÓN DE MARCADORES: el backend no devuelve lat/lng por parada en
 * PropuestaMultientrega (ParadaPropuesta trae orden/asignacionId/codigo/
 * direccion, sin coordenadas). Por eso el mapa dibuja el recorrido completo
 * (polyline) + el origen (restaurante), y las paradas se numeran de forma
 * fiable en la LISTA de abajo. Para marcadores numerados precisos en el mapa
 * haría falta ampliar el backend para incluir lat/lng por parada (pendiente,
 * no se toca el backend en esta fase).
 */

function derivarDisponibles(asignaciones: EntregaResumen[]): EntregaResumen[] {
  // Seleccionables: asignaciones ASIGNADA que aún no están en un recorrido.
  return asignaciones.filter(
    (a) => a.estadoAsignacion === 'ASIGNADA' && a.recorridoId == null
  );
}

function kmDesdeMetros(metros: number): string {
  return (metros / 1000).toLocaleString('es', { maximumFractionDigits: 1 });
}

function minutosDesdeSegundos(segundos: number): string {
  return Math.max(1, Math.round(segundos / 60)).toString();
}

export default function MultientregaScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { datos, loading, error: errorCarga, noHabilitado, refrescar } = useMisEntregas();
  const { ubicacion } = useUbicacion();

  const [criterio, setCriterio] = useState<CriterioOptimizacion>('DURACION');

  const disponibles = useMemo(
    () => derivarDisponibles(datos?.asignaciones ?? []),
    [datos]
  );

  const {
    seleccion,
    propuesta,
    accion,
    error,
    puedeOptimizar,
    alternar,
    optimizar,
    iniciarRecorrido,
  } = useMultientrega(disponibles);

  const datosPorAsignacion = useMemo(() => {
    const mapa = new Map<number, EntregaResumen>();
    for (const d of disponibles) mapa.set(d.asignacionId, d);
    return mapa;
  }, [disponibles]);

  const paradasOrdenadas = useMemo<ParadaPropuesta[]>(
    () => (propuesta ? [...propuesta.paradas].sort((a, b) => a.orden - b.orden) : []),
    [propuesta]
  );

  const onIniciar = async () => {
    const iniciado = await iniciarRecorrido();
    if (iniciado) {
      await refrescar();
      router.replace('/(delivery)');
    }
  };

  const header = (
    <View style={[styles.header, { paddingTop: insets.top + 12 }]}>
      <TouchableOpacity
        onPress={() => router.back()}
        accessibilityRole="button"
        accessibilityLabel="Volver"
        hitSlop={10}
      >
        <Ionicons name="chevron-back" size={26} color={colors.neonOrange} />
      </TouchableOpacity>
      <View style={styles.headerTextos}>
        <Text style={styles.title}>Ruta de múltiples entregas</Text>
        <Text style={styles.subtitle}>
          Agrupa pedidos y optimiza el orden de paradas.
        </Text>
      </View>
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

  const cargandoOptimizar = accion === 'optimizar';
  const cargandoIniciar = accion === 'iniciar';

  return (
    <View style={styles.screen}>
      {header}
      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 40 }]}
      >
        {noHabilitado && (
          <View style={styles.stateBox}>
            <Ionicons name="lock-closed-outline" size={30} color={colors.warning} />
            <Text style={styles.stateTitle}>Aún no estás habilitado para repartir</Text>
          </View>
        )}

        {errorCarga && !noHabilitado && (
          <View style={styles.errorBox}>
            <Text style={styles.errorText}>{errorCarga}</Text>
            <TouchableOpacity onPress={() => void refrescar()} style={styles.retry}>
              <Text style={styles.retryText}>Reintentar</Text>
            </TouchableOpacity>
          </View>
        )}

        {!noHabilitado && !errorCarga && (
          <>
            {/* Selección de pedidos */}
            <View style={styles.section}>
              <View style={styles.headerRow}>
                <Text style={styles.sectionLabel}>PEDIDOS A AGRUPAR</Text>
                <Text style={styles.contador}>{seleccion.length} seleccionados</Text>
              </View>

              {disponibles.length === 0 ? (
                <View style={styles.stateBox}>
                  <Ionicons name="cube-outline" size={30} color={colors.textMuted} />
                  <Text style={styles.stateTitle}>Sin pedidos disponibles para agrupar</Text>
                  <Text style={styles.stateText}>
                    Solo se pueden agrupar pedidos asignados que aún no estén en un recorrido.
                  </Text>
                </View>
              ) : (
                disponibles.map((entrega) => {
                  const marcada = seleccion.includes(entrega.asignacionId);
                  return (
                    <TouchableOpacity
                      key={entrega.asignacionId}
                      style={[styles.item, marcada && styles.itemMarcado]}
                      onPress={() => alternar(entrega.asignacionId)}
                      accessibilityRole="checkbox"
                      accessibilityState={{ checked: marcada }}
                      accessibilityLabel={`Pedido ${entrega.codigo}`}
                    >
                      <Ionicons
                        name={marcada ? 'checkbox' : 'square-outline'}
                        size={22}
                        color={marcada ? colors.neonOrange : colors.textMuted}
                      />
                      <View style={styles.itemTextos}>
                        <Text style={styles.itemCodigo}>{entrega.codigo}</Text>
                        <Text style={styles.itemDireccion} numberOfLines={2}>
                          {entrega.direccion}
                        </Text>
                      </View>
                    </TouchableOpacity>
                  );
                })
              )}

              {seleccion.length === 1 && (
                <Text style={styles.ayuda}>
                  Selecciona al menos {MIN_PARADAS_MULTIENTREGA} pedidos. Para uno solo,
                  usa la entrega individual desde Mis entregas.
                </Text>
              )}
            </View>

            {/* Criterio + optimizar */}
            {disponibles.length > 0 && (
              <View style={styles.section}>
                <Text style={styles.sectionLabel}>CRITERIO</Text>
                <View style={styles.toggleFila}>
                  {(['DURACION', 'DISTANCIA'] as const).map((opcion) => {
                    const activo = criterio === opcion;
                    return (
                      <TouchableOpacity
                        key={opcion}
                        style={[styles.toggle, activo && styles.toggleActivo]}
                        onPress={() => setCriterio(opcion)}
                        accessibilityRole="button"
                        accessibilityState={{ selected: activo }}
                      >
                        <Text style={[styles.toggleText, activo && styles.toggleTextActivo]}>
                          {opcion === 'DURACION' ? 'Más rápido' : 'Menos distancia'}
                        </Text>
                      </TouchableOpacity>
                    );
                  })}
                </View>

                <TouchableOpacity
                  style={[styles.boton, (!puedeOptimizar || cargandoOptimizar) && styles.botonOff]}
                  onPress={() => void optimizar(criterio)}
                  disabled={!puedeOptimizar || accion !== null}
                  accessibilityRole="button"
                  accessibilityLabel="Optimizar ruta"
                >
                  {cargandoOptimizar ? (
                    <ActivityIndicator color="#000" />
                  ) : (
                    <Text style={styles.botonText}>Optimizar ruta</Text>
                  )}
                </TouchableOpacity>
              </View>
            )}

            {error && (
              <View style={styles.errorBox}>
                <Text style={styles.errorText}>{error}</Text>
              </View>
            )}

            {/* Propuesta: mapa + lista numerada + totales */}
            {propuesta && (
              <View style={styles.section}>
                <Text style={styles.sectionLabel}>RECORRIDO PROPUESTO</Text>

                <MapaRuta geometria={propuesta.geometria} ubicacionRepartidor={ubicacion} />

                <View style={styles.totales}>
                  <View style={styles.totalItem}>
                    <Text style={styles.totalValor}>
                      {kmDesdeMetros(propuesta.distanciaTotalM)} km
                    </Text>
                    <Text style={styles.totalLabel}>Distancia estimada</Text>
                  </View>
                  <View style={styles.totalItem}>
                    <Text style={styles.totalValor}>
                      {minutosDesdeSegundos(propuesta.duracionTotalS)} min
                    </Text>
                    <Text style={styles.totalLabel}>Duración estimada</Text>
                  </View>
                </View>

                <Text style={styles.ayuda}>
                  Totales estimados por el servicio de rutas. El orden lo optimiza
                  OpenRouteService; los marcadores precisos por parada requieren datos
                  adicionales (las paradas se numeran en la lista).
                </Text>

                {paradasOrdenadas.map((parada) => {
                  const detalle = datosPorAsignacion.get(parada.asignacionId);
                  return (
                    <View key={parada.asignacionId} style={styles.parada}>
                      <View style={styles.paradaNum}>
                        <Text style={styles.paradaNumText}>{parada.orden + 1}</Text>
                      </View>
                      <View style={styles.itemTextos}>
                        <Text style={styles.itemCodigo}>
                          {parada.codigo ?? detalle?.codigo ?? 'Pedido'}
                        </Text>
                        <Text style={styles.itemDireccion} numberOfLines={2}>
                          {parada.direccion ?? detalle?.direccion ?? 'Dirección no disponible'}
                        </Text>
                      </View>
                    </View>
                  );
                })}

                <TouchableOpacity
                  style={[styles.boton, cargandoIniciar && styles.botonOff]}
                  onPress={() => void onIniciar()}
                  disabled={accion !== null}
                  accessibilityRole="button"
                  accessibilityLabel="Iniciar recorrido"
                >
                  {cargandoIniciar ? (
                    <ActivityIndicator color="#000" />
                  ) : (
                    <Text style={styles.botonText}>Iniciar recorrido</Text>
                  )}
                </TouchableOpacity>
              </View>
            )}
          </>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
    paddingHorizontal: 16,
    paddingBottom: 14,
  },
  headerTextos: { flex: 1 },
  title: { fontSize: 22, fontWeight: '800', color: colors.textPrimary },
  subtitle: { color: colors.textMuted, marginTop: 4, fontSize: 13 },
  content: { paddingHorizontal: 20, gap: 16 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  section: { gap: 12 },
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sectionLabel: {
    color: colors.textSecondary,
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 1,
  },
  contador: { color: colors.neonOrange, fontSize: 12, fontWeight: '800' },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 14,
    borderRadius: 14,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  itemMarcado: { borderColor: colors.neonOrange },
  itemTextos: { flex: 1 },
  itemCodigo: { color: colors.textPrimary, fontWeight: '800', fontSize: 15 },
  itemDireccion: { color: colors.textMuted, fontSize: 13, marginTop: 2 },
  ayuda: { color: colors.textMuted, fontSize: 12, lineHeight: 17 },
  toggleFila: { flexDirection: 'row', gap: 10 },
  toggle: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 12,
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  toggleActivo: { borderColor: colors.neonOrange, backgroundColor: colors.neonOrangeAlpha },
  toggleText: { color: colors.textSecondary, fontWeight: '700', fontSize: 13 },
  toggleTextActivo: { color: colors.neonOrange },
  boton: {
    paddingVertical: 14,
    borderRadius: 14,
    alignItems: 'center',
    backgroundColor: colors.neonOrange,
  },
  botonOff: { opacity: 0.5 },
  botonText: { color: '#000', fontWeight: '800', fontSize: 15 },
  totales: { flexDirection: 'row', gap: 12 },
  totalItem: {
    flex: 1,
    padding: 14,
    borderRadius: 14,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  totalValor: { color: colors.textPrimary, fontWeight: '800', fontSize: 18 },
  totalLabel: { color: colors.textMuted, fontSize: 12, marginTop: 2 },
  parada: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 14,
    borderRadius: 14,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  paradaNum: {
    width: 30,
    height: 30,
    borderRadius: 15,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.neonOrange,
  },
  paradaNumText: { color: '#000', fontWeight: '800' },
  stateBox: {
    alignItems: 'center',
    gap: 8,
    padding: 24,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  stateTitle: { color: colors.textPrimary, fontWeight: '800', fontSize: 15, textAlign: 'center' },
  stateText: { color: colors.textMuted, fontSize: 13, textAlign: 'center' },
  errorBox: {
    padding: 16,
    borderRadius: 14,
    backgroundColor: 'rgba(255,0,80,.12)',
    gap: 10,
  },
  errorText: { color: colors.neonPink },
  retry: {
    alignSelf: 'flex-start',
    paddingVertical: 6,
    paddingHorizontal: 14,
    borderRadius: 10,
    backgroundColor: colors.neonPink,
  },
  retryText: { color: '#000', fontWeight: '800' },
});
