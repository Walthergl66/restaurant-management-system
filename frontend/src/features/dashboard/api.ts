'use client'
import api from '@/shared/api/client'
import type { ReporteVentas } from '@/features/reportes'
import type { PedidoResponse } from '@/features/pedidos'
import { obtenerCajaAbierta } from '@/features/caja'

export { obtenerCajaAbierta }

function fechaLocal(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function reporteEnRango(desde: string, hasta: string): Promise<ReporteVentas> {
  const { data } = await api.get('/reportes/ventas', {
    params: {
      desde: new Date(`${desde}T00:00:00`).toISOString(),
      hasta: new Date(`${hasta}T23:59:59`).toISOString(),
    },
  })
  return data
}

export async function obtenerReporteHoy(): Promise<ReporteVentas> {
  const hoy = fechaLocal(new Date())
  return reporteEnRango(hoy, hoy)
}

export async function obtenerReporteAyer(): Promise<ReporteVentas> {
  const ayer = new Date()
  ayer.setDate(ayer.getDate() - 1)
  const dia = fechaLocal(ayer)
  return reporteEnRango(dia, dia)
}

export async function obtenerReporteSemana(): Promise<ReporteVentas> {
  const hasta = new Date()
  const desde = new Date()
  desde.setDate(desde.getDate() - 6)
  return reporteEnRango(fechaLocal(desde), fechaLocal(hasta))
}

export async function listarPedidosRecientes(): Promise<PedidoResponse[]> {
  const { data } = await api.get('/pedidos')
  return (Array.isArray(data) ? data : (data.content ?? [])).slice(0, 8)
}
