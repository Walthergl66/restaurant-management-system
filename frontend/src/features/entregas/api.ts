'use client'
import api from '@/shared/api/client'
import type {
  AsignacionResponse,
  ConfigRestauranteResumen,
  ConfirmacionPendienteResponse,
  EstadoAsignacion,
  PedidoListoResponse,
  RepartidorResponse,
} from './types'

// ---- Repartidores (delivery:repartidores-gestionar) ----

export async function listarRepartidores(): Promise<RepartidorResponse[]> {
  const { data } = await api.get('/delivery/repartidores')
  return data
}

export async function listarRepartidoresHabilitados(): Promise<RepartidorResponse[]> {
  const { data } = await api.get('/delivery/repartidores/habilitados')
  return data
}

export async function habilitarRepartidor(usuarioId: number): Promise<RepartidorResponse> {
  const { data } = await api.post(`/delivery/repartidores/${usuarioId}/habilitar`)
  return data
}

export async function suspenderRepartidor(usuarioId: number): Promise<RepartidorResponse> {
  const { data } = await api.post(`/delivery/repartidores/${usuarioId}/suspender`)
  return data
}

// ---- Asignaciones (delivery:asignaciones-gestionar) ----

export async function listarPendientesDeAsignar(): Promise<PedidoListoResponse[]> {
  const { data } = await api.get('/delivery/asignaciones/pendientes-de-asignar')
  return data
}

export async function listarAsignaciones(
  filtro: { estado?: EstadoAsignacion; repartidorId?: number } = {},
): Promise<AsignacionResponse[]> {
  const params: Record<string, string | number> = {}
  if (filtro.estado) params.estado = filtro.estado
  if (filtro.repartidorId != null) params.repartidorId = filtro.repartidorId
  const { data } = await api.get('/delivery/asignaciones', { params })
  return data
}

export async function asignar(
  pedidoClienteId: number,
  repartidorId: number,
): Promise<AsignacionResponse> {
  const { data } = await api.post('/delivery/asignaciones', { pedidoClienteId, repartidorId })
  return data
}

export async function asignarLote(
  pedidoClienteIds: number[],
  repartidorId: number,
): Promise<AsignacionResponse[]> {
  const { data } = await api.post('/delivery/asignaciones/lote', { pedidoClienteIds, repartidorId })
  return data
}

export async function reasignar(
  pedidoClienteId: number,
  repartidorId: number,
): Promise<AsignacionResponse> {
  const { data } = await api.post(`/delivery/asignaciones/${pedidoClienteId}/reasignar`, {
    repartidorId,
  })
  return data
}

// ---- Ubicación del restaurante (delivery:config-gestionar) ----

export async function getConfigRestaurante(): Promise<ConfigRestauranteResumen> {
  const { data } = await api.get('/delivery/config-restaurante')
  return data
}

export async function actualizarDireccionRestaurante(
  direccion: string,
): Promise<ConfigRestauranteResumen> {
  const { data } = await api.put('/delivery/config-restaurante/direccion', { direccion })
  return data
}

export async function fijarCoordenadasRestaurante(
  lat: number,
  lng: number,
): Promise<ConfigRestauranteResumen> {
  const { data } = await api.put('/delivery/config-restaurante/coordenadas', { lat, lng })
  return data
}

// ---- Confirmaciones pendientes (delivery:confirmacion-resolver) ----

export async function listarConfirmacionesPendientes(): Promise<ConfirmacionPendienteResponse[]> {
  const { data } = await api.get('/delivery/confirmaciones/pendientes')
  return data
}

export async function resolverConfirmacion(
  paradaId: number,
  motivo: string,
): Promise<ConfirmacionPendienteResponse> {
  const { data } = await api.post(`/delivery/confirmaciones/${paradaId}/resolver`, { motivo })
  return data
}
