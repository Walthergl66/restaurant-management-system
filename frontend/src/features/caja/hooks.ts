'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import {
  obtenerCajaAbierta,
  listarCajas,
  listarMovimientos,
  abrirCaja,
  cerrarCaja,
  registrarEgreso,
} from './api'
import type { CajaResponse, MovimientoResponse } from './types'

export function useCajaAbierta() {
  return useQuery<CajaResponse | null>({
    queryKey: ['caja', 'abierta'],
    queryFn: obtenerCajaAbierta,
    refetchInterval: 30_000,
  })
}

export function useHistorialCajas() {
  return useQuery<CajaResponse[]>({
    queryKey: ['cajas'],
    queryFn: listarCajas,
  })
}

export function useMovimientos(cajaId?: number) {
  return useQuery<MovimientoResponse[]>({
    queryKey: ['caja', 'movimientos', cajaId],
    queryFn: () => listarMovimientos(cajaId as number),
    enabled: !!cajaId,
  })
}

export function useAbrirCaja(opts: { onAbierto?: () => void } = {}) {
  return useMutation({
    mutationFn: abrirCaja,
    onSuccess: () => {
      toast.success('Caja abierta')
      queryClient.invalidateQueries({ queryKey: ['caja'] })
      queryClient.invalidateQueries({ queryKey: ['cajas'] })
      opts.onAbierto?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useCerrarCaja(opts: { onCerrado?: () => void } = {}) {
  return useMutation({
    mutationFn: ({ id, montoReal }: { id: number; montoReal: number }) => cerrarCaja(id, montoReal),
    onSuccess: () => {
      toast.success('Caja cerrada')
      queryClient.invalidateQueries({ queryKey: ['caja'] })
      queryClient.invalidateQueries({ queryKey: ['cajas'] })
      opts.onCerrado?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useEgreso(opts: { onRegistrado?: () => void } = {}) {
  return useMutation<
    void,
    unknown,
    { cajaId: number; payload: { concepto: string; monto: number; metodo: string } }
  >({
    mutationFn: ({ cajaId, payload }) => registrarEgreso(cajaId, payload),
    onSuccess: (_data, variables) => {
      toast.success('Egreso registrado')
      queryClient.invalidateQueries({ queryKey: ['caja', 'movimientos', variables.cajaId] })
      opts.onRegistrado?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}
