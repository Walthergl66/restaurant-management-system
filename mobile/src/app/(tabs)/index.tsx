/**
 * Estación Burger — Pantalla de Menú/Home
 * Con efectos neón y profundidad basados en referencia
 */

import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  RefreshControl,
  ActivityIndicator,
  Image,
  TouchableOpacity,
  Dimensions,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';
import { menuService } from '@/features/menu';
import type { MenuDto, ProductoMenu } from '@/features/menu';
import { ProductCard } from '@/components/ProductCard';
import { Header } from '@/components/Header';
import { useCart } from '@/features/carrito';
import { useAuth } from '@/features/auth';

export default function MenuScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { totalItems } = useCart();
  const { usuario } = useAuth();
  const [menu, setMenu] = useState<MenuDto | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const categorias = menu?.categorias ?? [];
  const productos = menu
    ? menu.categorias.flatMap(categoria => categoria.productos)
    : [];

  const loadMenu = useCallback(async () => {
    try {
      setError(null);
      const data = await menuService.getMenu();
      setMenu(data);
    } catch (err: any) {
      setError(err.message || 'Error al cargar el menú');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadMenu();
  }, [loadMenu]);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    loadMenu();
  }, [loadMenu]);

  const filteredProducts: ProductoMenu[] = selectedCategory
    ? menu?.categorias.find(c => c.id === selectedCategory)
        ?.productos ?? []
    : productos;

  // Producto destacado del banner: el primero con imagen (o el primero).
  const productoDestacado =
    productos.find(p => p.imagenUrl) ?? productos[0] ?? null;
  const categoriaDestacada = productoDestacado
    ? menu?.categorias.find(c =>
        c.productos.some(p => p.id === productoDestacado.id)
      )?.nombre
    : undefined;

  const renderProduct = ({ item }: { item: ProductoMenu }) => (
    <ProductCard
      producto={item}
      onPress={() => router.push(`/product/${item.id}`)}
    />
  );

  if (loading) {
    return (
      <View style={[styles.container, styles.centered]}>
        <ActivityIndicator size="large" color={colors.neonOrange} />
      </View>
    );
  }

  if (error) {
    return (
      <View style={[styles.container, styles.centered]}>
        <Text style={styles.errorText}>{error}</Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <Header title="Estación Burger" nombre={usuario?.nombre} />

      <FlatList
        data={filteredProducts}
        renderItem={renderProduct}
        keyExtractor={item => item.id.toString()}
        numColumns={2}
        contentContainerStyle={styles.listContent}
        columnWrapperStyle={styles.row}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={colors.neonOrange}
            colors={[colors.neonOrange]}
          />
        }
        ListHeaderComponent={
          <>
            {/* Destacado del menú (datos reales) con imagen y overlay */}
            {productoDestacado && (
              <View style={styles.promoCard}>
                <Image
                  source={{
                    uri:
                      productoDestacado.imagenUrl ??
                      'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600',
                  }}
                  style={styles.promoImage}
                  resizeMode="cover"
                />
                <View style={styles.promoContent}>
                  <View style={styles.promoBadge}>
                    <Text style={styles.promoBadgeText}>
                      {(categoriaDestacada ?? 'Destacado').toUpperCase()}
                    </Text>
                  </View>
                  <Text style={styles.promoTitle} numberOfLines={2}>
                    {productoDestacado.nombre}
                  </Text>
                  {productoDestacado.descripcion ? (
                    <Text style={styles.promoDesc} numberOfLines={2}>
                      {productoDestacado.descripcion}
                    </Text>
                  ) : null}
                  <View style={styles.promoFooter}>
                    <Text style={styles.promoPrice}>
                      ${productoDestacado.precio.toFixed(2)}
                    </Text>
                    <TouchableOpacity
                      style={styles.promoButton}
                      onPress={() =>
                        router.push(`/product/${productoDestacado.id}`)
                      }
                    >
                      <Text style={styles.promoButtonText}>Pedir</Text>
                    </TouchableOpacity>
                  </View>
                </View>
              </View>
            )}

            {/* Category Pills con glow */}
            <View style={styles.pillsContainer}>
              <TouchableOpacity
                onPress={() => setSelectedCategory(null)}
                style={[
                  styles.pill,
                  selectedCategory === null && styles.pillActive,
                ]}
              >
                <Text
                  style={[
                    styles.pillText,
                    selectedCategory === null && styles.pillTextActive,
                  ]}
                >
                  Todos
                </Text>
              </TouchableOpacity>
              {categorias.map(cat => (
                <TouchableOpacity
                  key={cat.id}
                  onPress={() => setSelectedCategory(cat.id)}
                  style={[
                    styles.pill,
                    selectedCategory === cat.id && styles.pillActive,
                  ]}
                >
                  <Text
                    style={[
                      styles.pillText,
                      selectedCategory === cat.id && styles.pillTextActive,
                    ]}
                  >
                    {cat.nombre}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            {/* Section Title */}
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>Más Populares</Text>
              <TouchableOpacity onPress={() => setSelectedCategory(null)}>
                <Text style={styles.seeAll}>Ver Todo</Text>
              </TouchableOpacity>
            </View>
          </>
        }
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={styles.emptyText}>No hay productos</Text>
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
  row: {
    justifyContent: 'flex-start',
    gap: 16,
    marginBottom: 16,
  },
  empty: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 60,
  },
  emptyText: {
    color: colors.textMuted,
    fontSize: 16,
  },
  errorText: {
    color: colors.error,
    fontSize: 16,
    textAlign: 'center',
    paddingHorizontal: 32,
  },
  promoCard: {
    borderRadius: 18,
    marginBottom: 20,
    overflow: 'hidden',
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25,
    shadowRadius: 12,
    elevation: 6,
  },
  promoImage: {
    width: '100%',
    height: 170,
  },
  promoContent: {
    padding: 14,
    gap: 6,
  },
  promoBadge: {
    alignSelf: 'flex-start',
    backgroundColor: colors.neonPinkAlpha,
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 4,
    marginBottom: 4,
  },
  promoBadgeText: {
    color: colors.neonPink,
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  promoTitle: {
    color: colors.textOnSurface,
    fontSize: 18,
    fontWeight: '800',
  },
  promoDesc: {
    color: colors.textSecondary,
    fontSize: 13,
    lineHeight: 18,
  },
  promoFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 8,
  },
  promoPrice: {
    color: colors.neonOrange,
    fontSize: 24,
    fontWeight: '900',
  },
  promoButton: {
    backgroundColor: colors.neonOrange,
    borderRadius: 12,
    paddingVertical: 10,
    paddingHorizontal: 18,
  },
  promoButtonText: {
    color: colors.background,
    fontSize: 14,
    fontWeight: '800',
  },
  pillsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginBottom: 20,
  },
  pill: {
    minHeight: 40,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: colors.surfaceLight,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pillActive: {
    backgroundColor: colors.neonOrange,
    borderColor: colors.neonOrange,
    shadowColor: colors.neonOrange,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.35,
    shadowRadius: 8,
    elevation: 4,
  },
  pillText: {
    color: colors.textOnSurface,
    fontSize: 13,
    fontWeight: '600',
  },
  pillTextActive: {
    color: colors.background,
    fontWeight: '800',
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  sectionTitle: {
    color: colors.textPrimary,
    fontSize: 18,
    fontWeight: '800',
  },
  seeAll: {
    color: colors.neonPink,
    fontSize: 13,
    fontWeight: '700',
  },
});
