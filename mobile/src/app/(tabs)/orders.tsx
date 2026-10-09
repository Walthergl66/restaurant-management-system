/**
 * Estación Burger — Pantalla de Pedidos
 *
 * Historial real: GET /api/v1/clientes/historial (Page<T> de Spring).
 * Sondeo ligero mientras haya pedidos activos — el push en vivo
 * del backend es vía WebSocket (RF-43), pendiente de integrar.
 */

import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../../theme/colors';
import type { PedidoCliente } from '../../features/pedidos/types';
import { pedidosService } from '../../features/pedidos/pedidosService';
import { OrderCard } from '../../components/OrderCard';
import { Header } from '../../components/Header';

/** Estados que aún pueden cambiar (sondeo activo). */
const ESTADOS_ACTIVOS = ['BORRADOR', 'CONFIRMADO', 'EN_PREPARACION'];

export default function OrdersScreen() {
  const insets = useSafeAreaInsets();
  const params = useLocalSearchParams<{ nuevo?: string }>();
  const [pedidos, setPedidos] = useState<PedidoCliente[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadHistorial = useCallback(async () => {
    try {
      setError(null);
      const page = await pedidosService.getHistorial(0, 20);
      setPedidos(page.content);
    } catch (err: any) {
      setError(err.message || 'Error al cargar el historial');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadHistorial();
  }, [loadHistorial]);

  // Sondeo mientras haya pedidos en curso (RF-43: el push real es WebSocket)
  useEffect(() => {
    const hayActivos = pedidos.some(p => ESTADOS_ACTIVOS.includes(p.estado));
    if (!hayActivos) return;
    const timer = setInterval(loadHistorial, 15000);
    return () => clearInterval(timer);
  }, [pedidos, loadHistorial]);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    loadHistorial();
  }, [loadHistorial]);

  if (loading) {
    return (
      <View
        style={[styles.container, styles.centered, { paddingTop: insets.top }]}
      >
        <ActivityIndicator size="large" color={colors.neonOrange} />
      </View>
    );
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <Header title="Mis Pedidos" />

      {params.nuevo && (
        <View style={styles.banner}>
          <Ionicons name="checkmark-circle" size={18} color={colors.success} />
          <Text style={styles.bannerText}>
            Pedido {params.nuevo} confirmado
          </Text>
        </View>
      )}

      {error && (
        <View style={styles.banner}>
          <Ionicons name="alert-circle" size={18} color={colors.error} />
          <Text style={[styles.bannerText, { color: colors.error }]}>
            {error}
          </Text>
        </View>
      )}

      <FlatList
        data={pedidos}
        renderItem={({ item }) => (
          <OrderCard pedido={item} onPress={() => {}} />
        )}
        keyExtractor={item => item.codigo}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={colors.neonOrange}
            colors={[colors.neonOrange]}
          />
        }
        ListEmptyComponent={
          <View style={styles.empty}>
            <Ionicons
              name="receipt-outline"
              size={64}
              color={colors.textMuted}
            />
            <Text style={styles.emptyTitle}>Aún no tienes pedidos</Text>
            <Text style={styles.emptySubtitle}>
              Tu historial aparecerá aquí
            </Text>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  centered: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  listContent: {
    padding: 16,
    paddingBottom: 100,
  },
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginHorizontal: 16,
    marginBottom: 12,
    backgroundColor: colors.surface,
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  bannerText: {
    color: colors.textPrimary,
    fontSize: 14,
    fontWeight: '600',
    flex: 1,
  },
  empty: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 80,
    gap: 8,
  },
  emptyTitle: {
    color: colors.textPrimary,
    fontSize: 18,
    fontWeight: '700',
    marginTop: 8,
  },
  emptySubtitle: {
    color: colors.textMuted,
    fontSize: 14,
  },
});
