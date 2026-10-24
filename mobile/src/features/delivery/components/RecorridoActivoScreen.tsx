import React, { useCallback, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Modal,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors } from '@/theme/colors';
import { estiloEstadoParada, estiloEstadoRecorrido, estiloFaseConfirmacion } from '../estados';
import { useRecorridoActivo, type EstadoParadaUi } from '../useRecorridoActivo';
import { useUbicacion } from '../useUbicacion';
import type { ParadaResumen } from '../types';
import MapaRuta, { type ParadaMapa } from './MapaRuta';
import EstadoBadge from './EstadoBadge';

/**
 * Pantalla de un recorrido EN CURSO (FASE 9): mapa con la ruta + marcadores
 * numerados de cada parada (lat/lng del backend) + restaurante (origen) +
 * ubicación del repartidor; lista de paradas destacando la siguiente pendiente;
 * confirmación parada-por-parada (idempotente), incidencias, recálculo manual
 * y finalización solo con todo resuelto. Banner de "Sin conexión" y cola
 * offline idempotente para las entregas (ver useRecorridoActivo/colaOffline).
 */
export default function RecorridoActivoScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { id } = useLocalSearchParams<{ id: string }>();
  const recorridoId = Number(id);

  const {
    recorrido,
    cargando,
    error,
    sinConexion,
    estadoPorParada,
    accion,
    accionError,
    geometriaRecalculada,
    todasResueltas,
    refrescar,
    marcarEntregada,
    registrarIncidencia,
    recalcular,
    finalizar,
    sincronizarPendientes,
  } = useRecorridoActivo(recorridoId);

  const { ubicacion, permiso } = useUbicacion();

  const [incidenciaParada, setIncidenciaParada] = useState<number | null>(null);
  const [motivo, setMotivo] = useState('');

  // Al recuperar el foco, intenta sincronizar entregas encoladas offline.
  useFocusEffect(
    useCallback(() => {
      void sincronizarPendientes();
    }, [sincronizarPendientes])
  );

  const paradas = useMemo(() => recorrido?.paradas ?? [], [recorrido]);

  // Siguiente pendiente: la de menor orden aún sin resolver (se destaca).
  const siguientePendienteId = useMemo(() => {
    const pendientes = paradas
      .filter((p) => p.estado === 'PENDIENTE')
      .sort((a, b) => a.orden - b.orden);
    return pendientes.length > 0 ? pendientes[0].paradaId : null;
  }, [paradas]);

  // Marcadores del mapa solo para paradas con coordenadas fiables.
  const paradasMapa = useMemo<ParadaMapa[]>(
    () =>
      paradas
        .filter((p): p is ParadaResumen & { lat: number; lng: number } => p.lat != null && p.lng != null)
        .map((p) => ({
          orden: p.orden,
          latitude: p.lat,
          longitude: p.lng,
          codigo: p.codigo,
          resuelta: p.estado !== 'PENDIENTE',
        })),
    [paradas]
  );

  const origen = useMemo(
    () =>
      recorrido?.origenLat != null && recorrido?.origenLng != null
        ? { latitude: recorrido.origenLat, longitude: recorrido.origenLng }
        : null,
    [recorrido]
  );

  const geometria = geometriaRecalculada ?? '';
  const gpsDenegado = permiso === 'denegado';

  const onRecalcular = () => {
    if (!ubicacion) return;
    void recalcular(ubicacion.latitude, ubicacion.longitude);
  };

  const onFinalizar = async () => {
    const ok = await finalizar();
    if (ok) {
      router.back();
    }
  };

  const abrirIncidencia = (paradaId: number) => {
    setIncidenciaParada(paradaId);
    setMotivo('');
  };

  const confirmarIncidencia = async () => {
    if (incidenciaParada == null || motivo.trim().length === 0) return;
    const paradaId = incidenciaParada;
    setIncidenciaParada(null);
    await registrarIncidencia(paradaId, motivo.trim());
    setMotivo('');
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
        <Text style={styles.title}>Recorrido en curso</Text>
        {recorrido && (
          <Text style={styles.subtitle}>
            {recorrido.tipo === 'MULTIPLE' ? 'Multientrega' : 'Entrega individual'}
          </Text>
        )}
      </View>
      {recorrido && (
        <EstadoBadge
          etiqueta={estiloEstadoRecorrido(recorrido.estado).etiqueta}
          color={estiloEstadoRecorrido(recorrido.estado).color}
        />
      )}
    </View>
  );

  if (cargando) {
    return (
      <View style={styles.screen}>
        {header}
        <View style={styles.center}>
          <ActivityIndicator color={colors.neonOrange} />
        </View>
      </View>
    );
  }

  const resueltas = paradas.filter((p) => p.estado !== 'PENDIENTE').length;

  return (
    <View style={styles.screen}>
      {header}

      {sinConexion && (
        <View style={styles.bannerOffline} accessibilityLiveRegion="polite">
          <Ionicons name="cloud-offline-outline" size={16} color="#000" />
          <Text style={styles.bannerOfflineText}>
            Sin conexión. Las entregas se sincronizarán al volver la red.
          </Text>
          <TouchableOpacity
            onPress={() => void sincronizarPendientes()}
            accessibilityRole="button"
            accessibilityLabel="Reintentar sincronización"
          >
            <Ionicons name="refresh" size={16} color="#000" />
          </TouchableOpacity>
        </View>
      )}

      <ScrollView contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 40 }]}>
        {error && !recorrido && (
          <View style={styles.errorBox}>
            <Text style={styles.errorText}>{error}</Text>
            <TouchableOpacity onPress={() => void refrescar()} style={styles.retry}>
              <Text style={styles.retryText}>Reintentar</Text>
            </TouchableOpacity>
          </View>
        )}

        {recorrido && (
          <>
            <MapaRuta
              geometria={geometria}
              ubicacionRepartidor={ubicacion}
              paradas={paradasMapa}
              origen={origen}
            />

            <View style={styles.acciones}>
              <TouchableOpacity
                style={[styles.botonSec, (!ubicacion || accion === 'recalcular') && styles.botonOff]}
                onPress={onRecalcular}
                disabled={!ubicacion || accion !== null}
                accessibilityRole="button"
                accessibilityLabel="Recalcular ruta desde mi ubicación"
              >
                {accion === 'recalcular' ? (
                  <ActivityIndicator color={colors.neonOrange} />
                ) : (
                  <>
                    <Ionicons name="navigate-outline" size={18} color={colors.neonOrange} />
                    <Text style={styles.botonSecText}>Recalcular ruta</Text>
                  </>
                )}
              </TouchableOpacity>
            </View>
            {gpsDenegado && (
              <Text style={styles.ayuda}>
                Activa el permiso de ubicación para recalcular la ruta desde tu posición.
              </Text>
            )}

            <View style={styles.section}>
              <View style={styles.headerRow}>
                <Text style={styles.sectionLabel}>PARADAS</Text>
                <Text style={styles.contador}>
                  {resueltas}/{paradas.length} resueltas
                </Text>
              </View>

              {paradas
                .slice()
                .sort((a, b) => a.orden - b.orden)
                .map((parada) => (
                  <ParadaItem
                    key={parada.paradaId}
                    parada={parada}
                    esSiguiente={parada.paradaId === siguientePendienteId}
                    estadoUi={estadoPorParada[parada.paradaId] ?? 'idle'}
                    onEntregar={() => void marcarEntregada(parada.paradaId)}
                    onIncidencia={() => abrirIncidencia(parada.paradaId)}
                  />
                ))}
            </View>

            {accionError && (
              <View style={styles.errorBox}>
                <Text style={styles.errorText}>{accionError}</Text>
              </View>
            )}

            <TouchableOpacity
              style={[styles.boton, (!todasResueltas || accion === 'finalizar') && styles.botonOff]}
              onPress={() => void onFinalizar()}
              disabled={!todasResueltas || accion !== null}
              accessibilityRole="button"
              accessibilityLabel="Finalizar recorrido"
            >
              {accion === 'finalizar' ? (
                <ActivityIndicator color="#000" />
              ) : (
                <Text style={styles.botonText}>Finalizar recorrido</Text>
              )}
            </TouchableOpacity>
            {!todasResueltas && (
              <Text style={styles.ayuda}>
                Resuelve todas las paradas (entregadas o con incidencia) para finalizar.
              </Text>
            )}
          </>
        )}
      </ScrollView>

      {/* Modal de incidencia: motivo obligatorio. */}
      <Modal
        visible={incidenciaParada != null}
        transparent
        animationType="fade"
        onRequestClose={() => setIncidenciaParada(null)}
      >
        <View style={styles.modalFondo}>
          <View style={styles.modalCaja}>
            <Text style={styles.modalTitulo}>Registrar incidencia</Text>
            <Text style={styles.modalSub}>Indica el motivo por el que no se pudo entregar.</Text>
            <TextInput
              style={styles.input}
              value={motivo}
              onChangeText={setMotivo}
              placeholder="Ej. cliente ausente, dirección incorrecta…"
              placeholderTextColor={colors.textMuted}
              multiline
              accessibilityLabel="Motivo de la incidencia"
            />
            <View style={styles.modalBotones}>
              <TouchableOpacity
                style={[styles.botonSec, styles.modalBoton]}
                onPress={() => setIncidenciaParada(null)}
                accessibilityRole="button"
                accessibilityLabel="Cancelar"
              >
                <Text style={styles.botonSecText}>Cancelar</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.boton, styles.modalBoton, motivo.trim().length === 0 && styles.botonOff]}
                onPress={() => void confirmarIncidencia()}
                disabled={motivo.trim().length === 0}
                accessibilityRole="button"
                accessibilityLabel="Guardar incidencia"
              >
                <Text style={styles.botonText}>Guardar</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

interface ParadaItemProps {
  parada: ParadaResumen;
  esSiguiente: boolean;
  estadoUi: EstadoParadaUi;
  onEntregar: () => void;
  onIncidencia: () => void;
}

function ParadaItem({ parada, esSiguiente, estadoUi, onEntregar, onIncidencia }: ParadaItemProps) {
  const estilo = estiloEstadoParada(parada.estado);
  const resuelta = parada.estado !== 'PENDIENTE';
  const enviando = estadoUi === 'enviando';
  const pendienteSync = estadoUi === 'pendiente-sync';

  return (
    <View
      style={[
        styles.parada,
        resuelta && styles.paradaResuelta,
        esSiguiente && styles.paradaSiguiente,
      ]}
    >
      <View style={styles.paradaFila}>
        <View style={[styles.paradaNum, resuelta && styles.paradaNumResuelta]}>
          <Text style={styles.paradaNumText}>{parada.orden + 1}</Text>
        </View>
        <View style={styles.itemTextos}>
          <Text style={styles.itemCodigo}>{parada.codigo ?? 'Pedido'}</Text>
          <Text style={styles.itemDireccion} numberOfLines={2}>
            {parada.direccion ?? 'Dirección no disponible'}
          </Text>
        </View>
        <EstadoBadge etiqueta={estilo.etiqueta} color={estilo.color} />
      </View>

      {esSiguiente && !resuelta && (
        <Text style={styles.proximaNota}>Siguiente parada</Text>
      )}

      {/* Doble confirmación: tras entregar, la parada queda a la espera de que
          el cliente confirme. No bloquea al repartidor (puede seguir/finalizar). */}
      {parada.estado === 'ENTREGADA' && (
        <View style={styles.faseConfirmacion}>
          <EstadoBadge
            etiqueta={estiloFaseConfirmacion(parada.faseConfirmacion).etiqueta}
            color={estiloFaseConfirmacion(parada.faseConfirmacion).color}
          />
        </View>
      )}

      {pendienteSync && (
        <Text style={styles.syncNota}>Pendiente de sincronizar (sin conexión)</Text>
      )}

      {!resuelta && (
        <View style={styles.paradaBotones}>
          <TouchableOpacity
            style={[styles.boton, styles.paradaBoton, enviando && styles.botonOff]}
            onPress={onEntregar}
            disabled={enviando}
            accessibilityRole="button"
            accessibilityLabel={`Marcar entregado el pedido ${parada.codigo ?? ''}`}
          >
            {enviando ? (
              <ActivityIndicator color="#000" />
            ) : (
              <Text style={styles.botonText}>He llegado / Entregar</Text>
            )}
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.botonSec, styles.paradaBoton, enviando && styles.botonOff]}
            onPress={onIncidencia}
            disabled={enviando}
            accessibilityRole="button"
            accessibilityLabel={`Registrar incidencia del pedido ${parada.codigo ?? ''}`}
          >
            <Text style={styles.botonSecText}>Incidencia</Text>
          </TouchableOpacity>
        </View>
      )}
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
  content: { paddingHorizontal: 20, gap: 14 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  bannerOffline: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginHorizontal: 20,
    marginBottom: 6,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 10,
    backgroundColor: colors.warning,
  },
  bannerOfflineText: { flex: 1, color: '#000', fontWeight: '700', fontSize: 12 },
  section: { gap: 12 },
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sectionLabel: {
    color: colors.textSecondary,
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 1,
  },
  contador: { color: colors.neonOrange, fontSize: 12, fontWeight: '800' },
  acciones: { flexDirection: 'row', gap: 10 },
  ayuda: { color: colors.textMuted, fontSize: 12, lineHeight: 17 },
  itemTextos: { flex: 1 },
  itemCodigo: { color: colors.textPrimary, fontWeight: '800', fontSize: 15 },
  itemDireccion: { color: colors.textMuted, fontSize: 13, marginTop: 2 },
  parada: {
    padding: 14,
    borderRadius: 14,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    gap: 10,
  },
  paradaResuelta: { opacity: 0.6 },
  paradaSiguiente: { borderColor: colors.neonOrange },
  paradaFila: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  paradaNum: {
    width: 30,
    height: 30,
    borderRadius: 15,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.neonOrange,
  },
  paradaNumResuelta: { backgroundColor: colors.success },
  paradaNumText: { color: '#000', fontWeight: '800' },
  proximaNota: { color: colors.neonOrange, fontSize: 12, fontWeight: '700' },
  syncNota: { color: colors.warning, fontSize: 12, fontWeight: '700' },
  faseConfirmacion: { marginTop: 6, alignSelf: 'flex-start' },
  paradaBotones: { flexDirection: 'row', gap: 10 },
  paradaBoton: { flex: 1 },
  boton: {
    paddingVertical: 12,
    paddingHorizontal: 14,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.neonOrange,
  },
  botonText: { color: '#000', fontWeight: '800', fontSize: 14 },
  botonSec: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 12,
    paddingHorizontal: 14,
    borderRadius: 12,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.neonOrange,
  },
  botonSecText: { color: colors.neonOrange, fontWeight: '800', fontSize: 14 },
  botonOff: { opacity: 0.5 },
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
  modalFondo: {
    flex: 1,
    backgroundColor: colors.overlay,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  modalCaja: {
    width: '100%',
    borderRadius: 16,
    padding: 20,
    gap: 12,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  modalTitulo: { color: colors.textPrimary, fontWeight: '800', fontSize: 17 },
  modalSub: { color: colors.textMuted, fontSize: 13 },
  input: {
    minHeight: 72,
    borderRadius: 12,
    padding: 12,
    color: colors.textPrimary,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    textAlignVertical: 'top',
  },
  modalBotones: { flexDirection: 'row', gap: 10 },
  modalBoton: { flex: 1 },
});
