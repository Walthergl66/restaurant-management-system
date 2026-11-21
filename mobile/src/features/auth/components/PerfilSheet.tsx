import React from 'react';
import { Alert, Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { colors } from '@/theme/colors';
import { useAuth } from '../AuthContext';
import { etiquetaRol, iniciales } from '../perfil';

interface PerfilSheetProps {
  visible: boolean;
  onClose: () => void;
}

/**
 * Panel de perfil del personal (mesero, cocina, repartidor): muestra quién tiene la sesión abierta
 * y permite cerrarla. Se abre desde el ícono de perfil de la cabecera.
 */
export function PerfilSheet({ visible, onClose }: PerfilSheetProps) {
  const { usuario, logout } = useAuth();

  const confirmarSalida = () =>
    Alert.alert('Cerrar sesión', '¿Quieres cerrar tu sesión?', [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Cerrar sesión',
        style: 'destructive',
        onPress: () => {
          onClose();
          void logout();
        },
      },
    ]);

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <View style={styles.wrapper}>
        <Pressable style={styles.fondo} onPress={onClose} accessibilityLabel="Cerrar perfil" />
        <View style={styles.hoja}>
          <View style={styles.asa} />

          <View style={styles.cabecera}>
            <View style={styles.avatar}>
              <Text style={styles.iniciales}>{iniciales(usuario?.nombre)}</Text>
            </View>
            <Text style={styles.nombre}>{usuario?.nombre ?? 'Sin nombre'}</Text>
            {!!usuario?.rol && <Badge text={etiquetaRol(usuario.rol)} variant="pink" size="md" />}
          </View>

          <View style={styles.datos}>
            <Fila icono="mail-outline" etiqueta="Correo" valor={usuario?.username ?? '—'} />
            <Fila icono="shield-checkmark-outline" etiqueta="Rol" valor={etiquetaRol(usuario?.rol) || '—'} />
          </View>

          <Button
            title="Cerrar sesión"
            onPress={confirmarSalida}
            variant="outline"
            size="lg"
            icon={<Ionicons name="log-out-outline" size={20} color={colors.neonPink} />}
          />
        </View>
      </View>
    </Modal>
  );
}

interface FilaProps {
  icono: React.ComponentProps<typeof Ionicons>['name'];
  etiqueta: string;
  valor: string;
}

function Fila({ icono, etiqueta, valor }: FilaProps) {
  return (
    <View style={styles.fila}>
      <Ionicons name={icono} size={20} color={colors.textSecondary} />
      <View style={{ flex: 1 }}>
        <Text style={styles.filaEtiqueta}>{etiqueta}</Text>
        <Text style={styles.filaValor}>{valor}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { flex: 1, justifyContent: 'flex-end' },
  fondo: { ...StyleSheet.absoluteFill, backgroundColor: colors.overlay },
  hoja: {
    backgroundColor: colors.surface,
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    padding: 24,
    paddingBottom: 32,
    gap: 20,
  },
  asa: {
    alignSelf: 'center',
    width: 44,
    height: 4,
    borderRadius: 2,
    backgroundColor: colors.surfaceBorder,
  },
  cabecera: { alignItems: 'center', gap: 10 },
  avatar: {
    width: 76,
    height: 76,
    borderRadius: 38,
    backgroundColor: colors.neonOrange,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iniciales: { color: colors.background, fontSize: 28, fontWeight: '800' },
  nombre: { color: colors.textOnSurface, fontSize: 22, fontWeight: '800', textAlign: 'center' },
  datos: {
    backgroundColor: colors.surfaceLight,
    borderRadius: 16,
    paddingHorizontal: 16,
    paddingVertical: 4,
  },
  fila: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    paddingVertical: 12,
  },
  filaEtiqueta: { color: colors.textSecondary, fontSize: 12 },
  filaValor: { color: colors.textOnSurface, fontSize: 15, fontWeight: '600' },
});
