'use client'
import api from '@/shared/api/client'
import type { Mesa, MesaPayload } from './types'

export async function listarMesas(): Promise<Mesa[]> {
  const { data } = await api.get('/mesas', { params: { soloActivas: false } })
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

export async function cambiarHabilitacionMesa(id: number, habilitada: boolean): Promise<Mesa | void> {
  const { data } = await api.patch(`/mesas/${id}/habilitacion`, { habilitada })
  return data
}

export async function eliminarMesa(id: number): Promise<void> {
  await api.delete(`/mesas/${id}`)
}
