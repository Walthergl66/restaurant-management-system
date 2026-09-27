/**
 * Estación Burger — Tipografía
 * Sans-serif, pesos pesados para títulos, legible para descripciones
 */

import { Platform } from 'react-native';

export const typography = {
  // Fuentes
  fontFamily: Platform.select({
    ios: 'System',
    android: 'sans-serif',
    default: 'System',
  }),
  fontFamilyMedium: Platform.select({
    ios: 'System',
    android: 'sans-serif-medium',
    default: 'System',
  }),
  fontFamilyBold: Platform.select({
    ios: 'System',
    android: 'sans-serif',
    default: 'System',
  }),

  // Tamaños
  sizes: {
    xs: 11,
    sm: 13,
    base: 15,
    md: 17,
    lg: 20,
    xl: 24,
    xxl: 28,
    hero: 34,
  },

  // Pesos
  weights: {
    normal: '400' as const,
    medium: '500' as const,
    semibold: '600' as const,
    bold: '700' as const,
    heavy: '800' as const,
  },
} as const;
