'use client'
import api from '@/shared/api/client'
import type { CajaResponse, MovimientoResponse } from './types'

export async function obtenerCajaAbierta(): Promise<CajaResponse | null> {
  try {
    const { data } = await api.get('/cajas/abierta')
    return data
  } catch {
    return null
  }
}

export async function listarCajas(): Promise<CajaResponse[]> {
  const { data } = await api.get('/cajas')
  return Array.isArray(data) ? data.slice(0, 15) : []
}

export async function listarMovimientos(cajaId: number): Promise<MovimientoResponse[]> {
  const { data } = await api.get(`/cajas/${cajaId}/movimientos`)
  return data
}

export async function abrirCaja(montoInicial: number): Promise<void> {
  await api.post('/cajas/apertura', { montoInicial })
}

export async function cerrarCaja(id: number, montoReal: number): Promise<void> {
  await api.post(`/cajas/${id}/cierre`, { montoReal })
}

export async function registrarEgreso(
  cajaId: number,
  payload: { concepto: string; monto: number; metodo: string },
): Promise<void> {
  await api.post(`/cajas/${cajaId}/egresos`, payload)
}
