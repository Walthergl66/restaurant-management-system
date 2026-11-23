import type { LineaPedidoMesero } from './types';

/** Operaciones HTTP que intervienen en el envío de un pedido de mesa (inyectadas para poder probar la secuencia). */
export interface ApiPedidosMesa {
  crear(codigo: string, mesaId: number, notas?: string): Promise<void>;
  agregarLinea(codigo: string, linea: LineaPedidoMesero): Promise<void>;
  confirmar(codigo: string): Promise<void>;
  cancelarBorrador(codigo: string): Promise<void>;
}

/**
 * Crea el pedido, agrega sus líneas y lo confirma. Son varias llamadas: si una falla
 * después de crear el borrador, se cancela para no dejarlo huérfano (un borrador
 * abierto impide cerrar y cobrar la cuenta de la mesa). El error que se relanza es
 * siempre el original, aunque la cancelación también falle.
 */
export async function enviarPedidoMesa(
  api: ApiPedidosMesa,
  mesaId: number,
  codigo: string,
  lineas: LineaPedidoMesero[],
  notas?: string,
): Promise<void> {
  await api.crear(codigo, mesaId, notas);
  try {
    for (const linea of lineas) {
      await api.agregarLinea(codigo, linea);
    }
    await api.confirmar(codigo);
  } catch (error) {
    await api.cancelarBorrador(codigo).catch(() => undefined);
    throw error;
  }
}
