/**
 * Estación Burger — Estados logísticos del delivery (presentación)
 *
 * Mapea los estados logísticos (separados de la preparación del pedido) a una
 * etiqueta legible y un color de acento para el badge. Si llega un estado
 * desconocido, se muestra tal cual con color neutro (no rompe la UI).
 */

import { colors } from '@/theme/colors';

interface EstiloEstado {
  etiqueta: string;
  color: string;
}

const ESTADOS_ASIGNACION: Record<string, EstiloEstado> = {
  ASIGNADA: { etiqueta: 'Asignada', color: colors.neonOrange },
  EN_RECORRIDO: { etiqueta: 'En recorrido', color: colors.neonPink },
  ENTREGADA: { etiqueta: 'Entregada', color: colors.success },
  FALLIDA: { etiqueta: 'Fallida', color: colors.error },
  CANCELADA: { etiqueta: 'Cancelada', color: colors.textMuted },
};

const ESTADOS_PARADA: Record<string, EstiloEstado> = {
  PENDIENTE: { etiqueta: 'Pendiente', color: colors.neonOrange },
  ENTREGADA: { etiqueta: 'Entregada', color: colors.success },
  FALLIDA: { etiqueta: 'Fallida', color: colors.error },
};

const ESTADOS_RECORRIDO: Record<string, EstiloEstado> = {
  PLANIFICADO: { etiqueta: 'Planificado', color: colors.neonOrange },
  EN_CURSO: { etiqueta: 'En curso', color: colors.neonPink },
  FINALIZADO: { etiqueta: 'Finalizado', color: colors.success },
  CANCELADO: { etiqueta: 'Cancelado', color: colors.textMuted },
};

// Fase de la doble confirmación (separada del estado logístico). Tras marcar la
// entrega, el repartidor ve que queda pendiente la confirmación del cliente,
// pero NO se bloquea: puede seguir con otras paradas y finalizar el recorrido.
const FASES_CONFIRMACION: Record<string, EstiloEstado> = {
  SIN_CONFIRMAR: { etiqueta: 'Sin confirmar', color: colors.neonOrange },
  PENDIENTE_CONFIRMACION_CLIENTE: {
    etiqueta: 'Pendiente de confirmación del cliente',
    color: colors.neonPink,
  },
  VERIFICADA: { etiqueta: 'Entrega verificada', color: colors.success },
  FALLIDA: { etiqueta: 'Fallida', color: colors.error },
};

function resolver(mapa: Record<string, EstiloEstado>, estado: string): EstiloEstado {
  return mapa[estado] ?? { etiqueta: estado, color: colors.textSecondary };
}

export const estiloEstadoAsignacion = (estado: string) => resolver(ESTADOS_ASIGNACION, estado);
export const estiloEstadoParada = (estado: string) => resolver(ESTADOS_PARADA, estado);
export const estiloEstadoRecorrido = (estado: string) => resolver(ESTADOS_RECORRIDO, estado);
export const estiloFaseConfirmacion = (fase: string) => resolver(FASES_CONFIRMACION, fase);
