/**
 * Estación Burger — Layout raíz
 *
 * Envuelve la app en los proveedores (auth + carrito) y aplica el
 * gate de sesión: sin usuario autenticado se redirige a /login, y con
 * sesión iniciada las pantallas de auth se redirigen a las pestañas.
 */

import { useEffect, useRef } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { Stack, useRouter, useSegments } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { AuthProvider, useAuth, puedeAtenderMesas } from '@/features/auth';
import { CartProvider } from '@/features/carrito';
import { colors } from '@/theme/colors';

/** Rutas públicas de autenticación (no requieren sesión). */
const RUTAS_AUTH = ['login', 'registro', 'recuperar', 'verificar'];

/**
 * Calcula la ruta de redirección según el estado de sesión.
 * Devuelve null si no hay que redirigir.
 */
function computeRedirectTarget(
  segments: string[],
  isAuthenticated: boolean,
  esMesero: boolean,
): string | null {
  const enAuth = RUTAS_AUTH.includes(segments[0] as string);
  const enCliente = segments[0] === '(tabs)';
  const enMesero = segments[0] === '(waiter)';

  if (!isAuthenticated && !enAuth) return '/login';
  if (isAuthenticated && enAuth) return esMesero ? '/(waiter)' : '/(tabs)';
  if (isAuthenticated && esMesero && enCliente) return '/(waiter)';
  if (isAuthenticated && !esMesero && enMesero) return '/(tabs)';
  return null;
}

function RootNavigator() {
  const { loading, isAuthenticated, usuario } = useAuth();
  const segments = useSegments();
  const router = useRouter();
  const lastRedirect = useRef<string | null>(null);

  useEffect(() => {
    if (loading) return;

    const esMesero = puedeAtenderMesas(usuario);
    const target = computeRedirectTarget(segments, isAuthenticated, esMesero);
    if (target && target !== lastRedirect.current) {
      lastRedirect.current = target;
      router.replace(target);
    }
  }, [loading, isAuthenticated, usuario, segments, router]);

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
      <Stack.Screen name="(waiter)" options={{ headerShown: false }} />
      <Stack.Screen
        name="login"
        options={{ headerShown: false, presentation: 'modal' }}
      />
      <Stack.Screen
        name="registro"
        options={{ headerShown: false, presentation: 'modal' }}
      />
      <Stack.Screen
        name="verificar"
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
      <SessionContent />
    </AuthProvider>
  );
}

/** El carrito pertenece a una cuenta: se reinicia al salir o cambiar de usuario. */
function SessionContent() {
  const { usuario } = useAuth();
  return <CartProvider key={usuario?.id ?? 'sin-sesion'}>
    <StatusBar style="light" />
    <RootNavigator />
  </CartProvider>;
}

const styles = StyleSheet.create({
  splash: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
