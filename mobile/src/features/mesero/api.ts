import { apiClient } from '@/shared/api/apiClient';
import { enviarPedidoMesa } from './enviarPedido';
import type { ApiPedidosMesa } from './enviarPedido';
import type { MesaMesero, LineaPedidoMesero } from './types';

/** Contrato HTTP del pedido de mesa (PedidoController); la confirmación usa el código como Idempotency-Key. */
const apiPedidosMesa: ApiPedidosMesa = {
  async crear(codigo, mesaId, notas) {
    await apiClient.post('/pedidos', { codigo, mesaId, notas: notas || null });
  },
  async agregarLinea(codigo, linea) {
    await apiClient.post(`/pedidos/${codigo}/items`, linea);
  },
  async confirmar(codigo) {
    await apiClient.request(`/pedidos/${codigo}/confirmar`, {
      method: 'POST',
      headers: { 'Idempotency-Key': codigo },
    });
  },
  async cancelarBorrador(codigo) {
    await apiClient.delete(`/pedidos/${codigo}`);
  },
};

export const meseroService = {
  /** Todas las mesas, incluidas las deshabilitadas (p. ej. para mostrar el número de una mesa con pedido en curso). */
  async listarMesas(): Promise<MesaMesero[]> {
    return apiClient.get<MesaMesero[]>('/mesas');
  },

  async listarMesasHabilitadas(): Promise<MesaMesero[]> {
    const mesas = await this.listarMesas();
    return mesas.filter(mesa => mesa.activo);
  },

  /** Envía el pedido a cocina; si falla a mitad no deja un borrador abierto en la cuenta. */
  async enviarPedido(mesaId: number, codigo: string, lineas: LineaPedidoMesero[], notas?: string): Promise<void> {
    await enviarPedidoMesa(apiPedidosMesa, mesaId, codigo, lineas, notas);
  },
};
