/**
 * Estación Burger — Tarjeta de Pedido
 * Compatible con PedidoClienteSPI del backend
 */

import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '../theme/colors';
import { PedidoCliente } from '../types';

interface OrderCardProps {
  pedido: PedidoCliente;
  onPress: () => void;
}

const estadoConfig: Record<
  string,
  { label: string; color: string; icon: keyof typeof Ionicons.glyphMap }
> = {
  BORRADOR: { label: 'Borrador', color: colors.textMuted, icon: 'document-text' },
  CONFIRMADO: { label: 'Confirmado', color: colors.warning, icon: 'checkmark-done' },
  EN_PREPARACION: {
    label: 'En preparación',
    color: colors.neonOrange,
    icon: 'restaurant',
  },
  LISTO: { label: 'Listo', color: colors.success, icon: 'checkmark-circle' },
  ENTREGADO: { label: 'Entregado', color: colors.success, icon: 'bag-check' },
  ANULADO: { label: 'Anulado', color: colors.error, icon: 'close-circle' },
};

export function OrderCard({ pedido, onPress }: OrderCardProps) {
  const estado = estadoConfig[pedido.estado] || estadoConfig['BORRADOR'];

  return (
    <TouchableOpacity
      onPress={onPress}
      activeOpacity={0.8}
      style={styles.container}
    >
      <View style={styles.header}>
        <View>
          <Text style={styles.codigo}>#{pedido.codigo}</Text>
          <Text style={styles.fecha}>
            {pedido.creadoAt
              ? new Date(pedido.creadoAt).toLocaleDateString('es-ES', {
                  day: 'numeric',
                  month: 'short',
                  hour: '2-digit',
                  minute: '2-digit',
                })
              : ''}
          </Text>
        </View>
        <View style={[styles.estadoBadge, { borderColor: estado.color }]}>
          <Ionicons name={estado.icon} size={14} color={estado.color} />
          <Text style={[styles.estadoText, { color: estado.color }]}>
            {estado.label}
          </Text>
        </View>
      </View>

      <View style={styles.divider} />

      <View style={styles.footer}>
        <Text style={styles.lineas} numberOfLines={1}>
          {pedido.lineas
            .map(l => `${l.cantidad}x ${l.nombre}`)
            .join(', ')}
        </Text>
        <View style={styles.right}>
          {pedido.total !== undefined && (
            <Text style={styles.total}>${pedido.total.toFixed(2)}</Text>
          )}
          <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 4,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  codigo: {
    color: colors.textPrimary,
    fontSize: 17,
    fontWeight: '800',
  },
  fecha: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 2,
  },
  estadoBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 8,
    borderWidth: 1,
    backgroundColor: colors.surfaceLight,
  },
  estadoText: {
    fontSize: 12,
    fontWeight: '700',
  },
  divider: {
    height: 1,
    backgroundColor: colors.surfaceBorder,
    marginVertical: 12,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  lineas: {
    color: colors.textSecondary,
    fontSize: 13,
    flex: 1,
    marginRight: 12,
  },
  right: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  total: {
    color: colors.neonOrange,
    fontSize: 16,
    fontWeight: '800',
  },
});
