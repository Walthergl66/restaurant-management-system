/**
 * Estación Burger — Pantalla de Pedidos (con datos demo)
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
import { useRouter } from 'expo-router';
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
      { productoId: 1, nombreProducto: 'La Estación', precioUnitario: '8.99', cantidad: 2, extras: [{ extraId: 1, nombreExtra: 'Queso cheddar', precio: '1.50' }] },
      { productoId: 5, nombreProducto: 'Papas Rústicas', precioUnitario: '3.25', cantidad: 1, extras: [] },
      { productoId: 8, nombreProducto: 'Limonada de Fresa', precioUnitario: '2.25', cantidad: 2, extras: [] },
    ],
    total: '27.48',
    fechaCreacion: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
  },
  {
    codigo: 'PED-2026-002',
    estado: 'EN_PREPARACION',
    metodoPago: 'EFECTIVO',
    metodoEntrega: 'LOCAL',
    lineas: [
      { productoId: 2, nombreProducto: 'Clásica Burger', precioUnitario: '6.50', cantidad: 1, extras: [] },
      { productoId: 9, nombreProducto: 'Té Helado de Durazno', precioUnitario: '2.00', cantidad: 1, extras: [] },
    ],
    total: '8.50',
    fechaCreacion: new Date(Date.now() - 1000 * 60 * 15).toISOString(),
  },
  {
    codigo: 'PED-2026-003',
    estado: 'ENTREGADO',
    metodoPago: 'TARJETA',
    metodoEntrega: 'DOMICILIO',
    lineas: [
      { productoId: 4, nombreProducto: 'Doble Bacon Cheese', precioUnitario: '9.50', cantidad: 1, extras: [{ extraId: 2, nombreExtra: 'Bacon crujiente', precio: '2.00' }] },
      { productoId: 7, nombreProducto: 'Nachos Supreme', precioUnitario: '4.50', cantidad: 1, extras: [] },
      { productoId: 11, nombreProducto: 'Cheesecake de Frutos Rojos', precioUnitario: '4.25', cantidad: 1, extras: [] },
    ],
    total: '20.25',
    fechaCreacion: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(),
  },
];

export default function OrdersScreen() {
  const router = useRouter();
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
