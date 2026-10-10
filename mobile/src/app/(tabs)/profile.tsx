/**
 * Estación Burger — Pantalla de Perfil
 *
 * Usuario real desde la sesión (GET /api/v1/auth/me al login).
 */

import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../../theme/colors';
import { useAuth } from '../../features/auth/AuthContext';
import { Button } from '../../components/ui/Button';
import { Header } from '../../components/Header';
import { clientesService } from '../../features/clientes/clientesService';
import type { ClientePerfil } from '../../features/clientes/clientesService';

export default function ProfileScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { usuario, logout } = useAuth();
  const [loading, setLoading] = useState(false);
  const [perfil, setPerfil] = useState<ClientePerfil | null>(null);
  const [perfilError, setPerfilError] = useState<string | null>(null);

  useEffect(() => {
    let activo = true;
    clientesService
      .perfil()
      .then((p) => {
        if (activo) setPerfil(p);
      })
      .catch(() => {
        if (activo) setPerfilError('No se pudo cargar el perfil');
      });
    return () => {
      activo = false;
    };
  }, []);

  const handleLogout = async () => {
    setLoading(true);
    try {
      await logout();
      router.replace('/login');
    } finally {
      setLoading(false);
    }
  };

  const nombre = perfil?.nombre || usuario?.nombre || 'Invitado';
  const correo = usuario?.username || 'sin sesión';
  const inicial = nombre.charAt(0).toUpperCase();

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <Header title="Perfil" />

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.userCard}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>{inicial}</Text>
          </View>
          <Text style={styles.userName}>{nombre}</Text>
          <Text style={styles.userEmail}>@{correo}</Text>
          {usuario?.rol && (
            <View style={styles.rolBadge}>
              <Text style={styles.rolText}>{usuario.rol}</Text>
            </View>
          )}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Datos personales</Text>
          <ProfileRow
            icon="card-outline"
            label="Cédula"
            value={perfil?.cedula}
          />
          <ProfileRow
            icon="call-outline"
            label="Celular"
            value={perfil?.telefono}
          />
          {perfilError && (
            <Text style={styles.perfilError}>{perfilError}</Text>
          )}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Mi cuenta</Text>
          <ProfileRow
            icon="key-outline"
            label="Permisos"
            value={
              usuario?.permisos
                ? `${usuario.permisos.length} activos`
                : undefined
            }
          />
          <ProfileRow
            icon="location-outline"
            label="Direcciones"
            onPress={() => router.push('/direcciones')}
          />
          <ProfileRow
            icon="card-outline"
            label="Métodos de pago"
            onPress={() => router.push('/metodos-pago')}
          />
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Soporte</Text>
          <ProfileRow
            icon="help-circle-outline"
            label="Ayuda"
            onPress={() => router.push('/ayuda')}
          />
          <ProfileRow
            icon="document-text-outline"
            label="Términos y condiciones"
            onPress={() => router.push('/terminos')}
          />
          <ProfileRow
            icon="shield-checkmark-outline"
            label="Política de privacidad"
            onPress={() => router.push('/privacidad')}
          />
        </View>

        <Button
          title="Cerrar sesión"
          onPress={handleLogout}
          variant="outline"
          size="lg"
          loading={loading}
          icon={<Ionicons name="log-out-outline" size={20} color={colors.error} />}
          style={styles.logoutButton}
          textStyle={{ color: colors.error }}
        />

        <Text style={styles.version}>Estación Burger v1.0.0</Text>
      </ScrollView>
    </View>
  );
}

function ProfileRow({
  icon,
  label,
  value,
  onPress,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  value?: string;
  onPress?: () => void;
}) {
  return (
    <View style={styles.row}>
      <View style={styles.rowLeft}>
        <Ionicons name={icon} size={22} color={colors.neonOrange} />
        <Text style={styles.rowLabel}>{label}</Text>
      </View>
      {value ? (
        <Text style={styles.rowValue} numberOfLines={1}>
          {value}
        </Text>
      ) : (
        <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
      )}
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
  rolBadge: {
    marginTop: 10,
    backgroundColor: colors.neonOrangeAlpha,
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  rolText: {
    color: colors.neonOrange,
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  perfilError: {
    color: colors.textMuted,
    fontSize: 12,
    paddingHorizontal: 16,
    paddingVertical: 8,
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
  rowValue: {
    color: colors.textMuted,
    fontSize: 14,
    maxWidth: '60%',
  },
  logoutButton: {
    borderColor: colors.error,
    marginTop: 8,
  },
  version: {
    color: colors.textMuted,
    fontSize: 12,
    textAlign: 'center',
    marginTop: 24,
  },
});
