import { Stack } from 'expo-router';
import { colors } from '@/theme/colors';

export default function DeliveryLayout() {
  return (
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.background } }}>
      <Stack.Screen name="index" />
      <Stack.Screen name="entrega/[id]" />
    </Stack>
  );
}
