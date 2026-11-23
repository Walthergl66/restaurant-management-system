import type { PedidoMostrador, PedidoRetiroApi, PedidoSalonApi } from './types';

const soloLineas = (lineas: { nombreProducto: string; cantidad: number }[]) =>
  lineas.map(linea => ({ nombreProducto: linea.nombreProducto, cantidad: linea.cantidad }));

/** Pedido de salón listo; `numerosDeMesa` traduce el id de mesa a su número (si no está, no se inventa). */
export function desdeSalon(pedido: PedidoSalonApi, numerosDeMesa: Map<number, number>): PedidoMostrador {
  return {
    codigo: pedido.codigo,
    origen: 'SALON',
    mesaNumero: pedido.mesaId != null ? numerosDeMesa.get(pedido.mesaId) : undefined,
    creadoAt: pedido.createdAt,
    lineas: soloLineas(pedido.lineas),
  };
}

/** Pedido de la app para retirar, listo en el mostrador. */
export function desdeRetiro(pedido: PedidoRetiroApi): PedidoMostrador {
  return {
    codigo: pedido.codigo,
    origen: 'RETIRO',
    clienteNombre: pedido.clienteNombre,
    lineas: soloLineas(pedido.lineas),
  };
}

/**
 * Une las dos listas: primero los de salón, del más antiguo al más reciente, y después los de
 * retiro en el orden en que los entrega el backend (más antiguos primero). No modifica las listas recibidas.
 */
export function unirListos(salon: PedidoMostrador[], retiro: PedidoMostrador[]): PedidoMostrador[] {
  const hora = (pedido: PedidoMostrador) => (pedido.creadoAt ? Date.parse(pedido.creadoAt) : Number.MAX_SAFE_INTEGER);
  const salonPorAntiguedad = [...salon].sort((a, b) => hora(a) - hora(b));
  return [...salonPorAntiguedad, ...retiro];
}

/** Texto de origen para la tarjeta: "Mesa 5", "Salón" (sin número) o "Retiro · nombre". */
export function etiquetaEntrega(pedido: PedidoMostrador): string {
  if (pedido.origen === 'SALON') {
    return pedido.mesaNumero != null ? `Mesa ${pedido.mesaNumero}` : 'Salón';
  }
  return pedido.clienteNombre ? `Retiro · ${pedido.clienteNombre}` : 'Retiro';
}
