import React, { useMemo } from 'react';
import {
  ActivityIndicator,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAuth } from '@/features/auth';
import { colors } from '@/theme/colors';
import { useMisEntregas } from '../useMisEntregas';
import { estiloEstadoRecorrido } from '../estados';
import EntregaCard from './EntregaCard';
import EstadoBadge from './EstadoBadge';

/**
 * Pantalla "Mis entregas" del repartidor: lista de asignaciones activas y
 * recorridos abiertos. El acceso a esta sección ya está filtrado por el gate
 * (rol + estado operativo). Las acciones de operar (iniciar/entregar/finalizar)
 * se cablean en fases 7-9.
 */
export default function MisEntregasScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { logout } = useAuth();
  const { datos, loading, refreshing, error, noHabilitado, refrescar } = useMisEntregas();

  const asignaciones = useMemo(() => datos?.asignaciones ?? [], [datos]);
  const recorridos = useMemo(() => datos?.recorridos ?? [], [datos]);

  const header = (
    <View style={[styles.header, { paddingTop: insets.top + 12 }]}>
      <View>
        <Text style={styles.title}>Mis entregas</Text>
        <Text style={styles.subtitle}>Tus pedidos asignados y recorridos.</Text>
      </View>
      <TouchableOpacity
        onPress={() => void logout()}
        accessibilityRole="button"
        accessibilityLabel="Cerrar sesión"
        hitSlop={10}
      >
        <Ionicons name="log-out-outline" size={25} color={colors.neonPink} />
      </TouchableOpacity>
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

  return (
    <View style={styles.screen}>
      {header}
      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 40 }]}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => void refrescar()}
            tintColor={colors.neonOrange}
          />
        }
      >
        {noHabilitado && (
          <View style={styles.stateBox}>
            <Ionicons name="lock-closed-outline" size={30} color={colors.warning} />
            <Text style={styles.stateTitle}>Aún no estás habilitado para repartir</Text>
            <Text style={styles.stateText}>
              Un administrador debe habilitarte y asignarte pedidos para que aparezcan aquí.
            </Text>
          </View>
        )}

        {error && !noHabilitado && (
          <View style={styles.errorBox}>
            <Text style={styles.error}>{error}</Text>
            <TouchableOpacity onPress={() => void refrescar()} style={styles.retry}>
              <Text style={styles.retryText}>Reintentar</Text>
            </TouchableOpacity>
          </View>
        )}

        {!noHabilitado && !error && recorridos.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionLabel}>RECORRIDOS ACTIVOS</Text>
            {recorridos.map((recorrido) => {
              const estado = estiloEstadoRecorrido(recorrido.estado);
              const resueltas = recorrido.paradas.filter(
                (p) => p.estado !== 'PENDIENTE'
              ).length;
              return (
                <View key={recorrido.recorridoId} style={styles.recorridoCard}>
                  <View style={styles.headerRow}>
                    <Text style={styles.recorridoTipo}>
                      {recorrido.tipo === 'MULTIPLE' ? 'Multientrega' : 'Entrega individual'}
                    </Text>
                    <EstadoBadge etiqueta={estado.etiqueta} color={estado.color} />
                  </View>
                  <Text style={styles.recorridoMeta}>
                    {resueltas}/{recorrido.paradas.length} paradas resueltas
                  </Text>
                </View>
              );
            })}
          </View>
        )}

        {!noHabilitado && !error && (
          <View style={styles.section}>
            <Text style={styles.sectionLabel}>PEDIDOS ASIGNADOS</Text>
            {asignaciones.length === 0 ? (
              <View style={styles.stateBox}>
                <Ionicons name="cube-outline" size={30} color={colors.textMuted} />
                <Text style={styles.stateTitle}>Sin entregas asignadas</Text>
                <Text style={styles.stateText}>
                  Cuando un administrador te asigne pedidos, aparecerán aquí.
                </Text>
              </View>
            ) : (
              asignaciones.map((entrega) => (
                <EntregaCard
                  key={entrega.asignacionId}
                  entrega={entrega}
                  onPress={() => router.push(`/(delivery)/entrega/${entrega.asignacionId}`)}
                />
              ))
            )}
          </View>
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
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingBottom: 14,
  },
  title: { fontSize: 26, fontWeight: '800', color: colors.textPrimary },
  subtitle: { color: colors.textMuted, marginTop: 4 },
  content: { paddingHorizontal: 20, gap: 12 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  section: { gap: 12, marginTop: 10 },
  sectionLabel: {
    color: colors.textSecondary,
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 1,
  },
  recorridoCard: {
    padding: 16,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    gap: 8,
  },
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  recorridoTipo: { color: colors.textPrimary, fontWeight: '800', fontSize: 15 },
  recorridoMeta: { color: colors.textMuted, fontSize: 12 },
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
  error: { color: colors.neonPink },
  retry: { alignSelf: 'flex-start', paddingVertical: 6, paddingHorizontal: 14, borderRadius: 10, backgroundColor: colors.neonPink },
  retryText: { color: '#000', fontWeight: '800' },
});
