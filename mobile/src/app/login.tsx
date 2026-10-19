/**
 * Estación Burger — Login
 * Estética neón basada en referencia
 */

import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Image,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  useWindowDimensions,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';
import { useAuth } from '@/features/auth';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { ApiError } from '@/shared/api/apiError';
import logo from '../../public/Estacion-Burger-Logo.png';

export default function LoginScreen() {
  const router = useRouter();
  const { width, height } = useWindowDimensions();
  // useWindowDimensions refleja el tamaño de la ventana sin necesidad de onLayout,
  // evitando el re-render que cerraba el teclado en Android al redimensionar el KAV.
  const compact = height < 740;
  const logoHeight = Math.min(compact ? 100 : 130, (width - 40) * 937 / 1678);
  const logoWidth = logoHeight * 1678 / 937;
  const { login } = useAuth();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    if (!username.trim() || !password.trim()) {
      setError('Ingresa usuario y contraseña');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await login(username.trim(), password);
      router.replace('/(tabs)');
    } catch (err) {
      if (
        err instanceof ApiError &&
        err.type === 'urn:problem:restaurante:correo-no-verificado'
      ) {
        // La cuenta existe pero falta verificar el correo: llevar al paso 2.
        router.push({
          pathname: '/verificar',
          params: { correo: username.trim().toLowerCase() },
        });
        return;
      }
      setError(err instanceof ApiError ? err.message : 'Error al iniciar sesión');
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.container} edges={['top', 'right', 'bottom', 'left']}>
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      onLayout={({ nativeEvent: { layout } }) => {
        setViewport(previous => previous.width === layout.width && previous.height === layout.height
          ? previous
          : { width: layout.width, height: layout.height });
      }}
    >
      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        contentInsetAdjustmentBehavior="never"
        automaticallyAdjustContentInsets={false}
        automaticallyAdjustKeyboardInsets={false}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
      >
        <View style={styles.content}>
        <View style={[styles.header, compact && styles.headerCompact]}>
          <View style={[styles.logoFrame, { width: logoWidth, height: logoHeight }]}>
            <Image
              source={logo}
              style={styles.logo}
              resizeMode="contain"
              accessibilityLabel="Estación Burger"
            />
          </View>
          <Text style={[styles.welcome, compact && styles.welcomeCompact]}>¡BIENVENIDO!</Text>
          <Text style={styles.tagline}>Sabor extremo con el flow del neón</Text>
        </View>

        <View style={styles.form}>
          <Input
            compact={compact}
            label="EMAIL O USUARIO"
            placeholder="carlos.estacion@burger.com"
            value={username}
            onChangeText={setUsername}
            icon="mail-outline"
            autoCapitalize="none"
            autoCorrect={false}
          />

          <Input
            compact={compact}
            label="CONTRASEÑA"
            placeholder="••••••••"
            value={password}
            onChangeText={setPassword}
            icon="lock-closed-outline"
            isPassword
          />

          {error && (
            <View style={styles.errorContainer}>
              <Ionicons name="alert-circle" size={16} color={colors.error} />
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}

          <TouchableOpacity
            style={[styles.forgotButton, compact && styles.forgotButtonCompact]}
            onPress={() => router.push('/recuperar')}
          >
            <Text style={styles.forgotText}>¿Olvidé mi contraseña?</Text>
          </TouchableOpacity>

          <Button
            title="INICIAR SESIÓN"
            onPress={handleLogin}
            loading={loading}
            size={compact ? 'md' : 'lg'}
            style={styles.loginButton}
          />

          <View style={[styles.divider, compact && styles.dividerCompact]}>
            <View style={styles.dividerLine} />
            <Text style={styles.dividerText}>o</Text>
            <View style={styles.dividerLine} />
          </View>

          <Button
            title="CREAR CUENTA"
            onPress={() => router.push('/registro')}
            variant="outline"
            size={compact ? 'md' : 'lg'}
            style={styles.createButton}
          />
        </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 12,
  },
  scroll: {
    flex: 1,
  },
  content: {
    width: '100%',
    maxWidth: 420,
    flexShrink: 0,
  },
  header: {
    alignItems: 'center',
    marginBottom: 24,
  },
  headerCompact: {
    marginBottom: 12,
  },
  logoFrame: {
    flexShrink: 0,
    marginBottom: 12,
    overflow: 'hidden',
  },
  logo: {
    position: 'absolute',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
  },
  welcome: {
    color: colors.neonOrange,
    fontSize: 32,
    fontWeight: '900',
    letterSpacing: 1,
    marginBottom: 6,
    textShadowColor: colors.neonOrange,
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 10,
  },
  tagline: {
    color: colors.textSecondary,
    fontSize: 14,
    letterSpacing: 0.3,
    textAlign: 'center',
  },
  welcomeCompact: {
    fontSize: 26,
    lineHeight: 32,
  },
  form: {
    width: '100%',
    maxWidth: 420,
    alignSelf: 'center',
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
  },
  forgotButton: {
    alignSelf: 'center',
    minHeight: 44,
    justifyContent: 'center',
    marginBottom: 12,
  },
  forgotButtonCompact: {
    marginBottom: 4,
  },
  forgotText: {
    color: colors.neonPink,
    fontSize: 13,
    fontWeight: '600',
  },
  loginButton: {
    borderRadius: 14,
  },
  divider: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 24,
  },
  dividerCompact: {
    marginVertical: 12,
  },
  dividerLine: {
    flex: 1,
    height: 1,
    backgroundColor: colors.surfaceBorder,
  },
  dividerText: {
    color: colors.textMuted,
    fontSize: 13,
    marginHorizontal: 16,
  },
  createButton: {
    borderRadius: 14,
    borderColor: colors.neonPink,
  },
});
