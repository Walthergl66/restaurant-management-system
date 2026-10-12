'use client'
import { useQuery } from '@tanstack/react-query'
import { obtenerReporteVentas, obtenerResumenFinanzas } from './api'
import type { ReporteVentas, ResumenFinanzas, RangoFechas } from './types'

export function useReporteVentas(rango: RangoFechas) {
  return useQuery<ReporteVentas>({
    queryKey: ['reportes', 'ventas', rango.desde, rango.hasta],
    queryFn: () => obtenerReporteVentas(rango),
  })
}

export function useResumenFinanzas(rango: RangoFechas) {
  return useQuery<ResumenFinanzas>({
    queryKey: ['finanzas', rango.desde, rango.hasta],
    queryFn: () => obtenerResumenFinanzas(rango),
  })
}
