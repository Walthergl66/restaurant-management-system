import React from 'react';
import {
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { Button } from '@/components/ui/Button';
import { claveLinea } from '@/features/carrito';
import type { CartItem } from '@/features/carrito';
import { colors } from '@/theme/colors';
import { LineaResumen } from './LineaResumen';

interface ResumenPedidoSheetProps {
  visible: boolean;
  mesaNumero: string;
  items: CartItem[];
  totalPrice: number;
  notas: string;
  enviando: boolean;
  onNotas: (texto: string) => void;
  onClose: () => void;
  onConfirmar: () => void;
  /** Quita la línea y abre la pantalla de producto para volver a elegirla. */
  onEditar: (item: CartItem) => void;
  /** Suma una unidad más de la misma combinación. */
  onDuplicar: (item: CartItem) => void;
  onEliminar: (item: CartItem) => void;
}

/** Hoja inferior con el resumen del pedido: líneas con acciones, notas, total y envío a cocina. */
export function ResumenPedidoSheet({
  visible,
  mesaNumero,
  items,
  totalPrice,
  notas,
  enviando,
  onNotas,
  onClose,
  onConfirmar,
  onEditar,
  onDuplicar,
  onEliminar,
}: ResumenPedidoSheetProps) {
  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <KeyboardAvoidingView style={styles.wrapper} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <Pressable style={styles.fondo} onPress={onClose} accessibilityLabel="Cerrar resumen" />
        <View style={styles.hoja}>
          <View style={styles.asa} />
          <Text style={styles.titulo}>Pedido · Mesa {mesaNumero}</Text>

          <ScrollView style={styles.lista} keyboardShouldPersistTaps="handled">
            {items.length === 0 ? (
              <Text style={styles.vacio}>Todavía no hay productos en el pedido.</Text>
            ) : (
              items.map(item => (
                <LineaResumen
                  key={claveLinea(item)}
                  item={item}
                  onEditar={() => onEditar(item)}
                  onDuplicar={() => onDuplicar(item)}
                  onEliminar={() => onEliminar(item)}
                />
              ))
            )}

            <TextInput
              style={styles.notas}
              value={notas}
              onChangeText={onNotas}
              placeholder="Notas generales (opcional)"
              placeholderTextColor={colors.textMuted}
              multiline
              maxLength={500}
            />
          </ScrollView>

          <View style={styles.total}>
            <Text style={styles.totalEtiqueta}>Total</Text>
            <Text style={styles.totalValor}>${totalPrice.toFixed(2)}</Text>
          </View>

          <Button
            title="Confirmar y enviar a cocina"
            onPress={onConfirmar}
            loading={enviando}
            disabled={items.length === 0 || enviando}
            size="lg"
          />
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  wrapper: { flex: 1, justifyContent: 'flex-end' },
  fondo: { ...StyleSheet.absoluteFill, backgroundColor: colors.overlay },
  hoja: {
    backgroundColor: colors.surface,
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    padding: 20,
    paddingBottom: 28,
    maxHeight: '85%',
    gap: 12,
  },
  asa: {
    alignSelf: 'center',
    width: 44,
    height: 4,
    borderRadius: 2,
    backgroundColor: colors.surfaceBorder,
  },
  titulo: { color: colors.textOnSurface, fontSize: 20, fontWeight: '800' },
  lista: { flexGrow: 0 },
  vacio: { color: colors.textSecondary, textAlign: 'center', paddingVertical: 24 },
  notas: {
    backgroundColor: colors.surfaceLight,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    color: colors.textOnSurface,
    borderRadius: 12,
    padding: 12,
    minHeight: 48,
    marginTop: 12,
  },
  total: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingTop: 4 },
  totalEtiqueta: { color: colors.textOnSurface, fontSize: 20, fontWeight: '800' },
  totalValor: { color: colors.textOnSurface, fontSize: 22, fontWeight: '800' },
});
