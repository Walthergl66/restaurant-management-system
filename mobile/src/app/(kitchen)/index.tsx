import { Text, TouchableOpacity, View } from 'react-native';
import { useAuth } from '@/features/auth';
import { colors } from '@/theme/colors';

/** Marcador provisional: la pantalla de cocina real (comandas y estados) la reemplaza en la Tarea 6. */
export default function CocinaPendiente() {
  const { logout } = useAuth();
  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background, padding: 24 }}>
      <Text style={{ color: colors.textPrimary, fontSize: 18, textAlign: 'center' }}>La pantalla de cocina estará disponible próximamente.</Text>
      <TouchableOpacity onPress={() => void logout()} style={{ marginTop: 24 }}>
        <Text style={{ color: colors.neonOrange, fontSize: 15 }}>Cerrar sesión</Text>
      </TouchableOpacity>
    </View>
  );
}
