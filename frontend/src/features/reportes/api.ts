'use client'
import api from '@/shared/api/client'
import type { ReporteVentas, ResumenFinanzas, RangoFechas } from './types'

function rangoIso({ desde, hasta }: RangoFechas) {
  return {
    desde: new Date(`${desde}T00:00:00`).toISOString(),
    hasta: new Date(`${hasta}T23:59:59`).toISOString(),
  }
}

export async function obtenerReporteVentas(rango: RangoFechas): Promise<ReporteVentas> {
  const { data } = await api.get('/reportes/ventas', { params: rangoIso(rango) })
  return data
}

export async function obtenerResumenFinanzas(rango: RangoFechas): Promise<ResumenFinanzas> {
  const { data } = await api.get('/finanzas/resumen', { params: rangoIso(rango) })
  return data
}
