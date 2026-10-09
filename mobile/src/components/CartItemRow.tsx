/**
 * Estación Burger — Item del Carrito
 */

import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  Image,
  TouchableOpacity,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../theme/colors';
import type { CartItem } from '../features/carrito/types';
import { useCart } from '../features/carrito/CartContext';

interface CartItemRowProps {
  item: CartItem;
}

export function CartItemRow({ item }: CartItemRowProps) {
  const { updateQuantity, removeItem } = useCart();
  const extrasTotal = item.extras.reduce(
    (sum, e) => sum + e.precio,
    0
  );
  const unitPrice = item.producto.precio + extrasTotal;
  const totalPrice = unitPrice * item.cantidad;

  return (
    <View style={styles.container}>
      <View style={styles.imageContainer}>
        {item.producto.imagenUrl ? (
          <Image
            source={{ uri: item.producto.imagenUrl }}
            style={styles.image}
            resizeMode="cover"
          />
        ) : (
          <View style={styles.imagePlaceholder}>
            <Ionicons name="fast-food" size={24} color={colors.neonOrange} />
          </View>
        )}
      </View>

      <View style={styles.content}>
        <View style={styles.header}>
          <Text style={styles.name} numberOfLines={1}>
            {item.producto.nombre}
          </Text>
          <TouchableOpacity
            onPress={() => removeItem(item.producto.id, item.extras)}
            style={styles.removeButton}
          >
            <Ionicons name="trash-outline" size={18} color={colors.error} />
          </TouchableOpacity>
        </View>

        {item.extras.length > 0 && (
          <Text style={styles.extras}>
            + {item.extras.map(e => e.nombre).join(', ')}
          </Text>
        )}

        <View style={styles.footer}>
          <View style={styles.quantityControls}>
            <TouchableOpacity
              onPress={() =>
                updateQuantity(item.producto.id, item.extras, item.cantidad - 1)
              }
              style={styles.qtyButton}
            >
              <Ionicons name="remove" size={16} color={colors.textPrimary} />
            </TouchableOpacity>
            <Text style={styles.quantity}>{item.cantidad}</Text>
            <TouchableOpacity
              onPress={() =>
                updateQuantity(item.producto.id, item.extras, item.cantidad + 1)
              }
              style={styles.qtyButton}
            >
              <Ionicons name="add" size={16} color={colors.textPrimary} />
            </TouchableOpacity>
          </View>
          <Text style={styles.totalPrice}>${totalPrice.toFixed(2)}</Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: colors.surface,
    borderRadius: 16,
    padding: 12,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 4,
  },
  imageContainer: {
    width: 70,
    height: 70,
    borderRadius: 12,
    overflow: 'hidden',
    backgroundColor: colors.surfaceLight,
  },
  image: {
    width: '100%',
    height: '100%',
  },
  imagePlaceholder: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    flex: 1,
    marginLeft: 12,
    justifyContent: 'space-between',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  name: {
    color: colors.textPrimary,
    fontSize: 15,
    fontWeight: '700',
    flex: 1,
    marginRight: 8,
  },
  removeButton: {
    padding: 4,
  },
  extras: {
    color: colors.neonPink,
    fontSize: 12,
    marginTop: 2,
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 8,
  },
  quantityControls: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surfaceLight,
    borderRadius: 10,
    paddingHorizontal: 4,
  },
  qtyButton: {
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  quantity: {
    color: colors.textPrimary,
    fontSize: 15,
    fontWeight: '700',
    minWidth: 28,
    textAlign: 'center',
  },
  totalPrice: {
    color: colors.neonOrange,
    fontSize: 17,
    fontWeight: '800',
  },
});
