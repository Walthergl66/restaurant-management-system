import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';

/** Grupo de la cocina (rol COCINA): Comandas (preparar) y Mostrador (entregar pedidos de retiro). */
export default function KitchenLayout() {
  return (
    <Tabs
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
          title: 'Comandas',
          tabBarIcon: ({ color, size }) => <Ionicons name="flame-outline" size={size} color={color as string} />,
        }}
      />
      <Tabs.Screen
        name="mostrador"
        options={{
          title: 'Mostrador',
          tabBarIcon: ({ color, size }) => <Ionicons name="bag-handle-outline" size={size} color={color as string} />,
        }}
      />
    </Tabs>
  );
}
