/**
 * Estación Burger — Layout raíz
 */

import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { AuthProvider } from '../features/auth/AuthContext';
import { CartProvider } from '../features/carrito/CartContext';
import { colors } from '../theme/colors';

export default function RootLayout() {
  return (
    <AuthProvider>
      <CartProvider>
        <StatusBar style="light" />
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
            name="product/[id]"
            options={{ headerShown: false }}
          />
        </Stack>
      </CartProvider>
    </AuthProvider>
  );
}
