/**
 * Estación Burger — Layout de pestañas
 */

import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';
import { useCart } from '@/features/carrito';
import { View, Text, StyleSheet } from 'react-native';

function TabBarBadge({ count }: { count: number }) {
  if (count === 0) return null;
  return (
    <View style={styles.badge}>
      <Text style={styles.badgeText}>{count > 99 ? '99+' : count}</Text>
    </View>
  );
}

export default function TabsLayout() {
  const { totalItems } = useCart();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopColor: colors.surfaceBorder,
          borderTopWidth: 1,
          height: 64,
          paddingBottom: 8,
          paddingTop: 8,
        },
        tabBarActiveTintColor: colors.neonOrange,
        tabBarInactiveTintColor: colors.textMuted,
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: '600',
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Menú',
          tabBarIcon: ({ focused, size }) => (
            <Ionicons name="restaurant" size={size} color={focused ? colors.neonOrange : colors.textMuted} />
          ),
        }}
      />
      <Tabs.Screen
        name="cart"
        options={{
          title: 'Carrito',
          tabBarIcon: ({ focused, size }) => (
            <View>
              <Ionicons name="bag" size={size} color={focused ? colors.neonOrange : colors.textMuted} />
              <TabBarBadge count={totalItems} />
            </View>
          ),
        }}
      />
      <Tabs.Screen
        name="orders"
        options={{
          title: 'Pedidos',
          tabBarIcon: ({ focused, size }) => (
            <Ionicons name="receipt" size={size} color={focused ? colors.neonOrange : colors.textMuted} />
          ),
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Perfil',
          tabBarIcon: ({ focused, size }) => (
            <Ionicons name="person" size={size} color={focused ? colors.neonOrange : colors.textMuted} />
          ),
        }}
      />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  badge: {
    position: 'absolute',
    top: -4,
    right: -10,
    backgroundColor: colors.neonPink,
    borderRadius: 10,
    minWidth: 18,
    height: 18,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 4,
  },
  badgeText: {
    color: colors.background,
    fontSize: 10,
    fontWeight: '800',
  },
});
