'use client'
import { useQuery } from '@tanstack/react-query'
import type { ReporteVentas } from '@/features/reportes'
import type { PedidoResponse } from '@/features/pedidos'
import type { CajaResponse } from '@/features/caja'
import {
  obtenerReporteHoy,
  obtenerReporteAyer,
  obtenerReporteSemana,
  listarPedidosRecientes,
  obtenerCajaAbierta,
} from './api'

export function useReporte() {
  return useQuery<ReporteVentas>({
    queryKey: ['reporte', 'hoy'],
    queryFn: obtenerReporteHoy,
    refetchInterval: 60_000,
  })
}

export function useReporteAyer() {
  return useQuery<ReporteVentas>({
    queryKey: ['reporte', 'ayer'],
    queryFn: obtenerReporteAyer,
    refetchInterval: 60_000,
  })
}

export function useReporteSemana() {
  return useQuery<ReporteVentas>({
    queryKey: ['reporte', 'semana'],
    queryFn: obtenerReporteSemana,
    refetchInterval: 60_000,
  })
}

export function usePedidos() {
  return useQuery<PedidoResponse[]>({
    queryKey: ['pedidos', 'recientes'],
    queryFn: listarPedidosRecientes,
    refetchInterval: 15_000,
  })
}

export function useCaja() {
  return useQuery<CajaResponse | null>({
    queryKey: ['caja', 'abierta'],
    queryFn: obtenerCajaAbierta,
    refetchInterval: 60_000,
  })
}
