import { apiClient } from '../../core/api/apiClient';
import type { MesaMesero, LineaPedidoMesero } from './types';

export const meseroService = {
  async listarMesasHabilitadas(): Promise<MesaMesero[]> {
    const mesas = await apiClient.get<MesaMesero[]>('/mesas');
    return mesas.filter(mesa => mesa.activo);
  },

  async crearPedido(mesaId: number, codigo: string, lineas: LineaPedidoMesero[], notas?: string) {
    await apiClient.post('/pedidos', { codigo, mesaId, notas: notas || null });
    for (const linea of lineas) {
      await apiClient.post(`/pedidos/${codigo}/items`, linea);
    }
    return apiClient.request(`/pedidos/${codigo}/confirmar`, {
      method: 'POST',
      headers: { 'Idempotency-Key': codigo },
    });
  },
};
