/**
 * Estación Burger — Tarjeta con bordes suavizados
 */

import React, { ReactNode } from 'react';
import { View, StyleSheet, ViewStyle } from 'react-native';
import { colors } from '../../theme/colors';

interface CardProps {
  children: ReactNode;
  style?: ViewStyle;
  variant?: 'default' | 'elevated' | 'outline';
  onPress?: () => void;
}

export function Card({
  children,
  style,
  variant = 'default',
  onPress,
}: CardProps) {
  return (
    <View
      style={[
        styles.base,
        variant === 'default' && styles.default,
        variant === 'elevated' && styles.elevated,
        variant === 'outline' && styles.outline,
        style,
      ]}
    >
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  base: {
    borderRadius: 18,
    overflow: 'hidden',
  },
  default: {
    backgroundColor: colors.surface,
  },
  elevated: {
    backgroundColor: colors.surface,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 12,
    elevation: 8,
  },
  outline: {
    backgroundColor: 'transparent',
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
});
