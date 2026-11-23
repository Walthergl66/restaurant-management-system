import React from 'react';
import { Image, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { QuantityStepper } from '@/components/ui/QuantityStepper';
import { useCart } from '@/features/carrito';
import type { ProductoMenu } from '@/features/menu';
import { colors } from '@/theme/colors';

interface ProductoFilaProps {
  producto: ProductoMenu;
  /** Abre la pantalla de producto para elegir extras e ingredientes. */
  onPersonalizar: () => void;
}

/** Fila de producto para tomar pedidos: foto redonda, precio y stepper − / +, separada por una línea. */
export function ProductoFila({ producto, onPersonalizar }: ProductoFilaProps) {
  const { items, addItem, updateQuantity, getItemQuantity } = useCart();
  const cantidad = getItemQuantity(producto.id);

  const cambiar = (nueva: number) => {
    if (nueva > cantidad) {
      addItem(producto, 1, [], []);
      return;
    }
    // Al bajar se descuenta de la última variante agregada de este producto.
    const lineas = items.filter(item => item.producto.id === producto.id);
    const ultima = lineas[lineas.length - 1];
    if (ultima) {
      updateQuantity(producto.id, ultima.extras, ultima.cantidad - 1, ultima.ingredientesRemovidos);
    }
  };

  return (
    <View style={styles.fila}>
      {producto.imagenUrl ? (
        <Image source={{ uri: producto.imagenUrl }} style={styles.imagen} resizeMode="cover" />
      ) : (
        <View style={[styles.imagen, styles.placeholder]}>
          <Ionicons name="fast-food" size={30} color={colors.neonOrange} />
        </View>
      )}

      <View style={styles.info}>
        <Text style={styles.nombre} numberOfLines={2}>{producto.nombre}</Text>
        <Text style={styles.precio}>${producto.precio.toFixed(2)}</Text>
        <View style={styles.acciones}>
          <QuantityStepper valor={cantidad} onCambiar={cambiar} />
        </View>
      </View>

      <TouchableOpacity
        onPress={onPersonalizar}
        style={styles.personalizar}
        accessibilityLabel={`Personalizar ${producto.nombre}`}
      >
        <Ionicons name="chevron-forward" size={22} color={colors.textMuted} />
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  fila: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.surfaceBorder,
  },
  imagen: {
    width: 84,
    height: 84,
    borderRadius: 42,
    backgroundColor: colors.surfaceLight,
  },
  placeholder: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  info: {
    flex: 1,
    marginLeft: 16,
    gap: 4,
  },
  nombre: {
    color: colors.textPrimary,
    fontSize: 16,
    fontWeight: '700',
  },
  precio: {
    color: colors.textMuted,
    fontSize: 14,
  },
  acciones: {
    flexDirection: 'row',
    marginTop: 6,
  },
  personalizar: {
    padding: 6,
  },
});
