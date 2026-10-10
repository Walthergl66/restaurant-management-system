/**
 * Estación Burger — Registro de cliente (RF-45)
 *
 * Captura nombre, cédula, celular, correo y contraseña segura (con medidor de
 * rango). Al crear la cuenta NO inicia sesión: navega a la verificación del
 * correo, que es donde se emiten los tokens. Estética neón del login.
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
import { PasswordStrength } from '../features/auth/PasswordStrength';
import {
  esCedulaValida,
  esCelularValido,
  esCorreoValido,
  evaluarPassword,
} from '../features/auth/validation';
import { ApiError, firstFieldErrorMessage } from '../core/api/apiError';

export default function RegistroScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { registro } = useAuth();

  const [nombre, setNombre] = useState('');
  const [correo, setCorreo] = useState('');
  const [cedula, setCedula] = useState('');
  const [celular, setCelular] = useState('');
  const [password, setPassword] = useState('');
  const [confirmar, setConfirmar] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleRegistro = async () => {
    if (
      !nombre.trim() ||
      !correo.trim() ||
      !cedula.trim() ||
      !celular.trim() ||
      !password
    ) {
      setError('Completa todos los campos');
      return;
    }
    if (!esCorreoValido(correo)) {
      setError('Ingresa un correo válido');
      return;
    }
    if (!esCedulaValida(cedula)) {
      setError('La cédula ecuatoriana no es válida');
      return;
    }
    if (!esCelularValido(celular)) {
      setError('El celular debe tener 10 dígitos y empezar con 09');
      return;
    }
    if (!evaluarPassword(password).cumple) {
      setError('La contraseña no cumple los requisitos de seguridad');
      return;
    }
    if (password !== confirmar) {
      setError('Las contraseñas no coinciden');
      return;
    }

    setError(null);
    setLoading(true);
    try {
      await registro({
        username: correo.trim().toLowerCase(),
        nombre: nombre.trim(),
        cedula: cedula.trim(),
        celular: celular.trim(),
        password,
      });
      router.replace({
        pathname: '/verificar',
        params: { correo: correo.trim().toLowerCase() },
      });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(firstFieldErrorMessage(err.fieldErrors) ?? err.message);
      } else {
        setError('Error al crear la cuenta');
      }
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
            label="CORREO"
            placeholder="carlos@correo.com"
            value={correo}
            onChangeText={setCorreo}
            icon="mail-outline"
            keyboardType="email-address"
            autoCapitalize="none"
            autoCorrect={false}
            maxLength={50}
          />

          <Input
            label="CÉDULA"
            placeholder="1710034065"
            value={cedula}
            onChangeText={setCedula}
            icon="card-outline"
            keyboardType="number-pad"
            maxLength={10}
          />

          <Input
            label="CELULAR"
            placeholder="0991234567"
            value={celular}
            onChangeText={setCelular}
            icon="call-outline"
            keyboardType="phone-pad"
            maxLength={10}
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

          <PasswordStrength password={password} />

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
