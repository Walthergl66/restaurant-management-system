/**
 * Estación Burger — Header con saludo y carrito
 * Estética neón basada en referencia
 */

import React from 'react';
import { View, Text, StyleSheet, Image } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../theme/colors';
import logo from '../../assets/EstacionLogo.jpeg';

interface HeaderProps {
  title?: string;
  /** Nombre del usuario para el saludo (desde /auth/me). */
  nombre?: string;
  showLogo?: boolean;
  right?: React.ReactNode;
}

export function Header({ title, nombre, showLogo = true, right }: HeaderProps) {
  const insets = useSafeAreaInsets();
  const saludo = obtenerSaludo();

  return (
    <View style={[styles.container, { paddingTop: insets.top + 12 }]}>
      <View style={styles.content}>
        <View style={styles.left}>
          {showLogo && (
            <Image source={logo} style={styles.logo} resizeMode="cover" />
          )}
          <View style={styles.greetingContainer}>
            <Text style={styles.greeting}>{saludo}</Text>
            <Text style={styles.title}>
              {nombre ? `¡Hola, ${nombre}!` : '¡Hola!'}
            </Text>
          </View>
        </View>
        {right && <View style={styles.right}>{right}</View>}
      </View>
    </View>
  );
}

/** Saludo según la hora del día. */
function obtenerSaludo(): string {
  const hora = new Date().getHours();
  if (hora < 12) return '¡Buenos días!';
  if (hora < 19) return '¡Buenas tardes!';
  return '¡Buenas noches!';
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.background,
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.surfaceBorder,
  },
  content: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
  },
  left: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  logo: {
    width: 44,
    height: 44,
    borderRadius: 22,
    borderWidth: 2,
    borderColor: colors.neonOrange,
  },
  greetingContainer: {
    gap: 2,
  },
  greeting: {
    color: colors.textSecondary,
    fontSize: 12,
  },
  title: {
    color: colors.textPrimary,
    fontSize: 18,
    fontWeight: '800',
  },
  right: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
});
