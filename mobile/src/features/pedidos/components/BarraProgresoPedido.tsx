/**
 * Estación Burger — Barra de progreso del pedido (6 etapas)
 *
 * Refleja estados REALES del backend combinando el `estado` de
 * preparación del pedido (snapshot) con el `seguimiento` logístico y de
 * doble confirmación. Sin dependencias nuevas de UI (solo RN + iconos).
 *
 * Mapeo exacto:
 *   1 Pedido recibido           = estado CONFIRMADO
 *   2 Preparando                = EN_PREPARACION
 *   3 Listo / empacado          = LISTO
 *   4 En camino                 = faseLogistica == EN_CAMINO
 *   5 Llegó / pend. confirmación = ENTREGADO_POR_REPARTIDOR + PENDIENTE_CONFIRMACION_CLIENTE
 *   6 Entrega confirmada        = entregaVerificada == true
 *
 * Pedidos ANULADOS: no se sigue el progreso, se muestra cancelación.
 * Pedidos sin entrega (RETIRAR o seguimiento.tieneEntrega == false):
 * solo las etapas de preparación (1-3) + cierre al ENTREGADO.
 */

import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';
import type { SeguimientoEntrega } from '../types';

type EstadoEtapa = 'completado' | 'actual' | 'pendiente';

interface Etapa {
  clave: string;
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
}

const ETAPAS_ENTREGA: Etapa[] = [
  { clave: 'recibido', label: 'Pedido recibido', icon: 'receipt-outline' },
  { clave: 'preparando', label: 'Preparando', icon: 'restaurant-outline' },
  { clave: 'listo', label: 'Listo / empacado', icon: 'cube-outline' },
  { clave: 'en_camino', label: 'En camino', icon: 'bicycle-outline' },
  {
    clave: 'llego',
    label: 'Llegó / pendiente de confirmación',
    icon: 'location-outline',
  },
  {
    clave: 'confirmada',
    label: 'Entrega confirmada',
    icon: 'checkmark-done-outline',
  },
];

const ETAPAS_RETIRO: Etapa[] = [
  { clave: 'recibido', label: 'Pedido recibido', icon: 'receipt-outline' },
  { clave: 'preparando', label: 'Preparando', icon: 'restaurant-outline' },
  { clave: 'listo', label: 'Listo / empacado', icon: 'cube-outline' },
  { clave: 'entregado', label: 'Entregado', icon: 'bag-check-outline' },
];

interface BarraProgresoPedidoProps {
  /** Estado de PREPARACIÓN del PedidoCliente (CONFIRMADO/EN_PREPARACION/...). */
  estadoPreparacion: string;
  /** Seguimiento logístico; null si aún no se cargó o no hay entrega. */
  seguimiento: SeguimientoEntrega | null;
  /** metodoEntrega del pedido (DOMICILIO / RETIRAR / ...). */
  metodoEntrega: string;
  /** El pedido está anulado. */
  anulado: boolean;
}

/**
 * Índice (0-based) de la etapa ACTUAL dentro de la secuencia de entrega.
 * Todas las etapas <= índice se consideran completadas o actual.
 */
function indiceEntrega(
  estado: string,
  seg: SeguimientoEntrega | null
): number {
  if (seg?.entregaVerificada) return 5; // 6 Entrega confirmada
  if (
    seg?.faseLogistica === 'ENTREGADO_POR_REPARTIDOR' &&
    seg?.faseConfirmacion === 'PENDIENTE_CONFIRMACION_CLIENTE'
  ) {
    return 4; // 5 Llegó / pendiente de confirmación
  }
  if (seg?.faseLogistica === 'EN_CAMINO') return 3; // 4 En camino
  if (estado === 'LISTO') return 2; // 3 Listo
  if (estado === 'EN_PREPARACION') return 1; // 2 Preparando
  // CONFIRMADO (o estados previos tratados como recibido)
  return 0; // 1 Pedido recibido
}

/** Índice de la etapa actual en la secuencia de retiro (sin reparto). */
function indiceRetiro(estado: string): number {
  if (estado === 'ENTREGADO') return 3;
  if (estado === 'LISTO') return 2;
  if (estado === 'EN_PREPARACION') return 1;
  return 0;
}

export function BarraProgresoPedido({
  estadoPreparacion,
  seguimiento,
  metodoEntrega,
  anulado,
}: BarraProgresoPedidoProps) {
  if (anulado) {
    return (
      <View style={styles.canceladoCard} accessibilityRole="summary">
        <Ionicons name="close-circle" size={22} color={colors.error} />
        <View style={styles.canceladoTexto}>
          <Text style={styles.canceladoTitulo}>Pedido anulado</Text>
          <Text style={styles.canceladoSub}>
            Este pedido fue cancelado y no continuará su progreso.
          </Text>
        </View>
      </View>
    );
  }

  const esEntrega =
    metodoEntrega === 'DOMICILIO' && seguimiento?.tieneEntrega !== false;

  const etapas = esEntrega ? ETAPAS_ENTREGA : ETAPAS_RETIRO;
  const actual = esEntrega
    ? indiceEntrega(estadoPreparacion, seguimiento)
    : indiceRetiro(estadoPreparacion);

  return (
    <View style={styles.wrapper}>
      {etapas.map((etapa, index) => {
        const estado: EstadoEtapa =
          index < actual
            ? 'completado'
            : index === actual
              ? 'actual'
              : 'pendiente';
        const esUltima = index === etapas.length - 1;
        const activa = estado !== 'pendiente';
        return (
          <View
            key={etapa.clave}
            style={styles.fila}
            accessibilityRole="text"
            accessibilityLabel={`${etapa.label}: ${
              estado === 'completado'
                ? 'completado'
                : estado === 'actual'
                  ? 'en curso'
                  : 'pendiente'
            }`}
          >
            <View style={styles.columnaIcono}>
              <View
                style={[
                  styles.punto,
                  activa && styles.puntoActivo,
                  estado === 'actual' && styles.puntoActual,
                ]}
              >
                <Ionicons
                  name={
                    estado === 'completado' ? 'checkmark' : etapa.icon
                  }
                  size={14}
                  color={activa ? colors.background : colors.textMuted}
                />
              </View>
              {!esUltima && (
                <View
                  style={[styles.linea, activa && styles.lineaActiva]}
                />
              )}
            </View>
            <View style={styles.columnaTexto}>
              <Text
                style={[
                  styles.etiqueta,
                  activa && styles.etiquetaActiva,
                  estado === 'actual' && styles.etiquetaActual,
                ]}
              >
                {etapa.label}
              </Text>
            </View>
          </View>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    gap: 0,
  },
  fila: {
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  columnaIcono: {
    alignItems: 'center',
    width: 32,
  },
  punto: {
    width: 28,
    height: 28,
    borderRadius: 14,
    borderWidth: 2,
    borderColor: colors.surfaceBorder,
    backgroundColor: colors.surfaceLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  puntoActivo: {
    backgroundColor: colors.neonOrange,
    borderColor: colors.neonOrange,
  },
  puntoActual: {
    shadowColor: colors.neonOrange,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.9,
    shadowRadius: 8,
    elevation: 6,
  },
  linea: {
    width: 2,
    flex: 1,
    minHeight: 22,
    backgroundColor: colors.surfaceBorder,
    marginVertical: 2,
  },
  lineaActiva: {
    backgroundColor: colors.neonOrange,
  },
  columnaTexto: {
    flex: 1,
    paddingLeft: 12,
    paddingTop: 4,
    paddingBottom: 14,
  },
  etiqueta: {
    color: colors.textMuted,
    fontSize: 14,
    fontWeight: '600',
  },
  etiquetaActiva: {
    color: colors.textPrimary,
  },
  etiquetaActual: {
    color: colors.neonOrange,
    fontWeight: '800',
  },
  canceladoCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.surface,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.error,
    padding: 14,
  },
  canceladoTexto: {
    flex: 1,
  },
  canceladoTitulo: {
    color: colors.error,
    fontSize: 16,
    fontWeight: '800',
  },
  canceladoSub: {
    color: colors.textSecondary,
    fontSize: 13,
    marginTop: 2,
  },
});
