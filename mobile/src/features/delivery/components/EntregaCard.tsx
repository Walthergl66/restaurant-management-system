import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';
import { estiloEstadoAsignacion } from '../estados';
import type { EntregaResumen } from '../types';
import EstadoBadge from './EstadoBadge';

/** Tarjeta de una entrega asignada: código, dirección, cliente, estado y acceso al detalle. */
export default function EntregaCard({
  entrega,
  onPress,
}: {
  entrega: EntregaResumen;
  onPress: () => void;
}) {
  const estado = estiloEstadoAsignacion(entrega.estadoAsignacion);
  return (
    <TouchableOpacity
      style={styles.card}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={`Pedido ${entrega.codigo}, ${entrega.clienteNombre}, ${estado.etiqueta}. Ver detalle`}
    >
      <View style={styles.headerRow}>
        <Text style={styles.codigo}>#{entrega.codigo}</Text>
        <EstadoBadge etiqueta={estado.etiqueta} color={estado.color} />
      </View>

      <View style={styles.row}>
        <Ionicons name="location-outline" size={16} color={colors.neonOrange} />
        <Text style={styles.direccion} numberOfLines={2}>
          {entrega.direccion}
        </Text>
      </View>

      <View style={styles.row}>
        <Ionicons name="person-outline" size={16} color={colors.textMuted} />
        <Text style={styles.cliente}>{entrega.clienteNombre}</Text>
      </View>

      <View style={styles.footer}>
        <Text style={styles.verDetalle}>Ver detalle</Text>
        <Ionicons name="chevron-forward" size={18} color={colors.neonPink} />
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    padding: 16,
    borderRadius: 16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    gap: 10,
  },
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  codigo: { color: colors.textPrimary, fontSize: 16, fontWeight: '900' },
  row: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  direccion: { color: colors.textSecondary, fontSize: 14, flex: 1 },
  cliente: { color: colors.textMuted, fontSize: 13 },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    gap: 2,
    marginTop: 2,
  },
  verDetalle: { color: colors.neonPink, fontSize: 12, fontWeight: '800' },
});
