/**
 * Estación Burger — Layout raíz
 *
 * Envuelve la app en los proveedores (auth + carrito) y aplica el
 * gate de sesión: sin usuario autenticado se redirige a /login, y con
 * sesión iniciada las pantallas de auth se redirigen a las pestañas.
 */

import { useEffect } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { Stack, useRouter, useSegments } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { AuthProvider, useAuth } from '../features/auth/AuthContext';
import { CartProvider } from '../features/carrito/CartContext';
import { colors } from '../theme/colors';

/** Rutas públicas de autenticación (no requieren sesión). */
const RUTAS_AUTH = ['login', 'registro', 'recuperar'];

function RootNavigator() {
  const { loading, isAuthenticated } = useAuth();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;

    const enAuth = RUTAS_AUTH.includes(segments[0] as string);
    if (!isAuthenticated && !enAuth) {
      router.replace('/login');
    } else if (isAuthenticated && enAuth) {
      router.replace('/(tabs)');
    }
  }, [loading, isAuthenticated, segments, router]);

  if (loading) {
    return (
      <View style={styles.splash}>
        <ActivityIndicator size="large" color={colors.neonOrange} />
      </View>
    );
  }

  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: colors.background },
        animation: 'slide_from_right',
      }}
    >
      <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
      <Stack.Screen
        name="login"
        options={{ headerShown: false, presentation: 'modal' }}
      />
      <Stack.Screen
        name="registro"
        options={{ headerShown: false, presentation: 'modal' }}
      />
      <Stack.Screen
        name="recuperar"
        options={{ headerShown: false, presentation: 'modal' }}
      />
      <Stack.Screen name="product/[id]" options={{ headerShown: false }} />
      <Stack.Screen name="pedido/[codigo]" options={{ headerShown: false }} />
      <Stack.Screen name="direcciones" options={{ headerShown: false }} />
      <Stack.Screen name="metodos-pago" options={{ headerShown: false }} />
      <Stack.Screen name="ayuda" options={{ headerShown: false }} />
      <Stack.Screen name="terminos" options={{ headerShown: false }} />
      <Stack.Screen name="privacidad" options={{ headerShown: false }} />
    </Stack>
  );
}

export default function RootLayout() {
  return (
    <AuthProvider>
      <CartProvider>
        <StatusBar style="light" />
        <RootNavigator />
      </CartProvider>
    </AuthProvider>
  );
}

const styles = StyleSheet.create({
  splash: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
