'use client'
import api from '@/shared/api/client'
import type { ReporteVentas } from '@/features/reportes'
import type { PedidoResponse } from '@/features/pedidos'
import { obtenerCajaAbierta } from '@/features/caja'

export { obtenerCajaAbierta }

export async function obtenerReporteHoy(): Promise<ReporteVentas> {
  const desdeIso = new Date(`${new Date().toISOString().split('T')[0]}T00:00:00`).toISOString()
  const hastaIso = new Date().toISOString()
  const { data } = await api.get('/reportes/ventas', { params: { desde: desdeIso, hasta: hastaIso } })
  return data
}

export async function listarPedidosRecientes(): Promise<PedidoResponse[]> {
  const { data } = await api.get('/pedidos')
  return (Array.isArray(data) ? data : (data.content ?? [])).slice(0, 8)
}
