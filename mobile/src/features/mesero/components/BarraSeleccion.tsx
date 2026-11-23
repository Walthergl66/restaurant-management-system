import React from 'react';
import { StyleSheet, Text, TouchableOpacity } from 'react-native';
import { colors } from '@/theme/colors';

interface BarraSeleccionProps {
  totalItems: number;
  onVerPedido: () => void;
  /** Espacio inferior para no tapar la barra de gestos del dispositivo. */
  inferior?: number;
}

/** Barra fija inferior: "N productos seleccionados · Ver pedido". Se oculta si no hay nada seleccionado. */
export function BarraSeleccion({ totalItems, onVerPedido, inferior = 0 }: BarraSeleccionProps) {
  if (totalItems === 0) return null;
  return (
    <TouchableOpacity
      onPress={onVerPedido}
      activeOpacity={0.9}
      style={[styles.barra, { bottom: 16 + inferior }]}
      accessibilityRole="button"
      accessibilityLabel={`${totalItems} productos seleccionados, ver pedido`}
    >
      <Text style={styles.cantidad}>
        {totalItems} {totalItems === 1 ? 'producto seleccionado' : 'productos seleccionados'}
      </Text>
      <Text style={styles.accion}>Ver pedido</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  barra: {
    position: 'absolute',
    left: 16,
    right: 16,
    backgroundColor: colors.neonPink,
    borderRadius: 18,
    paddingHorizontal: 20,
    paddingVertical: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    shadowColor: colors.neonPink,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.5,
    shadowRadius: 12,
    elevation: 8,
  },
  cantidad: {
    color: colors.textPrimary,
    fontSize: 15,
    fontWeight: '600',
  },
  accion: {
    color: colors.textPrimary,
    fontSize: 16,
    fontWeight: '800',
  },
});
