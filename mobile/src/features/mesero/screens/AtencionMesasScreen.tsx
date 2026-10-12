import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, RefreshControl, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../../auth/AuthContext';
import { puedeAtenderMesas } from '../../auth/permisos';
import { useCart } from '../../carrito/CartContext';
import { aLineaPedido, claveLinea } from '../../carrito/lineas';
import { menuService } from '../../menu/menuService';
import type { ProductoMenu } from '../../menu/types';
import { meseroService } from '../meseroService';
import type { MesaMesero } from '../types';
import { generarCodigoPedido } from '../../pedidos/idempotencia';
import { colors } from '../../../theme/colors';

export default function WaiterScreen() {
  const router = useRouter();
  const { usuario, logout } = useAuth();
  const { items, totalItems, totalPrice, clearCart } = useCart();
  const [mesas, setMesas] = useState<MesaMesero[]>([]);
  const [productos, setProductos] = useState<ProductoMenu[]>([]);
  const [mesaId, setMesaId] = useState<number | null>(null);
  const [notas, setNotas] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const autorizado = puedeAtenderMesas(usuario);
  const cargar = useCallback(async () => {
    setError(null);
    try {
      const [disponibles, menu] = await Promise.all([meseroService.listarMesasHabilitadas(), menuService.getMenu()]);
      setMesas(disponibles);
      setProductos(menu.categorias.flatMap(categoria => categoria.productos));
      setMesaId(actual => disponibles.some(mesa => mesa.id === actual) ? actual : null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los datos');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { if (autorizado) void cargar(); }, [cargar, autorizado]);

  const confirmar = async () => {
    if (!mesaId || items.length === 0) return;
    setSending(true);
    const codigo = generarCodigoPedido();
    try {
      await meseroService.crearPedido(mesaId, codigo, items.map(aLineaPedido), notas);
      clearCart();
      setNotas('');
      Alert.alert('Pedido enviado', `El pedido ${codigo} fue confirmado y enviado a cocina.`);
    } catch (e) {
      Alert.alert('No se pudo crear el pedido', e instanceof Error ? e.message : 'Inténtalo nuevamente');
    } finally {
      setSending(false);
    }
  };

  if (!autorizado) return <View style={styles.center}><Text style={styles.error}>No tienes permisos de mesero.</Text></View>;
  if (loading) return <View style={styles.center}><ActivityIndicator color={colors.neonOrange} /></View>;

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content} refreshControl={<RefreshControl refreshing={loading} onRefresh={cargar} tintColor={colors.neonOrange} />}>
      <View style={styles.header}><Text style={styles.title}>Atención de mesas</Text><TouchableOpacity onPress={() => void logout()}><Ionicons name="log-out-outline" size={25} color={colors.neonPink} /></TouchableOpacity></View>
      <Text style={styles.subtitle}>Selecciona una mesa habilitada y agrega productos.</Text>

      {error && <View style={styles.errorBox}><Text style={styles.error}>{error}</Text></View>}

      <Text style={styles.label}>MESA</Text>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.row}>
        {mesas.map(mesa => (
          <TouchableOpacity key={mesa.id} style={[styles.mesa, mesaId === mesa.id && styles.selected]} onPress={() => setMesaId(mesa.id)}>
            <Text style={[styles.mesaText, mesaId === mesa.id && styles.selectedText]}>Mesa {mesa.numero}</Text>
            <Text style={styles.meta}>{mesa.capacidad} personas</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>
      {mesas.length === 0 && <Text style={styles.empty}>No hay mesas habilitadas.</Text>}

      <Text style={styles.label}>PRODUCTOS</Text>
      <View style={styles.products}>
        {productos.map(producto => (
          <TouchableOpacity key={producto.id} style={styles.product} onPress={() => router.push(`/(waiter)/product/${producto.id}`)}>
            <View style={{ flex: 1 }}><Text style={styles.productName}>{producto.nombre}</Text><Text style={styles.price}>${producto.precio.toFixed(2)}</Text></View>
            <Ionicons name="options" size={20} color={colors.neonPink} />
          </TouchableOpacity>
        ))}
      </View>

      <View style={styles.summary}>
        <Text style={styles.summaryTitle}>Pedido actual · {totalItems} productos</Text>
        {items.map(item => <Text key={claveLinea(item)} style={styles.item}>{item.cantidad}× {item.producto.nombre}{item.extras.length ? ` + ${item.extras.map(e => e.nombre).join(', ')}` : ''}{item.ingredientesRemovidos.length ? ` · Sin: ${item.ingredientesRemovidos.join(', ')}` : ''}</Text>)}
        <TextInput style={styles.input} value={notas} onChangeText={setNotas} placeholder="Notas generales (opcional)" placeholderTextColor={colors.textMuted} />
        <View style={styles.total}><Text style={styles.summaryTitle}>Total</Text><Text style={styles.price}>${totalPrice.toFixed(2)}</Text></View>
        <TouchableOpacity disabled={!mesaId || totalItems === 0 || sending} onPress={confirmar} style={[styles.button, (!mesaId || totalItems === 0 || sending) && styles.disabled]}>
          {sending ? <ActivityIndicator color="#000" /> : <Text style={styles.buttonText}>CONFIRMAR Y ENVIAR A COCINA</Text>}
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen:{flex:1,backgroundColor:colors.background},content:{padding:20,paddingBottom:100},center:{flex:1,alignItems:'center',justifyContent:'center',backgroundColor:colors.background},header:{flexDirection:'row',alignItems:'center',justifyContent:'space-between'},title:{fontSize:26,fontWeight:'800',color:colors.textPrimary},subtitle:{color:colors.textMuted,marginTop:4,marginBottom:22},label:{color:colors.textSecondary,fontSize:11,fontWeight:'800',letterSpacing:1,marginTop:18,marginBottom:10},row:{gap:10},mesa:{padding:14,borderRadius:14,backgroundColor:colors.surface,borderWidth:1,borderColor:colors.surfaceBorder},selected:{borderColor:colors.neonOrange},mesaText:{color:colors.textPrimary,fontWeight:'700'},selectedText:{color:colors.neonOrange},meta:{color:colors.textMuted,fontSize:11,marginTop:3},products:{gap:8},product:{flexDirection:'row',alignItems:'center',padding:14,borderRadius:14,backgroundColor:colors.surface,borderWidth:1,borderColor:colors.surfaceBorder},productName:{color:colors.textPrimary,fontWeight:'700'},price:{color:colors.neonOrange,fontWeight:'800',marginTop:3},summary:{marginTop:24,padding:16,borderRadius:16,backgroundColor:colors.surface,borderWidth:1,borderColor:colors.surfaceBorder},summaryTitle:{color:colors.textPrimary,fontWeight:'800'},item:{color:colors.textSecondary,fontSize:12,marginTop:7},input:{marginTop:14,padding:12,borderRadius:10,backgroundColor:colors.background,color:colors.textPrimary,borderWidth:1,borderColor:colors.surfaceBorder},total:{flexDirection:'row',justifyContent:'space-between',alignItems:'center',marginTop:14},button:{marginTop:16,padding:15,borderRadius:12,alignItems:'center',backgroundColor:colors.neonOrange},disabled:{opacity:.4},buttonText:{color:'#000',fontWeight:'900'},errorBox:{padding:12,borderRadius:10,backgroundColor:'rgba(255,0,80,.12)'},error:{color:colors.neonPink},empty:{color:colors.textMuted},
});
