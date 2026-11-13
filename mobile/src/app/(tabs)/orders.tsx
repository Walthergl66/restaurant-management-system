/**
 * Estación Burger — Pantalla de Pedidos ("Mis pedidos")
 *
 * Historial real: GET /api/v1/clientes/historial (Page<T> de Spring).
 * Separa pedidos ACTIVOS (aún en curso o entrega sin verificar) de los
 * ANTERIORES. Para los activos a DOMICILIO se suscribe al estado en vivo
 * por WebSocket (RF-43) y refresca el seguimiento ligero como respaldo,
 * disparando una alerta in-app cuando el repartidor confirma su llegada.
 */

import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SectionList,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';
import type { PedidoCliente, SeguimientoEntrega } from '@/features/pedidos';
import {
  pedidosService,
  suscribirEstadoPedido,
  useAlertaLlegada,
} from '@/features/pedidos';
import { useAuth } from '@/features/auth';
import { authStorage } from '@/shared/storage/authStorage';
import { OrderCard } from '@/components/OrderCard';
import { Header } from '@/components/Header';

/** Estados de preparación que aún pueden cambiar (pedido en curso). */
const ESTADOS_ACTIVOS = ['BORRADOR', 'CONFIRMADO', 'EN_PREPARACION', 'LISTO'];

/** Un pedido es "activo" mientras no esté cerrado (ENTREGADO/ANULADO). */
function esActivo(p: PedidoCliente): boolean {
  return ESTADOS_ACTIVOS.includes(p.estado);
}

export default function OrdersScreen() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const params = useLocalSearchParams<{ nuevo?: string }>();
  const { isAuthenticated, loading: authLoading } = useAuth();
  const { procesar: procesarAlerta, reset: resetAlerta } = useAlertaLlegada();
  const [pedidos, setPedidos] = useState<PedidoCliente[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [alertaCodigo, setAlertaCodigo] = useState<string | null>(null);

  const loadHistorial = useCallback(async () => {
    if (!isAuthenticated) {
      setLoading(false);
      setRefreshing(false);
      return;
    }
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
  }, [isAuthenticated]);

  useEffect(() => {
    if (!authLoading) {
      if (isAuthenticated) {
        loadHistorial();
      } else {
        setLoading(false);
      }
    }
  }, [authLoading, isAuthenticated, loadHistorial]);

  const activos = pedidos.filter(esActivo);
  const anteriores = pedidos.filter(p => !esActivo(p));

  // Suscripción en vivo a los pedidos activos a DOMICILIO: al llegar un
  // cambio por el socket, refresca el seguimiento ligero y dispara la
  // alerta de llegada (anti-duplicado por código en useAlertaLlegada).
  const codigosDomicilio = activos
    .filter(p => p.metodoEntrega === 'DOMICILIO')
    .map(p => p.codigo)
    .join(',');

  useEffect(() => {
    if (!isAuthenticated || !codigosDomicilio) return;
    const codigos = codigosDomicilio.split(',');
    let cancelado = false;
    const unsubs: Array<() => void> = [];

    const refrescar = async (codigo: string) => {
      try {
        const seg: SeguimientoEntrega =
          await pedidosService.getSeguimiento(codigo);
        if (cancelado) return;
        procesarAlerta(codigo, seg, () => setAlertaCodigo(codigo));
      } catch {
        // respaldo: ignorar fallos del seguimiento
      }
    };

    (async () => {
      const stored = await authStorage.load();
      if (!stored || cancelado) return;
      for (const codigo of codigos) {
        const unsub = suscribirEstadoPedido(
          codigo,
          stored.accessToken,
          () => void refrescar(codigo)
        );
        unsubs.push(unsub);
      }
    })();

    return () => {
      cancelado = true;
      unsubs.forEach(u => u());
    };
  }, [isAuthenticated, codigosDomicilio, procesarAlerta]);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    loadHistorial();
  }, [loadHistorial]);

  const abrirAlerta = useCallback(() => {
    if (!alertaCodigo) return;
    const codigo = alertaCodigo;
    setAlertaCodigo(null);
    resetAlerta(codigo);
    router.push(`/pedido/${codigo}`);
  }, [alertaCodigo, resetAlerta, router]);

  if (loading) {
    return (
      <View
        style={[styles.container, styles.centered, { paddingTop: insets.top }]}
      >
        <ActivityIndicator size="large" color={colors.neonOrange} />
      </View>
    );
  }

  const sections = [
    { title: 'Activos', data: activos },
    { title: 'Anteriores', data: anteriores },
  ].filter(s => s.data.length > 0);

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

      {alertaCodigo && (
        <View
          style={styles.alertaLlegada}
          accessibilityRole="button"
          accessibilityLabel="Tu pedido ha llegado, confirma su recepción"
          onTouchEnd={abrirAlerta}
        >
          <Ionicons name="notifications" size={20} color={colors.neonOrange} />
          <Text style={styles.alertaText}>
            Tu pedido ha llegado. Confirma su recepción.
          </Text>
          <Ionicons
            name="chevron-forward"
            size={18}
            color={colors.neonOrange}
          />
        </View>
      )}

      <SectionList
        sections={sections}
        renderItem={({ item }) => (
          <OrderCard
            pedido={item}
            onPress={() => router.push(`/pedido/${item.codigo}`)}
          />
        )}
        renderSectionHeader={({ section }) =>
          sections.length > 1 ? (
            <Text style={styles.sectionTitle}>{section.title}</Text>
          ) : null
        }
        keyExtractor={item => item.codigo}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        stickySectionHeadersEnabled={false}
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
  sectionTitle: {
    color: colors.textSecondary,
    fontSize: 13,
    fontWeight: '800',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 10,
    marginTop: 6,
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
  alertaLlegada: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginHorizontal: 16,
    marginBottom: 12,
    backgroundColor: colors.neonOrangeAlpha,
    borderRadius: 12,
    padding: 14,
    borderWidth: 1,
    borderColor: colors.neonOrange,
  },
  alertaText: {
    color: colors.textPrimary,
    fontSize: 14,
    fontWeight: '700',
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
