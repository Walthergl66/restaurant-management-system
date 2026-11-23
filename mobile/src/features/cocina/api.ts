import { apiClient } from '@/shared/api/apiClient';
import type { AccionComanda, AreaCocina, ComandaCocina } from './types';

const ENDPOINTS = {
  comandas: '/comandas',
  enPreparacion: (id: number) => `/comandas/${id}/en-preparacion`,
  listo: (id: number) => `/comandas/${id}/listo`,
  areas: '/areas',
} as const;

export const cocinaService = {
  /** Comandas que la cocina debe ver ahora: pendientes, en preparación y las listas recientes. */
  async listarActivas(areaId?: number | null): Promise<ComandaCocina[]> {
    const filtro = areaId != null ? `&areaId=${areaId}` : '';
    return apiClient.get<ComandaCocina[]>(`${ENDPOINTS.comandas}?activas=true${filtro}`);
  },

  async listarAreas(): Promise<AreaCocina[]> {
    const areas = await apiClient.get<AreaCocina[]>(ENDPOINTS.areas);
    return areas.filter(area => area.activo);
  },

  /**
   * Aplica una acción. "Listo" sobre una orden pendiente encadena preparación y listo (ambas
   * llamadas son idempotentes); una cancelación se da por enterada con "listo".
   */
  async ejecutar(comanda: ComandaCocina, accion: AccionComanda): Promise<void> {
    if (accion === 'PREPARAR') {
      await apiClient.post(ENDPOINTS.enPreparacion(comanda.id));
      return;
    }
    if (accion === 'LISTO' && comanda.estado === 'PENDIENTE') {
      await apiClient.post(ENDPOINTS.enPreparacion(comanda.id));
    }
    await apiClient.post(ENDPOINTS.listo(comanda.id));
  },
};
