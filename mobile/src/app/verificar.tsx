/**
 * Estación Burger — Verificación de correo (RF-45)
 *
 * Segundo paso del registro: confirma el código de 6 dígitos enviado al
 * correo. Al validarlo el backend emite los tokens y la sesión inicia.
 */

import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../theme/colors';
import { useAuth } from '../features/auth/AuthContext';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { ApiError } from '../core/api/apiError';

export default function VerificarScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { verificarEmail, reenviarVerificacion } = useAuth();
  const { correo } = useLocalSearchParams<{ correo?: string }>();

  const [codigo, setCodigo] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [reenviando, setReenviando] = useState(false);

  const handleVerificar = async () => {
    if (!correo) {
      setError('No se pudo identificar el correo; vuelve a registrarte');
      return;
    }
    if (!/^\d{6}$/.test(codigo)) {
      setError('El código debe tener 6 dígitos');
      return;
    }
    setError(null);
    setAviso(null);
    setLoading(true);
    try {
      await verificarEmail(correo, codigo);
      router.replace('/(tabs)');
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Error al verificar el correo');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleReenviar = async () => {
    if (!correo) return;
    setError(null);
    setAviso(null);
    setReenviando(true);
    try {
      await reenviarVerificacion(correo);
      setAviso('Te enviamos un nuevo código a tu correo');
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('No se pudo reenviar el código');
      }
    } finally {
      setReenviando(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView
        contentContainerStyle={[
          styles.scrollContent,
          { paddingTop: insets.top + 24 },
        ]}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.header}>
          <TouchableOpacity
            onPress={() => router.replace('/login')}
            style={styles.backButton}
          >
            <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
          </TouchableOpacity>
        </View>

        <View style={styles.intro}>
          <View style={styles.iconCircle}>
            <Ionicons name="mail-unread-outline" size={40} color={colors.neonOrange} />
          </View>
          <Text style={styles.title}>VERIFICA TU CORREO</Text>
          <Text style={styles.subtitle}>
            Enviamos un código de 6 dígitos a{'\n'}
            <Text style={styles.correo}>{correo ?? 'tu correo'}</Text>
          </Text>
        </View>

        <View style={styles.form}>
          <Input
            label="CÓDIGO"
            placeholder="000000"
            value={codigo}
            onChangeText={(v) => setCodigo(v.replace(/\D/g, ''))}
            icon="keypad-outline"
            keyboardType="number-pad"
            maxLength={6}
            style={styles.codigoInput}
            textAlign="center"
          />

          {error && (
            <View style={styles.errorContainer}>
              <Ionicons name="alert-circle" size={16} color={colors.error} />
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}

          {aviso && (
            <View style={styles.avisoContainer}>
              <Ionicons name="checkmark-circle" size={16} color={colors.success} />
              <Text style={styles.avisoText}>{aviso}</Text>
            </View>
          )}

          <Button
            title="VERIFICAR"
            onPress={handleVerificar}
            loading={loading}
            size="lg"
            style={styles.submitButton}
          />

          <TouchableOpacity
            style={styles.resendButton}
            onPress={handleReenviar}
            disabled={reenviando}
          >
            <Text style={styles.resendText}>
              {reenviando ? 'Enviando…' : '¿No recibiste el código? Reenviar'}
            </Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: 24,
    paddingBottom: 40,
  },
  header: {
    flexDirection: 'row',
    marginBottom: 16,
  },
  backButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
  },
  intro: {
    alignItems: 'center',
    marginBottom: 32,
  },
  iconCircle: {
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: colors.neonOrangeAlpha,
    borderWidth: 1,
    borderColor: colors.neonOrange,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 20,
  },
  title: {
    color: colors.neonOrange,
    fontSize: 26,
    fontWeight: '900',
    letterSpacing: 1,
    marginBottom: 12,
    textAlign: 'center',
    textShadowColor: colors.neonOrange,
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 10,
  },
  subtitle: {
    color: colors.textSecondary,
    fontSize: 14,
    textAlign: 'center',
    lineHeight: 20,
  },
  correo: {
    color: colors.textPrimary,
    fontWeight: '700',
  },
  form: {
    width: '100%',
  },
  codigoInput: {
    letterSpacing: 12,
    fontSize: 24,
    fontWeight: '700',
  },
  errorContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 16,
    paddingHorizontal: 4,
  },
  errorText: {
    color: colors.error,
    fontSize: 13,
    flex: 1,
  },
  avisoContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 16,
    paddingHorizontal: 4,
  },
  avisoText: {
    color: colors.success,
    fontSize: 13,
    flex: 1,
  },
  submitButton: {
    borderRadius: 14,
    marginTop: 8,
  },
  resendButton: {
    alignSelf: 'center',
    marginTop: 24,
  },
  resendText: {
    color: colors.neonPink,
    fontSize: 14,
    fontWeight: '600',
  },
});
