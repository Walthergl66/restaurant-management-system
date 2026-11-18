/**
 * Estación Burger — Neon Dark Theme
 * Paleta basada en el logo: naranja/amarillo neón + magenta/fucsia neón
 */

export const colors = {
  // Fondo
  background: '#0A0A0A',
  surface: '#F8EEE2',
  surfaceLight: '#EFE1D2',
  surfaceBorder: '#D4C1AE',

  // Acentos corporativos (web)
  neonOrange: '#F5A623',
  neonOrangeGlow: '#FFB84D',
  neonPink: '#C31562',
  neonPinkGlow: '#E91E8C',

  // Texto
  textPrimary: '#F5EEE2',
  textOnSurface: '#211D1B',
  textSecondary: '#6F625A',
  textMuted: '#887A70',

  // Estados
  success: '#22C55E',
  warning: '#F59E0B',
  error: '#EF4444',

  // Transparencias
  overlay: 'rgba(0, 0, 0, 0.7)',
  neonOrangeAlpha: 'rgba(245, 166, 35, 0.15)',
  neonPinkAlpha: 'rgba(195, 21, 98, 0.15)',
} as const;
