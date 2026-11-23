import type { AccionComanda, ComandaCocina, ComandasPorEstado } from './types';

/**
 * Agrupa las comandas por estado. Dentro de cada grupo van las más antiguas primero (número de
 * comanda), y las cancelaciones pendientes se ponen delante porque cambian lo que cocina ya preparaba.
 */
export function agruparPorEstado(comandas: ComandaCocina[]): ComandasPorEstado {
  const grupos: ComandasPorEstado = { PENDIENTE: [], EN_PREPARACION: [], LISTO: [] };
  for (const comanda of comandas) {
    grupos[comanda.estado].push(comanda);
  }
  const antiguasPrimero = (a: ComandaCocina, b: ComandaCocina) => a.numeroComanda - b.numeroComanda;
  const cancelacionesPrimero = (a: ComandaCocina, b: ComandaCocina) =>
    Number(b.tipo === 'CANCELACION') - Number(a.tipo === 'CANCELACION') || antiguasPrimero(a, b);

  grupos.PENDIENTE.sort(cancelacionesPrimero);
  grupos.EN_PREPARACION.sort(antiguasPrimero);
  grupos.LISTO.sort(antiguasPrimero);
  return grupos;
}

/** De dónde viene la comanda: la mesa del salón o la forma de entrega del pedido de la app. */
export function etiquetaOrigen(comanda: ComandaCocina): string {
  if (comanda.mesaNumero != null) return `Mesa ${comanda.mesaNumero}`;
  if (comanda.canal === 'APP') {
    if (comanda.entrega === 'RETIRAR') return 'Retiro';
    if (comanda.entrega === 'DOMICILIO') return 'Domicilio';
    return 'Pedido app';
  }
  return 'Salón';
}

/**
 * Acciones disponibles. Una orden pendiente puede prepararse o terminarse de una vez; una
 * cancelación no se prepara, solo se da por enterada.
 */
export function accionesDe(comanda: ComandaCocina): AccionComanda[] {
  if (comanda.tipo === 'CANCELACION') {
    return comanda.estado === 'LISTO' ? [] : ['ENTERADO'];
  }
  if (comanda.estado === 'PENDIENTE') return ['PREPARAR', 'LISTO'];
  if (comanda.estado === 'EN_PREPARACION') return ['LISTO'];
  return [];
}
