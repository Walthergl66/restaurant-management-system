/**
 * Estación Burger — Pantalla de Perfil (RF-45)
 *
 * Información personal editable: celular y correo (correo requiere
 * re-verificación). Nombre y cédula son de solo lectura.
 */

import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';
import { useAuth } from '@/features/auth';
import { Button } from '@/components/ui/Button';
import { Header } from '@/components/Header';
import { clientesService } from '@/features/clientes';
import type { ClientePerfil } from '@/features/clientes';
import { ApiError } from '@/shared/api/apiError';
import { esCelularValido, esCorreoValido } from '@/features/auth';

export default function ProfileScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { usuario, logout } = useAuth();

  const [loading, setLoading] = useState(false);
  const [perfil, setPerfil] = useState<ClientePerfil | null>(null);
  const [perfilError, setPerfilError] = useState<string | null>(null);

  const [editandoCelular, setEditandoCelular] = useState(false);
  const [editandoCorreo, setEditandoCorreo] = useState(false);
  const [celularDraft, setCelularDraft] = useState('');
  const [correoDraft, setCorreoDraft] = useState('');
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(async () => {
    try {
      setPerfilError(null);
      const p = await clientesService.perfil();
      setPerfil(p);
    } catch {
      setPerfilError('No se pudo cargar el perfil');
    }
  }, []);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  const handleLogout = async () => {
    setLoading(true);
    try {
      await logout();
      router.replace('/login');
    } finally {
      setLoading(false);
    }
  };

  const iniciarEdicionCelular = () => {
    setCelularDraft(perfil?.telefono ?? '');
    setEditandoCelular(true);
    setEditandoCorreo(false);
  };

  const iniciarEdicionCorreo = () => {
    setCorreoDraft(usuario?.username ?? '');
    setEditandoCorreo(true);
    setEditandoCelular(false);
  };

  const cancelarEdicion = () => {
    setEditandoCelular(false);
    setEditandoCorreo(false);
  };

  const guardarCelular = async () => {
    if (!esCelularValido(celularDraft)) {
      setPerfilError('El celular debe tener 10 dígitos y empezar con 09');
      return;
    }
    setGuardando(true);
    try {
      const p = await clientesService.actualizarPerfil({
        celular: celularDraft.trim(),
        correo: usuario?.username ?? '',
      });
      setPerfil(p);
      setEditandoCelular(false);
      setPerfilError(null);
    } catch (err) {
      setPerfilError(err instanceof ApiError ? err.message : 'No se pudo actualizar');
    } finally {
      setGuardando(false);
    }
  };

  const guardarCorreo = async () => {
    if (!esCorreoValido(correoDraft)) {
      setPerfilError('Ingresa un correo válido');
      return;
    }
    setGuardando(true);
    try {
      await clientesService.actualizarPerfil({
        celular: perfil?.telefono ?? '',
        correo: correoDraft.trim().toLowerCase(),
      });
      setEditandoCorreo(false);
      setPerfilError(null);
      await cargar();
    } catch (err) {
      setPerfilError(err instanceof ApiError ? err.message : 'No se pudo actualizar');
    } finally {
      setGuardando(false);
    }
  };

  const nombre = perfil?.nombre || usuario?.nombre || 'Invitado';
  const correo = usuario?.username || 'sin sesión';
  const inicial = nombre.charAt(0).toUpperCase();

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={[styles.wrapper, { paddingTop: insets.top }]}>
        <Header title="Perfil" />

        <ScrollView
          contentContainerStyle={styles.content}
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
        >
          <View style={styles.userCard}>
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>{inicial}</Text>
            </View>
            <Text style={styles.userName}>{nombre.toLocaleUpperCase('es')}</Text>
            <Text style={styles.userEmail}>@{correo}</Text>
            {usuario?.rol && (
              <View style={styles.rolBadge}>
                <Text style={styles.rolText}>{usuario.rol}</Text>
              </View>
            )}
          </View>

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Información personal</Text>

            <View style={styles.row}>
              <View style={styles.rowLeft}>
                <Ionicons name="person-outline" size={22} color={colors.neonOrange} />
                <Text style={styles.rowLabel}>Nombre</Text>
              </View>
              <Text style={styles.rowValue} numberOfLines={1}>
                {nombre.toLocaleUpperCase('es')}
              </Text>
            </View>

            <View style={styles.row}>
              <View style={styles.rowLeft}>
                <Ionicons name="card-outline" size={22} color={colors.neonOrange} />
                <Text style={styles.rowLabel}>Cédula</Text>
              </View>
              <Text style={styles.rowValue} numberOfLines={1}>
                {perfil?.cedula ?? '—'}
              </Text>
            </View>

            {editandoCelular ? (
              <View style={styles.editRow}>
                <View style={styles.rowLeft}>
                  <Ionicons name="call-outline" size={22} color={colors.neonOrange} />
                  <TextInput
                    style={styles.editInput}
                    value={celularDraft}
                    onChangeText={setCelularDraft}
                    keyboardType="phone-pad"
                    maxLength={10}
                    placeholder="0991234567"
                    placeholderTextColor={colors.textMuted}
                    autoFocus
                  />
                </View>
                <View style={styles.editActions}>
                  <TouchableOpacity onPress={guardarCelular} disabled={guardando}>
                    <Ionicons name="checkmark" size={22} color={colors.success} />
                  </TouchableOpacity>
                  <TouchableOpacity onPress={cancelarEdicion}>
                    <Ionicons name="close" size={22} color={colors.error} />
                  </TouchableOpacity>
                </View>
              </View>
            ) : (
              <TouchableOpacity style={styles.row} onPress={iniciarEdicionCelular}>
                <View style={styles.rowLeft}>
                  <Ionicons name="call-outline" size={22} color={colors.neonOrange} />
                  <Text style={styles.rowLabel}>Celular</Text>
                </View>
                <View style={styles.rowRight}>
                  <Text style={styles.rowValue} numberOfLines={1}>
                    {perfil?.telefono ?? '—'}
                  </Text>
                  <Ionicons name="pencil" size={14} color={colors.textMuted} />
                </View>
              </TouchableOpacity>
            )}

            {editandoCorreo ? (
              <View style={styles.editRow}>
                <View style={styles.rowLeft}>
                  <Ionicons name="mail-outline" size={22} color={colors.neonOrange} />
                  <TextInput
                    style={styles.editInput}
                    value={correoDraft}
                    onChangeText={setCorreoDraft}
                    keyboardType="email-address"
                    autoCapitalize="none"
                    autoCorrect={false}
                    maxLength={50}
                    placeholder="correo@ejemplo.com"
                    placeholderTextColor={colors.textMuted}
                    autoFocus
                  />
                </View>
                <View style={styles.editActions}>
                  <TouchableOpacity onPress={guardarCorreo} disabled={guardando}>
                    <Ionicons name="checkmark" size={22} color={colors.success} />
                  </TouchableOpacity>
                  <TouchableOpacity onPress={cancelarEdicion}>
                    <Ionicons name="close" size={22} color={colors.error} />
                  </TouchableOpacity>
                </View>
              </View>
            ) : (
              <TouchableOpacity style={styles.row} onPress={iniciarEdicionCorreo}>
                <View style={styles.rowLeft}>
                  <Ionicons name="mail-outline" size={22} color={colors.neonOrange} />
                  <Text style={styles.rowLabel}>Correo</Text>
                </View>
                <View style={styles.rowRight}>
                  <Text style={styles.rowValue} numberOfLines={1}>
                    {correo}
                  </Text>
                  <Ionicons name="pencil" size={14} color={colors.textMuted} />
                </View>
              </TouchableOpacity>
            )}
          </View>

          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Mi cuenta</Text>
            <ProfileRow
              icon="key-outline"
              label="Permisos"
              value={usuario?.permisos ? `${usuario.permisos.length} activos` : undefined}
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
    </KeyboardAvoidingView>
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
    <TouchableOpacity style={styles.row} onPress={onPress} disabled={!onPress}>
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
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  wrapper: {
    flex: 1,
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
    flex: 1,
  },
  rowRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  rowLabel: {
    color: colors.textPrimary,
    fontSize: 15,
    fontWeight: '500',
  },
  rowValue: {
    color: colors.textMuted,
    fontSize: 14,
    maxWidth: '55%',
    textAlign: 'right',
  },
  editRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: colors.surfaceBorder,
  },
  editInput: {
    flex: 1,
    color: colors.textPrimary,
    fontSize: 15,
    paddingVertical: 6,
    paddingHorizontal: 12,
    backgroundColor: colors.surfaceLight,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.neonOrange,
  },
  editActions: {
    flexDirection: 'row',
    gap: 16,
    marginLeft: 12,
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
