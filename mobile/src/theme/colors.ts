/**
 * Estación Burger — Neon Dark Theme
 * Paleta basada en el logo: naranja/amarillo neón + magenta/fucsia neón
 */

export const colors = {
  // Fondo
  background: '#0A0A0A',
  surface: '#1A1A1A',
  surfaceLight: '#2A2A2A',
  surfaceBorder: '#333333',

  // Acentos neón
  neonOrange: '#FF8C00',
  neonOrangeGlow: '#FFA500',
  neonPink: '#FF10F0',
  neonPinkGlow: '#FF69B4',

  // Texto
  textPrimary: '#FFFFFF',
  textSecondary: '#B0B0B0',
  textMuted: '#666666',

  // Estados
  success: '#00FF88',
  warning: '#FFAA00',
  error: '#FF3355',

  // Transparencias
  overlay: 'rgba(0, 0, 0, 0.7)',
  neonOrangeAlpha: 'rgba(255, 140, 0, 0.15)',
  neonPinkAlpha: 'rgba(255, 16, 240, 0.15)',
} as const;
