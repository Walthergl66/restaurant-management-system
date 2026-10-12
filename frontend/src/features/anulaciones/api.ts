'use client'
import api from '@/shared/api/client'
import type { AnulacionResponse, EstadoAnulacion } from './types'

export async function listarAnulaciones(filtro: EstadoAnulacion | 'TODOS'): Promise<AnulacionResponse[]> {
  const params: Record<string, string> = {}
  if (filtro !== 'TODOS') params.estado = filtro
  const { data } = await api.get('/anulaciones', { params })
  return data
}

export async function aprobarAnulacion(id: number): Promise<void> {
  await api.patch(`/anulaciones/${id}/aprobar`)
}

export async function rechazarAnulacion(id: number): Promise<void> {
  await api.patch(`/anulaciones/${id}/rechazar`)
}
