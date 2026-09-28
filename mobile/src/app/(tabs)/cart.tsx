/**
 * Estación Burger — Pantalla de Carrito
 */

import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../../theme/colors';
import { useCart } from '../../store/CartContext';
import { CartItemRow } from '../../components/CartItemRow';
import { Button } from '../../components/ui/Button';
import { Header } from '../../components/Header';

export default function CartScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { items, totalPrice, clearCart } = useCart();

  if (items.length === 0) {
    return (
      <View style={[styles.container, { paddingTop: insets.top }]}>
        <Header title="Carrito" />
        <View style={styles.emptyContainer}>
          <Ionicons name="bag-outline" size={80} color={colors.textMuted} />
          <Text style={styles.emptyTitle}>Tu carrito está vacío</Text>
          <Text style={styles.emptySubtitle}>
            Agrega productos deliciosos del menú
          </Text>
          <Button
            title="Ver menú"
            onPress={() => router.push('/(tabs)')}
            style={styles.emptyButton}
          />
        </View>
      </View>
    );
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <Header
        title="Carrito"
        right={
          <TouchableOpacity onPress={clearCart} style={styles.clearButton}>
            <Text style={styles.clearText}>Limpiar</Text>
          </TouchableOpacity>
        }
      />

      <FlatList
        data={items}
        renderItem={({ item }) => <CartItemRow item={item} />}
        keyExtractor={item => `${item.producto.id}-${item.extras.map(e => e.id).join(',')}`}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
      />

      <View style={[styles.footer, { paddingBottom: insets.bottom + 16 }]}>
        <View style={styles.totalRow}>
          <Text style={styles.totalLabel}>Total</Text>
          <Text style={styles.totalValue}>${totalPrice.toFixed(2)}</Text>
        </View>
        <Button
          title="Proceder al pago"
          onPress={() => {
            // TODO: Navegar a checkout
          }}
          size="lg"
          icon={<Ionicons name="arrow-forward" size={20} color={colors.background} />}
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
  listContent: {
    padding: 16,
    paddingBottom: 200,
  },
  clearButton: {
    padding: 8,
  },
  clearText: {
    color: colors.error,
    fontSize: 13,
    fontWeight: '600',
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
});
