/**
 * Estación Burger — API del módulo delivery (repartidor)
 *
 * Llamadas tipadas al backend (prefijo /api/v1, ya incluido en apiClient).
 * Reutiliza apiClient (inyecta JWT, refresca en 401, mapea ApiError). El móvil
 * nunca llama a ORS directamente ni conoce la api-key.
 */

import { apiClient } from '@/shared/api/apiClient';
import type {
  CriterioOptimizacion,
  MisEntregas,
  ParadaResponse,
  PropuestaMultientrega,
  RecorridoResumen,
  RutaCalculada,
  TipoRecorrido,
} from './types';

export const deliveryService = {
  /** Asignaciones activas + recorridos abiertos del repartidor autenticado. */
  misEntregas(): Promise<MisEntregas> {
    return apiClient.get<MisEntregas>('/delivery/mis-entregas');
  },

  /** Ruta individual (restaurante -> destino) para una asignación propia. */
  rutaIndividual(asignacionId: number): Promise<RutaCalculada> {
    return apiClient.post<RutaCalculada>('/delivery/rutas/individual', { asignacionId });
  },

  /** Propuesta de multientrega con el orden de paradas optimizado por ORS. */
  optimizar(
    asignacionIds: number[],
    criterio?: CriterioOptimizacion
  ): Promise<PropuestaMultientrega> {
    return apiClient.post<PropuestaMultientrega>('/delivery/rutas/optimizar', {
      asignacionIds,
      criterio: criterio ?? null,
    });
  },

  /** Crear un recorrido (INDIVIDUAL o MULTIPLE) con las paradas en orden. */
  crearRecorrido(
    tipo: TipoRecorrido,
    asignacionIds: number[],
    criterio?: CriterioOptimizacion
  ): Promise<RecorridoResumen> {
    return apiClient.post<RecorridoResumen>('/delivery/recorridos', {
      tipo,
      asignacionIds,
      criterio: criterio ?? null,
    });
  },

  /** Iniciar un recorrido planificado (lo pasa a EN_CURSO). */
  iniciarRecorrido(recorridoId: number): Promise<RecorridoResumen> {
    return apiClient.post<RecorridoResumen>(`/delivery/recorridos/${recorridoId}/iniciar`);
  },

  /** Recalcular la ruta de las paradas pendientes desde la ubicación actual. */
  recalcular(recorridoId: number, lat: number, lng: number): Promise<RutaCalculada> {
    return apiClient.post<RutaCalculada>(`/delivery/recorridos/${recorridoId}/recalcular`, {
      lat,
      lng,
    });
  },

  /** Finalizar un recorrido en curso (solo si todas las paradas están resueltas). */
  finalizarRecorrido(recorridoId: number): Promise<RecorridoResumen> {
    return apiClient.post<RecorridoResumen>(`/delivery/recorridos/${recorridoId}/finalizar`);
  },

  /** Marcar una parada entregada (idempotente por Idempotency-Key). */
  marcarEntregada(paradaId: number, idempotencyKey: string): Promise<ParadaResponse> {
    return apiClient.request<ParadaResponse>(`/delivery/paradas/${paradaId}/entregada`, {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
    });
  },

  /** Registrar una incidencia de entrega (marca la parada FALLIDA). */
  registrarIncidencia(paradaId: number, motivo: string): Promise<ParadaResponse> {
    return apiClient.post<ParadaResponse>(`/delivery/paradas/${paradaId}/incidencia`, { motivo });
  },

  /** Recorridos finalizados/cancelados del repartidor autenticado. */
  historial(): Promise<RecorridoResumen[]> {
    return apiClient.get<RecorridoResumen[]>('/delivery/recorridos/historial');
  },
};
