'use client'
import api from '@/shared/api/client'
import type { AdicionPayload, CobroPayload, CuentaResponse } from './types'

export async function listarCuentas(): Promise<CuentaResponse[]> {
  const { data } = await api.get('/cuentas')
  return Array.isArray(data) ? data : (data.content ?? [])
}

export async function cerrarCuenta(id: number): Promise<void> {
  await api.patch(`/cuentas/${id}/cerrar`)
}

export async function crearAdicion(id: number, payload: AdicionPayload): Promise<{ pedidoCodigo: string }> {
  const { data } = await api.post(`/cuentas/${id}/adiciones`, payload)
  return data
}

export async function cobrarCuenta(id: number, payload: CobroPayload): Promise<void> {
  await api.post(`/cuentas/${id}/cobros`, payload)
}
