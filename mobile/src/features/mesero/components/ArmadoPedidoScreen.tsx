import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, Alert, BackHandler, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { ChipTabs } from '@/components/ChipTabs';
import { aLineaPedido, useCart } from '@/features/carrito';
import type { CartItem } from '@/features/carrito';
import { menuService } from '@/features/menu';
import type { CategoriaMenu } from '@/features/menu';
import { generarCodigoPedido } from '@/features/pedidos';
import { colors } from '@/theme/colors';
import { meseroService } from '../api';
import { BarraSeleccion } from './BarraSeleccion';
import { ProductoFila } from './ProductoFila';
import { ResumenPedidoSheet } from './ResumenPedidoSheet';

/** Paso 2 del mesero: armar el pedido de una mesa y enviarlo a cocina. */
export default function ArmadoPedidoScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { id, numero } = useLocalSearchParams<{ id: string; numero?: string }>();
  const mesaId = Number(id);
  const mesaNumero = numero ?? id;
  const { items, totalItems, totalPrice, removeItem, updateQuantity, clearCart } = useCart();

  const [categorias, setCategorias] = useState<CategoriaMenu[]>([]);
  const [categoriaId, setCategoriaId] = useState<number | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [resumenVisible, setResumenVisible] = useState(false);
  const [notas, setNotas] = useState('');
  const [enviando, setEnviando] = useState(false);

  const cargarMenu = useCallback(async () => {
    setCargando(true);
    setError(null);
    try {
      const menu = await menuService.getMenu();
      setCategorias(menu.categorias.filter(categoria => categoria.productos.length > 0));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo cargar el menú');
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    void cargarMenu();
  }, [cargarMenu]);

  const productos = useMemo(
    () => categorias
      .filter(categoria => categoriaId === null || categoria.id === categoriaId)
      .flatMap(categoria => categoria.productos),
    [categorias, categoriaId],
  );

  /** Pide confirmación si hay productos sin enviar; al salir se descarta el pedido en curso. */
  const salir = useCallback(() => {
    if (totalItems === 0) {
      router.back();
      return;
    }
    Alert.alert('Salir del pedido', 'Tienes productos sin enviar. Si sales se descartan.', [
      { text: 'Seguir pidiendo', style: 'cancel' },
      {
        text: 'Descartar y salir',
        style: 'destructive',
        onPress: () => {
          clearCart();
          router.back();
        },
      },
    ]);
  }, [totalItems, clearCart, router]);

  // Botón atrás de Android: misma confirmación (mientras esta pantalla esté enfocada).
  useFocusEffect(
    useCallback(() => {
      const suscripcion = BackHandler.addEventListener('hardwareBackPress', () => {
        if (resumenVisible) {
          setResumenVisible(false);
          return true;
        }
        salir();
        return true;
      });
      return () => suscripcion.remove();
    }, [salir, resumenVisible]),
  );

  const editar = (item: CartItem) => {
    removeItem(item.producto.id, item.extras, item.ingredientesRemovidos);
    setResumenVisible(false);
    router.push(`/(waiter)/product/${item.producto.id}`);
  };

  const duplicar = (item: CartItem) =>
    updateQuantity(item.producto.id, item.extras, item.cantidad + 1, item.ingredientesRemovidos);

  const eliminar = (item: CartItem) => removeItem(item.producto.id, item.extras, item.ingredientesRemovidos);

  const enviar = async () => {
    if (totalItems === 0 || enviando) return;
    setEnviando(true);
    const codigo = generarCodigoPedido();
    try {
      await meseroService.enviarPedido(mesaId, codigo, items.map(aLineaPedido), notas);
      clearCart();
      setNotas('');
      setResumenVisible(false);
      Alert.alert('Pedido enviado', `El pedido ${codigo} fue confirmado y enviado a cocina.`, [
        { text: 'Aceptar', onPress: () => router.replace('/(waiter)') },
      ]);
    } catch (e) {
      Alert.alert('No se pudo enviar el pedido', e instanceof Error ? e.message : 'Inténtalo nuevamente');
    } finally {
      setEnviando(false);
    }
  };

  return (
    <View style={[styles.screen, { paddingTop: insets.top + 8 }]}>
      <View style={styles.header}>
        <TouchableOpacity onPress={salir} accessibilityLabel="Volver a las mesas" style={styles.atras}>
          <Ionicons name="chevron-back" size={26} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.title}>Pedido · Mesa {mesaNumero}</Text>
        <View style={styles.atras} />
      </View>

      {cargando ? (
        <View style={styles.center}>
          <ActivityIndicator color={colors.neonOrange} />
        </View>
      ) : error ? (
        <View style={styles.center}>
          <Text style={styles.error}>{error}</Text>
          <TouchableOpacity onPress={() => void cargarMenu()}>
            <Text style={styles.retry}>Reintentar</Text>
          </TouchableOpacity>
        </View>
      ) : categorias.length === 0 ? (
        <View style={styles.center}>
          <Text style={styles.empty}>
            No hay productos disponibles. Pídele al administrador que active el menú.
          </Text>
        </View>
      ) : (
        <>
          <ChipTabs<number>
            opciones={categorias.map(categoria => ({ id: categoria.id, etiqueta: categoria.nombre }))}
            seleccionado={categoriaId}
            onSelect={setCategoriaId}
            conTodas
          />
          <ScrollView contentContainerStyle={{ paddingTop: 8, paddingBottom: insets.bottom + 110 }}>
            {productos.map(producto => (
              <ProductoFila
                key={producto.id}
                producto={producto}
                onPersonalizar={() => router.push(`/(waiter)/product/${producto.id}`)}
              />
            ))}
          </ScrollView>
        </>
      )}

      <BarraSeleccion
        totalItems={totalItems}
        onVerPedido={() => setResumenVisible(true)}
        inferior={insets.bottom}
      />

      <ResumenPedidoSheet
        visible={resumenVisible}
        mesaNumero={String(mesaNumero)}
        items={items}
        totalPrice={totalPrice}
        notas={notas}
        enviando={enviando}
        onNotas={setNotas}
        onClose={() => setResumenVisible(false)}
        onConfirmar={() => void enviar()}
        onEditar={editar}
        onDuplicar={duplicar}
        onEliminar={eliminar}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32, gap: 12 },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingBottom: 4 },
  atras: { width: 42, padding: 8 },
  title: { flex: 1, color: colors.textPrimary, fontSize: 20, fontWeight: '800', textAlign: 'center' },
  empty: { color: colors.textMuted, fontSize: 14, textAlign: 'center' },
  error: { color: colors.error, fontSize: 14, textAlign: 'center' },
  retry: { color: colors.neonOrange, fontSize: 14, fontWeight: '700' },
});
