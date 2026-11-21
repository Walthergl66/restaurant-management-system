import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';

/**
 * Pestañas del mesero: Mesas (tomar pedidos), Cocina (marcar comandas) y Listos (entregar pedidos). El armado del pedido y la
 * pantalla de producto son rutas del grupo que no aparecen en la barra y la ocultan mientras se usan.
 * `history` hace que "volver" regrese a la pantalla anterior (p. ej. del producto al pedido).
 */
export default function WaiterLayout() {
  return (
    <Tabs
      backBehavior="history"
      screenOptions={{
        headerShown: false,
        sceneStyle: { backgroundColor: colors.background },
        tabBarActiveTintColor: colors.neonOrange,
        tabBarInactiveTintColor: colors.textMuted,
        tabBarStyle: { backgroundColor: colors.background, borderTopColor: colors.surfaceBorder },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Mesas',
          tabBarIcon: ({ color, size }) => <Ionicons name="restaurant-outline" size={size} color={color as string} />,
        }}
      />
      <Tabs.Screen
        name="cocina"
        options={{
          title: 'Cocina',
          tabBarIcon: ({ color, size }) => <Ionicons name="flame-outline" size={size} color={color as string} />,
        }}
      />
      <Tabs.Screen
        name="listos"
        options={{
          title: 'Listos',
          tabBarIcon: ({ color, size }) => <Ionicons name="checkmark-done-outline" size={size} color={color as string} />,
        }}
      />
      <Tabs.Screen name="mesa/[id]" options={{ href: null, tabBarStyle: { display: 'none' } }} />
      <Tabs.Screen name="product/[id]" options={{ href: null, tabBarStyle: { display: 'none' } }} />
    </Tabs>
  );
}
