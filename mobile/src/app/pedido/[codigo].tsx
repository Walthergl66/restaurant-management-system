/**
 * Estación Burger — Detalle de Pedido con estado en vivo (RF-43)
 *
 * Carga el snapshot (GET /clientes/pedidos/{codigo}) y luego se
 * suscribe por STOMP a /topic/pedido/{codigo} para recibir el estado
 * en tiempo real sin recargar.
 */

import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../../theme/colors';
import { pedidosService } from '../../features/pedidos/pedidosService';
import { suscribirEstadoPedido } from '../../features/pedidos/estadoSocket';
import { authStorage } from '../../core/storage/authStorage';
import type { PedidoCliente } from '../../features/pedidos/types';
import { Button } from '../../components/ui/Button';

/** Secuencia de estados de un pedido presencial/entrega. */
const PASOS = ['CONFIRMADO', 'EN_PREPARACION', 'LISTO', 'ENTREGADO'] as const;

const ETIQUETA_ESTADO: Record<string, string> = {
  BORRADOR: 'Borrador',
  CONFIRMADO: 'Confirmado',
  EN_PREPARACION: 'En preparación',
  LISTO: 'Listo',
  ENTREGADO: 'Entregado',
  ANULADO: 'Anulado',
};

export default function PedidoDetalleScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { codigo } = useLocalSearchParams<{ codigo: string }>();

  const [pedido, setPedido] = useState<PedidoCliente | null>(null);
  const [loading, setLoading] = useState(true);
  const [enVivo, setEnVivo] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cargar = useCallback(async () => {
    if (!codigo) return;
    try {
      setError(null);
      const data = await pedidosService.getPedido(codigo);
      setPedido(data);
    } catch (err: any) {
      setError(err.message || 'Error al cargar el pedido');
    } finally {
      setLoading(false);
    }
  }, [codigo]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  // Suscripción en vivo (RF-43)
  useEffect(() => {
    if (!codigo) return;
    let unsubscribe: (() => void) | undefined;
    let cancelado = false;

    (async () => {
      const stored = await authStorage.load();
      if (!stored || cancelado) return;
      unsubscribe = suscribirEstadoPedido(codigo, stored.accessToken, p => {
        setPedido(p);
        setEnVivo(true);
      });
    })();

    return () => {
      cancelado = true;
      unsubscribe?.();
    };
  }, [codigo]);

  if (loading) {
    return (
      <View style={[styles.container, styles.centered]}>
        <ActivityIndicator size="large" color={colors.neonOrange} />
      </View>
    );
  }

  if (error || !pedido) {
    return (
      <View style={[styles.container, styles.centered]}>
        <Text style={styles.errorText}>{error || 'Pedido no encontrado'}</Text>
        <Button title="Volver" onPress={() => router.back()} />
      </View>
    );
  }

  const pasoActual = PASOS.indexOf(pedido.estado as (typeof PASOS)[number]);
  const anulado = pedido.estado === 'ANULADO';

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>#{pedido.codigo}</Text>
        <View style={styles.backBtn} />
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {/* Estado */}
        <View style={styles.estadoCard}>
          <View style={styles.estadoTop}>
            <View>
              <Text style={styles.estadoLabel}>Estado</Text>
              <Text style={styles.estadoValor}>
                {ETIQUETA_ESTADO[pedido.estado] || pedido.estado}
              </Text>
            </View>
            <View style={[styles.liveBadge, enVivo && styles.liveBadgeOn]}>
              <View
                style={[styles.liveDot, enVivo && styles.liveDotOn]}
              />
              <Text style={[styles.liveText, enVivo && styles.liveTextOn]}>
                {enVivo ? 'EN VIVO' : 'ACTUALIZANDO'}
              </Text>
            </View>
          </View>

          {!anulado && (
            <View style={styles.timeline}>
              {PASOS.map((paso, index) => {
                const alcanzado = index <= pasoActual;
                const actual = index === pasoActual;
                return (
                  <View key={paso} style={styles.pasoItem}>
                    <View
                      style={[
                        styles.pasoDot,
                        alcanzado && styles.pasoDotAlcanzado,
                        actual && styles.pasoDotActual,
                      ]}
                    >
                      {alcanzado && (
                        <Ionicons
                          name="checkmark"
                          size={12}
                          color={colors.background}
                        />
                      )}
                    </View>
                    <Text
                      style={[
                        styles.pasoText,
                        alcanzado && styles.pasoTextAlcanzado,
                      ]}
                    >
                      {ETIQUETA_ESTADO[paso]}
                    </Text>
                  </View>
                );
              })}
            </View>
          )}
        </View>

        {/* Líneas */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Tu pedido</Text>
          {pedido.lineas.map((linea, index) => (
            <View key={index} style={styles.linea}>
              <View style={styles.lineaCantidad}>
                <Text style={styles.lineaCantidadText}>{linea.cantidad}x</Text>
              </View>
              <View style={styles.lineaInfo}>
                <Text style={styles.lineaNombre}>{linea.nombre}</Text>
                {linea.extras.length > 0 && (
                  <Text style={styles.lineaExtras}>
                    + {linea.extras.map(e => e.nombre).join(', ')}
                  </Text>
                )}
              </View>
              <Text style={styles.lineaSubtotal}>
                ${linea.subtotal.toFixed(2)}
              </Text>
            </View>
          ))}
        </View>

        {/* Detalles */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Detalles</Text>
          <DetalleRow
            icon="card-outline"
            label="Método de pago"
            value={pedido.metodoPago}
          />
          <DetalleRow
            icon={
              pedido.metodoEntrega === 'DOMICILIO'
                ? 'bicycle-outline'
                : 'walk-outline'
            }
            label="Entrega"
            value={pedido.metodoEntrega}
          />
          <DetalleRow
            icon="time-outline"
            label="Fecha"
            value={new Date(pedido.creadoAt).toLocaleString('es-ES', {
              day: 'numeric',
              month: 'short',
              hour: '2-digit',
              minute: '2-digit',
            })}
          />
        </View>
      </ScrollView>

      {/* Total */}
      <View style={[styles.footer, { paddingBottom: insets.bottom + 16 }]}>
        <Text style={styles.totalLabel}>Total</Text>
        <Text style={styles.totalValue}>${pedido.total.toFixed(2)}</Text>
      </View>
    </View>
  );
}

function DetalleRow({
  icon,
  label,
  value,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  value: string;
}) {
  return (
    <View style={styles.detalleRow}>
      <View style={styles.detalleLeft}>
        <Ionicons name={icon} size={18} color={colors.neonOrange} />
        <Text style={styles.detalleLabel}>{label}</Text>
      </View>
      <Text style={styles.detalleValue}>{value}</Text>
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
    paddingHorizontal: 32,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  backBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitle: {
    color: colors.textPrimary,
    fontSize: 18,
    fontWeight: '800',
  },
  content: {
    padding: 16,
    paddingBottom: 120,
  },
  estadoCard: {
    backgroundColor: colors.surface,
    borderRadius: 20,
    padding: 20,
    marginBottom: 20,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  estadoTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  estadoLabel: {
    color: colors.textSecondary,
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  estadoValor: {
    color: colors.neonOrange,
    fontSize: 22,
    fontWeight: '800',
    marginTop: 4,
  },
  liveBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: colors.surfaceLight,
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  liveBadgeOn: {
    backgroundColor: colors.neonOrangeAlpha,
  },
  liveDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: colors.textMuted,
  },
  liveDotOn: {
    backgroundColor: colors.success,
  },
  liveText: {
    color: colors.textMuted,
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  liveTextOn: {
    color: colors.success,
  },
  timeline: {
    gap: 12,
  },
  pasoItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  pasoDot: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: colors.surfaceBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pasoDotAlcanzado: {
    backgroundColor: colors.neonOrange,
    borderColor: colors.neonOrange,
  },
  pasoDotActual: {
    shadowColor: colors.neonOrange,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.8,
    shadowRadius: 8,
    elevation: 6,
  },
  pasoText: {
    color: colors.textMuted,
    fontSize: 14,
    fontWeight: '600',
  },
  pasoTextAlcanzado: {
    color: colors.textPrimary,
  },
  section: {
    marginBottom: 20,
  },
  sectionTitle: {
    color: colors.textPrimary,
    fontSize: 16,
    fontWeight: '800',
    marginBottom: 12,
  },
  linea: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: 14,
    padding: 14,
    marginBottom: 10,
  },
  lineaCantidad: {
    width: 36,
    height: 36,
    borderRadius: 10,
    backgroundColor: colors.neonOrangeAlpha,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  lineaCantidadText: {
    color: colors.neonOrange,
    fontSize: 14,
    fontWeight: '800',
  },
  lineaInfo: {
    flex: 1,
  },
  lineaNombre: {
    color: colors.textPrimary,
    fontSize: 15,
    fontWeight: '600',
  },
  lineaExtras: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 2,
  },
  lineaSubtotal: {
    color: colors.neonOrange,
    fontSize: 15,
    fontWeight: '800',
  },
  detalleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.surface,
    borderRadius: 12,
    padding: 14,
    marginBottom: 8,
  },
  detalleLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  detalleLabel: {
    color: colors.textSecondary,
    fontSize: 14,
  },
  detalleValue: {
    color: colors.textPrimary,
    fontSize: 14,
    fontWeight: '700',
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.surface,
    paddingHorizontal: 20,
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: colors.surfaceBorder,
  },
  totalLabel: {
    color: colors.textSecondary,
    fontSize: 15,
    fontWeight: '600',
  },
  totalValue: {
    color: colors.neonOrange,
    fontSize: 28,
    fontWeight: '800',
  },
  errorText: {
    color: colors.error,
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 16,
  },
});
