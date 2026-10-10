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
import { colors } from '../../theme/colors';
import { menuService } from '../../features/menu/menuService';
import type { MenuDto, ProductoMenu } from '../../features/menu/types';
import { ProductCard } from '../../components/ProductCard';
import { Header } from '../../components/Header';
import { useCart } from '../../features/carrito/CartContext';
import { useAuth } from '../../features/auth/AuthContext';

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
            {/* Promo Card con imagen de fondo y overlay */}
            <View style={styles.promoCard}>
              {/* Imagen de fondo */}
              <Image
                source={{ uri: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600' }}
                style={styles.promoBgImage}
                resizeMode="cover"
              />
              {/* Overlay degradado para legibilidad */}
              <LinearGradient
                colors={['rgba(10,10,10,0.6)', 'rgba(10,10,10,0.45)', 'rgba(26,10,18,0.65)']}
                start={[0, 0]}
                end={[1, 1]}
                style={styles.promoOverlay}
              />
              {/* Borde neón */}
              <LinearGradient
                colors={['rgba(255,16,240,0.15)', 'rgba(255,140,0,0.08)']}
                start={[0, 0]}
                end={[1, 1]}
                style={styles.promoBorder}
              />

              <View style={styles.promoContent}>
                <LinearGradient
                  colors={[colors.neonPink, colors.neonPinkGlow]}
                  start={[0, 0]}
                  end={[1, 1]}
                  style={styles.promoBadge}
                >
                  <Text style={styles.promoBadgeText}>PROMO ESTACIÓN</Text>
                </LinearGradient>
                <View style={styles.promoTextContainer}>
                  <Text style={styles.promoTitle}>Combo Estación</Text>
                  <Text style={styles.promoDesc}>Burger + Papas grandes + Bebida</Text>
                  <View style={styles.promoPricing}>
                    <Text style={styles.promoPrice}>$12.90</Text>
                    <Text style={styles.promoOriginal}>$18.50</Text>
                  </View>
                </View>
                <TouchableOpacity style={styles.promoButton}>
                  <LinearGradient
                    colors={[colors.neonOrange, colors.neonOrangeGlow]}
                    start={[0, 0]}
                    end={[1, 0]}
                    style={styles.promoButtonGradient}
                  >
                    <Text style={styles.promoButtonText}>Pedir</Text>
                  </LinearGradient>
                </TouchableOpacity>
              </View>
            </View>

            {/* Category Pills con glow */}
            <View style={styles.pillsContainer}>
              <TouchableOpacity
                onPress={() => setSelectedCategory(null)}
                style={[
                  styles.pill,
                  selectedCategory === null && styles.pillActive,
                ]}
              >
                {selectedCategory === null ? (
                  <LinearGradient
                    colors={[colors.neonOrange, colors.neonOrangeGlow]}
                    start={[0, 0]}
                    end={[1, 1]}
                    style={styles.pillGradient}
                  >
                    <Text style={[styles.pillText, styles.pillTextActive]}>
                      Todos
                    </Text>
                  </LinearGradient>
                ) : (
                  <Text style={styles.pillText}>Todos</Text>
                )}
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
                  {selectedCategory === cat.id ? (
                    <LinearGradient
                      colors={[colors.neonOrange, colors.neonOrangeGlow]}
                      start={[0, 0]}
                      end={[1, 1]}
                      style={styles.pillGradient}
                    >
                      <Text style={[styles.pillText, styles.pillTextActive]}>
                        {cat.nombre}
                      </Text>
                    </LinearGradient>
                  ) : (
                    <Text style={styles.pillText}>{cat.nombre}</Text>
                  )}
                </TouchableOpacity>
              ))}
            </View>

            {/* Section Title */}
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>Más Populares</Text>
              <TouchableOpacity>
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
    borderRadius: 20,
    marginBottom: 20,
    overflow: 'hidden',
    height: 180,
    shadowColor: colors.neonPink,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 16,
    elevation: 12,
  },
  promoBgImage: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    width: '100%',
    height: '100%',
  },
  promoOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
  promoBorder: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.neonPink,
  },
  promoContent: {
    flex: 1,
    padding: 16,
    justifyContent: 'space-between',
  },
  promoBadge: {
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 4,
    alignSelf: 'flex-start',
    marginBottom: 8,
  },
  promoBadgeText: {
    color: colors.background,
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  promoTextContainer: {
    flex: 1,
  },
  promoTitle: {
    color: colors.textPrimary,
    fontSize: 20,
    fontWeight: '800',
    marginBottom: 4,
    textShadowColor: 'rgba(0, 0, 0, 0.8)',
    textShadowOffset: { width: 0, height: 1 },
    textShadowRadius: 4,
  },
  promoDesc: {
    color: '#E0E0E0',
    fontSize: 12,
    marginBottom: 8,
    textShadowColor: 'rgba(0, 0, 0, 0.8)',
    textShadowOffset: { width: 0, height: 1 },
    textShadowRadius: 4,
  },
  promoPricing: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  promoPrice: {
    color: colors.neonOrange,
    fontSize: 26,
    fontWeight: '900',
    textShadowColor: 'rgba(0, 0, 0, 0.9)',
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 8,
  },
  promoOriginal: {
    color: '#CCCCCC',
    fontSize: 14,
    textDecorationLine: 'line-through',
    textShadowColor: 'rgba(0, 0, 0, 0.8)',
    textShadowOffset: { width: 0, height: 1 },
    textShadowRadius: 4,
  },
  promoButton: {
    borderRadius: 12,
    overflow: 'hidden',
    alignSelf: 'flex-start',
  },
  promoButtonGradient: {
    paddingVertical: 10,
    paddingHorizontal: 20,
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 6,
  },
  promoButtonText: {
    color: colors.background,
    fontSize: 14,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  pillsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginBottom: 20,
  },
  pill: {
    paddingHorizontal: 18,
    paddingVertical: 10,
    borderRadius: 25,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    overflow: 'hidden',
  },
  pillActive: {
    backgroundColor: 'transparent',
    borderColor: colors.neonOrange,
    shadowColor: colors.neonOrange,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.6,
    shadowRadius: 12,
    elevation: 8,
  },
  pillGradient: {
    marginHorizontal: -18,
    marginVertical: -10,
    paddingHorizontal: 18,
    paddingVertical: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pillText: {
    color: colors.textSecondary,
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
