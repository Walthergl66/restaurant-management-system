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
import { AuthProvider, useAuth } from '@/features/auth';
import { puedeAtenderMesas, puedeRepartir } from '@/features/auth';
import { CartProvider } from '@/features/carrito';
import { colors } from '@/theme/colors';

/** Rutas públicas de autenticación (no requieren sesión). */
const RUTAS_AUTH = ['login', 'registro', 'recuperar', 'verificar'];

function RootNavigator() {
  const { loading, isAuthenticated, usuario } = useAuth();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;

    const enAuth = RUTAS_AUTH.includes(segments[0] as string);
    const enCliente = segments[0] === '(tabs)';
    const enMesero = segments[0] === '(waiter)';
    const enDelivery = segments[0] === '(delivery)';

    // Prioridad de roles clara: un repartidor NO es mesero ni cliente. El gate
    // se decide por permiso; el backend valida todo igualmente.
    const esRepartidor = puedeRepartir(usuario);
    const esMesero = !esRepartidor && puedeAtenderMesas(usuario);

    // Grupo de inicio que corresponde al usuario autenticado.
    const grupoInicio = esRepartidor ? '/(delivery)' : esMesero ? '/(waiter)' : '/(tabs)';

    if (!isAuthenticated && !enAuth) {
      router.replace('/login');
    } else if (isAuthenticated && enAuth) {
      router.replace(grupoInicio);
    } else if (isAuthenticated && esRepartidor && (enCliente || enMesero)) {
      // Un repartidor nunca permanece en los grupos de cliente/mesero.
      router.replace('/(delivery)');
    } else if (isAuthenticated && esMesero && (enCliente || enDelivery)) {
      router.replace('/(waiter)');
    } else if (isAuthenticated && !esRepartidor && !esMesero && (enMesero || enDelivery)) {
      // Cliente (sin permisos de mesero ni reparto) fuera de (waiter)/(delivery).
      router.replace('/(tabs)');
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
      <Stack.Screen name="(delivery)" options={{ headerShown: false }} />
      {/* Las pantallas de auth NO van como presentation:'modal': en Android el
          contenedor modal nativo se reanima al abrir/cambiar el teclado y eso
          le roba el foco al TextInput, haciendo rebotar el foco entre campos.
          Como pantallas normales del Stack el teclado se comporta de forma
          estable. El diseño (página completa) no cambia. */}
      <Stack.Screen name="login" options={{ headerShown: false }} />
      <Stack.Screen name="registro" options={{ headerShown: false }} />
      <Stack.Screen name="verificar" options={{ headerShown: false }} />
      <Stack.Screen name="recuperar" options={{ headerShown: false }} />
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
