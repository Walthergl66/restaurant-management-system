/**
 * Estación Burger — Checkout
 *
 * Flujo RF-41 (idempotente, RF-24/25):
 *   1. (DOMICILIO) POST /clientes/direcciones → direccionId
 *   2. POST /clientes/pedidos → pedido en BORRADOR
 *   3. POST /clientes/pedidos/{codigo}/confirmar (MISMA idempotencyKey)
 *
 * La idempotencyKey se genera UNA vez al entrar a la pantalla:
 * si falla la red y se reintenta, el backend devuelve el mismo
 * pedido en lugar de duplicarlo.
 */

import React, { useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../theme/colors';
import { useCart } from '../features/carrito/CartContext';
import { pedidosService } from '../features/pedidos/pedidosService';
import {
  generarCodigoPedido,
  generarIdempotencyKey,
} from '../features/pedidos/idempotencia';
import { CartItemRow } from '../components/CartItemRow';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Header } from '../components/Header';

const METODOS_PAGO = ['EFECTIVO', 'TARJETA'] as const;
const METODOS_ENTREGA = ['RETIRAR', 'DOMICILIO'] as const;

type MetodoPago = (typeof METODOS_PAGO)[number];
type MetodoEntrega = (typeof METODOS_ENTREGA)[number];

export default function CheckoutScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { items, totalPrice, clearCart } = useCart();

  const [metodoPago, setMetodoPago] = useState<MetodoPago>('EFECTIVO');
  const [metodoEntrega, setMetodoEntrega] =
    useState<MetodoEntrega>('RETIRAR');
  const [direccion, setDireccion] = useState({
    etiqueta: '',
    direccion: '',
    telefono: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Idempotencia: una clave y un código por visita a esta pantalla
  const idempotencyKey = useRef(generarIdempotencyKey());
  const codigoPedido = useRef(generarCodigoPedido());

  const esDomicilio = metodoEntrega === 'DOMICILIO';
  const direccionValida =
    direccion.etiqueta.trim().length > 0 &&
    direccion.direccion.trim().length > 0;

  const handleConfirmar = async () => {
    if (items.length === 0) {
      router.back();
      return;
    }

    if (esDomicilio && !direccionValida) {
      setError('Para domicilio completa etiqueta y dirección');
      return;
    }

    setError(null);
    setLoading(true);
    try {
      let direccionId: number | null = null;
      if (esDomicilio) {
        const nueva = await pedidosService.agregarDireccion({
          etiqueta: direccion.etiqueta.trim(),
          direccion: direccion.direccion.trim(),
          telefono: direccion.telefono.trim() || undefined,
        });
        direccionId = nueva.id;
      }

      // 1) Crear BORRADOR (idempotente)
      const pedido = await pedidosService.crearPedido({
        codigo: codigoPedido.current,
        metodoPago,
        metodoEntrega,
        direccionId,
        idempotencyKey: idempotencyKey.current,
        items: items.map(item => ({
          productoId: item.producto.id,
          cantidad: item.cantidad,
          extraIds: item.extras.map(extra => extra.id),
          observaciones: item.observaciones,
        })),
      });

      // 2) Confirmar con la MISMA clave (congela el total)
      const confirmado = await pedidosService.confirmarPedido(
        pedido.codigo,
        idempotencyKey.current
      );

      clearCart();
      router.replace({
        pathname: '/(tabs)/orders',
        params: { nuevo: confirmado.codigo },
      });
    } catch (err: any) {
      setError(err.message || 'Error al confirmar el pedido');
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={{ paddingTop: insets.top }}>
        <Header
          title="Checkout"
          right={
            <TouchableOpacity
              onPress={() => router.back()}
              style={styles.closeButton}
            >
              <Ionicons name="close" size={24} color={colors.textPrimary} />
            </TouchableOpacity>
          }
        />
      </View>

      {items.length === 0 ? (
        <View style={styles.emptyContainer}>
          <Ionicons
            name="bag-outline"
            size={72}
            color={colors.textMuted}
          />
          <Text style={styles.emptyTitle}>Carrito vacío</Text>
          <Text style={styles.emptySubtitle}>
            Agrega productos antes de confirmar
          </Text>
          <Button
            title="Volver al menú"
            onPress={() => router.back()}
            style={styles.emptyButton}
          />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={styles.content}
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
        >
          {/* Resumen */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Tu pedido</Text>
            {items.map(item => (
              <CartItemRow
                key={`${item.producto.id}-${item.extras
                  .map(e => e.id)
                  .join(',')}`}
                item={item}
              />
            ))}
          </View>

          {/* Método de pago */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Método de pago</Text>
            <View style={styles.opcionesRow}>
              {METODOS_PAGO.map(opcion => (
                <Opcion
                  key={opcion}
                  label={opcion}
                  icon={
                    opcion === 'EFECTIVO' ? 'cash-outline' : 'card-outline'
                  }
                  seleccionado={metodoPago === opcion}
                  onPress={() => setMetodoPago(opcion)}
                />
              ))}
            </View>
          </View>

          {/* Método de entrega */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Entrega</Text>
            <View style={styles.opcionesRow}>
              {METODOS_ENTREGA.map(opcion => (
                <Opcion
                  key={opcion}
                  label={opcion === 'RETIRAR' ? 'RECOGER' : 'DOMICILIO'}
                  icon={
                    opcion === 'RETIRAR'
                      ? 'walk-outline'
                      : 'bicycle-outline'
                  }
                  seleccionado={metodoEntrega === opcion}
                  onPress={() => setMetodoEntrega(opcion)}
                />
              ))}
            </View>
          </View>

          {/* Dirección (solo domicilio) */}
          {esDomicilio && (
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>Dirección</Text>
              <Input
                label="ETIQUETA"
                placeholder="Casa, oficina..."
                value={direccion.etiqueta}
                onChangeText={text =>
                  setDireccion(prev => ({ ...prev, etiqueta: text }))
                }
                icon="bookmark-outline"
                maxLength={40}
              />
              <Input
                label="DIRECCIÓN"
                placeholder="Calle, número, colonia..."
                value={direccion.direccion}
                onChangeText={text =>
                  setDireccion(prev => ({ ...prev, direccion: text }))
                }
                icon="location-outline"
                maxLength={200}
              />
              <Input
                label="TELÉFONO (OPCIONAL)"
                placeholder="555 000 0000"
                value={direccion.telefono}
                onChangeText={text =>
                  setDireccion(prev => ({ ...prev, telefono: text }))
                }
                icon="call-outline"
                keyboardType="phone-pad"
                maxLength={20}
              />
            </View>
          )}

          {error && (
            <View style={styles.errorContainer}>
              <Ionicons
                name="alert-circle"
                size={16}
                color={colors.error}
              />
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}
        </ScrollView>
      )}

      {/* Footer */}
      {items.length > 0 && (
        <View
          style={[styles.footer, { paddingBottom: insets.bottom + 16 }]}
        >
          <View style={styles.totalRow}>
            <Text style={styles.totalLabel}>Total</Text>
            <Text style={styles.totalValue}>
              ${totalPrice.toFixed(2)}
            </Text>
          </View>
          <Button
            title={
              loading
                ? 'Confirmando...'
                : `Confirmar pedido · ${items.reduce(
                    (sum, i) => sum + i.cantidad,
                    0
                  )}`
            }
            onPress={handleConfirmar}
            loading={loading}
            size="lg"
            icon={
              <Ionicons
                name="checkmark-circle"
                size={20}
                color={colors.background}
              />
            }
          />
        </View>
      )}
    </KeyboardAvoidingView>
  );
}

function Opcion({
  label,
  icon,
  seleccionado,
  onPress,
}: {
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
  seleccionado: boolean;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity
      onPress={onPress}
      style={[styles.opcion, seleccionado && styles.opcionSeleccionada]}
      activeOpacity={0.8}
    >
      <Ionicons
        name={icon}
        size={20}
        color={seleccionado ? colors.background : colors.neonOrange}
      />
      <Text
        style={[
          styles.opcionText,
          seleccionado && styles.opcionTextSeleccionado,
        ]}
      >
        {label}
      </Text>
      {seleccionado && (
        <Ionicons
          name="checkmark-circle"
          size={16}
          color={colors.background}
        />
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  closeButton: {
    padding: 8,
  },
  content: {
    padding: 16,
    paddingBottom: 140,
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
  opcionesRow: {
    flexDirection: 'row',
    gap: 10,
  },
  opcion: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: colors.surface,
    borderRadius: 14,
    paddingVertical: 14,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  opcionSeleccionada: {
    backgroundColor: colors.neonOrange,
    borderColor: colors.neonOrange,
  },
  opcionText: {
    color: colors.textPrimary,
    fontSize: 13,
    fontWeight: '700',
  },
  opcionTextSeleccionado: {
    color: colors.background,
  },
  errorContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 16,
    paddingHorizontal: 4,
  },
  errorText: {
    color: colors.error,
    fontSize: 13,
    flex: 1,
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: colors.surface,
    paddingHorizontal: 16,
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: colors.surfaceBorder,
  },
  totalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
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
  emptyContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 48,
  },
  emptyTitle: {
    color: colors.textPrimary,
    fontSize: 22,
    fontWeight: '800',
    marginTop: 24,
    marginBottom: 8,
  },
  emptySubtitle: {
    color: colors.textSecondary,
    fontSize: 15,
    textAlign: 'center',
    marginBottom: 32,
  },
  emptyButton: {
    paddingHorizontal: 32,
  },
});
