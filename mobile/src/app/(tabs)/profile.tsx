/**
 * Estación Burger — Pantalla de Perfil (con datos demo)
 */

import React from 'react';
import { View, Text, StyleSheet, ScrollView, Image } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../../theme/colors';
import { Button } from '../../components/ui/Button';
import { Header } from '../../components/Header';
import logo from '../../../assets/EstacionLogo.jpeg';

const demoUsuario = {
  nombre: 'Carlos Mendoza',
  username: 'carlosm',
  rol: 'CLIENTE',
};

export default function ProfileScreen() {
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <Header title="Perfil" />

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.userCard}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>C</Text>
          </View>
          <Text style={styles.userName}>{demoUsuario.nombre}</Text>
          <Text style={styles.userEmail}>@{demoUsuario.username}</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Mi cuenta</Text>
          <ProfileRow icon="person-outline" label="Datos personales" />
          <ProfileRow icon="location-outline" label="Direcciones" />
          <ProfileRow icon="card-outline" label="Métodos de pago" />
          <ProfileRow icon="notifications-outline" label="Notificaciones" />
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Soporte</Text>
          <ProfileRow icon="help-circle-outline" label="Ayuda" />
          <ProfileRow icon="document-text-outline" label="Términos y condiciones" />
          <ProfileRow icon="shield-checkmark-outline" label="Política de privacidad" />
        </View>

        <Text style={styles.version}>Estación Burger v1.0.0</Text>
      </ScrollView>
    </View>
  );
}

function ProfileRow({
  icon,
  label,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
}) {
  return (
    <View style={styles.row}>
      <View style={styles.rowLeft}>
        <Ionicons name={icon} size={22} color={colors.neonOrange} />
        <Text style={styles.rowLabel}>{label}</Text>
      </View>
      <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    padding: 16,
    paddingBottom: 100,
  },
  userCard: {
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: 20,
    padding: 24,
    marginBottom: 24,
  },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: colors.neonOrange,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
    shadowColor: colors.neonOrange,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 12,
    elevation: 8,
  },
  avatarText: {
    color: colors.background,
    fontSize: 32,
    fontWeight: '800',
  },
  userName: {
    color: colors.textPrimary,
    fontSize: 20,
    fontWeight: '800',
    marginBottom: 4,
  },
  userEmail: {
    color: colors.textSecondary,
    fontSize: 14,
  },
  section: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    marginBottom: 16,
    overflow: 'hidden',
  },
  sectionTitle: {
    color: colors.textSecondary,
    fontSize: 13,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 8,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: colors.surfaceBorder,
  },
  rowLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  rowLabel: {
    color: colors.textPrimary,
    fontSize: 15,
    fontWeight: '500',
  },
  version: {
    color: colors.textMuted,
    fontSize: 12,
    textAlign: 'center',
    marginTop: 24,
  },
});
