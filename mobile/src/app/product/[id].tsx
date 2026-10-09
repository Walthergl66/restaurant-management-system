/**
 * Estación Burger — Detalle de Producto
 */

import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Image,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Dimensions,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../../theme/colors';
import { menuService } from '../../features/menu/menuService';
import { useCart } from '../../features/carrito/CartContext';
import type { ExtraMenu, ProductoMenu } from '../../features/menu/types';
import { Button } from '../../components/ui/Button';

const { width } = Dimensions.get('window');

export default function ProductDetailScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { id } = useLocalSearchParams<{ id: string }>();
  const { addItem } = useCart();

  const [producto, setProducto] = useState<ProductoMenu | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedExtras, setSelectedExtras] = useState<ExtraMenu[]>([]);
  const [cantidad, setCantidad] = useState(1);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadProducto();
  }, [id]);

  const loadProducto = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await menuService.getProducto(parseInt(id!, 10));
      setProducto(data);
    } catch (err: any) {
      setError(err.message || 'Error al cargar producto');
    } finally {
      setLoading(false);
    }
  };

  const toggleExtra = (extra: ExtraMenu) => {
    setSelectedExtras(prev =>
      prev.find(e => e.id === extra.id)
        ? prev.filter(e => e.id !== extra.id)
        : [...prev, extra]
    );
  };

  const handleAddToCart = () => {
    if (!producto) return;
    addItem(producto, cantidad, selectedExtras);
    router.back();
  };

  if (loading) {
    return (
      <View style={[styles.container, styles.centered]}>
        <ActivityIndicator size="large" color={colors.neonOrange} />
      </View>
    );
  }

  if (error || !producto) {
    return (
      <View style={[styles.container, styles.centered]}>
        <Text style={styles.errorText}>{error || 'Producto no encontrado'}</Text>
        <Button title="Volver" onPress={() => router.back()} />
      </View>
    );
  }

  const extrasTotal = selectedExtras.reduce(
    (sum, e) => sum + e.precio,
    0
  );
  const unitPrice = producto.precio + extrasTotal;
  const totalPrice = unitPrice * cantidad;

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header con botón atrás */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{producto.nombre}</Text>
        <View style={styles.backBtn} />
      </View>

      <ScrollView
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* Imagen */}
        <View style={styles.imageContainer}>
          {producto.imagenUrl ? (
            <Image
              source={{ uri: producto.imagenUrl }}
              style={styles.image}
              resizeMode="cover"
            />
          ) : (
            <View style={styles.imagePlaceholder}>
              <Ionicons name="fast-food" size={80} color={colors.neonOrange} />
            </View>
          )}
        </View>

        <View style={styles.content}>
          {/* Info */}
          <Text style={styles.name}>{producto.nombre}</Text>
          <Text style={styles.description}>{producto.descripcion}</Text>

          {/* Precio base */}
          <View style={styles.priceRow}>
            <Text style={styles.priceLabel}>Precio</Text>
            <Text style={styles.price}>${producto.precio}</Text>
          </View>

          {/* Extras */}
          {producto.extras && producto.extras.length > 0 && (
            <View style={styles.extrasSection}>
              <Text style={styles.sectionTitle}>Extras</Text>
              {producto.extras.map(extra => {
                const isSelected = selectedExtras.find(e => e.id === extra.id);
                return (
                  <TouchableOpacity
                    key={extra.id}
                    onPress={() => toggleExtra(extra)}
                    style={[styles.extraRow, isSelected && styles.extraRowSelected]}
                  >
                    <View style={styles.extraLeft}>
                      <Text style={styles.extraName}>{extra.nombre}</Text>
                      {extra.descripcion ? (
                        <Text style={styles.extraDesc}>{extra.descripcion}</Text>
                      ) : null}
                    </View>
                    <View style={styles.extraRight}>
                      <Text style={styles.extraPrice}>+${extra.precio}</Text>
                      <View
                        style={[
                          styles.checkbox,
                          isSelected && styles.checkboxSelected,
                        ]}
                      >
                        {isSelected && (
                          <Ionicons name="checkmark" size={14} color={colors.background} />
                        )}
                      </View>
                    </View>
                  </TouchableOpacity>
                );
              })}
            </View>
          )}

          {/* Cantidad */}
          <View style={styles.quantitySection}>
            <Text style={styles.sectionTitle}>Cantidad</Text>
            <View style={styles.quantityControls}>
              <TouchableOpacity
                onPress={() => setCantidad(Math.max(1, cantidad - 1))}
                style={styles.qtyBtn}
              >
                <Ionicons name="remove" size={20} color={colors.textPrimary} />
              </TouchableOpacity>
              <Text style={styles.quantityText}>{cantidad}</Text>
              <TouchableOpacity
                onPress={() => setCantidad(cantidad + 1)}
                style={styles.qtyBtn}
              >
                <Ionicons name="add" size={20} color={colors.textPrimary} />
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </ScrollView>

      {/* Footer con botón agregar */}
      <View style={[styles.footer, { paddingBottom: insets.bottom + 16 }]}>
        <View style={styles.totalRow}>
          <Text style={styles.totalLabel}>Total</Text>
          <Text style={styles.totalValue}>${totalPrice.toFixed(2)}</Text>
        </View>
        <Button
          title="Agregar al carrito"
          onPress={handleAddToCart}
          size="lg"
          icon={<Ionicons name="bag-add" size={20} color={colors.background} />}
        />
      </View>
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
    fontWeight: '700',
    flex: 1,
    textAlign: 'center',
  },
  scrollContent: {
    paddingBottom: 200,
  },
  imageContainer: {
    width: width,
    height: width * 0.7,
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
    padding: 20,
  },
  name: {
    color: colors.textPrimary,
    fontSize: 26,
    fontWeight: '800',
    marginBottom: 8,
  },
  description: {
    color: colors.textSecondary,
    fontSize: 15,
    lineHeight: 22,
    marginBottom: 20,
  },
  priceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 24,
  },
  priceLabel: {
    color: colors.textSecondary,
    fontSize: 15,
    fontWeight: '600',
  },
  price: {
    color: colors.neonOrange,
    fontSize: 28,
    fontWeight: '800',
  },
  extrasSection: {
    marginBottom: 24,
  },
  sectionTitle: {
    color: colors.textPrimary,
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 12,
  },
  extraRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.surface,
    borderRadius: 14,
    padding: 14,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  extraRowSelected: {
    borderColor: colors.neonOrange,
    backgroundColor: colors.neonOrangeAlpha,
  },
  extraLeft: {
    flex: 1,
  },
  extraName: {
    color: colors.textPrimary,
    fontSize: 15,
    fontWeight: '600',
  },
  extraDesc: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 2,
  },
  extraRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  extraPrice: {
    color: colors.neonPink,
    fontSize: 15,
    fontWeight: '700',
  },
  checkbox: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: colors.surfaceBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxSelected: {
    backgroundColor: colors.neonOrange,
    borderColor: colors.neonOrange,
  },
  quantitySection: {
    marginBottom: 24,
  },
  quantityControls: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 20,
  },
  qtyBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  quantityText: {
    color: colors.textPrimary,
    fontSize: 24,
    fontWeight: '800',
    minWidth: 40,
    textAlign: 'center',
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
  errorText: {
    color: colors.error,
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 16,
    paddingHorizontal: 32,
  },
});
