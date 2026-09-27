/**
 * Estación Burger — Badge neón
 */

import React from 'react';
import { View, Text, StyleSheet, ViewStyle } from 'react-native';
import { colors } from '../../theme/colors';

interface BadgeProps {
  text: string;
  variant?: 'orange' | 'pink' | 'success' | 'error' | 'neutral';
  size?: 'sm' | 'md';
  style?: ViewStyle;
}

export function Badge({
  text,
  variant = 'orange',
  size = 'sm',
  style,
}: BadgeProps) {
  return (
    <View
      style={[
        styles.base,
        variant === 'orange' && styles.orange,
        variant === 'pink' && styles.pink,
        variant === 'success' && styles.success,
        variant === 'error' && styles.error,
        variant === 'neutral' && styles.neutral,
        size === 'md' && styles.md,
        style,
      ]}
    >
      <Text
        style={[
          styles.text,
          variant === 'orange' && styles.textOrange,
          variant === 'pink' && styles.textPink,
          variant === 'success' && styles.textSuccess,
          variant === 'error' && styles.textError,
          variant === 'neutral' && styles.textNeutral,
          size === 'md' && styles.textMd,
        ]}
      >
        {text}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  base: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 8,
    alignSelf: 'flex-start',
  },
  md: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 10,
  },
  orange: {
    backgroundColor: colors.neonOrangeAlpha,
    borderWidth: 1,
    borderColor: colors.neonOrange,
  },
  pink: {
    backgroundColor: colors.neonPinkAlpha,
    borderWidth: 1,
    borderColor: colors.neonPink,
  },
  success: {
    backgroundColor: 'rgba(0, 255, 136, 0.15)',
    borderWidth: 1,
    borderColor: colors.success,
  },
  error: {
    backgroundColor: 'rgba(255, 51, 85, 0.15)',
    borderWidth: 1,
    borderColor: colors.error,
  },
  neutral: {
    backgroundColor: colors.surfaceLight,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  text: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.3,
  },
  textMd: {
    fontSize: 13,
  },
  textOrange: {
    color: colors.neonOrange,
  },
  textPink: {
    color: colors.neonPink,
  },
  textSuccess: {
    color: colors.success,
  },
  textError: {
    color: colors.error,
  },
  textNeutral: {
    color: colors.textSecondary,
  },
});
