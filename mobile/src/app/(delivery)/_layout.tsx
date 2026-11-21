import { Stack } from 'expo-router';
import { puedeRepartir, useAuth } from '@/features/auth';
import { colors } from '@/theme/colors';

export default function DeliveryLayout() {
  const { usuario } = useAuth();
  // En la web, varios grupos comparten la ruta "/": no se monta la sección del repartidor
  // (ni se consulta su API) hasta confirmar que este usuario puede repartir.
  if (!puedeRepartir(usuario)) return null;

  return (
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.background } }}>
      <Stack.Screen name="index" />
      <Stack.Screen name="entrega/[id]" />
      <Stack.Screen name="multientrega" />
      <Stack.Screen name="recorrido/[id]" />
    </Stack>
  );
}
