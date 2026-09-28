/**
 * Estación Burger — Tarjeta de Producto
 * Con efectos neón y profundidad mejorados
 */

import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  Image,
  TouchableOpacity,
} from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../theme/colors';
import { Producto } from '../types';
import { useCart } from '../store/CartContext';

const CARD_WIDTH = 160;

interface ProductCardProps {
  producto: Producto;
  onPress: () => void;
}

export function ProductCard({ producto, onPress }: ProductCardProps) {
  const { addItem, getItemQuantity } = useCart();
  const quantity = getItemQuantity(producto.id);

  return (
    <TouchableOpacity
      onPress={onPress}
      activeOpacity={0.9}
      style={styles.container}
    >
      <LinearGradient
        colors={[colors.surface, '#151515']}
        style={styles.gradient}
      >
        {/* Imagen con overlay para profundidad */}
        <View style={styles.imageContainer}>
          <LinearGradient
            colors={['#1a1a1a', '#0d0d0d']}
            style={styles.imageGradient}
          >
            {producto.imagenUrl ? (
              <Image
                source={{ uri: producto.imagenUrl }}
                style={styles.image}
                resizeMode="cover"
              />
            ) : (
              <View style={styles.imagePlaceholder}>
                <Ionicons name="fast-food" size={36} color={colors.neonOrange} />
              </View>
            )}
          </LinearGradient>
          {quantity > 0 && (
            <View style={styles.quantityBadge}>
              <Text style={styles.quantityText}>{quantity}</Text>
            </View>
          )}
        </View>

        {/* Contenido */}
        <View style={styles.content}>
          <Text style={styles.name} numberOfLines={2}>
            {producto.nombre}
          </Text>
          <Text style={styles.description} numberOfLines={2}>
            {producto.descripcion}
          </Text>

          <View style={styles.footer}>
            <Text style={styles.price}>${producto.precio}</Text>
            <TouchableOpacity
              onPress={() => addItem(producto)}
              style={styles.addButton}
              activeOpacity={0.7}
            >
              <Ionicons name="add" size={18} color={colors.background} />
            </TouchableOpacity>
          </View>
        </View>
      </LinearGradient>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    width: CARD_WIDTH,
    borderRadius: 18,
    overflow: 'hidden',
    backgroundColor: colors.surface,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.5,
    shadowRadius: 16,
    elevation: 12,
  },
  gradient: {
    flex: 1,
  },
  imageContainer: {
    width: '100%',
    height: 120,
  },
  imageGradient: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  image: {
    width: '100%',
    height: '100%',
    opacity: 0.85,
  },
  imagePlaceholder: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  quantityBadge: {
    position: 'absolute',
    top: 8,
    right: 8,
    backgroundColor: colors.neonPink,
    borderRadius: 12,
    minWidth: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 6,
    shadowColor: colors.neonPink,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.6,
    shadowRadius: 8,
    elevation: 6,
  },
  quantityText: {
    color: colors.background,
    fontSize: 12,
    fontWeight: '800',
  },
  content: {
    padding: 12,
  },
  name: {
    color: colors.textPrimary,
    fontSize: 14,
    fontWeight: '700',
    marginBottom: 4,
  },
  description: {
    color: colors.textSecondary,
    fontSize: 11,
    lineHeight: 15,
    marginBottom: 10,
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  price: {
    color: colors.neonOrange,
    fontSize: 16,
    fontWeight: '800',
    textShadowColor: colors.neonOrange,
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 6,
  },
  addButton: {
    backgroundColor: colors.neonPink,
    borderRadius: 10,
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: colors.neonPink,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.7,
    shadowRadius: 10,
    elevation: 8,
    borderWidth: 1,
    borderColor: 'rgba(255, 16, 240, 0.3)',
  },
});
