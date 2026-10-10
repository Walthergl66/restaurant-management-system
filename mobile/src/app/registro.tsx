/**
 * Estación Burger — Registro de cliente (RF-45)
 *
 * Crea la cuenta pública (rol CLIENTE) y deja la sesión iniciada.
 * Estética consistente con el login neón.
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
import { useAuth } from '../features/auth/AuthContext';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';

export default function RegistroScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { registro } = useAuth();

  const [nombre, setNombre] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmar, setConfirmar] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleRegistro = async () => {
    if (!nombre.trim() || !username.trim() || !password) {
      setError('Completa todos los campos');
      return;
    }
    if (username.trim().length < 3) {
      setError('El usuario debe tener al menos 3 caracteres');
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
      await registro(username.trim(), nombre.trim(), password);
      router.replace('/(tabs)');
    } catch (err: any) {
      setError(err.message || 'Error al crear la cuenta');
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
          <Text style={styles.title}>CREAR CUENTA</Text>
          <Text style={styles.subtitle}>
            Regístrate para pedir y seguir tus pedidos
          </Text>
        </View>

        <View style={styles.form}>
          <Input
            label="NOMBRE"
            placeholder="Tu nombre"
            value={nombre}
            onChangeText={setNombre}
            icon="person-outline"
            autoCapitalize="words"
            maxLength={100}
          />

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

          <Input
            label="CONTRASEÑA"
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

          {error && (
            <View style={styles.errorContainer}>
              <Ionicons name="alert-circle" size={16} color={colors.error} />
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}

          <Button
            title="CREAR CUENTA"
            onPress={handleRegistro}
            loading={loading}
            size="lg"
            style={styles.submitButton}
          />

          <TouchableOpacity
            style={styles.loginLink}
            onPress={() => router.replace('/login')}
          >
            <Text style={styles.loginLinkText}>
              ¿Ya tienes cuenta? Inicia sesión
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
  submitButton: {
    borderRadius: 14,
    marginTop: 8,
  },
  loginLink: {
    alignSelf: 'center',
    marginTop: 24,
  },
  loginLinkText: {
    color: colors.neonPink,
    fontSize: 14,
    fontWeight: '600',
  },
});
