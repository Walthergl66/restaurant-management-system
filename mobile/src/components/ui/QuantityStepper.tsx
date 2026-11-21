/**
 * Estación Burger — Control de cantidad (− / +)
 * Genérico: no conoce carritos ni productos, solo un número.
 */

import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';

interface QuantityStepperProps {
  valor: number;
  onCambiar: (valor: number) => void;
  /** Valor mínimo al que puede bajar el control (por defecto 0). */
  min?: number;
}

export function QuantityStepper({ valor, onCambiar, min = 0 }: QuantityStepperProps) {
  return (
    <View style={styles.container}>
      <TouchableOpacity
        onPress={() => onCambiar(Math.max(min, valor - 1))}
        disabled={valor <= min}
        style={[styles.button, valor <= min && styles.disabled]}
        accessibilityRole="button"
        accessibilityLabel="Disminuir cantidad"
      >
        <Ionicons name="remove" size={16} color={colors.textOnSurface} />
      </TouchableOpacity>
      <Text style={styles.value}>{valor}</Text>
      <TouchableOpacity
        onPress={() => onCambiar(valor + 1)}
        style={styles.button}
        accessibilityRole="button"
        accessibilityLabel="Aumentar cantidad"
      >
        <Ionicons name="add" size={16} color={colors.textOnSurface} />
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surfaceLight,
    borderRadius: 10,
    paddingHorizontal: 4,
  },
  button: {
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  disabled: {
    opacity: 0.4,
  },
  value: {
    color: colors.textOnSurface,
    fontSize: 15,
    fontWeight: '700',
    minWidth: 28,
    textAlign: 'center',
  },
});
