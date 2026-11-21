import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import type { CartItem } from '@/features/carrito';
import { colors } from '@/theme/colors';

interface LineaResumenProps {
  item: CartItem;
  onEditar: () => void;
  onDuplicar: () => void;
  onEliminar: () => void;
}

/** Línea del resumen del pedido: cantidad, nombre, variantes y precio, con editar / duplicar / eliminar. */
export function LineaResumen({ item, onEditar, onDuplicar, onEliminar }: LineaResumenProps) {
  const extrasTotal = item.extras.reduce((suma, extra) => suma + extra.precio, 0);
  const subtotal = (item.producto.precio + extrasTotal) * item.cantidad;
  const detalle = [
    item.extras.length ? item.extras.map(extra => extra.nombre).join(', ') : null,
    item.ingredientesRemovidos.length ? `Sin: ${item.ingredientesRemovidos.join(', ')}` : null,
    item.observaciones || null,
  ].filter(Boolean).join(' · ');

  return (
    <View style={styles.linea}>
      <Text style={styles.cantidad}>{item.cantidad}</Text>
      <View style={styles.centro}>
        <Text style={styles.nombre}>{item.producto.nombre}</Text>
        {!!detalle && <Text style={styles.detalle}>{detalle}</Text>}
        <View style={styles.acciones}>
          <Accion icono="create-outline" etiqueta="Editar" onPress={onEditar} />
          <Accion icono="copy-outline" etiqueta="Duplicar" onPress={onDuplicar} />
          <Accion icono="trash-outline" etiqueta="Eliminar" onPress={onEliminar} />
        </View>
      </View>
      <Text style={styles.precio}>${subtotal.toFixed(2)}</Text>
    </View>
  );
}

interface AccionProps {
  icono: React.ComponentProps<typeof Ionicons>['name'];
  etiqueta: string;
  onPress: () => void;
}

function Accion({ icono, etiqueta, onPress }: AccionProps) {
  return (
    <TouchableOpacity onPress={onPress} style={styles.accion} accessibilityRole="button" accessibilityLabel={etiqueta}>
      <Ionicons name={icono} size={16} color={colors.textPrimary} />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  linea: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    paddingVertical: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.surfaceBorder,
    gap: 12,
  },
  cantidad: {
    color: colors.textOnSurface,
    fontSize: 16,
    fontWeight: '800',
    width: 22,
  },
  centro: { flex: 1, gap: 2 },
  nombre: { color: colors.textOnSurface, fontSize: 16, fontWeight: '700' },
  detalle: { color: colors.textSecondary, fontSize: 13 },
  precio: { color: colors.textOnSurface, fontSize: 16, fontWeight: '700' },
  acciones: { flexDirection: 'row', gap: 8, marginTop: 8 },
  accion: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: colors.textOnSurface,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
