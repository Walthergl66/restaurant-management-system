'use client'
import api from '@/shared/api/client'
import type { Mesa, MesaPayload, EstadoMesa } from './types'

export async function listarMesas(): Promise<Mesa[]> {
  const { data } = await api.get('/mesas')
  return Array.isArray(data) ? data : data.content ?? []
}

export async function crearMesa(body: MesaPayload): Promise<Mesa> {
  const { data } = await api.post('/mesas', body)
  return data
}

export async function actualizarMesa(id: number, body: MesaPayload): Promise<Mesa> {
  const { data } = await api.put(`/mesas/${id}`, body)
  return data
}

export async function cambiarEstadoMesa(id: number, estado: EstadoMesa): Promise<Mesa> {
  const { data } = await api.patch(`/mesas/${id}/estado`, { estado })
  return data
}

export async function eliminarMesa(id: number): Promise<void> {
  await api.delete(`/mesas/${id}`)
}
