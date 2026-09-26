/**
 * Estación Burger — Pantalla de Pedidos (con datos demo compatibles con el backend)
 */

import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  RefreshControl,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { colors } from '../../theme/colors';
import { PedidoCliente } from '../../types';
import { OrderCard } from '../../components/OrderCard';
import { Header } from '../../components/Header';

const demoPedidos: PedidoCliente[] = [
  {
    codigo: 'PED-2026-001',
    estado: 'LISTO',
    metodoPago: 'TARJETA',
    metodoEntrega: 'DOMICILIO',
    lineas: [
      { productoId: 1, nombre: 'La Estación', precio: 8.99, cantidad: 2, subtotal: 20.98, extras: [{ extraId: 1, nombre: 'Queso cheddar', precio: 1.5 }] },
      { productoId: 5, nombre: 'Papas Rústicas', precio: 3.25, cantidad: 1, subtotal: 3.25, extras: [] },
      { productoId: 8, nombre: 'Limonada de Fresa', precio: 2.25, cantidad: 2, subtotal: 4.5, extras: [] },
    ],
    total: 28.73,
    creadoAt: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
  },
  {
    codigo: 'PED-2026-002',
    estado: 'EN_PREPARACION',
    metodoPago: 'EFECTIVO',
    metodoEntrega: 'RETIRAR',
    lineas: [
      { productoId: 2, nombre: 'Clásica Burger', precio: 6.5, cantidad: 1, subtotal: 6.5, extras: [] },
      { productoId: 9, nombre: 'Té Helado de Durazno', precio: 2.0, cantidad: 1, subtotal: 2.0, extras: [] },
    ],
    total: 8.5,
    creadoAt: new Date(Date.now() - 1000 * 60 * 15).toISOString(),
  },
  {
    codigo: 'PED-2026-003',
    estado: 'ENTREGADO',
    metodoPago: 'TARJETA',
    metodoEntrega: 'DOMICILIO',
    lineas: [
      { productoId: 4, nombre: 'Doble Bacon Cheese', precio: 9.5, cantidad: 1, subtotal: 11.5, extras: [{ extraId: 2, nombre: 'Bacon crujiente', precio: 2.0 }] },
      { productoId: 7, nombre: 'Nachos Supreme', precio: 4.5, cantidad: 1, subtotal: 4.5, extras: [] },
      { productoId: 11, nombre: 'Cheesecake de Frutos Rojos', precio: 4.25, cantidad: 1, subtotal: 4.25, extras: [] },
    ],
    total: 20.25,
    creadoAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(),
  },
];

export default function OrdersScreen() {
  const insets = useSafeAreaInsets();
  const [refreshing, setRefreshing] = useState(false);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    setTimeout(() => setRefreshing(false), 1000);
  }, []);

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <Header title="Mis Pedidos" />

      <FlatList
        data={demoPedidos}
        renderItem={({ item }) => (
          <OrderCard pedido={item} onPress={() => {}} />
        )}
        keyExtractor={item => item.codigo}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={colors.neonOrange}
            colors={[colors.neonOrange]}
          />
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
  listContent: {
    padding: 16,
    paddingBottom: 100,
  },
});
