/**
 * Estación Burger — Recuperar contraseña (RF-45)
 *
 * Dos pasos:
 *   1. Solicitar: envía el usuario para generar un token de un solo uso.
 *   2. Restablecer: el usuario ingresa el código recibido + nueva contraseña.
 *
 * El backend responde igual exista o no el usuario (no filtra existencia).
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
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../theme/colors';
import { authService } from '../features/auth/authService';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';

type Paso = 'solicitar' | 'restablecer';

export default function RecuperarScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const [paso, setPaso] = useState<Paso>('solicitar');
  const [username, setUsername] = useState('');
  const [token, setToken] = useState('');
  const [password, setPassword] = useState('');
  const [confirmar, setConfirmar] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSolicitar = async () => {
    if (!username.trim()) {
      setError('Ingresa tu usuario');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await authService.solicitarRecuperacion(username.trim());
      setAviso(
        'Si el usuario existe, se generó un código de recuperación. Ingrésalo aquí abajo.'
      );
      setPaso('restablecer');
    } catch (err: any) {
      setError(err.message || 'No se pudo solicitar la recuperación');
    } finally {
      setLoading(false);
    }
  };

  const handleRestablecer = async () => {
    if (!token.trim()) {
      setError('Ingresa el código de recuperación');
      return;
    }
    if (password.length < 6) {
      setError('La contraseña debe tener al menos 6 caracteres');
      return;
    }
    if (password !== confirmar) {
      setError('Las contraseñas no coinciden');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await authService.restablecerPassword(token.trim(), password);
      setAviso(null);
      router.replace('/login');
    } catch (err: any) {
      setError(err.message || 'El código es inválido o ya expiró');
    } finally {
      setLoading(false);
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
            onPress={() => router.back()}
            style={styles.backButton}
          >
            <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
          </TouchableOpacity>
        </View>

        <View style={styles.intro}>
          <Text style={styles.title}>RECUPERAR</Text>
          <Text style={styles.subtitle}>
            {paso === 'solicitar'
              ? 'Te enviaremos un código para restablecer tu contraseña'
              : 'Ingresa el código y tu nueva contraseña'}
          </Text>
        </View>

        <View style={styles.form}>
          {paso === 'solicitar' ? (
            <>
              <Input
                label="USUARIO"
                placeholder="carlos.estacion"
                value={username}
                onChangeText={setUsername}
                icon="at-outline"
                autoCapitalize="none"
                autoCorrect={false}
                maxLength={50}
              />
              <Button
                title="ENVIAR CÓDIGO"
                onPress={handleSolicitar}
                loading={loading}
                size="lg"
                style={styles.submitButton}
              />
            </>
          ) : (
            <>
              {aviso && (
                <View style={styles.infoContainer}>
                  <Ionicons
                    name="information-circle"
                    size={16}
                    color={colors.neonOrange}
                  />
                  <Text style={styles.infoText}>{aviso}</Text>
                </View>
              )}

              <Input
                label="CÓDIGO DE RECUPERACIÓN"
                placeholder="Pega aquí el código recibido"
                value={token}
                onChangeText={setToken}
                icon="key-outline"
                autoCapitalize="none"
                autoCorrect={false}
              />

              <Input
                label="NUEVA CONTRASEÑA"
                placeholder="••••••••"
                value={password}
                onChangeText={setPassword}
                icon="lock-closed-outline"
                isPassword
                maxLength={72}
              />

              <Input
                label="CONFIRMAR CONTRASEÑA"
                placeholder="••••••••"
                value={confirmar}
                onChangeText={setConfirmar}
                icon="lock-closed-outline"
                isPassword
                maxLength={72}
              />

              <Button
                title="RESTABLECER"
                onPress={handleRestablecer}
                loading={loading}
                size="lg"
                style={styles.submitButton}
              />

              <TouchableOpacity
                style={styles.secondaryLink}
                onPress={() => {
                  setPaso('solicitar');
                  setError(null);
                  setAviso(null);
                }}
              >
                <Text style={styles.secondaryLinkText}>
                  Solicitar otro código
                </Text>
              </TouchableOpacity>
            </>
          )}

          {error && (
            <View style={styles.errorContainer}>
              <Ionicons name="alert-circle" size={16} color={colors.error} />
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}
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
    marginBottom: 32,
  },
  title: {
    color: colors.neonOrange,
    fontSize: 30,
    fontWeight: '900',
    letterSpacing: 1,
    marginBottom: 8,
    textShadowColor: colors.neonOrange,
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 10,
  },
  subtitle: {
    color: colors.textSecondary,
    fontSize: 14,
  },
  form: {
    width: '100%',
  },
  infoContainer: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
    marginBottom: 16,
    padding: 12,
    borderRadius: 12,
    backgroundColor: colors.neonOrangeAlpha,
  },
  infoText: {
    color: colors.textSecondary,
    fontSize: 13,
    flex: 1,
    lineHeight: 18,
  },
  errorContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 16,
    paddingHorizontal: 4,
  },
  errorText: {
    color: colors.error,
    fontSize: 13,
    flex: 1,
  },
  submitButton: {
    borderRadius: 14,
    marginTop: 8,
  },
  secondaryLink: {
    alignSelf: 'center',
    marginTop: 24,
  },
  secondaryLinkText: {
    color: colors.neonPink,
    fontSize: 14,
    fontWeight: '600',
  },
});
