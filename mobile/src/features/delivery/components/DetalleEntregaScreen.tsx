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
import { estiloEstadoAsignacion } from '../estados';
import EstadoBadge from './EstadoBadge';

/**
 * Detalle de una entrega asignada. Muestra dirección, cliente, teléfono y
 * estado logístico. El mapa llega en la fase 7 (placeholder reservado, SIN
 * importar react-native-maps). Las acciones de operar quedan maquetadas y
 * deshabilitadas en esta fase.
 */
export default function DetalleEntregaScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { id } = useLocalSearchParams<{ id: string }>();
  const asignacionId = Number(id);
  const { datos, loading, error, noHabilitado } = useMisEntregas();

  const entrega = datos?.asignaciones.find((e) => e.asignacionId === asignacionId);

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

        {/* Placeholder del mapa — se implementa en la fase 7 (react-native-maps). */}
        <View style={styles.mapPlaceholder} accessibilityLabel="Mapa próximamente">
          <Ionicons name="map-outline" size={34} color={colors.textMuted} />
          <Text style={styles.mapText}>Mapa (próximamente)</Text>
        </View>

        <View style={styles.card}>
          <Dato icono="location-outline" etiqueta="Dirección" valor={entrega.direccion} />
          <Dato icono="person-outline" etiqueta="Cliente" valor={entrega.clienteNombre} />
          <Dato
            icono="call-outline"
            etiqueta="Teléfono"
            valor={entrega.telefono ?? 'No disponible'}
          />
        </View>

        <View style={styles.actions}>
          <TouchableOpacity style={[styles.action, styles.actionDisabled]} disabled>
            <Ionicons name="play" size={18} color={colors.textMuted} />
            <Text style={styles.actionText}>Iniciar reparto</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.action, styles.actionDisabled]} disabled>
            <Ionicons name="checkmark-done" size={18} color={colors.textMuted} />
            <Text style={styles.actionText}>Marcar como entregado</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.action, styles.actionDisabled]} disabled>
            <Ionicons name="flag" size={18} color={colors.textMuted} />
            <Text style={styles.actionText}>Finalizar recorrido</Text>
          </TouchableOpacity>
          <Text style={styles.hint}>Las acciones de reparto se habilitan con el mapa (fase 7).</Text>
        </View>
      </ScrollView>
    </View>
  );
}

function Dato({ icono, etiqueta, valor }: { icono: any; etiqueta: string; valor: string }) {
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
  mapPlaceholder: {
    height: 160,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    borderStyle: 'dashed',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  mapText: { color: colors.textMuted, fontWeight: '700' },
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
  actionDisabled: { opacity: 0.5 },
  actionText: { color: colors.textSecondary, fontWeight: '800' },
  hint: { color: colors.textMuted, fontSize: 12, textAlign: 'center', marginTop: 4 },
});
