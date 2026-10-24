/**
 * Estación Burger — Detalle de Pedido con estado en vivo (RF-43)
 *
 * Carga el snapshot (GET /clientes/pedidos/{codigo}) y luego se
 * suscribe por STOMP a /topic/pedido/{codigo} para recibir el estado
 * en tiempo real sin recargar.
 */

import React, { useCallback, useEffect, useRef, useState } from 'react';
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
import { colors } from '@/theme/colors';
import {
  pedidosService,
  suscribirEstadoPedido,
  BarraProgresoPedido,
  useAlertaLlegada,
  generarIdempotencyKey,
} from '@/features/pedidos';
import { authStorage } from '@/shared/storage/authStorage';
import type { PedidoCliente, SeguimientoEntrega } from '@/features/pedidos';
import { ApiError } from '@/shared/api/apiError';
import { Button } from '@/components/ui/Button';

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
  const { procesar: procesarAlerta, reset: resetAlerta } = useAlertaLlegada();

  const [pedido, setPedido] = useState<PedidoCliente | null>(null);
  const [seguimiento, setSeguimiento] = useState<SeguimientoEntrega | null>(
    null
  );
  const [loading, setLoading] = useState(true);
  const [enVivo, setEnVivo] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [alertaVisible, setAlertaVisible] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [confirmError, setConfirmError] = useState<string | null>(null);
  // Clave de idempotencia por intento de confirmación (reutilizada en reintentos).
  const idempotencyKeyRef = useRef<string | null>(null);

  const refrescarSeguimiento = useCallback(async () => {
    if (!codigo) return;
    try {
      const seg = await pedidosService.getSeguimiento(codigo);
      setSeguimiento(seg);
      procesarAlerta(codigo, seg, () => setAlertaVisible(true));
    } catch {
      // El seguimiento es de respaldo: no bloquear la pantalla si falla.
    }
  }, [codigo, procesarAlerta]);

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
    refrescarSeguimiento();
  }, [cargar, refrescarSeguimiento]);

  // Suscripción en vivo (RF-43): al recibir cambios del pedido por el
  // socket, refresca el seguimiento ligero para detectar la llegada.
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
        void refrescarSeguimiento();
      });
    })();

    return () => {
      cancelado = true;
      unsubscribe?.();
      resetAlerta(codigo);
    };
  }, [codigo, refrescarSeguimiento, resetAlerta]);

  const onConfirmarRecepcion = useCallback(async () => {
    if (!codigo || confirmando) return;
    if (!idempotencyKeyRef.current) {
      idempotencyKeyRef.current = generarIdempotencyKey();
    }
    setConfirmando(true);
    setConfirmError(null);
    try {
      const seg = await pedidosService.confirmarRecepcion(
        codigo,
        idempotencyKeyRef.current
      );
      setSeguimiento(seg);
      setAlertaVisible(false);
      idempotencyKeyRef.current = null; // éxito: nuevo intento = nueva clave
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 422) {
          setConfirmError(
            'El repartidor aún no marcó la entrega. Inténtalo cuando llegue.'
          );
        } else if (err.status === 403) {
          setConfirmError('Solo puedes confirmar tus propios pedidos.');
        } else {
          setConfirmError(err.message);
        }
      } else {
        setConfirmError('No se pudo confirmar la recepción.');
      }
    } finally {
      setConfirmando(false);
    }
  }, [codigo, confirmando]);

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

  const anulado = pedido.estado === 'ANULADO';
  const pendienteConfirmacion =
    seguimiento?.faseConfirmacion === 'PENDIENTE_CONFIRMACION_CLIENTE';
  const verificada = seguimiento?.entregaVerificada === true;

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

          <View style={styles.timeline}>
            <BarraProgresoPedido
              estadoPreparacion={pedido.estado}
              seguimiento={seguimiento}
              metodoEntrega={pedido.metodoEntrega}
              anulado={anulado}
            />
          </View>

          {!anulado && pendienteConfirmacion && (
            <View style={styles.pendienteBanner}>
              <Ionicons
                name="time-outline"
                size={16}
                color={colors.warning}
              />
              <Text style={styles.pendienteText}>
                PENDIENTE DE CONFIRMACIÓN DEL CLIENTE
              </Text>
            </View>
          )}
        </View>

        {/* Alerta in-app: el repartidor llegó */}
        {!anulado && alertaVisible && pendienteConfirmacion && (
          <View style={styles.alertaLlegada}>
            <Ionicons
              name="notifications"
              size={20}
              color={colors.neonOrange}
            />
            <Text style={styles.alertaLlegadaText}>
              Tu pedido ha llegado. Confirma su recepción.
            </Text>
          </View>
        )}

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

      {/* Footer */}
      <View style={[styles.footer, { paddingBottom: insets.bottom + 16 }]}>
        {!anulado && pendienteConfirmacion && (
          <View style={styles.confirmarBox}>
            {confirmError && (
              <Text style={styles.confirmError}>{confirmError}</Text>
            )}
            <Button
              title="Confirmar recepción"
              onPress={onConfirmarRecepcion}
              loading={confirmando}
            />
          </View>
        )}
        {!anulado && verificada && (
          <View style={styles.verificadaBox}>
            <Ionicons
              name="checkmark-done-circle"
              size={18}
              color={colors.success}
            />
            <Text style={styles.verificadaText}>Entrega confirmada</Text>
          </View>
        )}
        <View style={styles.totalRow}>
          <Text style={styles.totalLabel}>Total</Text>
          <Text style={styles.totalValue}>${pedido.total.toFixed(2)}</Text>
        </View>
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
    backgroundColor: colors.surface,
    paddingHorizontal: 20,
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: colors.surfaceBorder,
  },
  confirmarBox: {
    marginBottom: 14,
    gap: 8,
  },
  confirmError: {
    color: colors.error,
    fontSize: 13,
    fontWeight: '600',
  },
  verificadaBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 12,
  },
  verificadaText: {
    color: colors.success,
    fontSize: 15,
    fontWeight: '800',
  },
  totalRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  pendienteBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 16,
    backgroundColor: colors.surfaceLight,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderWidth: 1,
    borderColor: colors.warning,
  },
  pendienteText: {
    color: colors.warning,
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0.5,
    flex: 1,
  },
  alertaLlegada: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: colors.neonOrangeAlpha,
    borderRadius: 14,
    padding: 14,
    marginBottom: 20,
    borderWidth: 1,
    borderColor: colors.neonOrange,
  },
  alertaLlegadaText: {
    color: colors.textPrimary,
    fontSize: 14,
    fontWeight: '700',
    flex: 1,
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
